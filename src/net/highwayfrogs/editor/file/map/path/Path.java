package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the PATH struct.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class Path extends GameObject {
    private final MAPFile mapFile;
    private final List<PathSegment> segments = new ArrayList<>();
    private transient int tempEntityIndexPointer;

    public Path(MAPFile mapFile) {
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        if (!this.mapFile.getMapConfig().isOldPathFormat())
            reader.skipPointer(); // Points to a -1 terminated entity index list of entities using this path. Seems to be invalid data in many cases. Since it appears to only ever be used for the retro beaver, we auto-generate it in that scenario.

        // Read segments.
        int segmentCount = reader.readInt();
        for (int j = 0; j < segmentCount; j++) {
            reader.jumpTemp(reader.readInt());
            PathType type = PathType.values()[reader.readInt()];
            PathSegment segment = type.makeNew(this);
            segment.load(reader);
            this.segments.add(segment);
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        if (!this.mapFile.getMapConfig().isOldPathFormat())
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
     * @param writer The writer to write data to.
     */
    public void writeEntityList(MAPFile mapFile, DataWriter writer) {
        Utils.verify(this.tempEntityIndexPointer > 0, "Path has not been saved yet.");

        List<Entity> pathEntities = getEntities(mapFile);
        if (!shouldSave(pathEntities)) {
            this.tempEntityIndexPointer = 0;
            return;
        }

        writer.writeAddressTo(this.tempEntityIndexPointer);
        this.tempEntityIndexPointer = 0;

        for (Entity entity : pathEntities)
            writer.writeShort((short) mapFile.getEntities().indexOf(entity));
        writer.writeShort(MAPFile.MAP_ANIMATION_TEXTURE_LIST_TERMINATOR);
    }

    /**
     * Gets the position of an entity on this path.
     * @param pathInfo Information about this path.
     * @return finishedPosition
     */
    public PathResult evaluatePosition(PathInfo pathInfo) {
        return getSegments().get(pathInfo.getSegmentId()).calculatePosition(pathInfo);
    }

    /**
     * Gets the length of all the segments combined.
     * @return totalLength
     */
    public int getTotalLength() {
        int totalLength = 0;
        for (int i = 0; i < getSegments().size(); i++)
            totalLength += getSegments().get(i).getLength();
        return totalLength;
    }

    private boolean shouldSave(List<Entity> pathEntities) {
        for (Entity testEntity : pathEntities)
            if ("ORG_BEAVER".equals(testEntity.getFormEntry().getFormTypeName()))
                return true; // This is the only case where this is ever used.
        return false;
    }

    private List<Entity> getEntities(MAPFile mapFile) {
        List<Entity> pathEntities = new LinkedList<>();
        int myPathId = mapFile.getPaths().indexOf(this);

        for (Entity entity : mapFile.getEntities()) {
            PathInfo info = entity.getPathInfo();
            if (info != null && info.getPathId() == myPathId)
                pathEntities.add(entity);
        }

        return pathEntities;
    }

    /**
     * Converts the path to the new map format.
     */
    public FroggerPath convertToNewFormat(FroggerMapFile mapFile) {
        FroggerPath newPath = new FroggerPath(mapFile);

        for (PathSegment segment : this.segments)
            newPath.getSegments().add(segment.convertToNewFormat(newPath));

        return newPath;
    }
}