package net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.MathUtils;

import java.util.Arrays;

/**
 * Represents the 'MR_QUATB_SCALE_TRANS' struct for the 'MR_ANIM_FILE_ID_QUATB_SCALE_TRANSFORMS' animated MOF transform type.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MRAnimatedMofTransformScaleByte extends MRAnimatedMofTransform {
    private byte c; // 'real'.
    private byte x; // Angle.
    private byte y;
    private byte z;
    private final short[] translation = new short[3];
    private final byte[] scaling = new byte[3]; // 3.5 fixed point scaling.
    private byte flags; // bit 0 set if scaling used.

    public static final byte FLAG_ENABLE_SCALING = Constants.BIT_FLAG_0;
    public static final int MR_QUAT_SCALE_TRANS_FIXED_POINT = 5;

    @Override
    public void load(DataReader reader) {
        this.c = reader.readByte();
        this.x = reader.readByte();
        this.y = reader.readByte();
        this.z = reader.readByte();
        for (int i = 0; i < this.translation.length; i++)
            this.translation[i] = reader.readShort();

        reader.readBytes(this.scaling);
        this.flags = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.c);
        writer.writeByte(this.x);
        writer.writeByte(this.y);
        writer.writeByte(this.z);
        for (int i = 0; i < this.translation.length; i++)
            writer.writeShort(this.translation[i]);

        writer.writeBytes(this.scaling);
        writer.writeByte(this.flags);
    }

    @Override
    public int hashCode() {
        return (((this.c & 0xFF) << 24) | ((this.x & 0xFF) << 16) | ((this.y & 0xFF) << 8) | (this.z & 0xFF))
                ^ Arrays.hashCode(this.translation) ^ Arrays.hashCode(this.scaling);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MRAnimatedMofTransformScaleByte))
            return false;

        MRAnimatedMofTransformScaleByte other = (MRAnimatedMofTransformScaleByte) obj;
        return this.c == other.c && this.x == other.x && this.y == other.y && this.z == other.z &&
                Arrays.equals(this.translation, other.translation) && Arrays.equals(this.scaling, other.scaling)
                && this.flags == other.flags;
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) { //TODO: Update from MR_QUATB_TRANS
        this.translation[0] = (short) matrix.getTransform()[0];
        this.translation[1] = (short) matrix.getTransform()[1];
        this.translation[2] = (short) matrix.getTransform()[2];

        int trace = matrix.getMatrix()[0][0] + matrix.getMatrix()[1][1] + matrix.getMatrix()[2][2];
        if (trace > 0) {
            int s = MathUtils.fixedSqrt((trace + 0x1000) << 12);
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
            int s = MathUtils.fixedSqrt(((matrix.getMatrix()[i][i] - (matrix.getMatrix()[j][j] + matrix.getMatrix()[k][k])) + 0x1000) << 12);
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
        matrix.getTransform()[0] = this.translation[0];
        matrix.getTransform()[1] = this.translation[1];
        matrix.getTransform()[2] = this.translation[2];
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
        matrix.getMatrix()[0][0] = (short) (0x1000 - (yy + zz));
        matrix.getMatrix()[0][1] = (short) (xy + wz);
        matrix.getMatrix()[0][2] = (short) (xz - wy);
        matrix.getMatrix()[1][0] = (short) (xy - wz);
        matrix.getMatrix()[1][1] = (short) (0x1000 - (xx + zz));
        matrix.getMatrix()[1][2] = (short) (yz + wx);
        matrix.getMatrix()[2][0] = (short) (xz + wy);
        matrix.getMatrix()[2][1] = (short) (yz - wx);
        matrix.getMatrix()[2][2] = (short) (0x1000 - (xx + yy));

        if ((this.flags & FLAG_ENABLE_SCALING) == FLAG_ENABLE_SCALING) { // If scaling is enabled.
            int scaleX = (this.scaling[0] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            int scaleY = (this.scaling[1] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            int scaleZ = (this.scaling[2] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            PSXMatrix.MRScaleMatrix(matrix, scaleX, scaleY, scaleZ);
        }
    }

    @Override
    public String toString() {
        return "MR_QUATB_SCALE_TRANS{c=" + this.c + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ",tx=" + this.translation[0] + ",ty=" + this.translation[1] + ",tz=" + this.translation[2] + ",flags=" + this.flags + ",s=" + Arrays.toString(this.scaling) + "}";
    }
}
