package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

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
}