package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents PATH_LINE.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class FroggerPathSegmentLine extends FroggerPathSegment {
    private final SVector start = new SVector();
    private final SVector end = new SVector();

    public FroggerPathSegmentLine(FroggerPath path) {
        super(path, FroggerPathSegmentType.LINE);
    }

    @Override
    public void setupNewSegment() {
        FroggerPath path = getPath();
        if (path.getSegments().size() > 0) {
            FroggerPathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
            this.start.setValues(lastSegment.calculatePosition(lastSegment.getLength()).getPosition());
        }

        this.end.setValues(this.start);
        this.end.add(new SVector(0, 0, 800));
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
    public FroggerPathResult calculatePosition(FroggerPathInfo info) {
        int deltaX = this.end.getX() - this.start.getX();
        int deltaY = this.end.getY() - this.start.getY();
        int deltaZ = this.end.getZ() - this.start.getZ();

        SVector result = new SVector();
        result.setX((short) (this.start.getX() + ((deltaX * info.getSegmentDistance()) / getLength())));
        result.setY((short) (this.start.getY() + ((deltaY * info.getSegmentDistance()) / getLength())));
        result.setZ((short) (this.start.getZ() + ((deltaZ * info.getSegmentDistance()) / getLength())));
        return new FroggerPathResult(result, new IVector(deltaX, deltaY, deltaZ).normalise());
    }

    @Override
    public void setupEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        super.setupEditor(pathPreview, editor);
        editor.addFloatVector("Start:", getStart(), () -> onUpdate(pathPreview), pathPreview.getController());
        editor.addFloatVector("End:", getEnd(), () -> onUpdate(pathPreview), pathPreview.getController());
    }

    @Override
    public void recalculateLength() {
        float deltaX = this.end.getFloatX() - this.start.getFloatX();
        float deltaY = this.end.getFloatY() - this.start.getFloatY();
        float deltaZ = this.end.getFloatZ() - this.start.getFloatZ();

        float length = (float)Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
        setLength(Utils.floatToFixedPointInt4Bit(length));
    }

    @Override
    public SVector getStartPosition() {
        return getStart();
    }
}