package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFBBox;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.List;

/**
 * Represents a MOF animation model. Struct "MR_ANIM_MODEL"
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimationModel extends GameObject {
    private MOFAnimationModelSet parent;
    private int animationType;
    private int flags;
    private int partCount;
    private int staticModelId;
    private MOFBBox boundingBox;
    // Bounding Box Set is unused.
    // Constraint is unused.

    private transient int tempCelsetPointerAddress;

    public static final int FLAG_GLOBAL_BBOXES_INCLUDED = Constants.BIT_FLAG_0;
    public static final int FLAG_PERCEL_BBOXES_INCLUDED = Constants.BIT_FLAG_1;

    public MOFAnimationModel(MOFAnimationModelSet set) {
        this.parent = set;
    }

    @Override
    public void load(DataReader reader) {
        this.animationType = reader.readUnsignedShortAsInt();
        this.flags = reader.readUnsignedShortAsInt();
        this.partCount = reader.readUnsignedShortAsInt();
        this.staticModelId = reader.readUnsignedShortAsInt();

        int celsetPointer = reader.readInt(); // Right after BBOX
        int bboxPointer = reader.readInt(); // Right after struct.
        int bboxSetPointer = reader.readInt(); // Unused.
        int constraintPointer = reader.readInt(); // Unused.

        Utils.verify(bboxSetPointer == 0, "BBOX Set Pointer was not null.");
        Utils.verify(constraintPointer == 0, "Constraint Pointer was not null.");
        Utils.verify(celsetPointer == getCelSetPointer(), "Invalid CelSet Pointer! (%d, %d)", celsetPointer, getCelSetPointer());

        reader.setIndex(bboxPointer);
        this.boundingBox = new MOFBBox();
        this.boundingBox.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.animationType);
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedShort(this.partCount);
        writer.writeUnsignedShort(this.staticModelId);

        this.tempCelsetPointerAddress = writer.getIndex();
        writer.writeInt(0); // Right after BBOX

        int calculatedBboxPointer = writer.getIndex() + (3 * Constants.POINTER_SIZE);
        writer.writeInt(calculatedBboxPointer);
        writer.writeInt(0);
        writer.writeInt(0);

        // Write BBOX
        Utils.verify(calculatedBboxPointer == writer.getIndex(), "Calculated wrong bbox pointer. (%d, %d)", calculatedBboxPointer, writer.getIndex());
        this.boundingBox.save(writer);
    }

    /**
     * Writes the cel pointer.
     * MUST BE CALLED AFTER CEL SETS ARE SAVED.
     * @param writer The DataWriter to write to.
     */
    public void writeCelPointer(DataWriter writer) {
        Utils.verify(this.tempCelsetPointerAddress > 0, "Normal save(DataWriter writer) has not been called yet.");
        writer.jumpTemp(this.tempCelsetPointerAddress);
        writer.writeInt(getCelSetPointer());
        writer.jumpReturn();
        this.tempCelsetPointerAddress = 0;
    }

    /**
     * Get the celsets used.
     * @return celSets
     */
    public List<MOFAnimationCelSet> getCelSets() {
        return getParent().getCelSets();
    }

    /**
     * Gets the pointer which gets saved and loaded for this file.
     * @return celsetPointer
     */
    public int getCelSetPointer() {
        return getCelSets().get(0).getDataPointer();
    }
}
