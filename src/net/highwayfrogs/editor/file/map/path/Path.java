package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the PATH struct.
 * TODO: Maybe we can have a system which prevents duplicate entity indice pointers.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class Path extends GameObject {
    private List<Short> entities = new ArrayList<>(); // An array of entity ids which follow this path.
    private List<PathSegment> segments = new ArrayList<>();

    private transient int entityPointerLocation;
    private static final short TERMINATOR = (short) 0xFF;

    @Override
    public void load(DataReader reader) {
        int entityIndicePointer = reader.readInt(); // "Note that entity_indices points to a (-1) terminated list of indices into the global entity list. (ie. the list of pointers after the entity table packet header)"

        if (entityIndicePointer > 0) {
            reader.jumpTemp(entityIndicePointer);

            short temp;
            while ((temp = reader.readShort()) != TERMINATOR)
                entities.add(temp);

            reader.jumpReturn();
        }

        int segmentCount = reader.readInt();

        // Read segments.
        for (int j = 0; j < segmentCount; j++) {
            reader.jumpTemp(reader.readInt());
            PathType type = PathType.values()[reader.readInt()];
            PathSegment segment = type.getMaker().get();
            segment.load(reader);
            this.segments.add(segment);
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.entityPointerLocation = writer.getIndex();
        writer.writeInt(0); // Placeholder until it's actually written.
        writer.writeInt(segments.size());

        int segmentPointer = writer.getIndex() + (Constants.POINTER_SIZE * segments.size());
        for (PathSegment segment : segments) {
            writer.writeInt(segmentPointer);

            writer.jumpTemp(segmentPointer);
            segment.save(writer);
            segmentPointer = writer.getIndex();
            writer.jumpReturn();
        }

        writer.setIndex(segmentPointer);
    }

    /**
     * Write the entity list.
     * @param writer The writer to write the data to.
     */
    public void writeEntityList(DataWriter writer) {
        Utils.verify(entityPointerLocation > 0, "Entity pointer location is not set!");

        int tempAddress = writer.getIndex();
        writer.jumpTemp(entityPointerLocation);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        for (short entity : this.entities)
            writer.writeShort(entity);
        writer.writeShort(TERMINATOR);

        this.entityPointerLocation = 0;
    }
}
