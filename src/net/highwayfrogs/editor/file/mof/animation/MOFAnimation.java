package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformType;
import net.highwayfrogs.editor.file.reader.ArraySource;
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

        short modelSetCount = reader.readShort();
        short staticFileCount = reader.readShort();
        int modelSetPointer = reader.readInt();   //
        int commonDataPointer = reader.readInt(); // MR_ANIM_COMMON_DATA.
        int staticFilePointer = reader.readInt(); // Points to pointers which point to MR_MOF.

        // Read model sets.
        /*
        reader.jumpTemp(modelSetPointer);
        for (int i = 0; i < modelSetCount; i++) {
            MOFAnimationModelSet modelSet = new MOFAnimationModelSet();
            modelSet.load(reader);
            modelSets.add(modelSet);
        }
        reader.jumpReturn();
        */

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
            reader.jumpTemp(mofPointers[i]);
            byte[] bytes = reader.readBytes(i == mofPointers.length - 1 ? reader.getRemaining() : mofPointers[i + 1] - reader.getIndex());
            DataReader mofReader = new DataReader(new ArraySource(bytes));

            MOFFile mof = new MOFFile();
            mof.load(mofReader);
            mofFiles.add(mof);
            reader.jumpReturn();
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: Don't forget to update the header.
        //TODO: Save
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
