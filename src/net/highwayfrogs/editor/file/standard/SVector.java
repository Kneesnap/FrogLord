package net.highwayfrogs.editor.file.standard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Vector comprised of shorts.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SVector extends GameObject {
    private short x;
    private short y;
    private short z;

    public static final int UNPADDED_BYTE_SIZE = 3 * Constants.SHORT_SIZE;
    public static final int PADDED_BYTE_SIZE = UNPADDED_BYTE_SIZE + Constants.SHORT_SIZE;
    public static final SVector EMPTY = new SVector();

    public SVector(SVector clone) {
        this(clone.getX(), clone.getY(), clone.getZ());
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
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(getX());
        writer.writeShort(getY());
        writer.writeShort(getZ());
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
     * Add another SVector to this one.
     * @param other The other SVector to add.
     */
    public void add(SVector other) {
        this.x += other.getX();
        this.y += other.getY();
        this.z += other.getZ();
    }

    /**
     * Subtract another SVector from this one.
     * @param other The other SVector to subtract.
     */
    public void subtract(SVector other) {
        this.x -= other.getX();
        this.y -= other.getY();
        this.z -= other.getZ();
    }

    /**
     * Multiply the values in this SVector.
     * @param multiplier The multiplier.
     */
    public void multiply(double multiplier) {
        this.x *= multiplier;
        this.y *= multiplier;
        this.z *= multiplier;
    }

    /**
     * Set all values held to zero.
     */
    public void zero() {
        this.x = (short) 0;
        this.y = (short) 0;
        this.z = (short) 0;
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

    /**
     * Get this vector as a Wavefront-OBJ vertex command.
     * @return vertexCommandString
     */
    public String toOBJString() {
        return "v " + -Utils.fixedPointShortToFloatNBits(getX(), 4) + " " + -Utils.fixedPointShortToFloatNBits(getY(), 4) + " " + Utils.fixedPointShortToFloatNBits(getZ(), 4);
    }

    /**
     * Get a coordinate string of this vector.
     * @return coordinateString
     */
    public String toCoordinateString() {
        return getX() + ", " + getY() + ", " + getZ();
    }

    @Override
    public String toString() {
        return "SVector<" + toCoordinateString() + ">";
    }
}
