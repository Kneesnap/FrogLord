package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MR_PART struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFPart extends GameObject {
    private short flags;
    private List<MOFPartcel> partcels = new ArrayList<>();

    private static final int FLAG_ANIMATED_POLYS = 1; // Does this contain some animated texture polys?

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        short partcelCount = reader.readShort();
        short verticeCount = reader.readShort();
        short normalCount = reader.readShort();
        short primitiveCount = reader.readShort();
        short hiliteCount = reader.readShort();

        int partcelPointer = reader.readInt();
        int primitivePointer = reader.readInt();
        int hilitePointer = reader.readInt(); // May be null
        int buffSize = reader.readInt(); // Size of a single set of preset polygon for this model part in bytes. TODO: This needs to be generated, because it's zero in stored form.
        int collprimPointer = reader.readInt(); // May be null.
        int matrixPointer = reader.readInt(); // May be null.
        int animatedTexturesPointer = reader.readInt(); // (Point to integer which is count.) Followed by: MR_PART_POLY_ANIM
        int flipbookPointer = reader.readInt(); // MR_PART_FLIPBOOK (MR_PART_FLIPBOOK_ACTION may follow?)

        // Read Partcels.
        reader.jumpTemp(partcelPointer);
        for (int i = 0; i < partcelCount; i++) {
            MOFPartcel partcel = new MOFPartcel(verticeCount, normalCount);
            partcel.load(reader);
            partcels.add(partcel);
        }
        reader.jumpReturn();

        // Read Primitives:
        reader.jumpTemp(primitivePointer);
        for (int i = 0; i < primitiveCount; i++) {
            short primType = reader.readShort(); // MR_MPRIM_HEADER
            short primCount = reader.readShort();

            //TODO: Read prims.
        }

        reader.jumpReturn();


        //TODO: BBox.
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: Save logic.
    }
}
