package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MR_ANIM_HEADER struct.
 * Must be encapsulated under MOFFile.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimation extends GameObject {
    private MOFFile holderMOF;
    private List<MOFAnimationModelSet> modelSets = new ArrayList<>();
    private List<MOFFile> mofFiles = new ArrayList<>();
    private MOFAnimCommonData commonData;

    public static final byte FILE_START_FRAME_AT_ZERO = (byte) 0x31; // '1'

    public MOFAnimation(MOFFile file) {
        this.holderMOF = file;
    }

    @Override
    public void load(DataReader reader) {
        Utils.verify(shouldStartAtFrameZero(), "Animations which do not start at frame-zero are not currently supported.");

        int modelSetCount = reader.readUnsignedShortAsInt();
        int staticFileCount = reader.readUnsignedShortAsInt();
        int modelSetPointer = reader.readInt();   // Right after header.
        int commonDataPointer = reader.readInt(); // Right after model set data.
        int staticFilePointer = reader.readInt(); // After common data pointer.

        // Read model sets.
        reader.jumpTemp(modelSetPointer);
        for (int i = 0; i < modelSetCount; i++) {
            MOFAnimationModelSet modelSet = new MOFAnimationModelSet();
            modelSet.load(reader);
            modelSets.add(modelSet);
        }
        reader.jumpReturn();

        // Read common data.
        reader.jumpTemp(commonDataPointer);
        this.commonData = new MOFAnimCommonData(this);
        this.commonData.load(reader);
        reader.jumpReturn();

        reader.jumpTemp(staticFilePointer);
        int[] mofPointers = new int[staticFileCount];
        for (int i = 0; i < mofPointers.length; i++)
            mofPointers[i] = reader.readInt();

        for (int i = 0; i < mofPointers.length; i++) {
            DataReader mofReader = reader.newReader(mofPointers[i], i == mofPointers.length - 1 ? -1 : mofPointers[i + 1] - reader.getIndex());
            MOFFile mof = new MOFFile();
            mof.load(mofReader);
            mofFiles.add(mof);
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.modelSets.size());
        writer.writeUnsignedShort(this.mofFiles.size());

        writer.writeInt(writer.getIndex() + (3 * Constants.POINTER_SIZE)); // Right after header.
        int commonDataPointer = writer.getIndex(); // Right after model set data.
        writer.writeInt(0);
        int staticFilePointer = writer.getIndex(); // After common data.
        writer.writeInt(0);

        // Write model sets.
        for (MOFAnimationModelSet modelSet : getModelSets())
            modelSet.save(writer);

        // Write common data.
        writer.writeAddressTo(commonDataPointer);
        this.commonData.save(writer);

        // Write MOF Files.
        writer.writeAddressTo(staticFilePointer);
        int[] mofPointers = new int[getMofFiles().size()];
        for (int i = 0; i < mofPointers.length; i++) {
            mofPointers[i] = writer.getIndex();
            writer.writeInt(0);
        }

        for (int i = 0; i < mofPointers.length; i++) {
            writer.writeAddressTo(mofPointers[i]);
            getMofFiles().get(i).save(writer);
        }
    }

    /**
     * Does this animation start at frame zero?
     * @return startAtFrameZero
     */
    public boolean shouldStartAtFrameZero() {
        return getHolderMOF().getSignature()[0] == FILE_START_FRAME_AT_ZERO;
    }

    /**
     * Get the TransformType for this animation.
     * @return transformType.
     */
    public TransformType getTransformType() {
        return TransformType.getType(getHolderMOF().getSignature()[1]);
    }
}
