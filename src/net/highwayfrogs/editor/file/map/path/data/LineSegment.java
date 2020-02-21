package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.path.*;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents PATH_LINE.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class LineSegment extends PathSegment {
    private SVector start = new SVector();
    private SVector end = new SVector();

    public LineSegment() {
        super(PathType.LINE, false);
    }

    @Override
    public void setupNewSegment(MAPFile map, Path path) {
        if (path.getSegments().size() > 0) {
            PathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
            this.start = lastSegment.calculatePosition(map, path, lastSegment.getLength()).getPosition();
        }

        this.end = new SVector(this.start).add(new SVector(0, 0, 800));
        onUpdate(null);
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
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        super.setupEditor(path, controller, editor);
        editor.addFloatVector("Start:", getStart(), () -> onUpdate(controller), controller);
        editor.addFloatVector("End:", getEnd(), () -> onUpdate(controller), controller);
    }

    @Override
    public void recalculateLength() {
        float deltaX = end.getFloatX() - start.getFloatX();
        float deltaY = end.getFloatY() - start.getFloatY();
        float deltaZ = end.getFloatZ() - start.getFloatZ();

        float length = (float)Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
        setLength(Utils.floatToFixedPointInt(length, 4));
    }

    @Override
    public SVector getStartPosition() {
        return getStart();
    }
}
