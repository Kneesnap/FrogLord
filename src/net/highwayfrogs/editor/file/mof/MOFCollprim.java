package net.highwayfrogs.editor.file.mof;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Shape3D;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.editor.RenderManager;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents MR_COLLPRIM
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFCollprim extends GameObject {
    private int flags;
    private SVector offset;
    private int radius2; // For cylinder base or sphere. Squared.
    private float xLength;
    private float yLength;
    private float zLength;
    private CollprimReactionType reaction;
    private PSXMatrix matrix; // Only present in JUN_PLANT.

    public static final int FLAG_STATIC = Constants.BIT_FLAG_0;
    public static final int FLAG_LAST_IN_LIST = Constants.BIT_FLAG_1;
    public static final int FLAG_COLLISION_DISABLED = Constants.BIT_FLAG_8;

    @Override
    public void load(DataReader reader) {
        CollprimType type = CollprimType.values()[reader.readUnsignedShortAsInt()];
        if (type != CollprimType.CUBOID)
            throw new RuntimeException("MOFCollprim was type " + type + ", which is not supported.");

        this.flags = reader.readUnsignedShortAsInt();
        reader.skipInt(); // Run-time.
        reader.skipInt(); // Run-time.
        this.offset = SVector.readWithPadding(reader);
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
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(CollprimType.CUBOID.ordinal());
        writer.writeUnsignedShort(this.flags);
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
     * Add this collprim to the 3D display.
     * @param manager  The render manager to add the display to.
     * @param listID   The list of collprims to add to.
     * @param material The collprim material.
     * @return display
     */
    public Shape3D addDisplay(RenderManager manager, String listID, PhongMaterial material) {
        Box box = manager.addBoundingBoxCenteredWithDimensions(listID, getOffset().getFloatX(), getOffset().getFloatY(), getOffset().getFloatZ(), this.xLength, this.yLength, this.zLength, material, true);

        box.setMouseTransparent(false);
        box.setOnMouseClicked(evt -> {
            System.out.println("Collprim Info:");
            System.out.println(" - Pos: " + getXLength() + ", " + getYLength() + ", " + getZLength());
            System.out.println(" - Flags: " + getFlags());
            System.out.println(" - Matrix: " + (getMatrix() != null ? getMatrix() : "None"));
            System.out.println(" - Radius2: " + getRadius2());
            System.out.println(" - Reaction: " + getReaction());
        });

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
        SAFE,
        DEADLY,
        BOUNCY,
        FORM, // Form callback.
    }
}
