package net.highwayfrogs.editor.games.sony.medievil.map;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.text.DecimalFormat;

/**
 * Represents MediEvil map collprim data.
 * IMPORTANT NOTE: It seems like MediEvil is NOT using the MR_COLLPRIM system after all.
 * Created by Kneesnap on 3/15/2024.
 */
@Getter
public class MediEvilMapCollprim extends SCGameData<MediEvilGameInstance> {
    private final MediEvilMapFile mapFile;
    private final PSXMatrix matrix = new PSXMatrix();
    private int flags = MediEvilMapCollprimType.NORMAL.getBitMask();
    private int xLength;
    private int yLength;
    private int zLength;
    private long radiusSq;

    public static final int WARP_FROM_ZONE_MASK = 0x007F;
    public static final int WARP_TO_ZONE_MASK = 0x3F80;
    public static final int WARP_TO_SHIFT = 7;

    public static final int FLAG_CAMERA_LOCK = Constants.BIT_FLAG_12;
    public static final int FLAG_CAMERA_PLUGIN = Constants.BIT_FLAG_11;
    public static final int CAMERA_SPLINE_ID_MASK = 0x00FF;
    public static final int CAMERA_PLUGIN_ID_MASK = 0x00FF;

    public static final int NORMAL_FLAG_HAS_COLLISION = Constants.BIT_FLAG_12;
    public static final int NORMAL_FLAG_IGNORE_ENTITY = Constants.BIT_FLAG_11;
    public static final int NORMAL_FLAG_IGNORE_PLAYER = Constants.BIT_FLAG_10;
    public static final int NORMAL_FLAG_FIRES_EVENT = Constants.BIT_FLAG_9; // Incompatible with IGNORE_PLAYER, since this fires an event for players in the zone.
    public static final int NORMAL_FLAG_IGNORE_CAMERA = Constants.BIT_FLAG_8;
    public static final int NORMAL_EVENT_ID_MASK = 0x00FF;
    public static final int NORMAL_MATERIAL_ID_MASK = 0x0007;

    private static final DecimalFormat RADIUS_FORMAT = new DecimalFormat("#.#####");

    public MediEvilMapCollprim(MediEvilMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.flags = reader.readUnsignedShortAsInt();
        this.xLength = reader.readUnsignedShortAsInt();
        this.yLength = reader.readUnsignedShortAsInt();
        this.zLength = reader.readUnsignedShortAsInt();
        this.radiusSq = reader.readUnsignedIntAsLong(); // 8 bits are used because multiplying two fixed point numbers together increases the position of the radius.
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedShort(this.xLength);
        writer.writeUnsignedShort(this.yLength);
        writer.writeUnsignedShort(this.zLength);
        writer.writeUnsignedInt(this.radiusSq);
    }

    /**
     * Creates an editor for the data under the given UI grid.
     * @param manager The UI manager to create the collprim editor for.
     */
    public <TManager extends BasicListMeshUIManager<?, MediEvilMapCollprim, Box>> void setupEditor(TManager manager, Box box) {
        GUIEditorGrid grid = manager.getEditorGrid();

        // Box Center Position
        grid.addMeshMatrix(this.matrix, manager.getController(), () -> {
            updateBoxPosition(box);
            updateBoxRotation(box);
        }, true);

        Label radiusLabel = grid.addLabel("Radius (Normal, Sq)", getRadiusLabelText());

        // Lengths
        grid.addFloatField("xLength", getFloatXLength(), newX -> {
            this.xLength = Utils.floatToFixedPointInt4Bit(newX);
            recalculateRadiusSquared();
            radiusLabel.setText(getRadiusLabelText());
            if (box != null)
                box.setWidth(newX * 2);
        }, null);

        grid.addFloatField("yLength", getFloatYLength(), newY -> {
            this.yLength = Utils.floatToFixedPointInt4Bit(newY);
            recalculateRadiusSquared();
            radiusLabel.setText(getRadiusLabelText());
            if (box != null)
                box.setHeight(newY * 2);
        }, null);

        grid.addFloatField("zLength", getFloatZLength(), newZ -> {
            this.zLength = Utils.floatToFixedPointInt4Bit(newZ);
            recalculateRadiusSquared();
            radiusLabel.setText(getRadiusLabelText());
            if (box != null)
                box.setDepth(newZ * 2);
        }, null);

        setupFlagsEditor(manager, grid);
    }

    private void recalculateRadiusSquared() {
        this.radiusSq = (int) (Math.sqrt((this.xLength * this.xLength) + (this.yLength * this.yLength) + (this.zLength * this.zLength)) * (1 << 4));
    }

    private String getRadiusLabelText() {
        return RADIUS_FORMAT.format(Math.sqrt(getRadiusSq()));
    }

    /**
     * Sets up the editor for collprim flags data.
     * @param grid The UI grid to create the UI under.
     */
    public <TManager extends BasicListMeshUIManager<?, MediEvilMapCollprim, Box>> void setupFlagsEditor(TManager manager, GUIEditorGrid grid) {
        MediEvilMapCollprimType collprimType = getType();

        // Allow changing collprim type.
        grid.addEnumSelector("Type", collprimType, MediEvilMapCollprimType.values(), false, newValue -> {
            // Update flags.
            this.flags &= ~MediEvilMapCollprimType.COLLPRIM_TYPE_FLAG_MASK;
            if (newValue != null)
                this.flags &= newValue.getBitMask();

            // Refresh the editor
            manager.updateEditor();
        });

        // Type-specific editors.
        if (collprimType == MediEvilMapCollprimType.NORMAL) {
            addFlagCheckbox(grid, "Has Collision", NORMAL_FLAG_HAS_COLLISION, null);
            addFlagCheckbox(grid, "Broadcasts Event", NORMAL_FLAG_FIRES_EVENT, manager::updateEditor).setDisable(testFlagMask(NORMAL_FLAG_IGNORE_PLAYER));
            addFlagCheckbox(grid, "Ignore Player", NORMAL_FLAG_IGNORE_PLAYER, manager::updateEditor).setDisable(testFlagMask(NORMAL_FLAG_FIRES_EVENT));
            addFlagCheckbox(grid, "Ignore Camera", NORMAL_FLAG_IGNORE_CAMERA, null);
            addFlagCheckbox(grid, "Ignore Entities", NORMAL_FLAG_IGNORE_ENTITY, null);

            if (testFlagMask(NORMAL_FLAG_FIRES_EVENT)) {
                grid.addIntegerField("Event ID", getNormalEventID(), this::setNormalEventID, value -> value >= 0 && value <= NORMAL_EVENT_ID_MASK);
            } else if (!testFlagMask(NORMAL_FLAG_IGNORE_PLAYER)) {
                grid.addIntegerField("Material ID", getNormalMaterialID(), this::setNormalMaterialID, value -> value >= 0 && value <= NORMAL_MATERIAL_ID_MASK);
            }
        } else if (collprimType == MediEvilMapCollprimType.WARP) {
            grid.addIntegerField("Source (Zone ID)", getWarpFromZoneID(), this::setWarpFromZoneID, value -> value >= 0 && value <= WARP_FROM_ZONE_MASK);
            grid.addIntegerField("Target (Zone ID)", getWarpZoneToID(), this::setWarpToZoneID, value -> value >= 0 && value <= (WARP_TO_ZONE_MASK >>> WARP_TO_SHIFT));
        } else if (collprimType == MediEvilMapCollprimType.CAMERA) {
            boolean cameraLock = testFlagMask(FLAG_CAMERA_LOCK);
            boolean usePlugin = testFlagMask(FLAG_CAMERA_PLUGIN);
            addFlagCheckbox(grid, "Camera Lock (Spline)", FLAG_CAMERA_LOCK, manager::updateEditor).setDisable(usePlugin);
            addFlagCheckbox(grid, "Use Plugin", FLAG_CAMERA_PLUGIN, manager::updateEditor).setDisable(cameraLock);

            if (usePlugin) {
                grid.addIntegerField("Plugin ID", getCameraPluginID(), this::setCameraPluginID, value -> value >= 0 && value <= CAMERA_PLUGIN_ID_MASK);
            } else if (cameraLock) {
                grid.addIntegerField("Spline ID", getCameraSplineID(), this::setCameraSplineID, value -> value >= 0 && value <= CAMERA_SPLINE_ID_MASK);
            }
        } else {
            grid.addIntegerField("Raw Flags Value", this.flags, newValue -> {
                this.flags = newValue;
                if (collprimType != getType())
                    manager.updateEditor(); // Flags have the capability of changing the collprim type. If that occurs, the editor should probably update.
            }, value -> value >= 0 && value <= 0xFFFF);
        }
    }

    private CheckBox addFlagCheckbox(GUIEditorGrid grid, String name, int flagMask, Runnable onChange) {
        return grid.addCheckBox(name, (this.flags & flagMask) == flagMask, newState -> {
            boolean oldState = (this.flags & flagMask) == flagMask;
            if (oldState == newState)
                return;

            if (newState) {
                this.flags |= flagMask;
            } else {
                this.flags &= ~flagMask;
            }

            if (onChange != null)
                onChange.run();
        });
    }

    /**
     * Gets the MediEvilMapCollprimType, if there is one.
     */
    public MediEvilMapCollprimType getType() {
        int maskedType = (this.flags & MediEvilMapCollprimType.COLLPRIM_TYPE_FLAG_MASK);
        for (int i = 0; i < MediEvilMapCollprimType.values().length; i++) {
            MediEvilMapCollprimType type = MediEvilMapCollprimType.values()[i];
            if (type.bitMask == maskedType)
                return type;
        }

        return null;
    }

    /**
     * Test if a flag mask is set.
     * @param flagMask The mask to test.
     * @return true iff the bit mask was set in the flags.
     */
    public boolean testFlagMask(int flagMask) {
        return (this.flags & flagMask) == flagMask;
    }

    private void requireType(MediEvilMapCollprimType type) {
        MediEvilMapCollprimType realType = getType();
        if (realType != type)
            throw new IllegalStateException("Expected the collprim to be of type " + type + ", but was actually of type " + realType + ".");
    }

    /**
     * Gets the material ID for the collprim.
     */
    public int getNormalEventID() {
        requireType(MediEvilMapCollprimType.NORMAL);
        if (!testFlagMask(NORMAL_FLAG_FIRES_EVENT))
            throw new IllegalStateException("The collprim does not have the event flag set, meaning it has no event ID.");

        return this.flags & NORMAL_EVENT_ID_MASK;
    }

    /**
     * Sets the event ID for the collprim.
     * @param newEventId The new event ID to apply.
     */
    public void setNormalEventID(int newEventId) {
        requireType(MediEvilMapCollprimType.NORMAL);
        if (!testFlagMask(NORMAL_FLAG_FIRES_EVENT))
            throw new IllegalStateException("The collprim must have the event flag set in order to set the event ID.");
        if ((newEventId & NORMAL_EVENT_ID_MASK) != newEventId)
            throw new IllegalArgumentException("Invalid event ID '" + newEventId + "' is not within the accepted range.");

        this.flags &= ~NORMAL_EVENT_ID_MASK;
        this.flags |= newEventId;
    }

    /**
     * Gets the material ID for the collprim.
     */
    public int getNormalMaterialID() {
        requireType(MediEvilMapCollprimType.NORMAL);
        if (testFlagMask(NORMAL_FLAG_IGNORE_PLAYER))
            throw new IllegalStateException("When the ignore player flag is set, there is no material ID tracked.");

        return this.flags & NORMAL_MATERIAL_ID_MASK;
    }

    /**
     * Sets the material ID for the collprim.
     * @param newMaterialId The new material ID to apply.
     */
    public void setNormalMaterialID(int newMaterialId) {
        requireType(MediEvilMapCollprimType.NORMAL);
        if (testFlagMask(NORMAL_FLAG_IGNORE_PLAYER))
            throw new IllegalStateException("When the ignore player flag is set, there is no material ID, and thus we cannot set one.");
        if ((newMaterialId & NORMAL_MATERIAL_ID_MASK) != newMaterialId)
            throw new IllegalArgumentException("Invalid material ID '" + newMaterialId + "' is not within the accepted range.");

        this.flags &= ~NORMAL_MATERIAL_ID_MASK;
        this.flags |= newMaterialId;
    }

    /**
     * Gets the ID of the zone warped from.
     */
    public int getWarpFromZoneID() {
        requireType(MediEvilMapCollprimType.WARP);
        return this.flags & WARP_FROM_ZONE_MASK;
    }

    /**
     * Sets the Zone ID warped from.
     * @param newZoneID The zone ID to apply.
     */
    public void setWarpFromZoneID(int newZoneID) {
        requireType(MediEvilMapCollprimType.WARP);
        if ((newZoneID & WARP_FROM_ZONE_MASK) != newZoneID)
            throw new IllegalArgumentException("Invalid zone ID '" + newZoneID + "' is not within the accepted range.");

        this.flags &= ~WARP_FROM_ZONE_MASK;
        this.flags |= newZoneID;
    }

    /**
     * Gets the ID of the zone warped to.
     */
    public int getWarpZoneToID() {
        requireType(MediEvilMapCollprimType.WARP);
        return (this.flags & WARP_TO_ZONE_MASK) >>> WARP_TO_SHIFT;
    }

    /**
     * Sets the Zone ID warped from.
     * @param newZoneID The zone ID to apply.
     */
    public void setWarpToZoneID(int newZoneID) {
        requireType(MediEvilMapCollprimType.WARP);
        int shiftedZoneID = newZoneID << WARP_TO_SHIFT;
        if ((shiftedZoneID & WARP_TO_ZONE_MASK) != shiftedZoneID)
            throw new IllegalArgumentException("Invalid zone ID '" + newZoneID + "' is not within the accepted range.");

        this.flags &= ~WARP_TO_ZONE_MASK;
        this.flags |= shiftedZoneID;
    }

    /**
     * Gets the ID of the camera plugin.
     */
    public int getCameraPluginID() {
        requireType(MediEvilMapCollprimType.CAMERA);
        if (!testFlagMask(FLAG_CAMERA_PLUGIN))
            throw new IllegalStateException("Cannot get the camera plugin ID, because this collprim does not have a plugin ID.");

        return (this.flags & CAMERA_PLUGIN_ID_MASK);
    }

    /**
     * Sets the plugin ID which the camera uses.
     * @param newPluginId The plugin ID to apply.
     */
    public void setCameraPluginID(int newPluginId) {
        requireType(MediEvilMapCollprimType.CAMERA);
        if (!testFlagMask(FLAG_CAMERA_PLUGIN))
            throw new IllegalStateException("Cannot set camera plugin ID, because the flag indicating the collprim has a plugin is not set.");

        if ((newPluginId & CAMERA_PLUGIN_ID_MASK) != newPluginId)
            throw new IllegalArgumentException("Invalid plugin ID " + newPluginId + " is not within the accepted range.");

        this.flags &= ~CAMERA_PLUGIN_ID_MASK;
        this.flags |= newPluginId;
    }

    /**
     * Gets the ID of the camera spline.
     */
    public int getCameraSplineID() {
        requireType(MediEvilMapCollprimType.CAMERA);
        if (!testFlagMask(FLAG_CAMERA_LOCK))
            throw new IllegalStateException("Cannot get the camera spline ID, because this collprim does not have a spline ID.");

        return (this.flags & CAMERA_SPLINE_ID_MASK);
    }

    /**
     * Sets the Spline ID the camera follows.
     * @param newSplineId The zone ID to apply.
     */
    public void setCameraSplineID(int newSplineId) {
        requireType(MediEvilMapCollprimType.CAMERA);
        if (!testFlagMask(FLAG_CAMERA_LOCK))
            throw new IllegalStateException("Cannot set camera spline ID, because the flag indicating the collprim has a spline is not set.");
        if ((newSplineId & CAMERA_SPLINE_ID_MASK) != newSplineId)
            throw new IllegalArgumentException("Invalid spline ID " + newSplineId + " is not within the accepted range.");

        this.flags &= ~CAMERA_SPLINE_ID_MASK;
        this.flags |= newSplineId;
    }

    /**
     * Gets the x length of this collprim in floating point form.
     */
    public float getFloatXLength() {
        return Utils.fixedPointIntToFloat4Bit(this.xLength);
    }

    /**
     * Gets the y length of this collprim in floating point form.
     */
    public float getFloatYLength() {
        return Utils.fixedPointIntToFloat4Bit(this.yLength);
    }

    /**
     * Gets the z length of this collprim in floating point form.
     */
    public float getFloatZLength() {
        return Utils.fixedPointIntToFloat4Bit(this.zLength);
    }

    /**
     * Gets the radius squared from the collprim in floating point form.
     */
    public double getFloatRadiusSq() {
        return Utils.fixedPointIntToFloatNBits(this.radiusSq, 8);
    }

    /**
     * Adds a 3D representation of this collprim to the 3D view.
     * @param manager The manager of 3D space.
     * @param displayList The display list to add the 3D representation to.
     * @param material The material to apply.
     * @return the 3d representation
     */
    public <TManager extends BasicListMeshUIManager<?, MediEvilMapCollprim, Box>> Box addDisplay(TManager manager, DisplayList displayList, PhongMaterial material) {
        float x = Utils.fixedPointIntToFloat4Bit(this.matrix.getTransform()[0]);
        float y = Utils.fixedPointIntToFloat4Bit(this.matrix.getTransform()[1]);
        float z = Utils.fixedPointIntToFloat4Bit(this.matrix.getTransform()[2]);

        Box box = displayList.addBoundingBoxCenteredWithDimensions(x, y, z, getFloatXLength() * 2, getFloatYLength() * 2, getFloatZLength() * 2, material, true);
        updateBoxRotation(box);
        box.setOnMouseClicked(evt -> manager.getValueSelectionBox().getSelectionModel().select(this));
        box.setMouseTransparent(false);
        return box;
    }

    private void updateBoxPosition(Box box) {
        if (box == null)
            return;

        Translate position = Scene3DUtils.get3DTranslation(box, true);
        position.setX(Utils.fixedPointIntToFloat4Bit(this.matrix.getTransform()[0]));
        position.setY(Utils.fixedPointIntToFloat4Bit(this.matrix.getTransform()[1]));
        position.setZ(Utils.fixedPointIntToFloat4Bit(this.matrix.getTransform()[2]));
    }

    private void updateBoxRotation(Box box) {
        if (box == null)
            return;

        int foundRotations = 0;
        for (Transform transform : box.getTransforms()) {
            if (!(transform instanceof Rotate))
                continue;

            Rotate rotate = (Rotate) transform;
            if (rotate.getAxis() == Rotate.X_AXIS) {
                foundRotations++;
                rotate.setAngle(Math.toDegrees(this.matrix.getRollAngle()));
            } else if (rotate.getAxis() == Rotate.Y_AXIS) {
                foundRotations++;
                rotate.setAngle(Math.toDegrees(-this.matrix.getPitchAngle()));
            } else if (rotate.getAxis() == Rotate.Z_AXIS) {
                foundRotations++;
                rotate.setAngle(Math.toDegrees(this.matrix.getYawAngle()));
            }
        }

        if (foundRotations == 0) { // There are no rotations, so add rotations.
            box.getTransforms().add(new Rotate(Math.toDegrees(this.matrix.getYawAngle()), Rotate.Z_AXIS));
            box.getTransforms().add(new Rotate(Math.toDegrees(-this.matrix.getPitchAngle()), Rotate.Y_AXIS));
            box.getTransforms().add(new Rotate(Math.toDegrees(this.matrix.getRollAngle()), Rotate.X_AXIS));
        }
    }

    /**
     * A registry of different collision primitive types.
     */
    @Getter
    @AllArgsConstructor
    public enum MediEvilMapCollprimType {
        ALL(-1),
        NONE(0),
        CAMERA(0x8000),
        WARP(0x4000),
        NORMAL(0xC000); // Seems to allow collision and/or events getting fired.
        private static final int COLLPRIM_TYPE_FLAG_MASK = 0xC000;

        private final int bitMask;
    }
}