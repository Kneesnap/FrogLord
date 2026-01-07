package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.MathUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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
    public FroggerPathResult calculatePosition(int segmentDistance) {
        int deltaX = this.end.getX() - this.start.getX();
        int deltaY = this.end.getY() - this.start.getY();
        int deltaZ = this.end.getZ() - this.start.getZ();

        int segmentLength = getLength();
        SVector result = new SVector();
        result.setX((short) (this.start.getX() + (segmentLength != 0 ? ((deltaX * segmentDistance) / segmentLength) : 0)));
        result.setY((short) (this.start.getY() + (segmentLength != 0 ? ((deltaY * segmentDistance) / segmentLength) : 0)));
        result.setZ((short) (this.start.getZ() + (segmentLength != 0 ? ((deltaZ * segmentDistance) / segmentLength) : 0)));
        return new FroggerPathResult(result, new IVector(deltaX, deltaY, deltaZ).normalise());
    }

    @Override
    public void moveDelta(SVector delta) {
        this.start.setX((short) (this.start.getX() + delta.getX()));
        this.start.setY((short) (this.start.getY() + delta.getY()));
        this.start.setZ((short) (this.start.getZ() + delta.getZ()));
        this.end.setX((short) (this.end.getX() + delta.getX()));
        this.end.setY((short) (this.end.getY() + delta.getY()));
        this.end.setZ((short) (this.end.getZ() + delta.getZ()));
    }

    @Override
    public void flip() {
        short tempX = this.end.getX();
        short tempY = this.end.getY();
        short tempZ = this.end.getZ();
        this.end.setValues(this.start);
        this.start.setValues(tempX, tempY, tempZ);
    }

    @Override
    public void setupEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        super.setupEditor(pathPreview, editor);
        editor.addFloatVector("Start", getStart(), () -> onUpdate(pathPreview), pathPreview.getController(),
                (vector, bits) -> selectPathPosition(pathPreview, vector, bits, null));
        editor.addFloatVector("End", getEnd(), () -> onUpdate(pathPreview), pathPreview.getController(),
                (vector, bits) -> selectPathPosition(pathPreview, vector, bits, null));
    }

    @Override
    public int calculateFixedPointLength() {
        // Worst case, this value is off by one from the original value. Most likely the original code used floating point numbers, and we got the fixed point rounded down numbers.
        // So, the original numbers had enough extra precision to account for one length unit (0.0625) of difference in the original calculations.
        int deltaX = this.end.getX() - this.start.getX();
        int deltaY = this.end.getY() - this.start.getY();
        int deltaZ = this.end.getZ() - this.start.getZ();
        return MathUtils.fixedSqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
    }

    @Override
    protected String getCalculatedIncorrectLengthString() {
        return "Start: " + this.start + ", End: " + this.end;
    }

    @Override
    protected int getIncorrectLengthTolerance() {
        // Worst case, this value is off by one from the original value. Most likely the original code used floating point numbers, and we got the fixed point rounded down numbers.
        // So, the original numbers had enough extra precision to account for one length unit (0.0625) of difference in the original calculations.
        return 1;
    }

    @Override
    public SVector getStartPosition() {
        return getStart();
    }
}