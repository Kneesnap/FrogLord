package net.highwayfrogs.editor.system.math;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a vector with two 32 bit floating point values.
 * This is a backport from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
@Getter
@Setter
public class Vector2f {
    private float x;
    private float y;

    /**
     * Defines a unit-length Vector2 that points towards the X-axis.
     */
    public static final Vector2f UNIT_X = new Vector2f(1f, 0.0f);

    /**
     * Defines a unit-length Vector2 that points towards the Y-axis.
     */
    public static final Vector2f UNIT_Y = new Vector2f(0.0f, 1f);

    /**
     * Defines a zero-length Vector2.
     */
    public static final Vector2f ZERO = new Vector2f(0.0f, 0.0f);

    /**
     * Defines an instance with all components set to 1.
     */
    public static final Vector2f ONE = new Vector2f(1f, 1f);

    public Vector2f() {
        this.x = 0;
        this.y = 0;
    }

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f(Vector2f vec) {
        this(vec.x, vec.y);
    }

    /**
     * Calculate the length of this vector.
     */
    public double calculateLength() {
        return Math.sqrt(calculateLengthSquared());
    }

    /**
     * Calculates the length of this vector, squared.
     */
    public double calculateLengthSquared() {
        return ((double) this.x * this.x) + ((double) this.y * this.y);
    }

    /**
     * Clones the vector.
     */
    public Vector2f clone() {
        return new Vector2f(this);
    }

    /**
     * Applies the absolute value of all of the components stored by this vector.
     */
    public Vector2f abs() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        return this;
    }

    /**
     * Adds scalar values to the vector.
     * @param x The x value to add.
     * @param y The y value to add.
     * @return this
     */
    public Vector2f add(float x, float y) {
        this.x += x;
        this.y += y;
        return this;
    }


    /**
     * Adds two vectors together.
     * @param other The vector to add.
     * @return this
     */
    public Vector2f add(Vector2f other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    /**
     * Subtracts scalar values from the vector.
     * @param x The x value to subtract.
     * @param y The y value to subtract.
     * @return this
     */
    public Vector2f subtract(float x, float y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    /**
     * Subtracts a vector.
     * @param other The vector to subtract.
     * @return this
     */
    public Vector2f subtract(Vector2f other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    /**
     * Multiplies by the provided vector.
     * @param other The vector to multiply
     * @return this
     */
    public Vector2f multiply(Vector2f other) {
        this.x *= other.x;
        this.y *= other.y;
        return this;
    }

    /**
     * Multiplies the vector by a float.
     * @param scale The scalar value to multiply the vector by.
     * @return this
     */
    public Vector2f multiplyScalar(float scale) {
        this.x *= scale;
        this.y *= scale;
        return this;
    }

    /**
     * Divides the vector by a scalar.
     * @param scale The scalar.
     * @return this
     */
    public Vector2f divideScalar(float scale) {
        float mult = 1.0f / scale;
        return this.multiplyScalar(mult);
    }

    /**
     * Negates the vector.
     * @return this
     */
    public Vector2f negate() {
        this.x = -this.x;
        this.y = -this.y;
        return this;
    }

    /**
     * Sets both the x and y scalar components of the vector.
     * @param x The new x value
     * @param y The new y value
     */
    public Vector2f setXY(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Copies the values of another vector to this one.
     * @param other The vector to copy values from
     */
    public Vector2f setXY(Vector2f other) {
        if (other == null)
            throw new NullPointerException("other");

        this.x = other.x;
        this.y = other.y;
        return this;
    }

    /**
     * Reads the contents of this vector from a string
     * @param input the input to parse
     */
    public void parse(String input) {
        if (input == null)
            throw new NullPointerException("input");

        String[] split = input.split(",?\\s+");
        if (split.length != 2)
            throw new NumberFormatException("'" + input + "' cannot be parsed as a Vector2f because it appears to have " + split.length + " values.");

        this.x = Float.parseFloat(split[0]);
        this.y = Float.parseFloat(split[1]);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Vector2f))
            return false;

        Vector2f otherVector = (Vector2f) other;
        return this.x == otherVector.x && this.y == otherVector.y;
    }

    /**
     * WARNING: If the position of the vector changes, the hashcode will too, thus breaking any data structures which rely on the value to remain constant.
     */
    @Override
    public int hashCode() {
        return Float.hashCode(this.x) * 397 ^ Float.hashCode(this.y);
    }

    /**
     * Gets this vector as a string which can be parsed.
     */
    public String toParseableString() {
        return this.x + ", " + this.y + ", ";
    }

    @Override
    public String toString() {
        return "Vector2f{x=" + this.x + ",y=" + this.y + "}";
    }

    /**
     * Returns a vector created from the smallest of the corresponding components of the given vectors.
     * @param a First operand.
     * @param b Second operand.
     * @return The component-wise minimum.
     */
    public static Vector2f componentMin(Vector2f a, Vector2f b) {
        return new Vector2f((Math.min(a.x, b.x)), (Math.min(a.y, b.y)));
    }

    /**
     * Returns a vector created from the largest of the corresponding components of the given vectors.
     * @param a First operand.
     * @param b Second operand.
     * @return The component-wise maximum.
     */
    public static Vector2f componentMax(Vector2f a, Vector2f b) {
        return new Vector2f((Math.max(a.x, b.x)), (Math.max(a.y, b.y)));
    }

    /**
     * Returns the Vector2f with the minimum magnitude. If the magnitudes are equal, the second vector
     * is selected.
     * @param left  Left operand.
     * @param right Right operand.
     * @return The minimum Vector2f.
     */
    public static Vector2f magnitudeMin(Vector2f left, Vector2f right) {
        return left.calculateLengthSquared() >= right.calculateLengthSquared() ? right : left;
    }

    /**
     * Returns the Vector2f with the maximum magnitude. If the magnitudes are equal, the first vector
     * is selected.
     * @param left  Left operand.
     * @param right Right operand.
     * @return The maximum Vector2f.
     */
    public static Vector2f magnitudeMax(Vector2f left, Vector2f right) {
        return left.calculateLengthSquared() < right.calculateLengthSquared() ? right : left;
    }

    /**
     * Clamp a vector to the given minimum and maximum vectors.
     * @param vec Input vector.
     * @param min Minimum vector.
     * @param max Maximum vector.
     * @return The clamped vector.
     */
    public static Vector2f clamp(Vector2f vec, Vector2f min, Vector2f max) {
        float resultX = vec.x < min.x ? min.x : (Math.min(vec.x, max.x));
        float resultY = vec.y < min.y ? min.y : (Math.min(vec.y, max.y));
        return new Vector2f(resultX, resultY);
    }

    /**
     * Returns a new Vector that is the linear blend of the 2 given Vectors.
     * @param a     First input vector.
     * @param b     Second input vector.
     * @param blend The blend factor. a when blend=0, b when blend=1.
     * @return a when blend=0, b when blend=1, and a linear combination otherwise.
     */
    public static Vector2f lerp(Vector2f a, Vector2f b, float blend) {
        float resultX = blend * (b.x - a.x) + a.x;
        float resultY = blend * (b.y - a.y) + a.y;
        return new Vector2f(resultX, resultY);
    }

    /**
     * Calculate the dot product of two vectors.
     * @param a the first input vector
     * @param b the second input vector
     * @return dotProduct
     */
    public static double dotProduct(Vector2f a, Vector2f b) {
        return (a.x * b.x) + (a.y * b.y);
    }
}