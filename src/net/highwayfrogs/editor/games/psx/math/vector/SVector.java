package net.highwayfrogs.editor.games.psx.math.vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Represents the PSX "SVECTOR" struct, defined in 'libgte.h' of the PSX PsyQ SDK.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SVector implements IBinarySerializable, Vector {
    private short x;
    private short y;
    private short z;
    private short padding; // You'd think this is all zero, but it seems like most (all?) of the later SC games stores vertex colors here.

    public static final int UNPADDED_BYTE_SIZE = 3 * Constants.SHORT_SIZE;
    public static final int PADDED_BYTE_SIZE = UNPADDED_BYTE_SIZE + Constants.SHORT_SIZE;
    public static final SVector EMPTY = new SVector();

    public SVector(SVector clone) {
        this(clone.getX(), clone.getY(), clone.getZ());
    }

    public SVector(IVector clone) {
        this((short) clone.getX(), (short) clone.getY(), (short) clone.getZ());
    }

    public SVector(int x, int y, int z) {
        setX((short) x);
        setY((short) y);
        setZ((short) z);
    }

    public SVector(float x, float y, float z) {
        setFloatX(x);
        setFloatY(y);
        setFloatZ(z);
    }

    @Override
    public void load(DataReader reader) {
        this.x = reader.readShort();
        this.y = reader.readShort();
        this.z = reader.readShort();
    }

    /**
     * Gets the padding unsigned.
     */
    public int getUnsignedPadding() {
        return DataUtils.shortToUnsignedInt(this.padding);
    }

    /**
     * Load a SVector with an extra 2 bytes of padding.
     * @param reader The reader to read from.
     */
    public void loadWithPadding(DataReader reader) {
        this.load(reader);
        this.padding = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(getX());
        writer.writeShort(getY());
        writer.writeShort(getZ());
    }

    /**
     * Clears the contents of the vector.
     */
    public void clear() {
        this.x = (short) 0;
        this.y = (short) 0;
        this.z = (short) 0;
        this.padding = (short) 0;
    }

    /**
     * Set the values of this vector.
     * @param copyVector the vector to copy values from
     */
    public void setValues(SVector copyVector) {
        this.x = copyVector.x;
        this.y = copyVector.y;
        this.z = copyVector.z;
        this.padding = copyVector.padding;
    }

    /**
     * Set the values of this vector.
     * @param x The x value to set.
     * @param y The y value to set.
     * @param z The z value to set.
     */
    public void setValues(short x, short y, short z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Set the values of this vector.
     * @param x The x value to set.
     * @param y The y value to set.
     * @param z The z value to set.
     */
    public void setValues(float x, float y, float z) {
        setValues(x, y, z, defaultBits());
    }

    /**
     * Set the values of this vector.
     * @param x The x value to set.
     * @param y The y value to set.
     * @param z The z value to set.
     */
    public void setValues(float x, float y, float z, int bits) {
        this.x = DataUtils.floatToFixedPointShort(x, bits);
        this.y = DataUtils.floatToFixedPointShort(y, bits);
        this.z = DataUtils.floatToFixedPointShort(z, bits);
    }

    /**
     * Equivalent to MR_SVEC_EQUALS_VEC
     * @param vec The array to read info from.
     */
    public void svecEqualsVec(int[] vec) {
        this.x = (short) vec[0];
        this.y = (short) vec[1];
        this.z = (short) vec[2];
    }

    /**
     * Equivalent to MR_SVEC_EQUALS_VEC
     * @param vec The array to read info from.
     */
    public void svecEqualsVec(IVector vec) {
        this.x = (short) vec.getX();
        this.y = (short) vec.getY();
        this.z = (short) vec.getZ();
    }

    /**
     * Clones the SVector.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public SVector clone() {
        return new SVector(this.x, this.y, this.z);
    }

    /**
     * Add another SVector to this one.
     * @param other The other SVector to add.
     */
    public SVector add(SVector other) {
        this.x += other.getX();
        this.y += other.getY();
        this.z += other.getZ();
        return this;
    }

    /**
     * Subtract another SVector from this one.
     * @param other The other SVector to subtract.
     */
    public SVector subtract(SVector other) {
        this.x -= other.getX();
        this.y -= other.getY();
        this.z -= other.getZ();
        return this;
    }

    /**
     * Multiply SVector.
     * @param factor The factor.
     */
    public SVector multiply(double factor) {
        this.x *= factor;
        this.y *= factor;
        this.z *= factor;
        return this;
    }

    /**
     * Write an SVector with an extra 2 bytes of padding.
     * @param writer The writer to write data to.
     */
    public void saveWithPadding(DataWriter writer) {
        save(writer);
        writer.writeShort(this.padding);
    }

    /**
     * Load a SVector with padding from a DataReader.
     * @param reader The data reader to read from.
     * @return vector
     */
    public static SVector readWithPadding(DataReader reader) {
        SVector vector = new SVector();
        vector.loadWithPadding(reader);
        return vector;
    }

    /**
     * Load a SVector without padding from a DataReader.
     * @param reader The data reader to read from.
     * @return vector
     */
    public static SVector readWithoutPadding(DataReader reader) {
        SVector vector = new SVector();
        vector.load(reader);
        return vector;
    }

    @Override
    public int hashCode() {
        return (this.x & 0xF800) + (this.z & 0x7C0) + (this.y & 0x3F);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SVector))
            return false;

        SVector otherV = (SVector) other;
        return otherV.getX() == getX() && otherV.getY() == getY() && otherV.getZ() == getZ();
    }

    @Override
    public float getFloatX(int bits) {
        return DataUtils.fixedPointShortToFloatNBits(getX(), bits);
    }

    @Override
    public float getFloatY(int bits) {
        return DataUtils.fixedPointShortToFloatNBits(getY(), bits);
    }

    @Override
    public float getFloatZ(int bits) {
        return DataUtils.fixedPointShortToFloatNBits(getZ(), bits);
    }

    @Override
    public void setFloatX(float xVal, int bits) {
        this.x = DataUtils.floatToFixedPointShort(xVal, bits);
    }

    @Override
    public void setFloatY(float yVal, int bits) {
        this.y = DataUtils.floatToFixedPointShort(yVal, bits);
    }

    @Override
    public void setFloatZ(float zVal, int bits) {
        this.z = DataUtils.floatToFixedPointShort(zVal, bits);
    }

    /**
     * Load SVector data from float text.
     * @param text The text to read SVector data from.
     * @return loadedSuccessfully
     */
    public boolean loadFromFloatText(String text) {
        return loadFromFloatText(text, 4);
    }

    /**
     * Load SVector data from float text.
     * @param text The text to read SVector data from.
     * @return loadedSuccessfully
     */
    public boolean loadFromFloatText(String text, int bits) {
        text = text.replace(" ", "");
        if (!text.contains(","))
            return false;

        String[] split = text.split(",");
        if (split.length != 3)
            return false;

        for (String testStr : split)
            if (!NumberUtils.isNumber(testStr))
                return false;

        setFloatX(Float.parseFloat(split[0]), bits);
        setFloatY(Float.parseFloat(split[1]), bits);
        setFloatZ(Float.parseFloat(split[2]), bits);
        return true;
    }

    @Override
    public String toString() {
        return toString0();
    }

    /**
     * Get the squared distance between this and another SVector.
     * @param other the other vector
     * @return distanceSquared
     */
    public double distanceSquared(SVector other) {
        return ((double) (other.getFloatX() - getFloatX()) * (double) (other.getFloatX() - getFloatX()))
                + ((double) (other.getFloatY() - getFloatY()) * (double) (other.getFloatY() - getFloatY()))
                + ((double) (other.getFloatZ() - getFloatZ()) * (double) (other.getFloatZ() - getFloatZ()));
    }
}