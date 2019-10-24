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
        reader.skipInt();
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
        double[] tmpVec = new double[3];
        tmpVec[0] = this.x;
        tmpVec[1] = this.y;
        tmpVec[2] = this.z;

        double res = Math.sqrt((tmpVec[0] * tmpVec[0]) + (tmpVec[1] * tmpVec[1]) + (tmpVec[2] * tmpVec[2]));
        tmpVec[0] /= res;
        tmpVec[1] /= res;
        tmpVec[2] /= res;

        tmpVec[0] *= (4096);   // 4096 => (1 << 12)
        tmpVec[1] *= (4096);
        tmpVec[2] *= (4096);

        this.x = (int)tmpVec[0];
        this.y = (int)tmpVec[1];
        this.z = (int)tmpVec[2];
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
        return Utils.fixedPointIntToFloatNBits(getX(), bits);
    }

    @Override
    public float getFloatY(int bits) {
        return Utils.fixedPointIntToFloatNBits(getY(), bits);
    }

    @Override
    public float getFloatZ(int bits) {
        return Utils.fixedPointIntToFloatNBits(getZ(), bits);
    }

    @Override
    public void setFloatX(float xVal, int bits) {
        this.x = Utils.floatToFixedPointInt(xVal, bits);
    }

    @Override
    public void setFloatY(float yVal, int bits) {
        this.y = Utils.floatToFixedPointInt(yVal, bits);
    }

    @Override
    public void setFloatZ(float zVal, int bits) {
        this.z = Utils.floatToFixedPointInt(zVal, bits);
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
