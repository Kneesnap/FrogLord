package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFBBox;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a MOF animation model. Struct "MR_ANIM_MODEL"
 * Eventually, support PERCEL_BBOXES_INCLUDED.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimationModel extends GameObject {
    @NonNull private ModelBoundingBoxMode mode = ModelBoundingBoxMode.GLOBAL_BBOXES_INCLUDED;
    // Bounding Box Set is unused.
    // Constraint is unused.

    private final transient MOFAnimationModelSet parent;
    private transient int tempCelsetPointerAddress;

    public enum ModelBoundingBoxMode {
        NONE, GLOBAL_BBOXES_INCLUDED, PERCEL_BBOXES_INCLUDED
    }

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
        this.mode = ModelBoundingBoxMode.values()[flags];

        int partCount = reader.readUnsignedShortAsInt(); // Does not always match real one.
        int staticModelId = reader.readUnsignedShortAsInt();
        Utils.verify(staticModelId == STATIC_MODEL_ID, "Invalid Animation Model ID! (%d)", staticModelId);

        int celsetPointer = reader.readInt(); // Right after BBOX
        int bboxPointer = reader.readInt(); // Right after struct.
        int bboxSetPointer = reader.readInt(); // Unused.
        int constraintPointer = reader.readInt(); // Unused.

        Utils.verify(bboxSetPointer == 0, "BBOX Set Pointer was not null.");
        Utils.verify(constraintPointer == 0, "Constraint Pointer was not null.");
        Utils.verify(celsetPointer == getCelSetPointer(), "Invalid CelSet Pointer! (%d, %d)", celsetPointer, getCelSetPointer());

        if (this.mode == ModelBoundingBoxMode.GLOBAL_BBOXES_INCLUDED) {
            reader.setIndex(bboxPointer);
            new MOFBBox().load(reader); // Unused.
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(DEFAULT_ANIMATION_TYPE);
        writer.writeUnsignedShort(this.mode.ordinal());
        writer.writeUnsignedShort(getParent().getParent().getStaticMOF().getParts().size());
        writer.writeUnsignedShort(STATIC_MODEL_ID);

        this.tempCelsetPointerAddress = writer.getIndex();
        writer.writeInt(0); // Right after BBOX

        int bboxPointer = writer.writeNullPointer();
        writer.writeNullPointer();
        writer.writeNullPointer();

        // Write BBOX
        if (this.mode == ModelBoundingBoxMode.GLOBAL_BBOXES_INCLUDED) {
            writer.writeAddressTo(bboxPointer);
            getParent().getParent().makeBoundingBox().save(writer);
        }
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