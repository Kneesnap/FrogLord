package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.file.mof.MOFBase;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
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
public class MOFAnimation extends MOFBase {
    private MOFAnimationModelSet modelSet;
    private MOFFile staticMOF;
    private MOFAnimCommonData commonData;

    public static final byte FILE_START_FRAME_AT_ZERO = (byte) 0x31; // '1'
    private static final int STATIC_MOF_COUNT = 1;

    public MOFAnimation(MOFHolder holder) {
        super(holder);
    }

    @Override
    public void onLoad(DataReader reader) {
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
        this.staticMOF = new MOFFile(getHolder());
        this.staticMOF.load(mofReader);
    }

    @Override
    public void onSave(DataWriter writer) {
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
        return getSignature()[0] == FILE_START_FRAME_AT_ZERO;
    }

    /**
     * Get the TransformType for this animation.
     * @return transformType.
     */
    public TransformType getTransformType() {
        return TransformType.getType(getSignature()[1]);
    }

    /**
     * Get an animation transform.
     * @param part     The MOFPart to apply to.
     * @param actionId The animation to get the transform for.
     * @param frame    The frame id to get the transform for.
     * @return transform
     */
    public TransformObject getTransform(MOFPart part, int actionId, int frame) {
        return getCommonData().getTransforms().get(getAnimationById(actionId).getTransformID(frame, part));
    }

    /**
     * Gets the MOFAnimation cel by its action id.
     * @param actionId The given action.
     * @return cel
     */
    public MOFAnimationCels getAnimationById(int actionId) {
        return getModelSet().getCelSet().getCels().get(actionId);
    }

    /**
     * Gets the amount of actions this MOFAnimation has.
     * @return actionCount
     */
    public int getAnimationCount() {
        return getModelSet().getCelSet().getCels().size();
    }
}
