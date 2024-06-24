package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import javafx.scene.control.TextField;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCMath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents PATH_ARC.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
@Setter
public class FroggerPathSegmentArc extends FroggerPathSegment {
    private final SVector start = new SVector();
    private final SVector center = new SVector();
    private final SVector normal = new SVector();
    private int pitch; // Delta Y in helix frame. (Can be opposite direction of normal)
    private transient float distance = (float) (Math.PI / 2); // Length of arc (in radians).

    public FroggerPathSegmentArc(FroggerPath path) {
        super(path, FroggerPathSegmentType.ARC);
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start.loadWithPadding(reader);
        this.center.loadWithPadding(reader);
        this.normal.loadWithPadding(reader);
        int readRadius = reader.readInt();
        this.pitch = reader.readInt();

        int calculatedRadius = calculateFixedRadius();
        int diff = readRadius - calculatedRadius;
        if (Math.abs(diff) >= 48) // TODO: Change this to 2 in the future.
            getLogger().warning("calculateFixedRadius() was too inaccurate! (Read Radius: " + readRadius + "/" + Utils.fixedPointIntToFloat4Bit(readRadius) + ", Calculated Radius: " + calculatedRadius + "/" + Utils.fixedPointIntToFloat4Bit(calculatedRadius) + ", Difference: " + diff + "/" + Utils.fixedPointIntToFloat4Bit(diff) + ").");

        this.distance = Utils.fixedPointIntToFloat4Bit(getLength()) / Utils.fixedPointIntToFloat4Bit(calculateFixedRadius());
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.center.saveWithPadding(writer);
        this.normal.saveWithPadding(writer);
        writer.writeInt(calculateFixedRadius());
        writer.writeInt(this.pitch);
    }

    @Override
    public FroggerPathResult calculatePosition(FroggerPathInfo info) {
        int segmentDistance = info.getSegmentDistance();

        IVector vec = new IVector(start.getX() - center.getX(), start.getY() - center.getY(), start.getZ() - center.getZ());
        final IVector vec2 = new IVector(normal.getX(), normal.getY(), normal.getZ());
        IVector vec3 = new IVector();
        SVector svec = new SVector();

        vec.normalise();                // Equivalent to MRNormaliseVEC ?? <- Check this! [AndyEder]
        vec3.outerProduct12(vec, vec2); // Equivalent to MROuterProduct12 ?? <- Check this! [AndyEder]

        PSXMatrix matrix = new PSXMatrix();
        matrix.getMatrix()[0][0] = (short) vec.getX();
        matrix.getMatrix()[1][0] = (short) vec.getY();
        matrix.getMatrix()[2][0] = (short) vec.getZ();
        matrix.getMatrix()[0][1] = (short) -vec2.getX();
        matrix.getMatrix()[1][1] = (short) -vec2.getY();
        matrix.getMatrix()[2][1] = (short) -vec2.getZ();
        matrix.getMatrix()[0][2] = (short) -vec3.getX();
        matrix.getMatrix()[1][2] = (short) -vec3.getY();
        matrix.getMatrix()[2][2] = (short) -vec3.getZ();

        int radius = calculateFixedRadius();
        final int c = radius * 0x6487;
        final int t = (segmentDistance << 12) / c;
        final int a = ((segmentDistance << 18) - (t * c)) / (radius * 0x192);

        int cos = SCMath.rcos(a);
        int sin = SCMath.rsin(a);
        svec.setX((short) ((cos * radius) >> 12));
        svec.setY((short) ((-getPitch() * segmentDistance) / getLength()));
        svec.setZ((short) ((sin * radius) >> 12));

        PSXMatrix.MRApplyRotMatrix(matrix, svec, vec);
        vec.add(this.center);
        svec.setValues((short) -sin, (short) 0, (short) cos);

        return new FroggerPathResult(new SVector(vec), PSXMatrix.MRApplyRotMatrix(matrix, svec, new IVector()));
    }

    @Override
    public void recalculateLength() {
        setLength(Utils.floatToFixedPointInt4Bit(Utils.fixedPointIntToFloat4Bit(calculateFixedRadius()) * this.distance));
    }

    @Override
    public SVector getStartPosition() {
        return getStart();
    }

    @Override
    public void setupEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        super.setupEditor(pathPreview, editor);

        editor.addDoubleSlider("Arc Length", this.distance, newDistance -> {
            this.distance = (float) (double) newDistance;
            onUpdate(pathPreview);
        }, 0, 2 * Math.PI);

        TextField radiusField = editor.addFloatField("Arc Radius:", Utils.fixedPointIntToFloat4Bit(calculateFixedRadius()), null, null); // Read-Only.
        editor.addFloatVector("Start:", getStart(), () -> {
            onUpdate(pathPreview);
            radiusField.setText(String.valueOf(Utils.fixedPointIntToFloat4Bit(calculateFixedRadius())));
        }, pathPreview.getController());
        editor.addFloatVector("Center:", getCenter(), () -> {
            onUpdate(pathPreview);
            radiusField.setText(String.valueOf(Utils.fixedPointIntToFloat4Bit(calculateFixedRadius())));
        }, pathPreview.getController());
        editor.addSVector("Normal:", 12, getNormal(), () -> onUpdate(pathPreview));

        editor.addFloatField("Pitch:", Utils.fixedPointIntToFloat4Bit(getPitch()), newValue -> {
            this.pitch = Utils.floatToFixedPointInt4Bit(newValue);
            onUpdate(pathPreview);
        }, null);
    }

    /**
     * Calculates the radius for this segment.
     * TODO: Make accurate.
     * NOTE: This isn't full accurate, but it's close enough for now.
     * @return radius
     */
    public int calculateFixedRadius() {
        double xDiff = (this.start.getFloatX() - this.center.getFloatX());
        double yDiff = (this.start.getFloatY() - this.center.getFloatY());
        double zDiff = (this.start.getFloatZ() - this.center.getFloatZ());
        return Utils.floatToFixedPointInt4Bit((float) Math.sqrt((xDiff * xDiff) + (zDiff * zDiff) + (yDiff * yDiff)));
    }

    @Override
    public void setupNewSegment() {
        FroggerPath path = getPath();
        if (path.getSegments().size() > 0) {
            FroggerPathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
            this.start.setValues(lastSegment.calculatePosition(lastSegment.getLength()).getPosition());
        }

        this.pitch = 0;
        this.center.setValues(this.start);
        this.center.add(new SVector(-400, 0, -400));
        this.normal.setFloatY(-1F, 12);
        onUpdate(null);
    }
}