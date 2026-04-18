package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

import java.util.List;

/**
 * Represents math capabilities used by Sony Cambridge games.
 * Created by Kneesnap on 9/7/2023.
 */
public class SCMath {
    private static short[] COS_ENTRIES;
    private static short[] SIN_ENTRIES;
    private static short[] ACOS_ENTRIES;
    private static final int COSTABLE_ENTRIES = 4096;
    private static final int ACOSTABLE_ENTRIES = 8192;
    public static final int FIXED_POINT_ONE = 4096;
    public static final IVector GAME_Y_AXIS_POS = new IVector(0, SCMath.FIXED_POINT_ONE, 0);
    public static final int MR_QUAT_EPSILON = 1;

    // So... why is PI funky?
    // When reverse engineering the proper math for Frogger path calculations, I was just slightly off.
    // I was confident I had the right algorithm but the values I was getting were slightly off. Instead of 0.5 for the path arc angle I'd get 0.499984......
    // Eventually, I tried solving for PI. What I got was consistent across different path arcs, and makes the angles look accurate.
    // This made no sense UNTIL I searched GitHub. This number is the most accurate representation of PI for a 16 bit floating point number.
    // Soooo, I think this suggests mappy may have been using 16 bit floating point numbers for certain operations?? If so that... actually makes sense.
    // Some paths still don't calculate cleanly, but the fact that this is a real recognized stand-in for Pi and works across a good chunk of paths makes me think it's correct.
    // Though it's possible the ones which don't calculate cleanly were changed via GUI slider, much like the slider seen in FrogLord UI.
    // I think this is the case since many of the non-even paths don't look right if you put the clean number in.
    public static final float MAPPY_PI_HALF16 = 3.140625F;

    private static void readCosTable() {
        if (COS_ENTRIES != null && SIN_ENTRIES != null)
            return;

        // Make tables.
        COS_ENTRIES = new short[COSTABLE_ENTRIES];
        SIN_ENTRIES = new short[COSTABLE_ENTRIES];

        // Read data.
        DataReader reader = new DataReader(new ArraySource(FileUtils.readBytesFromStream(FileUtils.getResourceStream("games/sony/COSTABLE"))));
        for (int i = 0; i < COSTABLE_ENTRIES; i++) {
            SIN_ENTRIES[i] = reader.readShort();
            COS_ENTRIES[i] = reader.readShort();
        }
    }

    private static void readACosTable() {
        if (ACOS_ENTRIES != null)
            return;

        // Make tables.
        ACOS_ENTRIES = new short[ACOSTABLE_ENTRIES];

        // Read data.
        DataReader reader = new DataReader(new ArraySource(FileUtils.readBytesFromStream(FileUtils.getResourceStream("games/sony/ACOSTABLE"))));
        for (int i = 0; i < ACOSTABLE_ENTRIES; i++)
            ACOS_ENTRIES[i] = reader.readShort();
    }

    /**
     * Perform rCos on an angle.
     * @param angle The angle to perform rCos on.
     * @return rCos
     */
    public static short rcos(int angle) {
        readCosTable();
        return COS_ENTRIES[angle & 0xFFF]; // Angle is a fixed point number between where 4096 is the integer 1. The mask removes the integer part.
    }

    /**
     * Perform rSin on an angle.
     * @param angle The angle to perform rSin on.
     * @return rSin
     */
    public static short rsin(int angle) {
        readCosTable();
        return SIN_ENTRIES[angle & 0xFFF]; // & 0xFFF cuts off the decimal point.
    }

    /**
     * Attempts to match the behavior of MR_ACOS_RAW
     * @param angle the angle to resolve
     */
    public static short acosRaw(int angle) {
        readACosTable();
        return ACOS_ENTRIES[angle + 0x1000];
    }

    /**
     * Converts a byte quaternion to a matrix.
     * TODO: This has not been validated.
     * Represents MR_QUATB_TO_MAT/MRQuaternionBToMatrix.
     * @param c the quaternion 'c' value
     * @param x the quaternion 'x' value
     * @param y the quaternion 'y' value
     * @param z the quaternion 'z' value
     * @param result the result matrix
     */
    public static void quatByteToMatrix(byte c, byte x, byte y, byte z, short[][] result) {
        int xs = x << 1;
        int ys = y << 1;
        int zs = z << 1;
        int wx = c * xs;
        int wy = c * ys;
        int wz = c * zs;
        int xx = x * xs;
        int xy = x * ys;
        int xz = x * zs;
        int yy = y * ys;
        int yz = y * zs;
        int zz = z * zs;

        // Oddly, every set is bit-shifted right 0 places. Not sure what that does, maybe it does something special in C.
        result[0][0] = (short) (0x1000 - (yy + zz));
        result[0][1] = (short) (xy + wz);
        result[0][2] = (short) (xz - wy);
        result[1][0] = (short) (xy - wz);
        result[1][1] = (short) (0x1000 - (xx + zz));
        result[1][2] = (short) (yz + wx);
        result[2][0] = (short) (xz + wy);
        result[2][1] = (short) (yz - wx);
        result[2][2] = (short) (0x1000 - (xx + yy));
    }

    /**
     * Converts a byte quaternion to a matrix.
     * Represents MR_QUAT_TO_MAT/MRQuaternionToMatrix.
     * @param c the quaternion 'c' value
     * @param x the quaternion 'x' value
     * @param y the quaternion 'y' value
     * @param z the quaternion 'z' value
     * @param matrix the result matrix
     */
    public static void quatToMatrix(short c, short x, short y, short z, short[][] matrix) {
        int xs = x << 1;
        int ys = y << 1;
        int zs = z << 1;
        int wx = c * xs;
        int wy = c * ys;
        int wz = c * zs;
        int xx = x * xs;
        int xy = x * ys;
        int xz = x * zs;
        int yy = y * ys;
        int yz = y * zs;
        int zz = z * zs;

        // Oddly, every set is bit-shifted right 0 places. Not sure what that does, maybe it does something special in C.
        matrix[0][0] = (short) (0x1000 - ((yy + zz) >> 12));
        matrix[0][1] = (short) ((xy + wz) >> 12);
        matrix[0][2] = (short) ((xz - wy) >> 12);
        matrix[1][0] = (short) ((xy - wz) >> 12);
        matrix[1][1] = (short) (0x1000 - ((xx + zz) >> 12));
        matrix[1][2] = (short) ((yz + wx) >> 12);
        matrix[2][0] = (short) ((xz + wy) >> 12);
        matrix[2][1] = (short) ((yz - wx) >> 12);
        matrix[2][2] = (short) (0x1000 - ((xx + yy) >> 12));
    }

    /**
     * Create a Bézier curve which roughly represents a curve which follows through the provided points.
     * @param instance the game instance to create the curve for
     * @param subDivisions the subdivisions of a spline curve which we'd like to create a representation of.
     * @return bezierCurve
     */
    public static MRBezierCurve createBezierCurve(SCGameInstance instance, List<SVector> subDivisions) {
        if (subDivisions == null)
            throw new NullPointerException("subDivisions");
        if (subDivisions.size() < 3)
            throw new IllegalArgumentException("Must be at least two points provided in the sub-division list. (Provided: " + subDivisions.size() + ")");

        if (subDivisions.size() == 4) {
            return createCubicBezierSpline(instance, subDivisions.get(0), subDivisions.get(1), subDivisions.get(2), subDivisions.get(3));
        } else if (subDivisions.size() == 3) {
            // If 3 is provided calculate (2/3 * (mid - start)) and (3/2 * (end - mid)) to use as p1 and p2.
            SVector startPoint = subDivisions.get(0);
            SVector midPoint = subDivisions.get(1);
            SVector endPoint = subDivisions.get(2);
            SVector splinePt1 = midPoint.clone().subtract(startPoint).multiply(2 / 3D).add(startPoint);
            SVector splinePt2 = endPoint.clone().subtract(midPoint).multiply(3 / 2D).add(midPoint);
            return createCubicBezierSpline(instance, startPoint, splinePt1, splinePt2, endPoint);
        } else {
            SVector subDivOne = getLinearlyInterpolatedPosition(subDivisions, 1 / 3D);
            SVector subDivTwo = getLinearlyInterpolatedPosition(subDivisions, 2 / 3D);
            return createCubicBezierSpline(instance, subDivisions.get(0), subDivOne, subDivTwo, subDivisions.get(subDivisions.size() - 1));
        }
    }

    private static SVector getLinearlyInterpolatedPosition(List<SVector> subDivisions, double t) {
        int subDivIndex = (int) (t * subDivisions.size());
        /*double clampT = (double) subDivIndex / (subDivisions.size() - 1);
        double multiplier = 1D + (t - clampT);
        return subDivisions.get(subDivIndex + 1).clone().subtract(subDivisions.get(subDivIndex))
                .multiply(multiplier)
                .add(subDivisions.get(subDivIndex));*/ // Do not linearly interpolate, because it can cause splines to overshoot their length at the end.
        return subDivisions.get(subDivIndex);
    }

    /**
     * Creates a cubic Bézier curve which intersects the given points.
     * @param instance the game instance to create for
     * @param p0 the start point of the curve (at t=0)
     * @param p1 the position at t=1/3.
     * @param p2 the position at t=2/3.
     * @param p3 the end point of the curve (at t=1)
     * @return bezierCurve
     */
    public static MRBezierCurve createCubicBezierSpline(SCGameInstance instance, SVector p0, SVector p1, SVector p2, SVector p3) {
        if (instance == null)
            throw new NullPointerException("instance");
        if (p0 == null)
            throw new NullPointerException("p0");
        if (p1 == null)
            throw new NullPointerException("p1");
        if (p2 == null)
            throw new NullPointerException("p2");
        if (p3 == null)
            throw new NullPointerException("p3");

        // For the purpose of simplicity, I will be solving this work here as if it were one-dimensional.
        // A cubic Bézier curve is formed from start point, start tangent, end point, end tangent.
        // a(t^3) + b(t^2) + ct + d.
        // where t is a value (time), between 0.0 and 1.0. (Or the equivalent scale in fixed point form)
        // So, given p(0) = p0, p(1/3) = p1, p(2/3) = p2, and p(1) = p3, we're trying to solve for the Bezier curve inputs.
        // p0 is the start point, p4 is the end point, so we need to solve for the start tangent p'(0) and end tangent p'(1).

        // Let c0, c1, c2, c3 be the curve control points, so c0 = p0, c3=p3.
        // The following is an explicit form of a Cubic Bézier curve from https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Cubic_B.C3.A9zier_curves
        // p(t) = ((1 - t)^3 * c0) + (3(1 - t)^2 * c1) + (3(1 - t)t^2 * c2) + t^3 * c3
        //  = ((1 - 3t + 3t^2 -t^3) * c0) + ((3t - 6t^2 + 3t^3) * c1) + ((3t^2 - 3t^3) * c2) + (t^3 * c3) Expand cubed parenthesis.
        //  = c0 - 3tc0 + 3t^2 * c0 - t^3 * c0 + 3t * c1 - 6t^2 * c1 + 3t^3 * c1 + 3t^2 * c2 - 3t^3 * c2 + t^3 * c3 Expand multiplication
        //  = c0 + 3tc1 - 3tc0 + 3t^2 * c0 - 6t^2 * c1 + 3t^2 * c2 - 3t^3 * c2 - t^3 * c0 + 3t^3 * c1 + t^3 * c3 Simplify
        //  = c0 + (3c1 - 3c0)t + (3c0 - 6c1 + 3c2)t^2 + (c3 - 3c2 + 3c1 - 1)t^3 Simplify

        // Now, if we treat this as a Lagrange curve...
        // a(t^3) + b(t^2) + ct + d.
        // a = c3 - 3c2 + 3c1 - 1
        // b = 3c0 - 6c1 + 3c2
        // c = 3c1 - 3c0
        // d = c0

        // By definition of how a Bézier curve is formed, our control points are solvable with:
        // c1 = p0 + p'(0)
        // c2 = p3 + p'(1)

        // p'() can be represented using the "Lagrange polynomial" concept, which I don't entirely understand.
        // But, the post here: https://math.stackexchange.com/questions/2190728/how-to-calculate-the-cubic-b%C3%A9zier-spline-points
        // used this method to resolve it down to the following logic:

        MRBezierCurve bezierCurve = new MRBezierCurve(instance);
        bezierCurve.getStart().setValues(p0);
        bezierCurve.getEnd().setValues(p3);

        short c1x = (short) ((-5 * p0.getX() + 18 * p1.getX() - 9 * p2.getX() + 2 * p3.getX()) / 6);
        short c1y = (short) ((-5 * p0.getY() + 18 * p1.getY() - 9 * p2.getY() + 2 * p3.getY()) / 6);
        short c1z = (short) ((-5 * p0.getZ() + 18 * p1.getZ() - 9 * p2.getZ() + 2 * p3.getZ()) / 6);
        short c2x = (short) ((2 * p0.getX() - 9 * p1.getX() + 18 * p2.getX() - 5 * p3.getX()) / 6);
        short c2y = (short) ((2 * p0.getY() - 9 * p1.getY() + 18 * p2.getY() - 5 * p3.getY()) / 6);
        short c2z = (short) ((2 * p0.getZ() - 9 * p1.getZ() + 18 * p2.getZ() - 5 * p3.getZ()) / 6);
        bezierCurve.getControl1().setValues(c1x, c1y, c1z);
        bezierCurve.getControl2().setValues(c2x, c2y, c2z);
        return bezierCurve;
    }
}