package net.highwayfrogs.editor.file.mof.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;

/**
 * Represents 'MR_QUAT_SCALE_TRANS'.
 * Not Used in: Frogger
 * Used in: Beast Wars
 * Created by Kneesnap on 5/23/2020.
 */
@Getter
public class MR_QUAT_SCALE_TRANS extends TransformObject {
    private short c; // 'real'. (1.3.12 format)
    private short x; // Angle.
    private short y;
    private short z;
    private short[] transform = new short[3];
    private byte[] scaling = new byte[3];
    private byte flags;


    public static final byte FLAG_ENABLE_SCALING = Constants.BIT_FLAG_0;
    public static final int MR_QUAT_SCALE_TRANS_FIXED_POINT = 5;

    @Override
    public void load(DataReader reader) {
        this.c = reader.readShort();
        this.x = reader.readShort();
        this.y = reader.readShort();
        this.z = reader.readShort();

        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readShort();

        this.scaling = reader.readBytes(3);
        this.flags = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.c);
        writer.writeShort(this.x);
        writer.writeShort(this.y);
        writer.writeShort(this.z);

        for (short aTransfer : this.transform)
            writer.writeShort(aTransfer);

        writer.writeBytes(this.scaling);
        writer.writeByte(this.flags);
    }

    @Override
    public int hashCode() {
        return ((this.c & 0xFF) << 24) | ((this.x & 0xFF) << 16) | ((this.y & 0xFF) << 8) | (this.z & 0xFF);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MR_QUAT_SCALE_TRANS))
            return false;

        MR_QUAT_SCALE_TRANS other = (MR_QUAT_SCALE_TRANS) obj;
        return this.c == other.c && this.x == other.x && this.y == other.y && this.z == other.z && Arrays.equals(this.transform, other.transform)
                && Arrays.equals(this.scaling, other.scaling) && this.flags == other.flags;
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) { //TODO: This is copied. It needs to be updated for MR_QUAT_SCALE_TRANS.
        this.transform[0] = (short) matrix.getTransform()[0];
        this.transform[1] = (short) matrix.getTransform()[1];
        this.transform[2] = (short) matrix.getTransform()[2];

        int trace = matrix.getMatrix()[0][0] + matrix.getMatrix()[1][1] + matrix.getMatrix()[2][2];
        if (trace > 0) {
            int s = Utils.fixedSqrt((trace + 0x1000) << 12);
            this.c = (byte) (s >> 7);
            this.x = (byte) (((matrix.getMatrix()[1][2] - matrix.getMatrix()[2][1]) << 5) / s);
            this.y = (byte) (((matrix.getMatrix()[2][0] - matrix.getMatrix()[0][2]) << 5) / s);
            this.z = (byte) (((matrix.getMatrix()[0][1] - matrix.getMatrix()[1][0]) << 5) / s);
        } else {
            int i = 0;
            if (matrix.getMatrix()[1][1] > matrix.getMatrix()[0][0])
                i = 1;
            if (matrix.getMatrix()[2][2] > matrix.getMatrix()[i][i])
                i = 2;

            int j = (i == 2 ? 0 : i + 1);
            int k = (j == 2 ? 0 : j + 1);
            int s = Utils.fixedSqrt(((matrix.getMatrix()[i][i] - (matrix.getMatrix()[j][j] + matrix.getMatrix()[k][k])) + 0x1000) << 12);
            byte v1 = (byte) (s >> 7);
            byte v2 = (byte) (((matrix.getMatrix()[i][j] + matrix.getMatrix()[j][i]) << 5) / s);
            byte v3 = (byte) (((matrix.getMatrix()[i][k] + matrix.getMatrix()[k][i]) << 5) / s);
            this.c = (byte) (((matrix.getMatrix()[j][k] - matrix.getMatrix()[k][j]) << 5) / s);

            switch (i) {
                case 0:
                    this.x = v1;
                    this.y = v2;
                    this.z = v3;
                    break;
                case 1:
                    this.y = v1;
                    this.z = v2;
                    this.x = v3;
                    break;
                case 2:
                    this.z = v1;
                    this.x = v2;
                    this.y = v3;
                    break;
            }
        }
    }

    @Override
    public PSXMatrix createMatrix() {
        PSXMatrix matrix = new PSXMatrix();
        matrix.getTransform()[0] = getTransform()[0];
        matrix.getTransform()[1] = getTransform()[1];
        matrix.getTransform()[2] = getTransform()[2];
        applyToMatrix(matrix);
        return matrix;
    }

    private void applyToMatrix(PSXMatrix matrix) {
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
        matrix.getMatrix()[0][0] = (short) (0x1000 - ((yy + zz) >> 12));
        matrix.getMatrix()[0][1] = (short) ((xy + wz) >> 12);
        matrix.getMatrix()[0][2] = (short) ((xz - wy) >> 12);
        matrix.getMatrix()[1][0] = (short) ((xy - wz) >> 12);
        matrix.getMatrix()[1][1] = (short) (0x1000 - ((xx + zz) >> 12));
        matrix.getMatrix()[1][2] = (short) ((yz + wx) >> 12);
        matrix.getMatrix()[2][0] = (short) ((xz + wy) >> 12);
        matrix.getMatrix()[2][1] = (short) ((yz - wx) >> 12);
        matrix.getMatrix()[2][2] = (short) (0x1000 - ((xx + yy) >> 12));

        if ((this.flags & FLAG_ENABLE_SCALING) == FLAG_ENABLE_SCALING) {
            int scaleX = (this.scaling[0] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            int scaleY = (this.scaling[1] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            int scaleZ = (this.scaling[2] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            PSXMatrix.MRScaleMatrix(matrix, scaleX, scaleY, scaleZ);
        }
    }

    @Override
    public String toString() {
        return "MR_QUAT_SCALE_TRANS<c=" + this.c + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ",tx=" + this.transform[0] + ",ty=" + this.transform[1] + ",tz=" + this.transform[2] + ",flags=" + this.flags + ",s=" + Arrays.toString(this.scaling) + ">";
    }
}
