package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the PATH struct.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class Path extends GameObject {
    private List<PathSegment> segments = new ArrayList<>();
    private transient int entityPointerLocation;

    @Override
    public void load(DataReader reader) {
        int entityIndicePointer = reader.readInt(); // pa_entity_indices, "Note that entity_indices points to a (-1) terminated list of indices into the global entity list. (ie. the list of pointers after the entity table packet header)"
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
     * Write the pointer to the entity indice list.
     * @param writer   The writer to write data to.
     * @param location The pointer.
     */
    public void writePointer(DataWriter writer, int location) {
        writer.jumpTemp(this.entityPointerLocation);
        writer.writeInt(location);
        writer.jumpReturn();
    }
}
