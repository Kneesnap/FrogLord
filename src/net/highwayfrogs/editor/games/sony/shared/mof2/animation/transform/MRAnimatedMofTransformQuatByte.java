package net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.MathUtils;

import java.util.Arrays;

/**
 * Represents the 'MR_QUATB_TRANS' struct for the 'MR_ANIM_FILE_ID_QUATB_TRANSFORMS' animated MOF transform type.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MRAnimatedMofTransformQuatByte extends MRAnimatedMofTransform {
    private byte c; // 'real'.
    private byte x; // Angle.
    private byte y;
    private byte z;
    private final short[] translation = new short[3];

    @Override
    public void load(DataReader reader) {
        this.c = reader.readByte();
        this.x = reader.readByte();
        this.y = reader.readByte();
        this.z = reader.readByte();
        for (int i = 0; i < this.translation.length; i++)
            this.translation[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.c);
        writer.writeByte(this.x);
        writer.writeByte(this.y);
        writer.writeByte(this.z);
        for (int i = 0; i < this.translation.length; i++)
            writer.writeShort(this.translation[i]);
    }

    @Override
    public int hashCode() {
        return (((this.c & 0xFF) << 24) | ((this.x & 0xFF) << 16) | ((this.y & 0xFF) << 8) | (this.z & 0xFF)) ^ Arrays.hashCode(this.translation);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MRAnimatedMofTransformQuatByte))
            return false;

        MRAnimatedMofTransformQuatByte other = (MRAnimatedMofTransformQuatByte) obj;
        return this.c == other.c && this.x == other.x && this.y == other.y && this.z == other.z && Arrays.equals(this.translation, other.translation);
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) {
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
    public PSXMatrix createInterpolatedResult() { //TODO: Move,. https://github.com/Kneesnap/Frogger/blob/a81c28bceea2f8696f9e399ee260180ae00dc7a8/source/API.SRC/MR_ANIM.C is how this is calculated. and https://github.com/Kneesnap/Frogger/blob/master/source/API.SRC/MR_QUAT.C
        // index_ptr	= cels_ptr->ac_cel_numbers + (params->ac_cel * 3);
        //
        //				// index_ptr points to a group of 3 MR_USHORTs (prev actual cel index, next actual cel index, interpolation param)
        //				quatb_prev 	= (MR_QUATB_TRANS*)(((MR_UBYTE*)env->ae_header->ah_common_data->ac_transforms) + ((cels_ptr->ac_transforms.ac_indices[(index_ptr[0] * parts) + part]) * tsize));
        //				quatb_next 	= (MR_QUATB_TRANS*)(((MR_UBYTE*)env->ae_header->ah_common_data->ac_transforms) + ((cels_ptr->ac_transforms.ac_indices[(index_ptr[1] * parts) + part]) * tsize));
        //				t			= index_ptr[2];
        //				MR_INTERPOLATE_QUATB_TO_MAT(&quatb_prev->q, &quatb_next->q, (MR_MAT*)&MRTemp_matrix, t);

        // ((MR_MAT34*)&MRTemp_matrix)->t[0]	= ((quatb_prev->t[0] * (0x1000 - t)) + (quatb_next->t[0] * t)) >> 12;
        //				((MR_MAT34*)&MRTemp_matrix)->t[1]	= ((quatb_prev->t[1] * (0x1000 - t)) + (quatb_next->t[1] * t)) >> 12;
        //				((MR_MAT34*)&MRTemp_matrix)->t[2]	= ((quatb_prev->t[2] * (0x1000 - t)) + (quatb_next->t[2] * t)) >> 12;
        return createMatrix();
    }

    @Override
    public PSXMatrix createMatrix() {
        PSXMatrix matrix = new PSXMatrix();
        matrix.getTransform()[0] = this.translation[0];
        matrix.getTransform()[1] = this.translation[1];
        matrix.getTransform()[2] = this.translation[2];
        applyToMatrix(matrix, this.c, this.x, this.y, this.z);
        return matrix;
    }

    private static void applyToMatrix(PSXMatrix matrix, byte c, byte x, byte y, byte z) {
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

    @Override
    public String toString() {
        return "MR_QUATB_TRANS{c=" + this.c + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ",tx=" + this.translation[0] + ",ty=" + this.translation[1] + ",tz=" + this.translation[2] + "}";
    }
}
