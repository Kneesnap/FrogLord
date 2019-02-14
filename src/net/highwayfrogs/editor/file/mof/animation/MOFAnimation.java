package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the MR_ANIM_HEADER struct.
 * Must be encapsulated under MOFFile.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimation extends GameObject {
    private MOFAnimationModelSet modelSet;
    private MOFFile staticMOF;
    private MOFAnimCommonData commonData;

    private transient MOFFile mofParent;

    public static final byte FILE_START_FRAME_AT_ZERO = (byte) 0x31; // '1'
    private static final int STATIC_MOF_COUNT = 1;

    public MOFAnimation(MOFFile file) {
        this.mofParent = file;
    }

    @Override
    public void load(DataReader reader) {
        Utils.verify(shouldStartAtFrameZero(), "Animations which do not start at frame-zero are not currently supported.");

        int modelSetCount = reader.readUnsignedShortAsInt();
        int staticFileCount = reader.readUnsignedShortAsInt();
        int modelSetPointer = reader.readInt();   // Right after header.
        int commonDataPointer = reader.readInt(); // Right after model set data.
        int staticFilePointer = reader.readInt(); // After common data pointer.

        Utils.verify(modelSetCount == 1, "Multiple model sets are not supported by FrogLord. (%d)", modelSetCount);
        Utils.verify(staticFileCount == 1, "FrogLord only supports one MOF. (%d)", staticFileCount);

        // Read model sets.
        reader.jumpTemp(modelSetPointer);
        this.modelSet = new MOFAnimationModelSet();
        this.modelSet.load(reader);
        reader.jumpReturn();

        // Read common data.
        reader.jumpTemp(commonDataPointer);
        this.commonData = new MOFAnimCommonData(this);
        this.commonData.load(reader);
        reader.jumpReturn();

        reader.jumpTemp(staticFilePointer);
        int mofPointer = reader.readInt();
        reader.jumpReturn();

        DataReader mofReader = reader.newReader(mofPointer, staticFilePointer - mofPointer);
        this.staticMOF = new MOFFile();
        this.staticMOF.setVloFile(getMofParent().getVloFile());
        this.staticMOF.load(mofReader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(1); // Model Set Count.
        writer.writeUnsignedShort(STATIC_MOF_COUNT);

        int modelSetPointer = writer.writeNullPointer(); // Right after header.
        int commonDataPointer = writer.writeNullPointer(); // Right after model set data.
        int staticFilePointer = writer.writeNullPointer(); // After common data.

        // Write model sets.
        writer.writeAddressTo(modelSetPointer);
        this.modelSet.save(writer);

        // Write common data.
        writer.writeAddressTo(commonDataPointer);
        this.commonData.save(writer);

        // Write static MOF.
        int mofPointer = writer.getIndex();
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter mofWriter = new DataWriter(receiver);
        getStaticMOF().save(mofWriter);
        mofWriter.closeReceiver();
        writer.writeBytes(receiver.toArray());

        // Write pointers.
        writer.writeAddressTo(staticFilePointer);
        writer.writeInt(mofPointer);
    }

    /**
     * Does this animation start at frame zero?
     * @return startAtFrameZero
     */
    public boolean shouldStartAtFrameZero() {
        return mofParent.getSignature()[0] == FILE_START_FRAME_AT_ZERO;
    }

    /**
     * Get the TransformType for this animation.
     * @return transformType.
     */
    public TransformType getTransformType() {
        return TransformType.getType(mofParent.getSignature()[1]);
    }

    /**
     * Get an animation transform.
     * @param part      The MOFPart to apply to.
     * @param cel       The cel who is changing.
     * @param virtualId The id.
     * @return transform
     */
    public TransformObject getTransform(MOFPart part, MOFAnimationCels cel, int virtualId) {
        return getCommonData().getTransforms().get(cel.getTransformID(virtualId, part));
    }
}
