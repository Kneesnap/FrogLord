package net.highwayfrogs.editor.file.map.path.data;

import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.path.*;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegmentSpline;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve;
import net.highwayfrogs.editor.games.sony.shared.spline.MRSplineMatrix;
import net.highwayfrogs.editor.utils.DataUtils;

/**
 * Holds Spline segment data.
 * Not entirely sure how we'll edit much of this data.
 * This could be using polynomial curves to form a shape. https://en.wikipedia.org/wiki/Spline_(mathematics)
 * Created by Kneesnap on 9/16/2018.
 */
public class SplineSegment extends PathSegment {
    private final MRSplineMatrix splineMatrix = new MRSplineMatrix(null);
    private final int[] smoothT = new int[4]; // Smooth T is the distance values to reach each point. (Translation)
    private final int[][] smoothC = new int[4][3]; // Smoothing coefficient data.

    private static final int SPLINE_FIX_INTERVAL = 0x200;

    public SplineSegment(Path path) {
        super(path, PathType.SPLINE);
    }

    @Override
    protected void loadData(DataReader reader) {
        // Read MR_SPLINE_MATRIX:
        this.splineMatrix.load(reader);

        // Read ps_smooth_t
        for (int i = 0; i < smoothT.length; i++)
            this.smoothT[i] = reader.readInt();

        // Read ps_smooth_c:
        for (int i = 0; i < smoothC.length; i++)
            for (int j = 0; j < smoothC[i].length; j++)
                this.smoothC[i][j] = reader.readInt();
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.splineMatrix.save(writer);

        for (int val : this.smoothT)
            writer.writeInt(val);

        for (int i = 0; i < smoothC.length; i++)
            for (int j = 0; j < smoothC[i].length; j++)
                writer.writeInt(this.smoothC[i][j]);
    }

    @Override
    public PathResult calculatePosition(PathInfo info) {
        return new PathResult(calculateSplinePoint(info.getSegmentDistance()), calculateSplineTangent(info.getSegmentDistance()));
    }

    // What follows is insanely nasty, but it is what the game engine does, so we have no choice...
    private int getSplineParamFromLength(int length) {
        length <<= 5;
        int d = length;

        int i;
        for (i = 3; i > 0; i--) {
            d = length - (this.smoothT[i - 1] >> MRSplineMatrix.SPLINE_WORLD_SHIFT);
            if (d >= 0)
                break;
        }

        if (i == 0)
            d = length;

        int e = d;
        d = 0;
        d += (this.smoothC[i][0] >> MRSplineMatrix.SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;

        d += (this.smoothC[i][1] >> MRSplineMatrix.SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;

        d += (this.smoothC[i][2] >> MRSplineMatrix.SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;
        d >>= 5;

        d += (i * SPLINE_FIX_INTERVAL);
        return (d << 1) >> 1;
    }

    // I hate this.
    private SVector calculateSplinePoint(int distance) {
        // TODO: TOSS (Maybe)
        int t = getSplineParamFromLength(distance);
        return this.splineMatrix.evaluatePosition(t);
    }

    private SVector calculateSplinePosition(int t) {
        // TODO: TOSS
        return this.splineMatrix.evaluatePosition(t);
    }

    private IVector calculateSplineTangent(int distance) {
        // TODO: TOSS (Maybe)
        int t = getSplineParamFromLength(distance);
        return this.splineMatrix.calculateTangentLine(t);
    }

    @Override
    public SVector getStartPosition() {
        return getStartPosition(null);
    }

    @Override
    public FroggerPathSegmentSpline convertToNewFormat(FroggerPath newPath) {
        FroggerPathSegmentSpline newSegment = new FroggerPathSegmentSpline(newPath);
        return MAPFile.copyToNewViaBytes(this, newSegment);
    }

    /**
     * Gets the start position of this spline segment.
     * @param result The vector to store the start position.
     * @return startPosition
     */
    public SVector getStartPosition(SVector result) {
        return this.splineMatrix.evaluatePosition(result, 0);
    }

    /**
     * Gets the end position of this spline segment.
     * @return endPosition
     */
    public SVector getEndPosition() {
        return getEndPosition(null);
    }

    /**
     * Gets the end position of this spline segment.
     * @param result The vector to store the end position.
     * @return endPosition
     */
    public SVector getEndPosition(SVector result) {
        return this.splineMatrix.evaluatePosition(result, MRSplineMatrix.SPLINE_PARAM_ONE);
    }

    @Override
    public void recalculateLength() {
        // TODO: Reimplement.
        // We leave this up to user input, since I've yet to come up with an algorithm which is accurate enough to get this right,
        // and it seems like just leaving it as-is even during changes will create valid results.
        final int maxLength = 1000 * (1 << 4);
        int bestLength = 0;
        double bestDistance = Double.MAX_VALUE;

        SVector endPoint = convertToBezierCurve().getEnd();
        for (int testLength = 0; testLength < maxLength; testLength++) {
            // ? this.setLength(testLength);
            SVector point = calculateSplinePoint(testLength);
            double distanceSq = point.distanceSquared(endPoint);
            if (distanceSq < bestDistance) {
                bestDistance = distanceSq;
                bestLength = testLength;
            } else if (bestDistance <= 10 && distanceSq > 10) {
                break;
            }
        }

        this.setLength(bestLength);
    }

    // TODO: TOSS
    private double calculateLengthWithOffset(int increment) {
        double hiDefLength = 0;
        SVector lastPos = calculateSplinePosition(0);
        for (int i = increment; i < 2048 + increment; i += increment) {
            SVector curPos = calculateSplinePosition(i);
            hiDefLength += Math.sqrt(curPos.distanceSquared(lastPos));
            lastPos = curPos;
        }

        return hiDefLength;
    }

    // TODO: TOSS
    private double test(int startT, int currentT, int segments) {
        SVector lastPos = calculateSplinePoint(startT);
        double fullLength = 0;
        for (int i = 1; i <= segments; i++) {
            int tempT = startT + (int) Math.round(((double) (currentT - startT) / segments) * i);
            SVector currPos = calculateSplinePoint(tempT);
            fullLength += Math.sqrt(currPos.distanceSquared(lastPos));
            lastPos = currPos;
        }

        return fullLength;
    }

    @Override
    public void setupNewSegment(MAPFile map) {
        SVector start;

        Path path = getPath();
        if (path.getSegments().isEmpty()) {
            start = new SVector();
        } else {
            PathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
            start = lastSegment.calculatePosition(map, lastSegment.getLength()).getPosition();
        }

        SVector cp1 = new SVector(start).add(new SVector(400, 0, 400));
        SVector cp2 = new SVector(start).add(new SVector(-400, 0, 400));
        SVector end = new SVector(start).add(new SVector(0, 0, 800));
        loadFromCurve(new MRBezierCurve(null, start, cp1, cp2, end));
        loadSmoothT(new float[]{.25F, .5F, .75F, 1F});
        //TODO: Make Smooth C. Calculate length too.
    }

    /**
     * Calculates T smoothing percentages.
     * @return tPercentages
     */
    public float[] calculateSmoothing() {
        float length = DataUtils.fixedPointIntToFloat4Bit(getLength());
        float[] result = new float[4];
        for (int i = 0; i < result.length; i++)
            result[i] = DataUtils.fixedPointIntToFloatNBits(this.smoothT[i], 12) / length;
        return result;
    }

    /**
     * Loads smooth T values, or the percentage
     * @param smoothingT The amount of distance each segment takes up. [0,1]
     */
    public void loadSmoothT(float[] smoothingT) {
        float length = DataUtils.fixedPointIntToFloat4Bit(getLength());
        for (int i = 0; i < this.smoothT.length; i++)
            this.smoothT[i] = DataUtils.floatToFixedPointInt((length * smoothingT[i]), 12);
    }

    /**
     * Loads curve data from a bezier curve.
     * @param curve The curve to load data from.
     */
    public void loadFromCurve(MRBezierCurve curve) {
        // TODO: Better editor.
        curve.toSplineMatrix(this.splineMatrix);
    }

    /**
     * Convert this to a Bézier curve.
     * This function has been carefully written to maximize decimal accuracy during conversion.
     * @return bezierCurve
     */
    public MRBezierCurve convertToBezierCurve() {
        // TODO: Probably doesn't need to exist.
        return this.splineMatrix.toBezierCurve();
    }
}