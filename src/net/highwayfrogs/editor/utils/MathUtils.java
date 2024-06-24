package net.highwayfrogs.editor.utils;

/**
 * Math related utility functions.
 * Created by AndyEder on 2/14/2019.
 */
public class MathUtils {

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
        return Math.max(minVal, Math.min(valueIn, maxVal));
    }
}