package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFFile;
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
    private List<MOFAnimationModelSet> modelSets = new ArrayList<>();
    private List<MOFFile> mofFiles = new ArrayList<>();
    private MOFAnimCommonData commonData;

    private static final byte[] SIGNATURE = "1ax".getBytes();

    @Override
    public void load(DataReader reader) {
        short modelSetCount = reader.readShort();
        short staticFileCount = reader.readShort();
        int modelSetPointer = reader.readInt();   //
        int commonDataPointer = reader.readInt(); // MR_ANIM_COMMON_DATA.
        int staticFilePointer = reader.readInt(); // Points to pointers which point to MR_MOF.

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
        this.commonData = new MOFAnimCommonData();
        this.commonData.load(reader);
        reader.jumpReturn();

        reader.jumpTemp(staticFilePointer);
        for (int i = 0; i < staticFileCount; i++) {
            reader.jumpTemp(reader.readInt());
            MOFFile mof = new MOFFile();
            mof.load(reader);
            mofFiles.add(mof);
            reader.jumpReturn();
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        //TODO
    }
}
