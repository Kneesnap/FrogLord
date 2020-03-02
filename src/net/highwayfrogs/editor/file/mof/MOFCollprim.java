package net.highwayfrogs.editor.file.mof;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Shape3D;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MOFController;
import net.highwayfrogs.editor.gui.editor.RenderManager;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents MR_COLLPRIM.
 * The way this works is, Frogger has a collision hilite.
 * When testing collision, it will check if any of Frogger's collision hilites are inside the collprim, or bbox. If a collprim exists, it will check the collprim, otherwise it will check the bbox.
 * If the collprim is the last one on the model (the vanilla game only ever has 1 collprim per model, though it's not necessarily that way always), use the last in list flag.
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
@Setter
public class MOFCollprim extends GameObject {
    private int flags; // Seems to always be two.
    private SVector offset = new SVector();
    private int radius2; // For cylinder base or sphere. It seems like we can safely ignore this value, leaving it as is.
    private float xLength;
    private float yLength;
    private float zLength;
    private CollprimReactionType reaction = CollprimReactionType.DEADLY;
    private PSXMatrix matrix; // Only present in JUN_PLANT.
    private transient MOFPart parent;

    public static final int FLAG_STATIC = Constants.BIT_FLAG_0; // Unused. I don't know either.
    public static final int FLAG_LAST_IN_LIST = Constants.BIT_FLAG_1;
    public static final int FLAG_COLLISION_DISABLED = Constants.BIT_FLAG_8; // Completely unused. Seems like maybe this was used to disable something without deleting it, for testing purposes.

    public MOFCollprim(MOFPart parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        CollprimType type = CollprimType.values()[reader.readUnsignedShortAsInt()];
        if (type != CollprimType.CUBOID)
            throw new RuntimeException("MOFCollprim was type " + type + ", which is not supported.");

        this.flags = reader.readUnsignedShortAsInt();
        reader.skipInt(); // Run-time.
        reader.skipInt(); // Run-time.
        this.offset.loadWithPadding(reader);
        this.radius2 = reader.readInt();
        this.xLength = Utils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.yLength = Utils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.zLength = Utils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4);
        this.reaction = CollprimReactionType.values()[reader.readUnsignedShortAsInt()];

        int matrixAddress = reader.readInt();
        if (matrixAddress != -1) {
            reader.skipBytes(matrixAddress);
            this.matrix = new PSXMatrix();
            this.matrix.load(reader);
        }

        Utils.verify((this.flags & FLAG_LAST_IN_LIST) == FLAG_LAST_IN_LIST, "There were multiple collprims specified, but FrogLord only supports one!");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(CollprimType.CUBOID.ordinal());
        writer.writeUnsignedShort(this.flags | FLAG_LAST_IN_LIST);
        writer.writeInt(0); // Run-time.
        writer.writeInt(0); // Run-time.
        this.offset.saveWithPadding(writer);
        writer.writeInt(this.radius2);
        writer.writeUnsignedShort(Utils.floatToFixedPointInt4Bit(this.xLength));
        writer.writeUnsignedShort(Utils.floatToFixedPointInt4Bit(this.yLength));
        writer.writeUnsignedShort(Utils.floatToFixedPointInt4Bit(this.zLength));
        writer.writeUnsignedShort(this.reaction.ordinal());

        boolean hasMatrix = (this.matrix != null);
        writer.writeInt(hasMatrix ? 0 : -1);
        if (hasMatrix)
            this.matrix.save(writer);
    }

    @Override
    public String toString() {
        return "<MOFCollprim Flags=" + this.flags + " Offset=[" + this.offset.toFloatString()
                + "] Len=[" + this.xLength + "," + this.yLength + "," + this.zLength + "] Radius2=" + this.radius2 + ">";
    }

    /**
     * Setup the editor to display collprim information.
     * @param controller The controller controlling the model.
     */
    public void setupEditor(MOFController controller, Box box) {
        GUIEditorGrid grid = controller.getUiController().getCollprimEditorGrid();
        grid.clearEditor();

        grid.addFloatVector("Position", getOffset(), () -> controller.updateRotation(box), controller, getOffset().defaultBits(), null, box);
        grid.addFloatField("xLength", getXLength(), newVal -> {
            this.xLength = newVal;
            box.setWidth(this.xLength * 2);
        }, null);

        grid.addFloatField("yLength", getYLength(), newVal -> {
            this.yLength = newVal;
            box.setHeight(this.yLength * 2);
        }, null);

        grid.addFloatField("zLength", getZLength(), newVal -> {
            this.zLength = newVal;
            box.setDepth(this.zLength * 2);
        }, null);

        grid.addEnumSelector("Reaction", getReaction(), CollprimReactionType.values(), false, newReaction -> this.reaction = newReaction);

        grid.addButton("Remove Collprim", () -> {
            getParent().setCollprim(null);
            grid.clearEditor();
            controller.updateMarker(null, 4, null, null); // Hide the active position display.
            controller.updateCollprimBoxes();
        });

        controller.getUiController().getCollprimPane().setExpanded(true);
    }

    /**
     * Add this collprim to the 3D display.
     * @param manager  The render manager to add the display to.
     * @param listID   The list of collprims to add to.
     * @param material The collprim material.
     * @return display
     */
    public Shape3D addDisplay(MOFController controller, RenderManager manager, String listID, PhongMaterial material) {
        Box box = manager.addBoundingBoxCenteredWithDimensions(listID, getOffset().getFloatX(), getOffset().getFloatY(), getOffset().getFloatZ(), this.xLength * 2, this.yLength * 2, this.zLength * 2, material, true);
        box.setOnMouseClicked(evt -> setupEditor(controller, box));
        box.setMouseTransparent(false);
        return box;
    }

    public enum CollprimType {
        CUBOID,
        CYLINDER_X,
        CYLINDER_Y,
        CYLINDER_Z,
        SPHERE,
    }

    public enum CollprimReactionType {
        SAFE, // Unused
        DEADLY, // Kills Frogger
        BOUNCY, // Unused.
        FORM, // Form callback.
    }
}
