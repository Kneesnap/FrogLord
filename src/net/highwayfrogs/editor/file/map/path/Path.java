package net.highwayfrogs.editor.file.map.path;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the PATH struct.
 * Created by Kneesnap on 9/16/2018.
 */
public class Path extends GameObject {
    private List<Short> entities = new ArrayList<>(); // An array of entity ids which follow this path.
    private List<PathSegment> segments = new ArrayList<>();

    @Override
    public void load(DataReader reader) {
        int entityIndicePointer = reader.readInt(); // "Note that entity_indices points to a (-1) terminated list of indices into the global entity list. (ie. the list of pointers after the entity table packet header)"
        // System.out.println("Entity Pointer: " + entityIndicePointer);

        /*if (entityIndicePointer > 0) {
            reader.jumpTemp(entityIndicePointer);

            short temp;
            while ((temp = reader.readShort()) != (short) -1)
                entities.add(temp);

            reader.jumpReturn();
        }*/

        int segmentCount = reader.readInt();
        int segmentPointer = reader.readInt();

        // System.out.println("Segment Pointer: " + segmentPointer);

        // Read segments. TODO: Bad pointer?
        /*reader.jumpTemp(segmentPointer);

        for (int j = 0; j < segmentCount; j++) {
            reader.jumpTemp(reader.readInt());

            PathType type = PathType.values()[reader.readInt()];
            PathSegment segment = type.getMaker().get();
            segment.load(reader);
            this.segments.add(segment);

            reader.jumpReturn();

        }

        reader.jumpReturn();*/
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: Save.
    }
}
