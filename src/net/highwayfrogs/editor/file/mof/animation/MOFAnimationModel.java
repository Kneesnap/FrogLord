package net.highwayfrogs.editor.file.mof.animation;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a MOF animation model. Struct "MR_ANIM_MODEL"
 * Created by Kneesnap on 8/25/2018.
 */
public class MOFAnimationModel extends GameObject {
    private short animationType;
    private short flags;
    private short staticModelId;

    @Override
    public void load(DataReader reader) {
        this.animationType = reader.readShort();
        this.flags = reader.readShort(); //TODO: Port
        short partCount = reader.readShort();
        this.staticModelId = reader.readShort();

        int celsetPointer = reader.readInt();
        int bboxPointer = reader.readInt();
        int bboxSetPointer = reader.readInt();
        int constraintPointer = reader.readInt(); // Unused.


        //TODO
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: Save
    }
}
