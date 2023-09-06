package net.highwayfrogs.editor.file.mof.animation.transform;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.Arrays;

/**
 * Implements the 'MR_QUAT_SCALE' transform type.
 * This has only been seen in Frogger, build 4 PSX.
 * Created by Kneesnap on 2/7/2023.
 */
public class MR_QUAT_SCALE extends TransformObject {
    private short c; // 'real'.
    private short x; // Angle.
    private short y;
    private short z;
    private final short[] transform = new short[3];
    private final short[] scale = new short[3]; // 3.5 fixed point scaling.
    private short flags;

    private static final PSXMatrix SCALE_MATRIX = PSXMatrix.newIdentityMatrix();

    @Override
    public void load(DataReader reader) {
        this.c = reader.readShort();
        this.x = reader.readShort();
        this.y = reader.readShort();
        this.z = reader.readShort();

        // There does not appear to be any padding between these.
        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readShort();
        for (int i = 0; i < this.scale.length; i++)
            this.scale[i] = reader.readUnsignedByteAsShort();
        this.flags = reader.readUnsignedByteAsShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.c);
        writer.writeShort(this.x);
        writer.writeShort(this.y);
        writer.writeShort(this.z);

        // There does not appear to be any padding between these.
        for (short aTransfer : this.transform)
            writer.writeShort(aTransfer);
        for (short aScale : this.scale)
            writer.writeUnsignedShort(aScale);
        writer.writeUnsignedShort(this.flags);
    }

    @Override
    public int hashCode() {
        return ((this.x | this.z) << 16) | (this.y | this.c);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MR_QUAT_SCALE))
            return false;

        MR_QUAT_SCALE other = (MR_QUAT_SCALE) obj;
        return this.x == other.x && this.y == other.y && this.z == other.z && this.c == other.c
                && this.flags == other.flags && Arrays.equals(this.transform, other.transform)
                && Arrays.equals(this.scale, other.scale);
    }

    @Override
    public short[] getTransform() {
        return this.transform;
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) {
        // I've only seen this in a single build of Frogger, seems a little pointless to support now. Consider in new FrogLord.
        throw new UnsupportedOperationException("Not currently supported.");
    }

    @Override
    public PSXMatrix createMatrix() {
        PSXMatrix matrix = new PSXMatrix();

        // Setup matrix.
        applyToMatrix(matrix, this.c, this.x, this.y, this.z);

        if ((this.flags & 1) == 1) {
            SCALE_MATRIX.getMatrix()[0][0] = (short) (this.scale[0] << 7);
            SCALE_MATRIX.getMatrix()[1][1] = (short) (this.scale[1] << 7);
            SCALE_MATRIX.getMatrix()[2][2] = (short) (this.scale[2] << 7);
            PSXMatrix.MRMulMatrixABB(SCALE_MATRIX, matrix);
        }

        // Apply transform.
        matrix.getTransform()[0] = getTransform()[0];
        matrix.getTransform()[1] = getTransform()[1];
        matrix.getTransform()[2] = getTransform()[2];
        return matrix;
    }

    private static void applyToMatrix(PSXMatrix matrix, short c, short x, short y, short z) {
        int xs = x << 1;
        int ys = y << 1;
        int zs = z << 1;
        int wx = c * xs;
        int wy = c * ys;
        int wz = c * zs;
        int xx = x * xs;
        int xy = x * ys;
        int xz = x * zs;
        int yy = y * ys;
        int yz = y * zs;
        int zz = z * zs;

        // Oddly, every set is bit-shifted right 0 places. Not sure what that does, maybe it does something special in C.
        matrix.getMatrix()[0][0] = (short) (0x1000 - (yy + zz));
        matrix.getMatrix()[0][1] = (short) (xy + wz);
        matrix.getMatrix()[0][2] = (short) (xz - wy);
        matrix.getMatrix()[1][0] = (short) (xy - wz);
        matrix.getMatrix()[1][1] = (short) (0x1000 - (xx + zz));
        matrix.getMatrix()[1][2] = (short) (yz + wx);
        matrix.getMatrix()[2][0] = (short) (xz + wy);
        matrix.getMatrix()[2][1] = (short) (yz - wx);
        matrix.getMatrix()[2][2] = (short) (0x1000 - (xx + yy));
    }
}