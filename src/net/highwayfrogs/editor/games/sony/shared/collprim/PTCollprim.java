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
import net.highwayfrogs.editor.games.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Seems to be a new / slightly changed collprim.
 * Created by Kneesnap on 5/15/2024.
 */
@Getter
@Setter
public abstract class PTCollprim extends SCSharedGameData implements ICollprim {
    private CollprimType type = CollprimType.CUBOID;
    private short flags;
    private float xLength = 3F;
    private float yLength = 3F;
    private float zLength = 3F;
    private float radiusSquared = 10F; // For cylinder base or sphere. It seems like we can safely ignore this value, leaving it as is.
    private final SVector offset = new SVector();

    public static final int SIZE_IN_BYTES = 28;

    // Observed flags:
    public static final int FLAG_BOUNDING_ENABLED = Constants.BIT_FLAG_0;
    public static final int FLAG_COLLISION_ENABLED = Constants.BIT_FLAG_1;

    public PTCollprim(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.type = CollprimType.values()[reader.readInt()];
        this.flags = reader.readShort();
        this.xLength = DataUtils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.yLength = DataUtils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.zLength = DataUtils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.radiusSquared = DataUtils.fixedPointIntToFloatNBits(reader.readInt(), 8); // 8 bits are used because multiplying two fixed point numbers together increases the position of the radius.
        this.offset.loadWithPadding(reader);
        setRawMatrixValue(reader, reader.readInt());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.type != null ? this.type.ordinal() : CollprimType.CUBOID.ordinal());
        writer.writeShort(updateFlags());
        writer.writeUnsignedShort(DataUtils.floatToFixedPointInt4Bit(this.xLength));
        writer.writeUnsignedShort(DataUtils.floatToFixedPointInt4Bit(this.yLength));
        writer.writeUnsignedShort(DataUtils.floatToFixedPointInt4Bit(this.zLength));
        writer.writeInt(DataUtils.floatToFixedPointInt(this.radiusSquared, 8));
        this.offset.saveWithPadding(writer);
        writeRawMatrixValue(writer);
    }

    /**
     * Updates the flags.
     * @return updatedFlags
     */
    public abstract short updateFlags();

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
     * Writes the raw matrix value.
     * @param writer The writer to write the value to.
     */
    protected abstract void writeRawMatrixValue(DataWriter writer);

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
     * @param manager The mesh UI manager managing the collprim.
     * @param adapter The adapter shape to setup for.
     * @param grid    The grid to setup under.
     */
    protected abstract <TManager extends MeshUIManager<?> & ICollprimEditorUI<? super ICollprim>> void setupMatrixCreator(TManager manager, CollprimShapeAdapter<?> adapter, GUIEditorGrid grid);

    @Override
    public String toString() {
        return "<PTCollprim[" + this.type + "] Flags=" + this.flags + " Offset=[" + this.offset.toFloatString()
                + "] Len=[" + this.xLength + "," + this.yLength + "," + this.zLength + "] RadiusSq=" + this.radiusSquared
                + " Has Matrix: " + hasMatrix() + ">";
    }

    /**
     * Applies the radius to the length.
     */
    public void applyRadiusToLength() {
        if (this.type == CollprimType.SPHERE || this.type == CollprimType.CYLINDER)
            this.xLength = (float) Math.sqrt(this.radiusSquared);
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
    public <TController extends MeshUIManager<?> & ICollprimEditorUI<? super ICollprim>> void setupEditor(TController editor, CollprimShapeAdapter<?> adapter) {
        GUIEditorGrid grid = editor.getCollprimEditorGrid();
        grid.clearEditor();

        // Primitive Type
        grid.addEnumSelector("Type", this.type, CollprimType.values(), false, newType -> {
            CollprimType oldType = this.type;
            this.type = newType;
            editor.onCollprimChangeType(this, adapter, oldType, newType); // Update the 3D shape display and UI.
        });

        // Flags
        grid.addSeparator();
        Label flagLabel = grid.addLabel("Flags", NumberUtils.toHexString(this.flags));
        grid.addCheckBox("Enable Bounding", testFlag(FLAG_BOUNDING_ENABLED), newValue -> {
            setFlag(FLAG_BOUNDING_ENABLED, newValue);
            flagLabel.setText(NumberUtils.toHexString(updateFlags()));
        });
        grid.addCheckBox("Enable Collision", testFlag(FLAG_COLLISION_ENABLED), newValue -> {
            setFlag(FLAG_COLLISION_ENABLED, newValue);
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

        // Add a button to remove the collprim if possible.
        grid.addButton("Remove Collprim", () -> {
            editor.getController().getMarkerManager().updateMarker(null, 4, null, null); // Hide the active position display.
            grid.clearEditor();
            editor.onCollprimRemove(this, adapter);
        });

        // Set which fields can be edited, dependent on the type.
        boolean isCuboid = (this.type == CollprimType.CUBOID);
        boolean isCylinderY = (this.type == CollprimType.CYLINDER);
        radiusSquaredField.setDisable(isCuboid);
        xLengthField.setDisable(!isCuboid);
        yLengthField.setDisable(!isCylinderY && !isCuboid);
        zLengthField.setDisable(!isCuboid);
    }

    /**
     * Add this collprim to the 3D display.
     * @param manager     The manager to update the display in.
     * @param displayList The list of collprims to add to.
     * @param material    The collprim material.
     * @return display
     */
    public <TManager extends MeshUIManager<?> & ICollprimEditorUI<? super ICollprim>> CollprimShapeAdapter<?> addDisplay(TManager manager, DisplayList displayList, PhongMaterial material) {
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
            case CYLINDER:
                Cylinder cylinder = displayList.addCylinder(x, y, z, 1, 2, material, true);
                adapter = new CylinderCollprimShapeAdapter(this, cylinder);
                break;
            default:
                throw new RuntimeException("Unsupported Collprim type " + this.type + ", cannot create display for " + this + ".");
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
        CYLINDER,
        SPHERE,
    }
}