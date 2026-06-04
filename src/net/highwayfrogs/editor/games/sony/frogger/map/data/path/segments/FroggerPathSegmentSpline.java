package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerSpline;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve;
import net.highwayfrogs.editor.games.sony.shared.spline.MRSplineMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Holds Spline segment data.
 * Not entirely sure how we'll edit much of this data.
 * This could be using polynomial curves to form a shape.
 * Reference: <a href="https://en.wikipedia.org/wiki/Spline_(mathematics)"/>
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
    @SuppressWarnings({"CommentedOutCode", "ExtractMethodRecommender"})
    private int getSplineParamFromLength(int length) {
        length <<= 5; // world shift?
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
        d >>= 5; // world shift.

        d += (i * SPLINE_FIX_INTERVAL);
        return (d << 1) >> 1; // Happens outside the function in the original.

        /*
        return ((((((((((
                (((this.smoothC[i][0] >> MRSplineMatrix.SPLINE_PARAM_SHIFT)) * e) >> 13)
                + (this.smoothC[i][1] >> MRSplineMatrix.SPLINE_PARAM_SHIFT)) * e) >> 13)
                + (this.smoothC[i][2] >> MRSplineMatrix.SPLINE_PARAM_SHIFT)) * e) >> 13) >> 5)
                + (i * SPLINE_FIX_INTERVAL)) << 1) >> 1;
         */
    }

    private SVector calculateSplinePoint(int distance) {
        int t = getSplineParamFromLength(distance);
        return this.splineMatrix.evaluatePosition(t);
    }

    private SVector calculateSplinePosition(int t) {
        return this.splineMatrix.evaluatePosition(t);
    }

    private IVector calculateSplineTangent(int distance) {
        int t = getSplineParamFromLength(distance);
        return this.splineMatrix.evaluateRotation(t);
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
    @SuppressWarnings("unused")
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
        SVector positionStart = calculateSplinePoint(0);
        SVector position1 = calculateSplinePosition(SPLINE_FIX_INTERVAL);
        SVector position2 = calculateSplinePosition(2 * SPLINE_FIX_INTERVAL);
        SVector position3 = calculateSplinePosition(3 * SPLINE_FIX_INTERVAL);
        SVector positionEnd = calculateSplinePosition(4 * SPLINE_FIX_INTERVAL);
        return DataUtils.floatToFixedPointInt4Bit((float) (Math.sqrt(positionStart.distanceSquared(position1))
                 + Math.sqrt(position1.distanceSquared(position2))
                 + Math.sqrt(position2.distanceSquared(position3))
                 + Math.sqrt(position3.distanceSquared(positionEnd)))) + 32; // Without the + 32 offset, all mismatches is off by no more than 70 units, with +32, everything is off by no more than 38 units.
    }

    @Override
    protected String getCalculatedIncorrectLengthString() {
        return null;
    }

    @Override
    protected int getIncorrectLengthTolerance() {
        return 38; // The maximum differences are off by less than 2.375 world units. (Which is just over an eighth of a grid square.)
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

        // The user doesn't really need to see this or even know it exists, this is just around for debugging purposes.
        /*editor.addBoldLabel("Smoothing Curve:");
        editor.addCenteredImageView(createSmoothingCurveImageView());*/
    }

    @SuppressWarnings("unused")
    private ImageView createSmoothingCurveImageView() {
        BufferedImage image = new BufferedImage(250, 250, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        int smoothTIndex = this.smoothT.length - 1;
        int[] pixelArray = ImageUtils.getWritablePixelIntegerArray(image);
        for (int i = 0; i < getLength(); i++) {
            boolean isSmoothTSwitch = false;
            if (smoothTIndex > 0 && (i << 5) > (this.smoothT[smoothTIndex - 1] >> MRSplineMatrix.SPLINE_WORLD_SHIFT)) {
                isSmoothTSwitch = true;
                smoothTIndex--;
            }

            int xPos = (i * image.getWidth()) / getLength();
            int yPos = image.getHeight() - (int) ((getSplineParamFromLength(i) / 2048D) * image.getHeight()) - 1;
            if (xPos >= 0 && yPos >= 0 && xPos < image.getWidth() && yPos < image.getHeight())
                pixelArray[(yPos * image.getWidth()) + xPos] = isSmoothTSwitch ? 0xFFFF0000 : 0xFF000000;
        }

        return new ImageView(SwingFXUtils.toFXImage(image, null));
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
    }

    @Override
    public void onUpdate(FroggerPathPreview pathPreview) {
        recalculateLength(pathPreview);
        calculateSmoothingCurve(); // Call after recalculating the length, but before updating the path preview.
        if (pathPreview != null)
            pathPreview.updatePath();
    }

    /**
     * Calculates proper arc-length smoothing coefficients for each of the four spline quarters.
     * This maps arc length to spline parameter t so entities traverse the spline at uniform world-space speed.
     *
     * Each quarter covers t ∈ [i*512, (i+1)*512] and fits the cubic f(e) = a·e³ + b·e² + c·e,
     * where e is arc-length offset in (world_units × 512) scale, f(0) = 0, f(eEnd) = 512.
     * The four segments are position-continuous (f(eEnd) = 512 enforced per quarter).
     *
     * The Horner evaluation in getSplineParamFromLength computes:
     *   d = smoothC[i][0]·e³/2⁵⁵ + smoothC[i][1]·e²/2⁴² + smoothC[i][2]·e/2²⁹
     * so the fixed-point coefficients are: C0 = a·2⁵⁵, C1 = b·2⁴², C2 = c·2²⁹.
     */
    public void calculateSmoothingCurve() {
        final int NUM_SAMPLES = 512; // must be divisible by 4
        final int SAMPLES_PER_QUARTER = NUM_SAMPLES / 4;

        // Build cumulative arc-length table.
        // arcLengths[k] = arc length at t = k * SPLINE_PARAM_ONE / NUM_SAMPLES, in world units.
        // distanceSquared() uses getFloatX() which divides raw short coords by 16, giving world units.
        double[] arcLengths = new double[NUM_SAMPLES + 1];
        arcLengths[0] = 0.0;
        SVector prevPos = this.splineMatrix.evaluatePosition(0);
        for (int k = 1; k <= NUM_SAMPLES; k++) {
            int t = k * MRSplineMatrix.SPLINE_PARAM_ONE / NUM_SAMPLES;
            SVector pos = this.splineMatrix.evaluatePosition(t);
            arcLengths[k] = arcLengths[k - 1] + Math.sqrt(prevPos.distanceSquared(pos));
            prevPos = pos;
        }

        // Normalize arc lengths so the total matches getLength()/16 (world units).
        // This ensures the last quarter's polynomial eEnd exactly matches the actual maximum input,
        // so the curve reaches SPLINE_PARAM_ONE = 2048 at distance = getLength().
        double totalArcLen = arcLengths[NUM_SAMPLES];
        if (totalArcLen > 0.0) {
            double scale = DataUtils.fixedPointIntToFloat4Bit(getLength()) / totalArcLen;
            for (int k = 1; k <= NUM_SAMPLES; k++)
                arcLengths[k] *= scale;
        }

        // smoothT[qi] = arc length at t = (qi+1)*SPLINE_FIX_INTERVAL, stored as 12-bit fixed point.
        // smoothT[3] (never used as a boundary threshold) is set to total arc length.
        for (int qi = 0; qi < this.smoothT.length; qi++)
            this.smoothT[qi] = (int) Math.round(arcLengths[(qi + 1) * SAMPLES_PER_QUARTER] * (1 << 12));


        // For each quarter, fit t_offset = a*e^3 + b*e^2 + c*e using constrained least squares.
        // Constraint f(eEnd) = SPLINE_FIX_INTERVAL is enforced by eliminating c:
        //   c = (SPLINE_FIX_INTERVAL - a*eEnd^3 - b*eEnd^2) / eEnd
        // Substituting: f(e) = a*(e^3 - eEnd^2*e) + b*(e^2 - eEnd*e) + SPLINE_FIX_INTERVAL*e/eEnd
        for (int qi = 0; qi < this.smoothC.length; qi++) {
            int startIdx = qi * SAMPLES_PER_QUARTER;
            double arcStart = arcLengths[startIdx];
            double arcEnd = arcLengths[startIdx + SAMPLES_PER_QUARTER];
            double eEnd = (arcEnd - arcStart) * (double) SPLINE_FIX_INTERVAL; // quarter arc length in (world_units * 512) scale

            if (eEnd < 1.0) { // degenerate quarter (zero or near-zero arc length)
                Arrays.fill(this.smoothC[qi], 0);
                continue;
            }

            double eEnd2 = eEnd * eEnd;
            double eEnd3 = eEnd2 * eEnd;
            double baseSlope = (double) SPLINE_FIX_INTERVAL / eEnd; // linear baseline ensuring f(eEnd)=512

            // Accumulate normal equations for 2x2 least-squares system [a, b]
            double sumUU = 0, sumUV = 0, sumVV = 0, sumUR = 0, sumVR = 0;
            for (int k = 1; k <= SAMPLES_PER_QUARTER; k++) {
                double e = (arcLengths[startIdx + k] - arcStart) * (double) SPLINE_FIX_INTERVAL;
                double tOffset = (double) k * SPLINE_FIX_INTERVAL / SAMPLES_PER_QUARTER;

                // Basis functions (zero at e=0, zero at e=eEnd after subtracting baseline)
                double u = e * e * e - e * eEnd2; // cubic basis
                double v = e * e - e * eEnd;       // quadratic basis
                double r = tOffset - baseSlope * e; // residual from linear baseline

                sumUU += u * u;
                sumUV += u * v;
                sumVV += v * v;
                sumUR += u * r;
                sumVR += v * r;
            }

            // Solve 2x2 normal equations via Cramer's rule
            double a = 0.0, b = 0.0;
            double det = sumUU * sumVV - sumUV * sumUV;
            if (det != 0.0) {
                a = (sumUR * sumVV - sumVR * sumUV) / det;
                b = (sumUU * sumVR - sumUV * sumUR) / det;
            }
            double c = ((double) SPLINE_FIX_INTERVAL - a * eEnd3 - b * eEnd2) / eEnd;

            // Convert polynomial coefficients to fixed-point smoothC values.
            // Horner eval: d = C0*e^3/2^55 + C1*e^2/2^42 + C2*e/2^29, so C0=a*2^55, C1=b*2^42, C2=c*2^29.
            this.smoothC[qi][0] = (int) Math.round(a * (double) (1L << 55)); // This has been tested, decimal precision is not an issue.
            this.smoothC[qi][1] = (int) Math.round(b * (double) (1L << 42));
            this.smoothC[qi][2] = (int) Math.round(c * (double) (1L << 29));
        }
    }

    /**
     * Loads curve data from a bezier curve.
     * @param curve The curve to load data from.
     */
    public void loadFromCurve(MRBezierCurve curve, FroggerPathPreview pathPreview) {
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
        return this.splineMatrix.toBezierCurve();
    }

    /**
     * Copies the spline data from an old Frogger spline to the object.
     * @param oldFroggerSpline the spline to copy data from
     */
    public void copyFrom(OldFroggerSpline oldFroggerSpline, SVector worldOffset) {
        if (oldFroggerSpline == null)
            throw new NullPointerException("oldFroggerSpline");

        setLength(null, oldFroggerSpline.calculateLength());

        // Converting directly produces a matrix which is broken for spline calculations.
        // However, the Bézier curve created from that matrix looks good.
        // So, we'll calculate a new matrix from the curve, and we're left with a valid spline matrix.
        // There's probably a less hacky way to do this, but I don't think it's worth figuring that out.
        MRSplineMatrix tempMatrix = oldFroggerSpline.toMatrix(this.splineMatrix);
        loadFromCurve(tempMatrix.toBezierCurve(), null);

        if (worldOffset != null)
            moveDelta(worldOffset);

        // Copy the data after conversion after moving.
        System.arraycopy(oldFroggerSpline.getSmoothT(), 0, this.smoothT, 0, this.smoothT.length);
        for (int i = 0; i < this.smoothC.length; i++)
            System.arraycopy(oldFroggerSpline.getSmoothC()[i], 0, this.smoothC[i], 0, this.smoothC[i].length);
    }
}