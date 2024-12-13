package net.highwayfrogs.editor.games.sony.shared.model.actionset;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

/**
 * Represents an action in an action set.
 * Created by Kneesnap on 5/15/2024.
 */
public class PTAction extends SCSharedGameData {
    @Getter private final PTActionSet actionSet;
    @Getter private int rootTransformIndex; // Root transformation index.
    @Getter private int[] virtualFrames; // This is signed fixed point 1.19.12. A value of 1.5 means to interpolate halfway between keyframe 1 and keyframe 2.
    @Getter private short[][] transformationIndices; // This is signed fixed point 1.3.12. A value of 1.5 means to interpolate halfway between keyframe 1 and keyframe 2.
    private transient int keyFrameIndexListPointer;
    private transient int transformIndexListPointer;

    public PTAction(PTActionSet actionSet) {
        super(actionSet != null ? actionSet.getGameInstance() : null);
        this.actionSet = actionSet;
    }

    @Override
    public void load(DataReader reader) {
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // This value seems to be calculated by the anim file load function.
        int actionIndex = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.POINTER_SIZE); // Runtime pointer.
        this.rootTransformIndex = reader.readUnsignedShortAsInt();
        int transformsPerKeyFrame = reader.readUnsignedShortAsInt();
        int keyFrameCount = reader.readUnsignedShortAsInt();
        this.transformationIndices = new short[keyFrameCount][transformsPerKeyFrame];
        this.virtualFrames = new int[reader.readUnsignedShortAsInt()];
        this.keyFrameIndexListPointer = reader.readInt();
        this.transformIndexListPointer = reader.readInt(); // List of transform indices for each keyframe of each transform.

        // Ensure the index isn't busted.
        int calculatedActionIndex = getActionIndex();
        if (actionIndex != calculatedActionIndex)
            throw new RuntimeException("The stored action index was " + actionIndex + ", but the action index was calculated to be " + calculatedActionIndex + ".");
    }

    /**
     * Reads the key frame list from the current position.
     * @param reader the reader to read it from
     */
    void readKeyFrameList(DataReader reader) {
        if (this.keyFrameIndexListPointer <= 0)
            throw new RuntimeException("Cannot read key frame list, the pointer is invalid.");

        reader.requireIndex(getLogger(), this.keyFrameIndexListPointer, "Expected key frame index list");
        for (int i = 0; i < this.virtualFrames.length; i++)
            this.virtualFrames[i] = reader.readInt();

        this.keyFrameIndexListPointer = -1;
    }

    /**
     * Reads the transform index list from the current position.
     * @param reader the reader to read it from
     */
    void readTransformIndexList(DataReader reader) {
        if (this.transformIndexListPointer <= 0)
            throw new RuntimeException("Cannot read transform index list, the pointer is invalid.");

        reader.requireIndex(getLogger(), this.transformIndexListPointer, "Expected transform index list");
        for (int keyFrame = 0; keyFrame < this.transformationIndices.length; keyFrame++) {
            short[] transformIndicesInKeyFrame = this.transformationIndices[keyFrame];
            for (int i = 0; i < transformIndicesInKeyFrame.length; i++)
                transformIndicesInKeyFrame[i] = reader.readShort();
        }

        this.transformIndexListPointer = -1;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort((short) 0);
        writer.writeUnsignedShort(getActionIndex());
        writer.writeNullPointer(); // Runtime pointer.
        writer.writeUnsignedShort(this.rootTransformIndex);
        writer.writeUnsignedShort(getTransformsPerKeyFrame()); // transformsPerKeyFrame
        writer.writeUnsignedShort(getKeyFrameCount()); // keyFrameCount
        writer.writeUnsignedShort(this.virtualFrames != null ? this.virtualFrames.length : 0); // Virtual Frame Count.
        this.keyFrameIndexListPointer = writer.writeNullPointer();
        this.transformIndexListPointer = writer.writeNullPointer();
    }

    /**
     * Writes the key frame list to the current position.
     * @param writer the writer to write the data to
     */
    void writeKeyFrameList(DataWriter writer) {
        if (this.keyFrameIndexListPointer <= 0)
            throw new RuntimeException("Cannot read key frame list, the pointer is invalid.");

        writer.writeAddressTo(this.keyFrameIndexListPointer);
        for (int i = 0; i < this.virtualFrames.length; i++)
            writer.writeInt(this.virtualFrames[i]);

        this.keyFrameIndexListPointer = -1;
    }

    /**
     * Writes the transform index list to the current position.
     * @param writer the writer to write the data to
     */
    void writeTransformIndexList(DataWriter writer) {
        if (this.transformIndexListPointer <= 0)
            throw new RuntimeException("Cannot write transform index list, the pointer is invalid.");

        writer.writeAddressTo(this.transformIndexListPointer);
        for (int keyFrame = 0; keyFrame < this.transformationIndices.length; keyFrame++) {
            short[] transformIndicesInKeyFrame = this.transformationIndices[keyFrame];
            for (int i = 0; i < transformIndicesInKeyFrame.length; i++)
                writer.writeShort(transformIndicesInKeyFrame[i]);
        }

        this.transformIndexListPointer = -1;
    }

    /**
     * Gets the index of this action.
     */
    public int getActionIndex() {
        return this.actionSet != null ? this.actionSet.getActions().indexOf(this) : -1;
    }

    /**
     * Gets the transforms per keyframe.
     */
    public int getTransformsPerKeyFrame() {
        return this.transformationIndices != null ? this.transformationIndices[0].length : 0;
    }

    /**
     * Gets the keyframe count.
     */
    public int getKeyFrameCount() {
        return this.transformationIndices != null ? this.transformationIndices.length : 0;
    }

    /**
     * Calculate the number of bits to shift.
     * Recreation of the algorithm in the .ANIM file process function.
     */
    public short getBitsShifted() {
        short value = 0;

        // Calculate the value...
        int keyFrameCount = getKeyFrameCount();
        for (int i = 15; i > 0; i--)
            if ((keyFrameCount & (1 << i)) != 0)
                value = (short) Math.max(0, i - 6);

        return value;
    }

    /**
     * Gets information used to identify the logger.
     */
    public String getLoggerInfo() {
        return (this.actionSet != null ? this.actionSet.getLoggerInfo() + ",Action=" + this.actionSet.getActions().indexOf(this) : Utils.getSimpleName(this));
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), PTAction::getLoggerInfo, this);
    }
}