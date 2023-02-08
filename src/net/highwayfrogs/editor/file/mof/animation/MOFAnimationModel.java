package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFBBox;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a MOF animation model. Struct "MR_ANIM_MODEL"
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimationModel extends GameObject {
    // Bounding Box Set is unused.
    // Constraint is unused.

    private final transient MOFAnimationModelSet parent;
    private transient int tempCelsetPointerAddress;

    public static final int FLAG_GLOBAL_BBOXES_INCLUDED = Constants.BIT_FLAG_0;
    public static final int FLAG_PERCEL_BBOXES_INCLUDED = Constants.BIT_FLAG_1;

    private static final int DEFAULT_ANIMATION_TYPE = 1;
    private static final int STATIC_MODEL_ID = 0; // It's always 0.

    public MOFAnimationModel(MOFAnimationModelSet set) {
        this.parent = set;
    }

    @Override
    public void load(DataReader reader) {
        int animationType = reader.readUnsignedShortAsInt();
        Utils.verify(animationType == DEFAULT_ANIMATION_TYPE, "Unknown animation type: %d.", animationType);

        int flags = reader.readUnsignedShortAsInt();
        boolean buildAllowsZeroFlag = getConfig().isSonyPresentation() || getConfig().isPSXAlpha();
        Utils.verify(flags == FLAG_GLOBAL_BBOXES_INCLUDED || (buildAllowsZeroFlag && flags == 0), "Global BBoxes is the only mode supported! (%s)", Utils.toHexString(flags));

        int partCount = reader.readUnsignedShortAsInt();
        int staticModelId = reader.readUnsignedShortAsInt();
        Utils.verify(staticModelId == STATIC_MODEL_ID, "Invalid Animation Model ID! (%d)", staticModelId);

        int celsetPointer = reader.readInt(); // Right after BBOX
        int bboxPointer = reader.readInt(); // Right after struct.
        int bboxSetPointer = reader.readInt(); // Unused.
        int constraintPointer = reader.readInt(); // Unused.

        Utils.verify(bboxSetPointer == 0, "BBOX Set Pointer was not null.");
        Utils.verify(constraintPointer == 0, "Constraint Pointer was not null.");
        Utils.verify(celsetPointer == getCelSetPointer(), "Invalid CelSet Pointer! (%d, %d)", celsetPointer, getCelSetPointer());

        reader.setIndex(bboxPointer);
        new MOFBBox().load(reader); // Unused.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(DEFAULT_ANIMATION_TYPE);
        writer.writeUnsignedShort(FLAG_GLOBAL_BBOXES_INCLUDED);
        writer.writeUnsignedShort(getParent().getParent().getStaticMOF().getParts().size());
        writer.writeUnsignedShort(STATIC_MODEL_ID);

        this.tempCelsetPointerAddress = writer.getIndex();
        writer.writeInt(0); // Right after BBOX

        int calculatedBboxPointer = writer.writeNullPointer();
        writer.writeNullPointer();
        writer.writeNullPointer();

        // Write BBOX
        writer.writeAddressTo(calculatedBboxPointer);
        getParent().getParent().makeBoundingBox().save(writer);
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
     * Get the celset this model owns.
     * @return celSets
     */
    public MOFAnimationCelSet getCelSet() {
        return getParent().getCelSet();
    }

    /**
     * Gets the pointer which gets saved and loaded for this file.
     * @return celsetPointer
     */
    public int getCelSetPointer() {
        return getCelSet().getDataPointer();
    }
}