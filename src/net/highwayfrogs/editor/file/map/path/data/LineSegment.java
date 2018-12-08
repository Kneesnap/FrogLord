package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents PATH_LINE.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class LineSegment extends PathSegment {
    private SVector start;
    private SVector end;

    public LineSegment() {
        super(PathType.LINE);
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start = SVector.readWithPadding(reader);
        this.end = SVector.readWithPadding(reader);
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.end.saveWithPadding(writer);
    }

    @Override
    protected SVector calculatePosition(PathInfo info) {
        int deltaX = end.getX() - start.getX();
        int deltaY = end.getY() - start.getY();
        int deltaZ = end.getZ() - start.getZ();

        SVector pathRunnerPosition = new SVector();
        pathRunnerPosition.setX((short) (start.getX() + ((deltaX * info.getSegmentDistance()) / getLength())));
        pathRunnerPosition.setY((short) (start.getY() + ((deltaY * info.getSegmentDistance()) / getLength())));
        pathRunnerPosition.setZ((short) (start.getZ() + ((deltaZ * info.getSegmentDistance()) / getLength())));
        return pathRunnerPosition;
    }
}
