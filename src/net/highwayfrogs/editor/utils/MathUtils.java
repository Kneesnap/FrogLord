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

    private static int crossProduct(int point1X, int point1Y, int point2X, int point2Y, int point3X, int point3Y) {
        int y1 = point1Y - point2Y;
        int y2 = point1Y - point3Y;
        int x1 = point1X - point2X;
        int x2 = point1X - point3X;
        return y2 * x1 - y1 * x2;
    }

    /**
     * Test if two lines intersect.
     * @param line1X1 the x coordinate at the start of the first line
     * @param line1Y1 the y coordinate at the start of the first line
     * @param line1X2 the x coordinate at the end of the first line
     * @param line1Y2 the y coordinate at the end of the first line
     * @param line2X1 the x coordinate at the start of the second line
     * @param line2Y1 the y coordinate at the start of the second line
     * @param line2X2 the x coordinate at the end of the second line
     * @param line2Y2 the y coordinate at the end of the second line
     * @return do lines intersect
     */
    public static boolean doLinesIntersect(int line1X1, int line1Y1, int line1X2, int line1Y2, int line2X1, int line2Y1, int line2X2, int line2Y2) {
        int d1 = crossProduct(line1X1, line1Y1, line1X2, line1Y2, line2X1, line2Y1);
        int d2 = crossProduct(line1X1, line1Y1, line1X2, line1Y2, line2X2, line2Y2);
        int d3 = crossProduct(line2X1, line2Y1, line2X2, line2Y2, line1X1, line1Y1);
        int d4 = crossProduct(line2X1, line2Y1, line2X2, line2Y2, line1X2, line1Y2);
        return ((d1 > 0 && d2 < 0 || d1 < 0 && d2 > 0) && (d3 > 0 && d4 < 0 || d3 < 0 && d4 > 0));
    }

    /**
     * Test if a sphere intersects a cube.
     * @param sphereX the x position at the center of the sphere
     * @param sphereY the y position at the center of the sphere
     * @param sphereZ the z position at the center of the sphere
     * @param sphereRadius  the radius of the sphere
     * @param cubeMinX the x position of the cube's minimum corner
     * @param cubeMinY the y position of the cube's minimum corner
     * @param cubeMinZ the z position of the cube's minimum corner
     * @param cubeMaxX the x position of the cube's maximum corner
     * @param cubeMaxY the y position of the cube's maximum corner
     * @param cubeMaxZ the z position of the cube's maximum corner
     * @return true iff the sphere and cube intersect
     */
    public static boolean doesSphereIntersectCube(float sphereX, float sphereY, float sphereZ, float sphereRadius,
                                                  float cubeMinX, float cubeMinY, float cubeMinZ,
                                                  float cubeMaxX, float cubeMaxY, float cubeMaxZ) {
        float distSquared = 0;
        if (sphereX < cubeMinX) {
            distSquared += (sphereX - cubeMinX) * (sphereX - cubeMinX);
        } else if (sphereX > cubeMaxX) {
            distSquared += (sphereX - cubeMaxX) * (sphereX - cubeMaxX);
        }

        if (sphereY < cubeMinY) {
            distSquared += (sphereY - cubeMinY) * (sphereY - cubeMinY);
        } else if (sphereY > cubeMaxY) {
            distSquared += (sphereY - cubeMaxY) * (sphereY - cubeMaxY);
        }

        if (sphereZ < cubeMinZ) {
            distSquared += (sphereZ - cubeMinZ) * (sphereZ - cubeMinZ);
        } else if (sphereZ > cubeMaxZ) {
            distSquared += (sphereZ - cubeMaxZ) * (sphereZ - cubeMaxZ);
        }

        return (sphereRadius * sphereRadius) >= distSquared
                || ((sphereX >= cubeMinX) && (sphereX <= cubeMaxX) // Not 100% sure if this is necessary, but I think so, if there's a small sphere in a large cube.
                && (sphereY >= cubeMinY) && (sphereY <= cubeMaxY)
                && (sphereZ >= cubeMinZ) && (sphereZ <= cubeMaxZ));
    }
}