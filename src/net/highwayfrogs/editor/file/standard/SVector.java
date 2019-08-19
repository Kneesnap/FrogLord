package net.highwayfrogs.editor.file.standard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Vector comprised of shorts.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SVector extends GameObject implements Vector {
    private short x;
    private short y;
    private short z;

    public static final int UNPADDED_BYTE_SIZE = 3 * Constants.SHORT_SIZE;
    public static final int PADDED_BYTE_SIZE = UNPADDED_BYTE_SIZE + Constants.SHORT_SIZE;
    public static final SVector EMPTY = new SVector();

    public SVector(SVector clone) {
        this(clone.getX(), clone.getY(), clone.getZ());
    }

    public SVector(IVector clone) {
        this((short) clone.getX(), (short) clone.getY(), (short) clone.getZ());
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
     * Load a SVector with an extra 2 bytes of padding.
     * @param reader The reader to read from.
     */
    public void loadWithPadding(DataReader reader) {
        this.load(reader);
        reader.skipShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(getX());
        writer.writeShort(getY());
        writer.writeShort(getZ());
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
    public void setValues(float x, float y, float z, int bits) {
        this.x = Utils.floatToFixedPointShort(x, bits);
        this.y = Utils.floatToFixedPointShort(y, bits);
        this.z = Utils.floatToFixedPointShort(z, bits);
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
     * Write an SVector with an extra 2 bytes of padding.
     * @param writer The writer to write data to.
     */
    public void saveWithPadding(DataWriter writer) {
        save(writer);
        writer.writeNull(Constants.SHORT_SIZE);
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
        return Utils.fixedPointShortToFloatNBits(getX(), bits);
    }

    @Override
    public float getFloatY(int bits) {
        return Utils.fixedPointShortToFloatNBits(getY(), bits);
    }

    @Override
    public float getFloatZ(int bits) {
        return Utils.fixedPointShortToFloatNBits(getZ(), bits);
    }

    @Override
    public void setFloatX(float xVal, int bits) {
        this.x = Utils.floatToFixedPointShort(xVal, bits);
    }

    @Override
    public void setFloatY(float yVal, int bits) {
        this.y = Utils.floatToFixedPointShort(yVal, bits);
    }

    @Override
    public void setFloatZ(float zVal, int bits) {
        this.z = Utils.floatToFixedPointShort(zVal, bits);
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
            if (!Utils.isNumber(testStr))
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
}
