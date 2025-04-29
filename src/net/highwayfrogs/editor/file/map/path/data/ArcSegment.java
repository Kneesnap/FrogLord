package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.path.*;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCMath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegmentArc;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents PATH_ARC.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
@Setter
public class ArcSegment extends PathSegment {
    private SVector start = new SVector();
    private SVector center = new SVector();
    private SVector normal = new SVector();
    private int pitch; // Delta Y in helix frame. (Can be opposite direction of normal)
    private transient float distance = (float) (Math.PI / 2); // Length of arc (in radians).

    public ArcSegment(Path path) {
        super(path, PathType.ARC);
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start.loadWithPadding(reader);
        this.center.loadWithPadding(reader);
        this.normal.loadWithPadding(reader);
        int readRadius = reader.readInt();
        this.pitch = reader.readInt();

        float diff = Math.abs(DataUtils.fixedPointIntToFloat4Bit(readRadius - getRadius()));
        if (diff >= 3)
            getPath().getMapFile().getLogger().warning("getRadius() calculation was too inaccurate in ArcSegment! (%f).", diff);

        this.distance = DataUtils.fixedPointIntToFloat4Bit(getLength()) / DataUtils.fixedPointIntToFloat4Bit(getRadius());
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.center.saveWithPadding(writer);
        this.normal.saveWithPadding(writer);
        writer.writeInt(getRadius());
        writer.writeInt(this.pitch);
    }

    @Override
    public PathResult calculatePosition(PathInfo info) {
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

        int radius = getRadius();
        final int c = radius * 0x6487;
        final int t = (segmentDistance << 12) / c;
        final int a = ((segmentDistance << 18) - (t * c)) / (radius * 0x192);

        int cos = SCMath.rcos(a);
        int sin = SCMath.rsin(a);
        svec.setX((short) ((cos * radius) >> 12));
        svec.setY((short) ((-getPitch() * segmentDistance) / getLength()));
        svec.setZ((short) ((sin * radius) >> 12));

        PSXMatrix.MRApplyRotMatrix(matrix, svec, vec);
        vec.add(center);
        svec.setValues((short) -sin, (short) 0, (short) cos);

        return new PathResult(new SVector(vec), PSXMatrix.MRApplyRotMatrix(matrix, svec, new IVector()));
    }

    @Override
    public void recalculateLength() {
        setLength(DataUtils.floatToFixedPointInt4Bit(DataUtils.fixedPointIntToFloat4Bit(getRadius()) * this.distance));
    }

    @Override
    public SVector getStartPosition() {
        return getStart();
    }

    @Override
    public FroggerPathSegment convertToNewFormat(FroggerPath newPath) {
        FroggerPathSegmentArc newSegment = new FroggerPathSegmentArc(newPath);
        newSegment.setLength(null, getLength());
        newSegment.getStart().setValues(this.start);
        newSegment.getCenter().setValues(this.center);
        newSegment.getNormal().setValues(this.normal);
        newSegment.setPitch(this.pitch);
        newSegment.setAngle(((getLength() - Math.abs(this.pitch)) / (2 * SCMath.MAPPY_PI_HALF16 * getRadius())));
        return newSegment;
    }

    /**
     * Gets the radius for this segment.
     * NOTE: This isn't full accurate, but it's close enough for now.
     * @return radius
     */
    public int getRadius() {
        double xDiff = (start.getFloatX() - center.getFloatX());
        double yDiff = (start.getFloatY() - center.getFloatY());
        double zDiff = (start.getFloatZ() - center.getFloatZ());
        return DataUtils.floatToFixedPointInt4Bit((float) Math.sqrt((xDiff * xDiff) + (zDiff * zDiff) + (yDiff * yDiff)));
    }

    @Override
    public void setupNewSegment(MAPFile map) {
        Path path = getPath();
        if (path.getSegments().size() > 0) {
            PathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
            this.start = lastSegment.calculatePosition(map, lastSegment.getLength()).getPosition();
        }

        this.pitch = 0;
        this.center = new SVector(this.start).add(new SVector(-400, 0, -400));
        this.normal.setFloatY(-1F, 12);
    }
}