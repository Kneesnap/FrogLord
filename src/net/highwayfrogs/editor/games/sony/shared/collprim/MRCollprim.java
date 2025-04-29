package net.highwayfrogs.editor.games.sony.shared.collprim;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.MOFController;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.RenderManager;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.function.Consumer;

/**
 * Represents a Collision Primitive / MR_COLLPRIM.
 * Created by Kneesnap on 9/16/2023.
 */
@Getter
@Setter
public abstract class MRCollprim extends SCSharedGameData implements ICollprim {
    private CollprimType type = CollprimType.CUBOID;
    private int flags;
    private final SVector offset = new SVector();
    private float radiusSquared = 10F; // For cylinder base or sphere. It seems like we can safely ignore this value, leaving it as is.
    private float xLength = 3F;
    private float yLength = 3F;
    private float zLength = 3F;
    private int userData;

    public static final int SIZE_IN_BYTES = 36;

    // Observed flags:
    public static final int FLAG_LAST_IN_LIST = Constants.BIT_FLAG_1; // Indicates the collprim is the last one in the memory.
    public static final int FLAG_COLLISION_DISABLED = Constants.BIT_FLAG_8; // Unused in Frogger, but seen in Beast Wars PC (Specifically Map files).

    public MRCollprim(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.type = CollprimType.values()[reader.readUnsignedShortAsInt()];
        this.flags = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(2 * Constants.POINTER_SIZE); // Run-time pointers.
        this.offset.loadWithPadding(reader);
        this.radiusSquared = DataUtils.fixedPointIntToFloatNBits(reader.readInt(), 8); // 8 bits are used because multiplying two fixed point numbers together increases the position of the radius.
        this.xLength = DataUtils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.yLength = DataUtils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.zLength = DataUtils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.userData = reader.readUnsignedShortAsInt();
        setRawMatrixValue(reader, reader.readInt());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.type.ordinal());
        writer.writeUnsignedShort(updateFlags());
        writer.writeInt(0); // Run-time.
        writer.writeInt(0); // Run-time.
        this.offset.saveWithPadding(writer);
        writer.writeInt(DataUtils.floatToFixedPointInt(this.radiusSquared, 8));
        writer.writeUnsignedShort(DataUtils.floatToFixedPointInt4Bit(this.xLength));
        writer.writeUnsignedShort(DataUtils.floatToFixedPointInt4Bit(this.yLength));
        writer.writeUnsignedShort(DataUtils.floatToFixedPointInt4Bit(this.zLength));
        writer.writeUnsignedShort(this.userData);
        writer.writeInt(getRawMatrixValue());
    }

    /**
     * Updates the flags.
     * @return updatedFlags
     */
    public abstract int updateFlags();

    /**
     * Gets the raw matrix value.
     * @return rawMatrixValue
     */
    public abstract int getRawMatrixValue();

    /**
     * Sets the raw matrix value.
     * @param reader         The data reader which the collprim is read from. Different extensions may or may not support this being null.
     * @param rawMatrixValue The new raw matrix value to apply.
     */
    public abstract void setRawMatrixValue(DataReader reader, int rawMatrixValue);

    /**
     * Gets the matrix this collprim uses.
     */
    public abstract PSXMatrix getMatrix();

    /**
     * Test if the collprim has the matrix.
     */
    public boolean hasMatrix() {
        return getMatrix() != null;
    }

    /**
     * Removes the active PSXMatrix from the collprim.
     */
    public abstract void removeMatrix();

    /**
     * Sets up the UI to allow for choosing or creating a matrix.
     * @param grid The grid to setup for.
     */
    protected abstract void setupMatrixCreator(MOFController controller, CollprimShapeAdapter<?> adapter, GUIEditorGrid grid);

    /**
     * Sets up the UI to allow for choosing or creating a matrix.
     * @param manager The mesh UI manager to setup this for.
     * @param adapter The adapter shape to setup for.
     * @param grid    The grid to setup under.
     */
    protected abstract <TManager extends MeshUIManager<?> & ICollprimEditorUI> void setupMatrixCreator(TManager manager, CollprimShapeAdapter<?> adapter, GUIEditorGrid grid);

    @Override
    public String toString() {
        return "<MRCollprim[" + this.type + "] Flags=" + this.flags + " Offset=[" + this.offset.toFloatString()
                + "] Len=[" + this.xLength + "," + this.yLength + "," + this.zLength + "] RadiusSq=" + this.radiusSquared
                + " Data: " + this.userData + " Has Matrix: " + hasMatrix() + ">";
    }

    /**
     * Test bitwise collprim flags.
     * @param flagMask The mask of flags to test.
     * @return If all the bits in the flag mask are set.
     */
    public boolean testFlag(int flagMask) {
        return (this.flags & flagMask) == flagMask;
    }

    /**
     * Set one or more bitwise flags to a certain state.
     * @param flagMask The mask of flags to set.
     * @param newState Whether to set them to 0 or 1.
     * @return If the flags were set before.
     */
    public boolean setFlag(int flagMask, boolean newState) {
        boolean wereFlagsSet = (this.flags & flagMask) == flagMask;
        this.flags &= ~flagMask;
        if (newState)
            this.flags |= flagMask;

        return wereFlagsSet;
    }

    /**
     * Setup the editor to display collprim information.
     * @param editor  The editor to build the UI in.
     * @param adapter The shape adapter which editing should update.
     */
    public <TController extends MeshUIManager<?> & ICollprimEditorUI> void setupEditor(TController editor, CollprimShapeAdapter<?> adapter) {
        GUIEditorGrid grid = editor.getCollprimEditorGrid();
        grid.clearEditor();

        // Primitive Type
        grid.addEnumSelector("Type", this.type, CollprimType.values(), false, newType -> {
            CollprimType oldType = this.type;
            this.type = newType;
            editor.onCollprimChangeType(this, adapter, oldType, newType); // Update the 3D shape display and UI.
        });

        if (this.type == CollprimType.CYLINDER_X || this.type == CollprimType.CYLINDER_Z)
            grid.addNormalLabel("FrogLord is not currently capable of rotating cylinders properly.");

        // Flags
        grid.addSeparator();
        Label flagLabel = grid.addLabel("Flags", NumberUtils.toHexString(this.flags));
        grid.addCheckBox("Disable Collision", testFlag(FLAG_COLLISION_DISABLED), newValue -> {
            setFlag(FLAG_COLLISION_DISABLED, newValue);
            flagLabel.setText(NumberUtils.toHexString(updateFlags()));
        });

        // Shape data
        grid.addSeparator();

        // Radius
        TextField radiusSquaredField = grid.addFloatField("Radius Squared", this.radiusSquared, newRadiusSq -> {
            this.radiusSquared = newRadiusSq;
            adapter.onRadiusSquaredUpdate(newRadiusSq);
        }, newRadiusSq -> newRadiusSq >= 0 && !Float.isNaN(newRadiusSq) && Float.isFinite(newRadiusSq));

        // Position
        grid.addFloatVector("Position", getOffset(), () -> editor.updateCollprimPosition(this, adapter), editor.getController(), getOffset().defaultBits(), null, adapter.getShape(), null);

        // Lengths
        TextField xLengthField = grid.addFloatField("xLength", getXLength(), newVal -> {
            this.xLength = newVal;
            adapter.onLengthXUpdate(newVal);
        }, null);

        TextField yLengthField = grid.addFloatField("yLength", getYLength(), newVal -> {
            this.yLength = newVal;
            adapter.onLengthYUpdate(newVal);
        }, null);

        TextField zLengthField = grid.addFloatField("zLength", getZLength(), newVal -> {
            this.zLength = newVal;
            adapter.onLengthZUpdate(newVal);
        }, null);

        PSXMatrix matrix = getMatrix();
        if (matrix != null) {
            grid.addBoldLabel("Matrix:");
            grid.addMeshMatrix(matrix, editor.getController(), () -> editor.updateCollprimPosition(this, adapter));
            grid.addButton("Remove Matrix", () -> {
                removeMatrix();
                editor.updateCollprimPosition(this, adapter); // Update the model display.
                editor.updateEditor(); // Refresh UI.
            });
        } else {
            setupMatrixCreator(editor, adapter, grid);
        }

        // Edit user data.
        if (getGameInstance().isFrogger()) {
            grid.addEnumSelector("Reaction", getFroggerReaction(), FroggerCollprimReactionType.values(), false, newReaction -> this.userData = newReaction.ordinal());
        } else {
            grid.addUnsignedShortField("User Data", this.userData, newValue -> this.userData = newValue);
        }

        // Add a button to remove the collprim if possible.
        grid.addButton("Remove Collprim", () -> {
            editor.getController().getMarkerManager().updateMarker(null, 4, null, null); // Hide the active position display.
            grid.clearEditor();
            editor.onCollprimRemove(this, adapter);
        });

        // Set which fields can be edited, dependent on the type.
        boolean isCuboid = (this.type == CollprimType.CUBOID);
        boolean isCylinderX = (this.type == CollprimType.CYLINDER_X);
        boolean isCylinderY = (this.type == CollprimType.CYLINDER_Y);
        boolean isCylinderZ = (this.type == CollprimType.CYLINDER_Z);
        radiusSquaredField.setDisable(isCuboid);
        xLengthField.setDisable(!isCylinderX && !isCuboid);
        yLengthField.setDisable(!isCylinderY && !isCuboid);
        zLengthField.setDisable(!isCylinderZ && !isCuboid);
    }

    /**
     * Setup the editor to display collprim information.
     * @param controller The controller controlling the model.
     */
    public void setupEditor(MOFController controller, CollprimShapeAdapter<?> adapter, Consumer<MRCollprim> removeHandler) {
        GUIEditorGrid grid = controller.getUiController().getCollprimEditorGrid();
        grid.clearEditor();

        // Primitive Type
        grid.addEnumSelector("Type", this.type, CollprimType.values(), false, newType -> {
            this.type = newType;
            controller.updateCollprimBoxes(true, this); // Update the 3D shape display and UI.
        });

        if (this.type == CollprimType.CYLINDER_X || this.type == CollprimType.CYLINDER_Z)
            grid.addNormalLabel("FrogLord is not currently capable of rotating cylinders properly.");

        // Flags
        grid.addSeparator();
        Label flagLabel = grid.addLabel("Flags", NumberUtils.toHexString(this.flags));
        grid.addCheckBox("Disable Collision", testFlag(FLAG_COLLISION_DISABLED), newValue -> {
            setFlag(FLAG_COLLISION_DISABLED, newValue);
            flagLabel.setText(NumberUtils.toHexString(updateFlags()));
        });

        // Shape data
        grid.addSeparator();

        // Radius
        TextField radiusSquaredField = grid.addFloatField("Radius Squared", this.radiusSquared, newRadiusSq -> {
            this.radiusSquared = newRadiusSq;
            adapter.onRadiusSquaredUpdate(newRadiusSq);
        }, newRadiusSq -> newRadiusSq >= 0 && Float.isFinite(newRadiusSq));

        // Position
        grid.addFloatVector("Position", getOffset(), () -> controller.updateRotation(adapter.getShape()), controller, getOffset().defaultBits(), null, adapter.getShape());

        // Lengths
        TextField xLengthField = grid.addFloatField("xLength", getXLength(), newVal -> {
            this.xLength = newVal;
            adapter.onLengthXUpdate(newVal);
        }, null);

        TextField yLengthField = grid.addFloatField("yLength", getYLength(), newVal -> {
            this.yLength = newVal;
            adapter.onLengthYUpdate(newVal);
        }, null);

        TextField zLengthField = grid.addFloatField("zLength", getZLength(), newVal -> {
            this.zLength = newVal;
            adapter.onLengthZUpdate(newVal);
        }, null);

        PSXMatrix matrix = getMatrix();
        if (matrix != null) {
            grid.addBoldLabel("Matrix:");
            grid.addMofMatrix(matrix, controller, () -> controller.updateCollprimBoxes(true, this));
            grid.addButton("Remove Matrix", () -> {
                removeMatrix();
                controller.updateCollprimBoxes(true, this); // Update the model display and UI.
            });
        } else {
            setupMatrixCreator(controller, adapter, grid);
        }

        // Edit user data.
        if (getGameInstance().isFrogger()) {
            grid.addEnumSelector("Reaction", getFroggerReaction(), FroggerCollprimReactionType.values(), false, newReaction -> this.userData = newReaction.ordinal());
        } else {
            grid.addUnsignedShortField("User Data", this.userData, newValue -> this.userData = newValue);
        }

        // Add a button to remove the collprim if possible.
        if (removeHandler != null) {
            grid.addButton("Remove Collprim", () -> {
                removeHandler.accept(this);
                controller.updateMarker(null, 4, null, null); // Hide the active position display.
                controller.updateCollprimBoxes();
                grid.clearEditor();
            });
        }

        // Set which fields can be edited, dependent on the type.
        boolean isCuboid = (this.type == CollprimType.CUBOID);
        boolean isCylinderX = (this.type == CollprimType.CYLINDER_X);
        boolean isCylinderY = (this.type == CollprimType.CYLINDER_Y);
        boolean isCylinderZ = (this.type == CollprimType.CYLINDER_Z);
        radiusSquaredField.setDisable(isCuboid);
        xLengthField.setDisable(!isCylinderX && !isCuboid);
        yLengthField.setDisable(!isCylinderY && !isCuboid);
        zLengthField.setDisable(!isCylinderZ && !isCuboid);

        // Expand the pane.
        controller.getUiController().getCollprimPane().setExpanded(true);
    }

    /**
     * Add this collprim to the 3D display.
     * @param manager  The render manager to add the display to.
     * @param listID   The list of collprims to add to.
     * @param material The collprim material.
     * @return display
     */
    public CollprimShapeAdapter<?> addDisplay(MOFController controller, RenderManager manager, String listID, PhongMaterial material, Consumer<MRCollprim> removeHandler) {
        float x = getOffset().getFloatX();
        float y = getOffset().getFloatY();
        float z = getOffset().getFloatZ();

        CollprimShapeAdapter<?> adapter;
        switch (this.type) {
            case CUBOID:
                Box box = manager.addBoundingBoxCenteredWithDimensions(listID, x, y, z, this.xLength * 2, this.yLength * 2, this.zLength * 2, material, true);
                adapter = new CuboidCollprimShapeAdapter(this, box);
                break;
            case SPHERE:
                Sphere sphere = manager.addSphere(listID, x, y, z, 1, material, true);
                adapter = new SphereCollprimShapeAdapter(this, sphere);
                break;
            case CYLINDER_X:
            case CYLINDER_Y:
            case CYLINDER_Z:
                // CYLINDER_Z indicates that the cylinder is aligned to the Z axis, conceptualized as an infinitely large line.
                // zLength thus indicates the height.
                // TODO: Unfortunately, FrogLord java does not align/rotate CYLINDER_X and CYLINDER_Z correctly.
                //  This is because JavaFX uses XYZ angles for rotations, instead of matrices. So, when we rotate the model to orient with mouse movement, we need to do a secondary rotation, something you need matrices for.
                //  In theory we could get matrix math code working for FrogLord java, but I think this is more effort than it is worth since matrix math is actually written for new FrogLord.
                Cylinder cylinder = manager.addCylinder(listID, x, y, z, 1, 2, material, true);
                adapter = new CylinderCollprimShapeAdapter(this, cylinder);
                break;
            default:
                throw new RuntimeException("Unsupported Collprim type " + this.type + ", cannot create display for " + toString() + ".");
        }

        adapter.getShape().setOnMouseClicked(evt -> setupEditor(controller, adapter, removeHandler));
        adapter.getShape().setMouseTransparent(false);
        adapter.onLengthXUpdate(this.xLength);
        adapter.onLengthYUpdate(this.yLength);
        adapter.onLengthZUpdate(this.zLength);
        adapter.onRadiusSquaredUpdate(this.radiusSquared);

        // TODO: Unfortunately, FrogLord java does not align/rotate the matrix (if it exists).
        //  This is because JavaFX uses XYZ angles for rotations, instead of matrices. This is fine for rotation based on the mouse, but the problem happens when we need to combine rotations.
        //  We need matrix math for this, which in theory we could write, but I don't particularly want to spend the time on this since new FrogLord will solve this anyways.
        //  In theory we could get matrix math code working for FrogLord java, but I think this is more effort than it is worth since matrix math is actually written for new FrogLord.

        return adapter;
    }

    /**
     * Add this collprim to the 3D display.
     * @param manager     The manager to update the display in.
     * @param displayList The list of collprims to add to.
     * @param material    The collprim material.
     * @return display
     */
    public <TManager extends MeshUIManager<?> & ICollprimEditorUI> CollprimShapeAdapter<?> addDisplay(TManager manager, DisplayList displayList, PhongMaterial material) {
        float x = getOffset().getFloatX();
        float y = getOffset().getFloatY();
        float z = getOffset().getFloatZ();

        CollprimShapeAdapter<?> adapter;
        switch (this.type) {
            case CUBOID:
                Box box = displayList.addBoundingBoxCenteredWithDimensions(x, y, z, this.xLength * 2, this.yLength * 2, this.zLength * 2, material, true);
                adapter = new CuboidCollprimShapeAdapter(this, box);
                break;
            case SPHERE:
                Sphere sphere = displayList.addSphere(x, y, z, 1, material, true);
                adapter = new SphereCollprimShapeAdapter(this, sphere);
                break;
            case CYLINDER_X:
            case CYLINDER_Y:
            case CYLINDER_Z:
                // CYLINDER_Z indicates that the cylinder is aligned to the Z axis, conceptualized as an infinitely large line.
                // zLength thus indicates the height.
                // TODO: Unfortunately, FrogLord java does not align/rotate CYLINDER_X and CYLINDER_Z correctly.
                //  This is because JavaFX uses XYZ angles for rotations, instead of matrices. So, when we rotate the model to orient with mouse movement, we need to do a secondary rotation, something you need matrices for.
                //  In theory we could get matrix math code working for FrogLord java, but I think this is more effort than it is worth since matrix math is actually written for new FrogLord.
                Cylinder cylinder = displayList.addCylinder(x, y, z, 1, 2, material, true);
                adapter = new CylinderCollprimShapeAdapter(this, cylinder);
                break;
            default:
                throw new RuntimeException("Unsupported Collprim type " + this.type + ", cannot create display for " + toString() + ".");
        }

        adapter.getShape().setOnMouseClicked(evt -> setupEditor(manager, adapter));
        adapter.getShape().setMouseTransparent(false);
        adapter.onLengthXUpdate(this.xLength);
        adapter.onLengthYUpdate(this.yLength);
        adapter.onLengthZUpdate(this.zLength);
        adapter.onRadiusSquaredUpdate(this.radiusSquared);

        // TODO: Unfortunately, FrogLord java does not align/rotate the matrix (if it exists).
        //  This is because JavaFX uses XYZ angles for rotations, instead of matrices. This is fine for rotation based on the mouse, but the problem happens when we need to combine rotations.
        //  We need matrix math for this, which in theory we could write, but I don't particularly want to spend the time on this since new FrogLord will solve this anyways.
        //  In theory we could get matrix math code working for FrogLord java, but I think this is more effort than it is worth since matrix math is actually written for new FrogLord.

        return adapter;
    }

    public enum CollprimType {
        CUBOID,
        CYLINDER_X, // Seen in Old Frogger
        CYLINDER_Y, // Seen in Old Frogger
        CYLINDER_Z, // Seen in Old Frogger
        SPHERE,
    }

    /**
     * Gets the FroggerCollprimReactionType, if this is Frogger.
     * This is only here because frogger exclusively uses MOFCollprim, and not a special implementation.
     */
    public FroggerCollprimReactionType getFroggerReaction() {
        // Old Frogger appears to always have this value as zero, so collision is likely handled through some other configuration (perhaps in the form data).
        if (!getGameInstance().isFrogger())
            throw new RuntimeException("Cannot get FroggerCollprimReactionType when the active game is not Frogger!");

        if (this.userData < 0 || this.userData >= FroggerCollprimReactionType.values().length)
            throw new RuntimeException("The value was " + this.userData + ", which is not a recognized FroggerCollprimReactionType.");

        return FroggerCollprimReactionType.values()[this.userData];
    }

    public enum FroggerCollprimReactionType {
        SAFE, // Unused
        DEADLY, // Kills Frogger
        BOUNCY, // Unused.
        FORM, // Form callback.
    }

    // Down here contains information I found by looking at the data found in various different games.

    // Old Frogger PSX (Milestone 3):
    // Every single collision primitive type is used.
    // 9 models use matrix (SWAMP.WAD/SWP_STAT_DEADTREE.XMR, SWAMP.WAD/SWP_STAT_LOG.XMR, SWAMP.WAD/SWP_STAT_PIPE_CURVED.XMR, FOREST.WAD/FOR_STAT_DEADBRANCH.XMR, ...) Some are cylinde
    // CUBOID: RadiusSquared is an oddly large value for CUBOID models.
    // SWAMP.WAD/SWP_STAT_DEADTREE1.XMR is interesting because despite being CYLINDER_Y, it has a value for zLength. SWAMP.WAD/SWP_STAT_PIPE.XMR is another example.
    // I believe this data is just garbage data, because it doesn't seem to tangibly mean anything, however FrogLord has been setup to display this info anyways.
    // User Data appears to be zero.
    // Out of 208 collprims, 19 have Flags=0 and 189 have Flags=2. All of the ones with flag 0 come from models with multiple collprims.

    // Frogger PSX (April Sony Build):
    // Both CYLINDER_Y and CUBOID primitive types are used, but no others.
    // All models have Flags=2.
    // Two models have matrices: MAP_RUSHED.WAD/ORG_CROCODILE.XAR and MAP_RUSHED.WAD/SKY_BIRD1.XAR.
    // Some of the cuboid models have the weird radius data seen in old frogger.

    // Frogger PSX Alpha:
    // This is a very strange build in that there is ONLY ONE collprim in ALL game files:
    // THEME_CAV.WAD/CAV_STAT_COBWEB.XMR -> <MRCollprim[CUBOID] Flags=2 Offset=[-0.5625, -15.5625, 0.0] Len=[8.3125,16.3125,0.4375] RadiusSq=0.0 Data: 3 Has Matrix: false>
    // The collprims seem to be added over time after this build.

    // Frogger PSX July Build 20:
    // ALL collprims are of CUBOID type, and ALL have Flags=2.
    // 1 model has a collprim with a matrix: THEME_JUN.WAD/JUN_PLANT.XAR

    // Frogger PSX Build 71 (NTSC Retail):
    // ALL collprims are of CUBOID type, and ALL have Flags=2.
    // 1 model has a collprim with a matrix: THEME_JUN.WAD/JUN_PLANT.XAR

    // Frogger PC Release (v3.0e):
    // ALL collprims are of CUBOID type, and ALL have Flags=2.
    // 1 model has a collprim with a matrix: THEME_JUN1.WAD/JUN_PLANT.XAR
    // The weird data in radius for cuboid models are seen.

    // Beast Wars PSX (October 1997):
    // CUBOID is the most common collision type, but SPHERE is also seen.
    // The weird data in radius for cuboid models are seen in both maps and models.
    // Most files are showing a collprim with flags=0, all others are flags=2.
    // Many collprims are seen as having a matrix, and it appears the matrices can be significant now. For example, the rotation in 'RM_LEV1.WAD/PRED_CARSHIP.XMR'.

    // Beast Wars PC (March 1998):
    // CUBOID is the most common collision type, but SPHERE is also seen.
    // The weird data in radius for cuboid models are seen in both maps and models.
    // 756 files are showing a collprim with flags=0, 569 are flags=2. Some of the maps contain flags=256 (disable collision)
    // 295 collprims are seen as having a matrix, and it appears the matrices can be significant now. For example, the rotation in 'RM_LEV1.WAD/PRED_CARSHIP.XMR'.

    // Medievil ECTS Prototype (September 7 1997):
    // No collprims seem to exist in MOFs.
    // TODO: What about in maps? If I recall, I saw a COLP section in the map files, which is what Beast Wars does.

    // MediEvil NTSC Release (October 1998):
    // No collprims seem to exist in MOFs.
    // TODO: What about in maps? If I recall, I saw a COLP section in the map files, which is what Beast Wars does.
}