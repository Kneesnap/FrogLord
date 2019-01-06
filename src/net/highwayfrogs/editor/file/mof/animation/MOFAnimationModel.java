package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFBBox;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a MOF animation model. Struct "MR_ANIM_MODEL"
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimationModel extends GameObject {
    private short animationType;
    private short flags;
    private short staticModelId;
    private MOFBBox boundingBox;
    // Bounding Box Set is unused.
    // Constraint is unused.

    @Override
    public void load(DataReader reader) {
        this.animationType = reader.readShort();
        this.flags = reader.readShort(); //TODO: Port
        short partCount = reader.readShort();
        this.staticModelId = reader.readShort();

        int celsetPointer = reader.readInt(); // Right after BBOX
        int bboxPointer = reader.readInt(); // Right after struct.
        int bboxSetPointer = reader.readInt(); // Unused.
        int constraintPointer = reader.readInt(); // Unused.

        Utils.verify(bboxSetPointer == 0, "BBOX Set Pointer was not null.");
        Utils.verify(constraintPointer == 0, "Constraint Pointer was not null.");

        reader.jumpTemp(bboxPointer);
        this.boundingBox = new MOFBBox();
        this.boundingBox.load(reader);
        reader.jumpReturn();

        //TODO: Read Celset
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: Save

        //TODO: Note, do not return to into somewhere before the final byte we write.
    }
}
