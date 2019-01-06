package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFBBox;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

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
    private List<MOFAnimationCelSet> celSets = new ArrayList<>();
    // Bounding Box Set is unused.
    // Constraint is unused.

    public static final int FLAG_GLOBAL_BBOXES_INCLUDED = Constants.BIT_FLAG_0;
    public static final int FLAG_PERCEL_BBOXES_INCLUDED = Constants.BIT_FLAG_1;

    @Override
    public void load(DataReader reader) {
        this.animationType = reader.readShort();
        this.flags = reader.readShort();
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

        // Read Celset.
        reader.jumpTemp(celsetPointer);
        for (int i = 0; i < partCount; i++) { // Part Count may not be the variable to use, but it seems alright for now.
            MOFAnimationCelSet celSet = new MOFAnimationCelSet();
            celSet.load(reader);
            celSets.add(celSet);
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.animationType);
        writer.writeShort(this.flags);
        writer.writeShort((short) getCelSets().size());
        writer.writeShort(this.staticModelId);

        int celsetPointer = writer.getIndex();
        writer.writeInt(0); // Right after BBOX

        int calculatedBboxPointer = writer.getIndex() + (3 * Constants.POINTER_SIZE);
        writer.writeInt(calculatedBboxPointer);
        writer.writeInt(0);
        writer.writeInt(0);

        // Write BBOX
        Utils.verify(calculatedBboxPointer == writer.getIndex(), "Calculated wrong bbox pointer. (%d, %d)", calculatedBboxPointer, writer.getIndex());
        this.boundingBox.save(writer);

        // Write Celset.
        int tempAddress = writer.getIndex();
        writer.jumpTemp(celsetPointer);
        writer.writeInt(tempAddress);
        writer.jumpReturn();
        this.celSets.forEach(celSet -> celSet.save(writer));
    }
}
