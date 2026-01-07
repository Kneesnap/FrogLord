package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
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
        return (((((((
                (((this.smoothC[i][0] >> MRSplineMatrix.SPLINE_PARAM_SHIFT) * e) >> 13)
                + (this.smoothC[i][1] >> MRSplineMatrix.SPLINE_PARAM_SHIFT) * e) >> 13)
                + (this.smoothC[i][2] >> MRSplineMatrix.SPLINE_PARAM_SHIFT) * e) >> 13) >> 5)
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

    /**
     * Calculates linear smoothing for the given path.
     * TODO: This is not consistent with the original behavior, we need a better system.
     */
    public void calculateLinearSmoothing() {
        // Generate linear smoothing curve to update the spline.
        calculateSmoothT();
        int newSmoothingConstant = ((SPLINE_FIX_INTERVAL << 21) / Math.max(32, getLength())) << 5;
        for (int i = 0; i < this.smoothC.length; i++) {
            Arrays.fill(this.smoothC[i], 0);
            this.smoothC[i][2] = newSmoothingConstant;
        }
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

        makeSmoothingCurveEditor(pathPreview, editor);
    }

    @SuppressWarnings("CommentedOutCode")
    private void makeSmoothingCurveEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        // TODO: We'll need a real smoothing curve editor in the future. Refer to Blender's RGB Curve Shader node for an example of what this should probably look like.
        editor.addBoldLabel("Smoothing Curve (Not Editable Yet):");
        editor.addCenteredImageView(createImageView());

        /*editor.addBoldLabel("Segment Length Percentages:");
        float[] smoothingT = calculateSmoothingAsPercentages();
        for (int i = 0; i < this.smoothT.length; i++) {
            final int index = i;
            editor.addFloatField("Segment #" + (i + 1) + ":", smoothingT[i], newValue -> {
                smoothingT[index] = newValue;
                //loadSmoothT(smoothingT);
                onUpdate(pathPreview);
            }, newValue -> newValue >= 0F && newValue <= 1.1F);
        }

        editor.addBoldLabel("Smooth C:");
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


    /**
     * Calculates T smoothing percentages.
     * @return tPercentages
     */
    @SuppressWarnings("unused")
    private float[] calculateSmoothingAsPercentages() {
        float length = DataUtils.fixedPointIntToFloat4Bit(getLength());
        float[] result = new float[4];
        for (int i = 0; i < result.length; i++)
            result[i] = DataUtils.fixedPointIntToFloatNBits(this.smoothT[i], 12) / length;
        return result;
    }

    private ImageView createImageView() {
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
        calculateLinearSmoothing(); // Call after recalculating the length, but before updating the path preview.
        if (pathPreview != null)
            pathPreview.updatePath();
    }

    /**
     * Recalculates smooth T values.
     */
    public void calculateSmoothT() {
        float length = DataUtils.fixedPointIntToFloat4Bit(getLength());
        for (int i = 0; i < this.smoothT.length; i++)
            this.smoothT[i] = DataUtils.floatToFixedPointInt((length * (i + 1) * .25F), 12);
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