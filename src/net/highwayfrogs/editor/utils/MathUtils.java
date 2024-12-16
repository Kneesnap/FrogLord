package net.highwayfrogs.editor.utils;

/**
 * Math related utility functions.
 * Created by AndyEder on 2/14/2019.
 */
public class MathUtils {
    public static final double PI_OVER_2 = Math.PI * .5;

    /**
     * Calculates the greatest common divisor between the two numbers using euclid's algorithm.
     * If both factors are 0, 0 will be returned. Otherwise, the greatest common factor will be returned, with the default as 1 if there are no other shared factors.
     * Based on <a href="https://stackoverflow.com/questions/13673600/how-to-write-a-simple-java-program-that-finds-the-greatest-common-divisor-betwee"/>.
     * @param a the first value
     * @param b the second value
     * @return greatestCommonDivisor
     */
    public static int gcd(int a, int b) {
        while (a != 0 && b != 0)  {
            int c = b;
            b = a % b;
            a = c;
        }

        // Either one is 0, so return the non-zero value
        return a + b;
    }

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
        return (valueIn < minVal) ? minVal : Math.min(valueIn, maxVal);
    }

    /**
     * Clamp an int value to a specified range.
     * @param valueIn the value to clamp.
     * @param minVal the minimum (inclusive) clamped value.
     * @param maxVal the maximum (inclusive) clamped value.
     * @return int
     */
    public static int clamp(int valueIn, int minVal, int maxVal) {
        return (valueIn < minVal) ? minVal : Math.min(valueIn, maxVal);
    }

    /**
     * Clamp a float value to a specified range.
     * @param valueIn the value to clamp.
     * @param minVal the minimum (inclusive) clamped value.
     * @param maxVal the maximum (inclusive) clamped value.
     * @return int
     */
    public static float clamp(float valueIn, float minVal, float maxVal) {
        return (valueIn < minVal) ? minVal : Math.min(valueIn, maxVal);
    }

    /**
     * Clamp a double value to a specified range.
     * @param valueIn the value to clamp.
     * @param minVal the minimum (inclusive) clamped value.
     * @param maxVal the maximum (inclusive) clamped value.
     * @return int
     */
    public static double clamp(double valueIn, double minVal, double maxVal) {
        return Math.max(minVal, Math.min(valueIn, maxVal));
    }

    /**
     * Raise x to the y. Math.pow is pretty intensive, so we do this instead.
     * @param x The base.
     * @param y The exponent.
     * @return value
     */
    public static int power(int x, int y) {
        int result = 1;
        while (y-- > 0)
            result *= x;
        return result;
    }

    /**
     * Get the square root of a fixed-point integer.
     * @param i The integer to get the square root of.
     * @return squareRoot
     */
    public static int fixedSqrt(int i) {
        return (int) Math.sqrt(i);
    }

    /**
     * A clamp method that clamps down on negative numbers, and up on positive numbers.
     * @param value The value to clamp.
     * @return clampedCeil
     */
    public static int ceilNegative(double value) {
        if ((int) value == value)
            return (int) value;

        return (value > 0) ? ((int) value + 1) : ((int) value - 1);
    }

    /**
     * Clamps an angle represented in degrees (where 0 represents no rotation) between -180 and 180.
     * @param angle the angle to clamp
     * @return clampedAngle
     */
    public static double clampAngleInDegrees(double angle) {
        return ((angle % 360F) + 540F) % 360F - 180F;
    }
}