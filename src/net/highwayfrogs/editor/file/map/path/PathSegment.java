package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * A single part of the path. When saved, this is broken up by <type,offset> -> segment data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public abstract class PathSegment extends GameObject {
    private final PathType type;
    private int length;
    private final transient Path path;

    public PathSegment(Path path, PathType type) {
        this.path = path;
        this.type = type;
    }

    @Override
    public void load(DataReader reader) {
        FroggerMapConfig mapConfig = this.path.getMapFile().getMapConfig();

        if (!mapConfig.isOldPathFormat())
            this.length = reader.readInt();
        this.loadData(reader);
        if (mapConfig.isOldPathFormat())
            recalculateLength();
    }

    @Override
    public void save(DataWriter writer) {
        //writer.writeInt(getType().ordinal()); // This line would break converting to the new format via MAPFile.copyToNewViaBytes() because there's no corresponding read.
        if (!this.path.getMapFile().getMapConfig().isOldPathFormat())
            writer.writeInt(this.length);
        saveData(writer);
    }

    /**
     * Setup this segment at the end of the given path.
     */
    public abstract void setupNewSegment(MAPFile map);

    /**
     * Load segment specific data.
     * @param reader Data source.
     */
    protected abstract void loadData(DataReader reader);

    /**
     * Write segment specific data.
     * @param writer the receiver to write data to.
     */
    protected abstract void saveData(DataWriter writer);

    /**
     * Calculate the position after a path is completed.
     * @param info The info to calculate with.
     * @return finishPosition
     */
    public abstract PathResult calculatePosition(PathInfo info);

    /**
     * Calculate the position along this segment.
     * @param map      The map containing the path.
     * @param distance The distance along this segment.
     * @return pathResult
     */
    public PathResult calculatePosition(MAPFile map, int distance) {
        PathInfo fakeInfo = new PathInfo();
        fakeInfo.setPath(map, this.path, this);
        fakeInfo.setSegmentDistance(distance);
        fakeInfo.setSpeed(1);
        return calculatePosition(fakeInfo);
    }

    /**
     * Recalculates the length of this segment.
     */
    public abstract void recalculateLength();

    /**
     * Gets the start position of this segment.
     * @return startPosition
     */
    public abstract SVector getStartPosition();

    /**
     * Sets the length of this segment.
     * @param newLength The segment length
     */
    @SuppressWarnings("ConstantConditions")
    public void setLength(int newLength) {
        this.length = newLength;
    }

    /**
     * Converts the path to the new map format.
     */
    public abstract FroggerPathSegment convertToNewFormat(FroggerPath newPath);
}