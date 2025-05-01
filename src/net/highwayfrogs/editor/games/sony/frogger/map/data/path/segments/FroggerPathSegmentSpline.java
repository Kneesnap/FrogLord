package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve;
import net.highwayfrogs.editor.games.sony.shared.spline.MRSplineMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Holds Spline segment data.
 * Not entirely sure how we'll edit much of this data.
 * This could be using polynomial curves to form a shape. https://en.wikipedia.org/wiki/Spline_(mathematics)
 * Created by Kneesnap on 9/16/2018.
 */
public class FroggerPathSegmentSpline extends FroggerPathSegment {
    private final MRSplineMatrix splineMatrix = new MRSplineMatrix(null);
    private final int[] smoothT = new int[4]; // Smooth T is the distance values to reach each point. (Translation)
    private final int[][] smoothC = new int[4][3]; // Smoothing coefficient data.
    private final transient MRBezierCurve tempBezierCurve;

    private static final int SPLINE_FIX_INTERVAL = 0x200;

    public FroggerPathSegmentSpline(FroggerPath path) {
        super(path, FroggerPathSegmentType.SPLINE);
        this.tempBezierCurve = new MRBezierCurve(getGameInstance());
    }

    @Override
    protected void loadData(DataReader reader) {
        // Read MR_SPLINE_MATRIX:
        this.splineMatrix.load(reader);

        // Read ps_smooth_t
        for (int i = 0; i < this.smoothT.length; i++)
            this.smoothT[i] = reader.readInt();

        // Read ps_smooth_c:
        for (int i = 0; i < this.smoothC.length; i++)
            for (int j = 0; j < this.smoothC[i].length; j++)
                this.smoothC[i][j] = reader.readInt();
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.splineMatrix.save(writer);

        // Write ps_smooth_t
        for (int i = 0; i < this.smoothT.length; i++)
            writer.writeInt(this.smoothT[i]);

        // Write ps_smooth_c:
        for (int i = 0; i < this.smoothC.length; i++)
            for (int j = 0; j < this.smoothC[i].length; j++)
                writer.writeInt(this.smoothC[i][j]);
    }

    @Override
    public FroggerPathResult calculatePosition(int segmentDistance) {
        return new FroggerPathResult(calculateSplinePoint(segmentDistance), calculateSplineTangent(segmentDistance));
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
    public void onManualLengthUpdate(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        getPath().setupEditor(pathPreview, editor);
    }

    @Override
    public int calculateFixedPointLength() {
        // TODO: Reimplement.
        // The following just ends up generating the same value each time.
        // That's because what actually determines the length of the spline seems to depend on the smoothing spline, which we don't support yet.
        // I've commented this out despite it seemingly mostly valid, because this is slow.
        /*final int maxLength = 1000 * (1 << 4);
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

        return bestLength;*/
        return getLength();
    }

    @Override
    protected String getCalculatedIncorrectLengthString() {
        return null;
    }

    @Override
    protected int getIncorrectLengthTolerance() {
        // TODO: ?
        return 5; // TODO: RANDOM
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
    public void setupEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        super.setupEditor(pathPreview, editor);

        MRBezierCurve curve = convertToBezierCurve();
        editor.addFloatVector("Start", curve.getStart(), () -> loadFromCurve(curve, pathPreview), pathPreview.getController(),
                (vector, bits) -> selectPathPosition(pathPreview, vector, bits, () -> loadFromCurve(curve, pathPreview)));
        editor.addFloatVector("Control 1", curve.getControl1(), () -> loadFromCurve(curve, pathPreview), pathPreview.getController(),
                (vector, bits) -> selectPathPosition(pathPreview, vector, bits, () -> loadFromCurve(curve, pathPreview)));
        editor.addFloatVector("Control 2", curve.getControl2(), () -> loadFromCurve(curve, pathPreview), pathPreview.getController(),
                (vector, bits) -> selectPathPosition(pathPreview, vector, bits, () -> loadFromCurve(curve, pathPreview)));
        editor.addFloatVector("End", curve.getEnd(), () -> loadFromCurve(curve, pathPreview), pathPreview.getController(),
                (vector, bits) -> selectPathPosition(pathPreview, vector, bits, () -> loadFromCurve(curve, pathPreview)));
        /*makeTEditor(pathPreview, editor);

        editor.addBoldLabel("Smooth C:"); //TODO: Make a real editor.
        for (int i = 0; i < this.smoothC.length; i++) {
            final int index1 = i;
            for (int j = 0; j < this.smoothC[i].length; j++) {
                final int index2 = j;
                editor.addSignedIntegerField(i + "," + j, this.smoothC[i][j], newVal -> {
                    this.smoothC[index1][index2] = newVal;
                    onUpdate(pathPreview);
                });
            }
        }*/
    }

    private final SVector pendingDeltaMovement = new SVector();
    private static final int DELTA_MOVEMENT_INTERVAL = (1 << MRSplineMatrix.SPLINE_WORLD_SHIFT); // 0.5

    @Override
    public void moveDelta(SVector delta) {
        // NOTE: MR Bézier curves are only capable of representing decimal numbers in intervals of 0.5.
        // So, if your delta X is -0.0625, the amount actually moved is -0.5.
        // So, if your delta X is 0.0625, the amount actually moved is 0.
        // So, if your delta X is 0.5625, the amount actually moved is 0.5.

        // Our solution to this is to track the delta movements in a separate SVector, and apply them only as they reach 0.5 or greater.
        this.pendingDeltaMovement.add(delta);
        int moveX = this.pendingDeltaMovement.getX() / DELTA_MOVEMENT_INTERVAL;
        int moveY = this.pendingDeltaMovement.getY() / DELTA_MOVEMENT_INTERVAL;
        int moveZ = this.pendingDeltaMovement.getZ() / DELTA_MOVEMENT_INTERVAL;
        if (moveX == 0 && moveY == 0 && moveZ == 0)
            return; // No change yet.

        // Calculate how much to move this time (and for future movements)
        short remainderX = (short) (this.pendingDeltaMovement.getX() % DELTA_MOVEMENT_INTERVAL);
        short remainderY = (short) (this.pendingDeltaMovement.getY() % DELTA_MOVEMENT_INTERVAL);
        short remainderZ = (short) (this.pendingDeltaMovement.getZ() % DELTA_MOVEMENT_INTERVAL);
        this.pendingDeltaMovement.setValues((short) (moveX * DELTA_MOVEMENT_INTERVAL),
                (short) (moveY * DELTA_MOVEMENT_INTERVAL),
                (short) (moveZ * DELTA_MOVEMENT_INTERVAL));

        // Apply the movement to the curve.
        MRBezierCurve bezierCurve = this.splineMatrix.toBezierCurve(this.tempBezierCurve);
        bezierCurve.getStart().add(this.pendingDeltaMovement);
        bezierCurve.getEnd().add(this.pendingDeltaMovement);
        bezierCurve.getControl1().add(this.pendingDeltaMovement);
        bezierCurve.getControl2().add(this.pendingDeltaMovement);
        bezierCurve.toSplineMatrix(this.splineMatrix);

        // Store the remainder for future movements.
        this.pendingDeltaMovement.setValues(remainderX, remainderY, remainderZ);
    }

    @Override
    public void flip() {
        MRBezierCurve bezierCurve = this.splineMatrix.toBezierCurve(this.tempBezierCurve);
        short tempX = bezierCurve.getStart().getX();
        short tempY = bezierCurve.getStart().getY();
        short tempZ = bezierCurve.getStart().getZ();
        bezierCurve.getStart().setValues(bezierCurve.getEnd());
        bezierCurve.getEnd().setValues(tempX, tempY, tempZ);

        tempX = bezierCurve.getControl1().getX();
        tempY = bezierCurve.getControl1().getY();
        tempZ = bezierCurve.getControl1().getZ();
        bezierCurve.getControl1().setValues(bezierCurve.getControl2());
        bezierCurve.getControl2().setValues(tempX, tempY, tempZ);

        // Convert back to spline matrix.
        bezierCurve.toSplineMatrix(this.splineMatrix);
    }

    @Override
    public void setupNewSegment() {
        SVector start;

        FroggerPath path = getPath();
        if (path.getSegments().isEmpty()) {
            start = new SVector();
        } else {
            FroggerPathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
            start = lastSegment.calculatePosition(lastSegment.getLength()).getPosition();
        }

        SVector cp1 = new SVector(start).add(new SVector(400, 0, 400));
        SVector cp2 = new SVector(start).add(new SVector(-400, 0, 400));
        SVector end = new SVector(start).add(new SVector(0, 0, 800));
        loadFromCurve(new MRBezierCurve(null, start, cp1, cp2, end), null);
        setLength(null, DataUtils.floatToFixedPointInt4Bit(48F));
        loadSmoothT(new float[]{.25F, .5F, .75F, 1F});
        this.smoothC[0][2] = 45000000; // IDK, MasterWario set these. I should probably fix it.,
        this.smoothC[1][2] = 45000000;
        this.smoothC[2][2] = 45000000;
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
    public void loadFromCurve(MRBezierCurve curve, FroggerPathPreview pathPreview) {
        // TODO: Better editor.
        this.pendingDeltaMovement.clear(); // When setting values directly, pending delta movement is no longer valid.
        curve.toSplineMatrix(this.splineMatrix);
        onUpdate(pathPreview);
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

    private void makeTEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        editor.addBoldLabel("Segment Length Percentages:");
        float[] smoothingT = calculateSmoothing();
        for (int i = 0; i < this.smoothT.length; i++) {
            final int index = i;
            editor.addFloatField("Segment #" + (i + 1) + ":", smoothingT[i], newValue -> {
                smoothingT[index] = newValue;
                loadSmoothT(smoothingT);
                onUpdate(pathPreview);
            }, newValue -> newValue >= 0F && newValue <= 1.1F);
        }
    }
}