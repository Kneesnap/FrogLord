package net.highwayfrogs.editor.file.standard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.DataUtils;

/**
 * Vector comprised of integers.
 * Created by Kneesnap on 8/24/2018.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class IVector extends GameObject implements Vector {
    private int x;
    private int y;
    private int z;

    public static final int UNPADDED_BYTE_SIZE = 3 * Constants.INTEGER_SIZE;
    public static final int PADDED_BYTE_SIZE = UNPADDED_BYTE_SIZE + Constants.INTEGER_SIZE;

    public IVector(IVector other) {
        this(other.x, other.y, other.z);
    }

    @Override
    public void load(DataReader reader) {
        this.x = reader.readInt();
        this.y = reader.readInt();
        this.z = reader.readInt();
    }

    /**
     * Load an IVector with an extra 4 bytes of padding.
     * @param reader The reader to read from.
     */
    public void loadWithPadding(DataReader reader) {
        this.load(reader);
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getX());
        writer.writeInt(getY());
        writer.writeInt(getZ());
    }

    /**
     * Write an IVector with an extra 4 bytes of padding.
     * @param writer The writer to write data to.
     */
    public void saveWithPadding(DataWriter writer) {
        save(writer);
        writer.writeNull(Constants.INTEGER_SIZE);
    }

    /**
     * Calculates the dot product between this vector and another vector.
     * @param other the other vector
     * @return dotProduct
     */
    public int dotProduct(IVector other) {
        return (this.x * other.x) + (this.y * other.y) + (this.z * other.z);
    }

    /**
     * Clears the contents of the vector.
     */
    public void clear() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    /**
     * Set the values of this vector.
     * @param copyVector the vector to copy values from
     */
    public void setValues(IVector copyVector) {
        this.x = copyVector.x;
        this.y = copyVector.y;
        this.z = copyVector.z;
    }

    /**
     * Set the values of this vector.
     * @param x The x value to set.
     * @param y The y value to set.
     * @param z The z value to set.
     */
    public void setValues(int x, int y, int z) {
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
        this.x = DataUtils.floatToFixedPointInt(x, bits);
        this.y = DataUtils.floatToFixedPointInt(y, bits);
        this.z = DataUtils.floatToFixedPointInt(z, bits);
    }

    /**
     * Equivalent to MR_VEC_EQUALS_SVEC
     * @param vec The array to read info from.
     */
    public void vecEqualsSvec(short[] vec) {
        this.x = vec[0];
        this.y = vec[1];
        this.z = vec[2];
    }

    /**
     * Equivalent to MR_VEC_EQUALS_SVEC
     * @param vec The array to read info from.
     */
    public void vecEqualsSvec(SVector vec) {
        this.x = vec.getX();
        this.y = vec.getY();
        this.z = vec.getZ();
    }

    /**
     * Equivalent to MRNormaliseVec ?? I think [AndyEder]
     */
    public IVector normalise() {
        /*long oldX = this.x;
        long oldY = this.y;
        long oldZ = this.z;
        long added = (oldX * oldX) + (oldY * oldY) + (oldZ * oldZ);
        if (added > 134217727L || added < 0L) // LIBREF46.PDF lists this restriction as throwing a processor exception.
            throw new RuntimeException("Tried to normalise a vector which exceeded the limit! " + added);
         */

        double tmpX = DataUtils.fixedPointIntToFloatNBits(this.x, 12);
        double tmpY = DataUtils.fixedPointIntToFloatNBits(this.y, 12);
        double tmpZ = DataUtils.fixedPointIntToFloatNBits(this.z, 12);

        double res = Math.sqrt((tmpX * tmpX) + (tmpY * tmpY) + (tmpZ * tmpZ));
        if (Math.abs(res) <= .000001)
            return this; // This is something I've added, I don't know what the actual game does here. Should probably figure that out at some point.

        tmpX /= res;
        tmpY /= res;
        tmpZ /= res;

        this.x = DataUtils.floatToFixedPointInt((float) tmpX, 12);
        this.y = DataUtils.floatToFixedPointInt((float) tmpY, 12);
        this.z = DataUtils.floatToFixedPointInt((float) tmpZ, 12);
        return this;
    }

    /**
     * Equivalent to MROuterProduct12 ?? I think [AndyEder]
     */
    public void outerProduct12(IVector vec0, IVector vec1) {
        this.x = (((vec0.getY() * vec1.getZ()) - (vec0.getZ() * vec1.getY())) >> 12);
        this.y = (((vec0.getZ() * vec1.getX()) - (vec0.getX() * vec1.getZ())) >> 12);
        this.z = (((vec0.getX() * vec1.getY()) - (vec0.getY() * vec1.getX())) >> 12);
    }

    /**
     * Equivalent to MR_ADD_VEC(b)
     * @param other The vector to add.
     */
    public IVector add(IVector other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }

    /**
     * Adds the values in another vector to this one.
     * @param other The vector to add.
     */
    public IVector add(SVector other) {
        this.x += other.getX();
        this.y += other.getY();
        this.z += other.getZ();
        return this;
    }

    @Override
    public String toString() {
        return toString0();
    }

    @Override
    public float getFloatX(int bits) {
        return DataUtils.fixedPointIntToFloatNBits(getX(), bits);
    }

    @Override
    public float getFloatY(int bits) {
        return DataUtils.fixedPointIntToFloatNBits(getY(), bits);
    }

    @Override
    public float getFloatZ(int bits) {
        return DataUtils.fixedPointIntToFloatNBits(getZ(), bits);
    }

    @Override
    public void setFloatX(float xVal, int bits) {
        this.x = DataUtils.floatToFixedPointInt(xVal, bits);
    }

    @Override
    public void setFloatY(float yVal, int bits) {
        this.y = DataUtils.floatToFixedPointInt(yVal, bits);
    }

    @Override
    public void setFloatZ(float zVal, int bits) {
        this.z = DataUtils.floatToFixedPointInt(zVal, bits);
    }

    /**
     * Load a SVector with padding from a DataReader.
     * @param reader The data reader to read from.
     * @return vector
     */
    public static IVector readWithPadding(DataReader reader) {
        IVector vector = new IVector();
        vector.loadWithPadding(reader);
        return vector;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IVector))
            return false;

        IVector otherV = (IVector) other;
        return otherV.getX() == getX() && otherV.getY() == getY() && otherV.getZ() == getZ();
    }

    public static void MROuterProduct12(IVector vec0, IVector vec1, IVector output) {
        output.outerProduct12(vec0, vec1);
    }
}