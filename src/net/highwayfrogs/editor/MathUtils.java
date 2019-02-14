package net.highwayfrogs.editor;

/**
 * Math related utility functions.
 * Created by AndyEder on 2/14/2019.
 */

// TODO: Grow this math library over time.
// TODO: Potentially move other math-related utility functions into this class?

public class MathUtils {

    /**
     * Clamp a short value to a specified range.
     * @param valueIn the value to clamp.
     * @param minVal the minimum (inclusive) clamped value.
     * @param maxVal the maximum (inclusive) clamped value.
     * @return int
     */
    public static short clamp(short valueIn, short minVal, short maxVal) {
        return (valueIn < minVal) ? minVal : (valueIn > maxVal) ? maxVal : valueIn;
    }

    /**
     * Clamp a long value to a specified range.
     * @param valueIn the value to clamp.
     * @param minVal the minimum (inclusive) clamped value.
     * @param maxVal the maximum (inclusive) clamped value.
     * @return int
     */
    public static long clamp(long valueIn, long minVal, long maxVal) {
        return (valueIn < minVal) ? minVal : (valueIn > maxVal) ? maxVal : valueIn;
    }

    /**
     * Clamp an int value to a specified range.
     * @param valueIn the value to clamp.
     * @param minVal the minimum (inclusive) clamped value.
     * @param maxVal the maximum (inclusive) clamped value.
     * @return int
     */
    public static int clamp(int valueIn, int minVal, int maxVal) {
        return (valueIn < minVal) ? minVal : (valueIn > maxVal) ? maxVal : valueIn;
    }

    /**
     * Clamp a float value to a specified range.
     * @param valueIn the value to clamp.
     * @param minVal the minimum (inclusive) clamped value.
     * @param maxVal the maximum (inclusive) clamped value.
     * @return int
     */
    public static float clamp(float valueIn, float minVal, float maxVal) {
        return (valueIn < minVal) ? minVal : (valueIn > maxVal) ? maxVal : valueIn;
    }

    /**
     * Clamp a double value to a specified range.
     * @param valueIn the value to clamp.
     * @param minVal the minimum (inclusive) clamped value.
     * @param maxVal the maximum (inclusive) clamped value.
     * @return int
     */
    public static double clamp(double valueIn, double minVal, double maxVal) {
        return (valueIn < minVal) ? minVal : (valueIn > maxVal) ? maxVal : valueIn;
    }
}
