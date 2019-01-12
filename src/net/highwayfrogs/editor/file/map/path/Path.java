package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
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
    private List<Short> entityIds = new ArrayList<>();
    private transient int tempEntityIndexPointer;

    @Override
    public void load(DataReader reader) {
        int entityIndexPointer = reader.readInt(); // pa_entity_indices, "Note that entity_indices points to a (-1) terminated list of indices into the global entity list. (ie. the list of pointers after the entity table packet header)"

        reader.jumpTemp(entityIndexPointer);
        short tempShort;
        while ((tempShort = reader.readShort()) != MAPFile.MAP_ANIMATION_TEXTURE_LIST_TERMINATOR)
            entityIds.add(tempShort);
        reader.jumpReturn();

        // Read segments.
        int segmentCount = reader.readInt();
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
        this.tempEntityIndexPointer = writer.writeNullPointer();
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
     * Write the entity index list.
     * @param writer he writer to write data to.
     */
    public void writeEntityList(DataWriter writer) {
        Utils.verify(this.tempEntityIndexPointer > 0, "Path has not been saved yet.");
        writer.writeAddressTo(this.tempEntityIndexPointer);
        this.tempEntityIndexPointer = 0;

        for (short entityId : getEntityIds())
            writer.writeShort(entityId);
        writer.writeShort(MAPFile.MAP_ANIMATION_TEXTURE_LIST_TERMINATOR);
    }

    /**
     * Gets the position of an entity on this path.
     * @param pathInfo Information about this path.
     * @return finishedPosition
     */
    public SVector evaluatePosition(PathInfo pathInfo) {
        return getSegments().get(pathInfo.getSegmentId()).calculatePosition(pathInfo);
    }
}
