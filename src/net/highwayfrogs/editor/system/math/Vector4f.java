package net.highwayfrogs.editor.system.math;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListDataEntry;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a vector with four 32 bit floating point values.
 * This is a backport from ModToolFramework.
 * Created by Kneesnap on 10/2/2023.
 */
@Getter
@Setter
public class Vector4f implements IBinarySerializable {
    protected float x;
    protected float y;
    protected float z;
    protected float w;

    /**
     * Defines a unit-length Vector4 that points towards the X-axis.
     */
    public static final Vector4f UNIT_X = new Vector4f(1f, 0f, 0f, 0f);

    /**
     * Defines a unit-length Vector4 that points towards the Y-axis.
     */
    public static final Vector4f UNIT_Y = new Vector4f(0f, 1f, 0f, 0f);

    /**
     * Defines a unit-length Vector4 that points towards the Z-axis.
     */
    public static final Vector4f UNIT_Z = new Vector4f(0f, 0f, 1f, 0f);

    /**
     * Defines a unit-length Vector4 that points towards the W-axis.
     */
    public static final Vector4f UNIT_W = new Vector4f(0f, 0f, 0f, 1f);

    /**
     * Defines a zero-length Vector4.
     */
    public static final Vector4f ZERO = new Vector4f(0f, 0f, 0f, 0f);

    /**
     * Defines an instance with all components set to 1.
     */
    public static final Vector4f ONE = new Vector4f(1f, 1f, 1f, 1f);

    public Vector4f() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.w = 0;
    }

    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4f(Vector3f vec, float w) {
        this(vec.getX(), vec.getY(), vec.getZ(), w);
    }

    public Vector4f(Vector4f vec) {
        this(vec.x, vec.y, vec.z, vec.w);
    }

    @Override
    public void load(DataReader reader) {
        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();
        this.w = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);
        writer.writeFloat(this.w);
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
        return ((double) this.x * this.x) + ((double) this.y * this.y) + ((double) this.z * this.z) + ((double) this.w * this.w);
    }

    /**
     * Clones the vector.
     */
    public Vector4f clone() {
        return new Vector4f(this);
    }

    /**
     * Applies the absolute value of the components stored by this vector.
     */
    public Vector4f abs() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        this.z = Math.abs(this.z);
        this.w = Math.abs(this.w);
        return this;
    }

    /**
     * Adds two vectors together.
     * @param other The vector to add.
     * @return this
     */
    public Vector4f add(Vector4f other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        this.w += other.w;
        return this;
    }

    /**
     * Subtracts a vector.
     * @param other The vector to subtract.
     * @return this
     */
    public Vector4f subtract(Vector4f other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
        this.w -= other.w;
        return this;
    }

    /**
     * Multiplies by the provided vector.
     * @param other The vector to multiply
     * @return this
     */
    public Vector4f multiply(Vector4f other) {
        this.x *= other.x;
        this.y *= other.y;
        this.z *= other.z;
        this.w *= other.w;
        return this;
    }

    /**
     * Multiplies the vector by a float.
     * @param scale The scalar value to multiply the vector by.
     * @return this
     */
    public Vector4f multiplyScalar(float scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
        this.w *= scale;
        return this;
    }

    /**
     * Divides the vector by a scalar.
     * @param scale The scalar.
     * @return this
     */
    public Vector4f divideScalar(float scale) {
        float mult = 1.0f / scale;
        return this.multiplyScalar(mult);
    }

    /**
     * Negates the vector.
     * @return this
     */
    public Vector4f negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        this.w = -this.w;
        return this;
    }

    /**
     * Normalises the vector.
     * @return this
     */
    public Vector4f normalise() {
        double magnitudeSq = (this.x * this.x) + (this.y * this.y) + (this.z * this.z) + (this.w * this.w);
        if (!Double.isFinite(magnitudeSq))
            throw new RuntimeException(this + " cannot be normalized, its magnitudeSq was: " + magnitudeSq);

        double inverseMagnitude = 1D / Math.sqrt(magnitudeSq);
        this.x = (float) (this.x * inverseMagnitude);
        this.y = (float) (this.y * inverseMagnitude);
        this.z = (float) (this.z * inverseMagnitude);
        this.w = (float) (this.w * inverseMagnitude);
        return this;
    }

    /**
     * Gets this as a Vector3f (dropping the w value)
     * @param output the output storage.
     * @return outputVector
     */
    public Vector3f getXYZ(Vector3f output) {
        if (output == null)
            output = new Vector3f();

        output.setXYZ(this.x, this.y, this.z);
        return output;
    }

    /**
     * Sets the x, y, z, and w scalar components of the vector.
     * @param x The new x value
     * @param y The new y value
     * @param z The new z value
     * @param w The new w value
     */
    public Vector4f setXYZW(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Copies the values of another vector to this one.
     * @param other The vector to copy values from
     */
    public Vector4f setXYZW(Vector4f other) {
        if (other == null)
            throw new NullPointerException("other");

        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.w = other.w;
        return this;
    }

    /**
     * Reads the contents of this vector from a string
     * @param input the input to parse
     */
    public void parse(String input, float defaultW) {
        if (input == null)
            throw new NullPointerException("input");

        String[] split = input.split(",?\\s+");
        if (split.length != 3 && split.length != 4)
            throw new NumberFormatException("'" + input + "' cannot be parsed as a Vector4f because it appears to have " + split.length + " values.");

        this.x = Float.parseFloat(split[0]);
        this.y = Float.parseFloat(split[1]);
        this.z = Float.parseFloat(split[2]);
        this.w = split.length > 3 ? Float.parseFloat(split[3]) : defaultW;
    }

    /**
     * Reads the contents of this vector from a string
     * @param input the input to parse
     */
    public void parse(String input) {
        if (input == null)
            throw new NullPointerException("input");

        String[] split = input.split(",?\\s+");
        if (split.length != 4)
            throw new NumberFormatException("'" + input + "' cannot be parsed as a Vector4f because it appears to have " + split.length + " values.");

        this.x = Float.parseFloat(split[0]);
        this.y = Float.parseFloat(split[1]);
        this.z = Float.parseFloat(split[2]);
        this.w = Float.parseFloat(split[3]);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Vector4f))
            return false;

        Vector4f otherVector = (Vector4f) other;
        return this.x == otherVector.x && this.y == otherVector.y && this.z == otherVector.z && this.w == otherVector.w;
    }

    /**
     * WARNING: If the position of the vector changes, the hashcode will too, thus breaking any data structures which rely on the value to remain constant.
     */
    @Override
    public int hashCode() {
        return Float.hashCode(this.x) ^ Float.hashCode(this.y) ^ Float.hashCode(this.z) ^ Float.hashCode(this.w);
    }

    /**
     * Adds the vector to the property list.
     * @param propertyList the property list to add to
     * @param name the name to put the vector into the property list with
     */
    public PropertyListDataEntry<Vector4f> addToPropertyList(PropertyListNode propertyList, String name) {
        return addToPropertyList(propertyList, name, Float.NaN);
    }

    /**
     * Adds the vector to the property list.
     * @param propertyList the property list to add to
     * @param name the name to put the vector into the property list with
     * @param skippedW the W value to skip.
     */
    public PropertyListDataEntry<Vector4f> addToPropertyList(PropertyListNode propertyList, String name, float skippedW) {
        return propertyList.add(name, this)
                .setDataToStringConverter(vector -> vector.toParseableString(skippedW))
                .setDataFromStringConverter(newText -> {
                    Vector4f newVector = new Vector4f();
                    newVector.parse(newText, skippedW);
                    return newVector;
                })
                .setDataHandler(this::setXYZW);
    }

    /**
     * Gets this vector as a string which can be parsed.
     */
    public String toParseableString() {
        return this.x + ", " + this.y + ", " + this.z + ", " + this.w;
    }

    /**
     * Gets this vector as a string which can be parsed.
     */
    public String toParseableString(float skippedW) {
        return this.x + ", " + this.y + ", " + this.z + (Math.abs(this.w - skippedW) < .00001D ? "" : ", " + this.w);
    }

    @Override
    public String toString() {
        return "Vector4f{x=" + this.x + ",y=" + this.y + ",z=" + this.z + ",w=" + this.w + "}";
    }

    /**
     * Returns a vector created from the smallest of the corresponding components of the given vectors.
     * @param a First operand.
     * @param b Second operand.
     * @return The component-wise minimum.
     */
    public static Vector4f componentMin(Vector4f a, Vector4f b) {
        return new Vector4f(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z), Math.min(a.w, b.w));
    }

    /**
     * Returns a vector created from the largest of the corresponding components of the given vectors.
     * @param a First operand.
     * @param b Second operand.
     * @return The component-wise maximum.
     */
    public static Vector4f componentMax(Vector4f a, Vector4f b) {
        return new Vector4f(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z), Math.max(a.w, b.w));
    }

    /**
     * Returns the Vector4f with the minimum magnitude. If the magnitudes are equal, the second vector
     * is selected.
     * @param left  Left operand.
     * @param right Right operand.
     * @return The minimum Vector4f.
     */
    public static Vector4f magnitudeMin(Vector4f left, Vector4f right) {
        return left.calculateLengthSquared() >= right.calculateLengthSquared() ? right : left;
    }

    /**
     * Returns the Vector4f with the maximum magnitude. If the magnitudes are equal, the first vector
     * is selected.
     * @param left  Left operand.
     * @param right Right operand.
     * @return The maximum Vector4f.
     */
    public static Vector4f magnitudeMax(Vector4f left, Vector4f right) {
        return left.calculateLengthSquared() < right.calculateLengthSquared() ? right : left;
    }

    /**
     * Clamp a vector to the given minimum and maximum vectors.
     * @param vec Input vector.
     * @param min Minimum vector.
     * @param max Maximum vector.
     * @return The clamped vector.
     */
    public static Vector4f clamp(Vector4f vec, Vector4f min, Vector4f max) {
        float resultX = vec.x < min.x ? min.x : Math.min(vec.x, max.x);
        float resultY = vec.y < min.y ? min.y : Math.min(vec.y, max.y);
        float resultZ = vec.z < min.z ? min.z : Math.min(vec.z, max.z);
        float resultW = vec.w < min.w ? min.w : Math.min(vec.w, max.w);
        return new Vector4f(resultX, resultY, resultZ, resultW);
    }

    /**
     * Returns a new Vector that is the linear blend of the 2 given Vectors.
     * @param a     First input vector.
     * @param b     Second input vector.
     * @param blend The blend factor. a when blend=0, b when blend=1.
     * @return a when blend=0, b when blend=1, and a linear combination otherwise.
     */
    public static Vector4f lerp(Vector4f a, Vector4f b, float blend) {
        return lerp(a, b, blend, null);
    }

    /**
     * Returns a new Vector that is the linear blend of the 2 given Vectors.
     * @param a First input vector.
     * @param b Second input vector.
     * @param blend The blend factor. a when blend=0, b when blend=1.
     * @param output The vector to save the lerp results within
     * @return a when blend=0, b when blend=1, and a linear combination otherwise.
     */
    public static Vector4f lerp(Vector4f a, Vector4f b, float blend, Vector4f output) {
        if (output == null)
            output = new Vector4f();

        float resultX = blend * (b.x - a.x) + a.x;
        float resultY = blend * (b.y - a.y) + a.y;
        float resultZ = blend * (b.z - a.z) + a.z;
        float resultW = blend * (b.w - a.w) + a.w;
        return output.setXYZW(resultX, resultY, resultZ, resultW);
    }

    /**
     * Calculate the dot product of two vectors.
     * @param a the first input vector
     * @param b the second input vector
     * @return dotProduct
     */
    public static double dotProduct(Vector4f a, Vector4f b) {
        return (a.x * b.x) + (a.y * b.y) + (a.z * b.z) + (a.w * b.w);
    }
}