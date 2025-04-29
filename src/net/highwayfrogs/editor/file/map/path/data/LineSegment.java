package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.path.*;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegmentLine;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents PATH_LINE.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class LineSegment extends PathSegment {
    private SVector start = new SVector();
    private SVector end = new SVector();

    public LineSegment(Path path) {
        super(path, PathType.LINE);
    }

    @Override
    public void setupNewSegment(MAPFile map) {
        Path path = getPath();
        if (path.getSegments().size() > 0) {
            PathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
            this.start = lastSegment.calculatePosition(map, lastSegment.getLength()).getPosition();
        }

        this.end = new SVector(this.start).add(new SVector(0, 0, 800));
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start.loadWithPadding(reader);
        this.end.loadWithPadding(reader);
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.end.saveWithPadding(writer);
    }

    @Override
    public PathResult calculatePosition(PathInfo info) {
        int deltaX = end.getX() - start.getX();
        int deltaY = end.getY() - start.getY();
        int deltaZ = end.getZ() - start.getZ();

        SVector result = new SVector();
        result.setX((short) (start.getX() + ((deltaX * info.getSegmentDistance()) / getLength())));
        result.setY((short) (start.getY() + ((deltaY * info.getSegmentDistance()) / getLength())));
        result.setZ((short) (start.getZ() + ((deltaZ * info.getSegmentDistance()) / getLength())));
        return new PathResult(result, new IVector(deltaX, deltaY, deltaZ).normalise());
    }

    @Override
    public void recalculateLength() {
        float deltaX = end.getFloatX() - start.getFloatX();
        float deltaY = end.getFloatY() - start.getFloatY();
        float deltaZ = end.getFloatZ() - start.getFloatZ();

        float length = (float)Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
        setLength(DataUtils.floatToFixedPointInt4Bit(length));
    }

    @Override
    public SVector getStartPosition() {
        return getStart();
    }

    @Override
    public FroggerPathSegmentLine convertToNewFormat(FroggerPath newPath) {
        FroggerPathSegmentLine newSegment = new FroggerPathSegmentLine(newPath);
        newSegment.setLength(null, getLength());
        newSegment.getStart().setValues(this.start);
        newSegment.getEnd().setValues(this.end);
        return newSegment;
    }
}