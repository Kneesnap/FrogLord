package net.highwayfrogs.editor.file.map.path;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the PATH_INFO struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class PathInfo extends GameObject {
    private int pathId;
    private int segmentId;
    private int segmentDistance;
    private int motionType;
    private int speed;

    @Override
    public void load(DataReader reader) {
        this.pathId = reader.readUnsignedShortAsInt();
        this.segmentId = reader.readUnsignedShortAsInt();
        this.segmentDistance = reader.readUnsignedShortAsInt();
        this.motionType = reader.readUnsignedShortAsInt();
        this.speed = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.pathId);
        writer.writeUnsignedShort(this.segmentId);
        writer.writeUnsignedShort(this.segmentDistance);
        writer.writeUnsignedShort(this.motionType);
        writer.writeUnsignedShort(this.speed);
        writer.writeUnsignedShort(0);
    }

    /**
     * Sets the id of the path to use. Resets the path progress.
     * @param newPathId The new path's id.
     */
    public void setPathId(int newPathId) {
        setPathId(newPathId, true);
    }

    /**
     * Sets the id of the path to use.
     * @param newPathId     The new path's id.
     * @param resetDistance Whether or not to reset the path progress.
     */
    public void setPathId(int newPathId, boolean resetDistance) {
        if (resetDistance) {
            this.segmentDistance = 0; // Start them at the start of the path when switching paths.
            this.segmentId = 0; // Start them at the start of the path when switching paths.
        }
        this.pathId = newPathId;
    }

    /**
     * Sets the new path to use.
     * @param map           The map which contains the new path.
     * @param newPath       The new path.
     * @param resetDistance Whether or not to reset the path progress.
     */
    public void setPath(MAPFile map, Path newPath, boolean resetDistance) {
        int newIndex = map.getPaths().indexOf(newPath);
        if (newIndex == -1)
            throw new RuntimeException("Cannot setPath, the supplied path was not registered in the supplied map!");
        setPathId(newIndex, resetDistance);
    }

    /**
     * Sets the new path to use.
     * @param map     The map which contains the new path.
     * @param newPath The new path.
     * @param segment The new segment to use.
     */
    public void setPath(MAPFile map, Path newPath, PathSegment segment) {
        setPath(map, newPath, true);
        setSegmentId(newPath.getSegments().indexOf(segment));
    }

    /**
     * Returns the path object associated with this path info. If the path doesn't exist, return null.
     * @param map The map to get the path from.
     * @return pathObject
     */
    public Path getPath(MAPFile map) {
        return getPathId() >= 0 && map.getPaths().size() > getPathId() ? map.getPaths().get(getPathId()) : null;
    }

    /**
     * Gets the total path distance this info is at. Note this is total path distance, not segment distance.
     * @param map The map to get the path from.
     * @return totalPathDistance
     */
    public int getTotalPathDistance(MAPFile map) {
        Path path = getPath(map);
        int totalDistance = getSegmentDistance(); // Get current segment's distance.
        for (int i = 0; i < getSegmentId(); i++) // Include the distance from all previous segments.
            totalDistance += path.getSegments().get(i).getLength();
        return totalDistance;
    }

    /**
     * Updates the distance this is along the path. Note this uses total path distance not segment distance.
     * @param map           The map this info belongs to.
     * @param totalDistance The total path distance.
     */
    public void setTotalPathDistance(MAPFile map, int totalDistance) {
        Path path = getPath(map);
        for (int i = 0; i < path.getSegments().size(); i++) {
            PathSegment segment = path.getSegments().get(i);
            if (totalDistance >= segment.getLength()) {
                totalDistance -= segment.getLength();
            } else { // Found it!
                this.segmentId = i;
                this.segmentDistance = totalDistance;
                break;
            }
        }
    }

    /**
     * Test if a flag is present.
     * @param type The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(PathMotionType type) {
        return (this.motionType & type.getFlag()) == type.getFlag();
    }

    /**
     * Set the flag state.
     * @param flag     The flag type.
     * @param newState The new state of the flag.
     */
    public void setFlag(PathMotionType flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.motionType |= flag.getFlag();
        } else {
            this.motionType ^= flag.getFlag();
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PathMotionType {
        ACTIVE(Constants.BIT_FLAG_0),
        BACKWARDS(Constants.BIT_FLAG_1),
        ONE_SHOT(Constants.BIT_FLAG_2),
        REPEAT(Constants.BIT_FLAG_3),
        FINISHED(Constants.BIT_FLAG_4);

        private final int flag;
    }
}
