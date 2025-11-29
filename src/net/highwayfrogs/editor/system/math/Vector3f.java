package net.highwayfrogs.editor.system.math;

import javafx.geometry.Point3D;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListDataEntry;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a vector with three 32 bit floating point values.
 * This is a backport from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
@Getter
@Setter
public class Vector3f implements IBinarySerializable {
    private float x;
    private float y;
    private float z;

    /**
     * Defines a unit-length Vector3 that points towards the X-axis.
     */
    public static final Vector3f UNIT_X = new Vector3f(1f, 0f, 0f);

    /**
     * Defines a unit-length Vector3 that points towards the Y-axis.
     */
    public static final Vector3f UNIT_Y = new Vector3f(0f, 1f, 0f);

    /**
     * Defines a unit-length Vector3 that points towards the Z-axis.
     */
    public static final Vector3f UNIT_Z = new Vector3f(0f, 0f, 1f);

    /**
     * Defines a zero-length Vector3.
     */
    public static final Vector3f ZERO = new Vector3f(0f, 0f, 0f);

    /**
     * Defines an instance with all components set to 1.
     */
    public static final Vector3f ONE = new Vector3f(1f, 1f, 1f);

    public Vector3f() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f(Vector3f vec) {
        this(vec.x, vec.y, vec.z);
    }

    @Override
    public void load(DataReader reader) {
        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);
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
        return ((double) this.x * this.x) + ((double) this.y * this.y) + ((double) this.z * this.z);
    }

    /**
     * Clones the vector.
     */
    public Vector3f clone() {
        return new Vector3f(this);
    }

    /**
     * Applies the absolute value of the components stored by this vector.
     */
    public Vector3f abs() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        this.z = Math.abs(this.z);
        return this;
    }

    /**
     * Adds two vectors together.
     * @param other The vector to add.
     * @return this
     */
    public Vector3f add(Vector3f other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }

    /**
     * Adds xyz components to this vector.
     * @param x The x value to add.
     * @param y The y value to add.
     * @param z The z value to add.
     * @return this
     */
    public Vector3f add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    /**
     * Subtracts a vector.
     * @param other The vector to subtract.
     * @return this
     */
    public Vector3f subtract(Vector3f other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
        return this;
    }

    /**
     * Multiplies by the provided vector.
     * @param other The vector to multiply
     * @return this
     */
    public Vector3f multiply(Vector3f other) {
        this.x *= other.x;
        this.y *= other.y;
        this.z *= other.z;
        return this;
    }

    /**
     * Multiply this against a Matrix4x4.
     * @param matrix The matrix to multiply against
     * @return this
     */
    public Vector3f multiply(Matrix4x4f matrix) {
        if (matrix == null)
            throw new NullPointerException("matrix");

        return matrix.multiply(this, this);
    }

    /**
     * Multiplies the vector by a float.
     * @param scale The scalar value to multiply the vector by.
     * @return this
     */
    public Vector3f multiplyScalar(float scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
        return this;
    }

    /**
     * Divides the vector by a scalar.
     * @param scale The scalar.
     * @return this
     */
    public Vector3f divideScalar(float scale) {
        float mult = 1.0f / scale;
        return this.multiplyScalar(mult);
    }

    /**
     * Negates the vector.
     * @return this
     */
    public Vector3f negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    /**
     * Normalises the vector.
     * @return this
     */
    public Vector3f normalise() {
        double magnitudeSq = (this.x * this.x) + (this.y * this.y) + (this.z * this.z);
        if (!Double.isFinite(magnitudeSq))
            throw new RuntimeException(this + " cannot be normalized, its magnitudeSq was: " + magnitudeSq);

        double inverseMagnitude = 1D / Math.sqrt(magnitudeSq);
        this.x = (float) (this.x * inverseMagnitude);
        this.y = (float) (this.y * inverseMagnitude);
        this.z = (float) (this.z * inverseMagnitude);
        return this;
    }

    /**
     * Sets the x, y, and z scalar components of the vector.
     * @param x The new x value
     * @param y The new y value
     * @param z The new z value
     */
    public Vector3f setXYZ(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Copies the values of another vector to this one.
     * @param other The vector to copy values from
     */
    public Vector3f setXYZ(Vector3f other) {
        if (other == null)
            throw new NullPointerException("other");

        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    /**
     * Copies the values of another vector to this one.
     * @param other The vector to copy values from
     */
    public Vector3f setXYZ(Point3D other) {
        if (other == null)
            throw new NullPointerException("other");

        this.x = (float) other.getX();
        this.y = (float) other.getY();
        this.z = (float) other.getZ();
        return this;
    }

    /**
     * Calculate the cross-product of this and another vector
     * @param other the other vector
     * @param result the vector to store the output within
     * @return crossProduct
     */
    public Vector3f crossProduct(Vector3f other, Vector3f result) {
        if (result == null)
            result = new Vector3f();

        result.setXYZ(((this.y * other.z) - (this.z * other.y)), ((this.z * other.x) - (this.x * other.z)), ((this.x * other.y) - (this.y * other.x)));
        return result;
    }

    /**
     * Calculate the cross-product of this and another vector.
     * @param other the other vector
     * @return crossProduct
     */
    public Vector3f crossProduct(Vector3f other) {
        return crossProduct(other, new Vector3f());
    }

    /**
     * Reads the contents of this vector from a string
     * @param input the input to parse
     */
    public void parse(String input) {
        if (input == null)
            throw new NullPointerException("input");

        String[] split = input.split(",?\\s+");
        if (split.length != 3)
            throw new NumberFormatException("'" + input + "' cannot be parsed as a Vector3f because it appears to have " + split.length + " values.");

        this.x = Float.parseFloat(split[0]);
        this.y = Float.parseFloat(split[1]);
        this.z = Float.parseFloat(split[2]);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Vector3f))
            return false;

        Vector3f otherVector = (Vector3f) other;
        return this.x == otherVector.x && this.y == otherVector.y && this.z == otherVector.z;
    }

    /**
     * WARNING: If the position of the vector changes, the hashcode will too, thus breaking any data structures which rely on the value to remain constant.
     */
    @Override
    public int hashCode() {
        return Float.hashCode(this.x) * 397 * Float.hashCode(this.y) ^ Float.hashCode(this.z);
    }

    /**
     * Adds the vector to the property list.
     * @param propertyList the property list to add to
     * @param name the name to put the vector into the property list with
     */
    public PropertyListDataEntry<Vector3f> addToPropertyList(PropertyListNode propertyList, String name) {
        return propertyList.add(name, this)
                .setDataToStringConverter(Vector3f::toParseableString)
                .setDataFromStringConverter(newText -> {
                    Vector3f newVector = new Vector3f();
                    newVector.parse(newText);
                    return newVector;
                })
                .setDataHandler(this::setXYZ);
    }

    /**
     * Gets this vector as a string which can be parsed.
     */
    public String toParseableString() {
        return this.x + ", " + this.y + ", " + this.z;
    }

    @Override
    public String toString() {
        return "Vector3f{x=" + this.x + ",y=" + this.y + ",z=" + this.z + "}";
    }

    /**
     * Returns a vector created from the smallest of the corresponding components of the given vectors.
     * @param a First operand.
     * @param b Second operand.
     * @return The component-wise minimum.
     */
    public static Vector3f componentMin(Vector3f a, Vector3f b) {
        return new Vector3f(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
    }

    /**
     * Returns a vector created from the largest of the corresponding components of the given vectors.
     * @param a First operand.
     * @param b Second operand.
     * @return The component-wise maximum.
     */
    public static Vector3f componentMax(Vector3f a, Vector3f b) {
        return new Vector3f(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    /**
     * Returns the Vector3f with the minimum magnitude. If the magnitudes are equal, the second vector
     * is selected.
     * @param left  Left operand.
     * @param right Right operand.
     * @return The minimum Vector3f.
     */
    public static Vector3f magnitudeMin(Vector3f left, Vector3f right) {
        return left.calculateLengthSquared() >= right.calculateLengthSquared() ? right : left;
    }

    /**
     * Returns the Vector3f with the maximum magnitude. If the magnitudes are equal, the first vector
     * is selected.
     * @param left  Left operand.
     * @param right Right operand.
     * @return The maximum Vector3f.
     */
    public static Vector3f magnitudeMax(Vector3f left, Vector3f right) {
        return left.calculateLengthSquared() < right.calculateLengthSquared() ? right : left;
    }

    /**
     * Clamp a vector to the given minimum and maximum vectors.
     * @param vec Input vector.
     * @param min Minimum vector.
     * @param max Maximum vector.
     * @return The clamped vector.
     */
    public static Vector3f clamp(Vector3f vec, Vector3f min, Vector3f max) {
        float resultX = vec.x < min.x ? min.x : Math.min(vec.x, max.x);
        float resultY = vec.y < min.y ? min.y : Math.min(vec.y, max.y);
        float resultZ = vec.z < min.z ? min.z : Math.min(vec.z, max.z);
        return new Vector3f(resultX, resultY, resultZ);
    }

    /**
     * Returns a new Vector that is the linear blend of the 2 given Vectors.
     * @param a     First input vector.
     * @param b     Second input vector.
     * @param blend The blend factor. a when blend=0, b when blend=1.
     * @return a when blend=0, b when blend=1, and a linear combination otherwise.
     */
    public static Vector3f lerp(Vector3f a, Vector3f b, float blend) {
        float resultX = blend * (b.x - a.x) + a.x;
        float resultY = blend * (b.y - a.y) + a.y;
        float resultZ = blend * (b.z - a.z) + a.z;
        return new Vector3f(resultX, resultY, resultZ);
    }

    /**
     * Returns a new Vector that is the linear blend of the 2 given Vectors.
     * @param a     First input vector.
     * @param b     Second input vector.
     * @param blend The blend factor. a when blend=0, b when blend=1.
     * @return a when blend=0, b when blend=1, and a linear combination otherwise.
     */
    public static Vector3f lerp(Vector3f a, Vector3f b, float blend, Vector3f result) {
        result.setX(blend * (b.x - a.x) + a.x);
        result.setY(blend * (b.y - a.y) + a.y);
        result.setZ(blend * (b.z - a.z) + a.z);
        return result;
    }

    /**
     * Calculate the dot product of two vectors.
     * @param a the first input vector
     * @param b the second input vector
     * @return dotProduct
     */
    public static double dotProduct(Vector3f a, Vector3f b) {
        return (a.x * b.x) + (a.y * b.y) + (a.z * b.z);
    }
}