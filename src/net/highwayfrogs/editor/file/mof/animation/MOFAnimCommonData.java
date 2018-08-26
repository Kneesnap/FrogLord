package net.highwayfrogs.editor.file.mof.animation;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the "MR_ANIM_COMMON_DATA" struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MOFAnimCommonData extends GameObject {
    private int flags; //TODO: Port flags.

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        short transformCount = reader.readShort();
        short rotationCount = reader.readShort();
        short translationCount = reader.readShort();
        short bboxCount = reader.readShort();

        int transformPointer = reader.readInt();
        int rotationPointer = reader.readInt();
        int translationPointer = reader.readInt();
        int bboxPointer = reader.readInt();

        //TODO
    }

    @Override
    public void save(DataWriter writer) {
        //TODO
    }
}
