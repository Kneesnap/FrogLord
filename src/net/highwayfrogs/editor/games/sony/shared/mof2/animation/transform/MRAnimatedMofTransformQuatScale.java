package net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCMath;
import net.highwayfrogs.editor.utils.MathUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Arrays;

/**
 * Represents the 'MR_QUAT_SCALE_TRANS' struct for the 'MR_ANIM_FILE_ID_QUAT_SCALE_TRANSFORMS' animated MOF transform type.
 * Not Used in: Frogger (except build 4 PSX)
 * Used in: Beast Wars
 * Created by Kneesnap on 5/23/2020.
 */
@Getter
public class MRAnimatedMofTransformQuatScale extends MRAnimatedMofTransform {
    private short c; // 'real'. (1.3.12 format)
    private short x; // Angle.
    private short y;
    private short z;
    private final short[] translation = new short[3];
    private final byte[] scaling = new byte[3]; // 3.5 fixed point scaling.
    private byte flags;

    public static final byte FLAG_ENABLE_SCALING = Constants.BIT_FLAG_0;
    public static final byte VALIDATION_FLAGS = FLAG_ENABLE_SCALING;
    public static final int MR_QUAT_SCALE_TRANS_FIXED_POINT = 5;

    @Override
    public void load(DataReader reader) {
        this.c = reader.readShort();
        this.x = reader.readShort();
        this.y = reader.readShort();
        this.z = reader.readShort();
        for (int i = 0; i < this.translation.length; i++)
            this.translation[i] = reader.readShort();

        reader.readBytes(this.scaling);
        this.flags = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.c);
        writer.writeShort(this.x);
        writer.writeShort(this.y);
        writer.writeShort(this.z);
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
        if (!(obj instanceof MRAnimatedMofTransformQuatScale))
            return false;

        MRAnimatedMofTransformQuatScale other = (MRAnimatedMofTransformQuatScale) obj;
        return this.c == other.c && this.x == other.x && this.y == other.y && this.z == other.z
                && Arrays.equals(this.translation, other.translation) && Arrays.equals(this.scaling, other.scaling)
                && this.flags == other.flags;
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) { //TODO: This is copied. It needs to be updated for MR_QUAT_SCALE_TRANS.
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
        SCMath.quatToMatrix(this.c, this.x, this.y, this.z, matrix.getMatrix());

        if ((this.flags & FLAG_ENABLE_SCALING) == FLAG_ENABLE_SCALING) {
            int scaleX = (this.scaling[0] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            int scaleY = (this.scaling[1] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            int scaleZ = (this.scaling[2] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT));
            PSXMatrix.MRScaleMatrix(matrix, scaleX, scaleY, scaleZ);
        }

        return matrix;
    }

    @Override
    public String toString() {
        return "MR_QUAT_SCALE_TRANS{c=" + this.c + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ",tx=" + this.translation[0] + ",ty=" + this.translation[1] + ",tz=" + this.translation[2] + ",flags=" + this.flags + ",s=" + Arrays.toString(this.scaling) + "}";
    }

    /**
     * Interpolates two quat transforms between each other, the same as the MR API.
     * @param previousTransform the previous transform
     * @param nextTransform     the next transform
     * @param t                 the time factor
     * @return interpolatedResult
     */
    public static PSXMatrix interpolate(MRAnimatedMofTransform previousTransform, MRAnimatedMofTransform nextTransform, int t) {
        if (!(previousTransform instanceof MRAnimatedMofTransformQuatScale))
            throw new ClassCastException("The provided previousTransform was a(n) " + Utils.getSimpleName(previousTransform) + ", not MRAnimatedMofTransformQuatScale!");
        if (!(nextTransform instanceof MRAnimatedMofTransformQuatScale))
            throw new ClassCastException("The provided nextTransform was a(n) " + Utils.getSimpleName(nextTransform) + ", not MRAnimatedMofTransformQuatScale!");

        MRAnimatedMofTransformQuatScale prevQuat = (MRAnimatedMofTransformQuatScale) previousTransform;
        MRAnimatedMofTransformQuatScale nextQuat = (MRAnimatedMofTransformQuatScale) nextTransform;

        PSXMatrix result = new PSXMatrix();
        MRInterpolateQuaternions(prevQuat, nextQuat, result, (short) t);

        if ((prevQuat.flags & FLAG_ENABLE_SCALING) == FLAG_ENABLE_SCALING || (nextQuat.flags & FLAG_ENABLE_SCALING) == FLAG_ENABLE_SCALING) {
            // Get interpolated scaling values
            int sp = prevQuat.scaling[0] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT);
            int sn = nextQuat.scaling[0] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT);
            int scaleX = ((sp * (0x1000 - t)) + (sn * t)) >> 12;
            sp = prevQuat.scaling[1] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT);
            sn = nextQuat.scaling[1] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT);
            int scaleY = ((sp * (0x1000 - t)) + (sn * t)) >> 12;
            sp = prevQuat.scaling[2] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT);
            sn = nextQuat.scaling[2] << (12 - MR_QUAT_SCALE_TRANS_FIXED_POINT);
            int scaleZ = ((sp * (0x1000 - t)) + (sn * t)) >> 12;
            PSXMatrix.MRScaleMatrix(result, scaleX, scaleY, scaleZ);
        }

        result.getTransform()[0] = ((prevQuat.getTranslation()[0] * (0x1000 - t)) + (nextQuat.getTranslation()[0] * t)) >> 12;
        result.getTransform()[1] = ((prevQuat.getTranslation()[1] * (0x1000 - t)) + (nextQuat.getTranslation()[1] * t)) >> 12;
        result.getTransform()[2] = ((prevQuat.getTranslation()[2] * (0x1000 - t)) + (nextQuat.getTranslation()[2] * t)) >> 12;
        return result;
    }

    static void MRInterpolateQuaternions(MRAnimatedMofTransformQuatScale startq, MRAnimatedMofTransformQuatScale endq, PSXMatrix result, short t) {
        short destC, destX, destY, destZ;
        if (t == 0) {
            destC = startq.c;
            destX = startq.x;
            destY = startq.y;
            destZ = startq.z;
        } else {
            short cosOmega = (short) (((startq.c * endq.c) +
                    (startq.x * endq.x) +
                    (startq.y * endq.y) +
                    (startq.z * endq.z)) >> 12);    // -0x1000..0x1000

            // If the above dot product is negative, it would be better to go between the
            // negative of the initial and the final, so that we take the shorter path.
            boolean bflip = false;
            if (cosOmega < 0) {
                bflip = true;
                cosOmega = (short) -cosOmega;
            }

            // Usual case
            short endScale, startScale;
            if ((0x1000 - cosOmega) > SCMath.MR_QUAT_EPSILON) {
                // Usual case
                cosOmega = (short) Math.max(-0x1000, Math.min(0x1000, cosOmega));
                short omega = SCMath.acosRaw(cosOmega);                    // omega = acos(cosomega)
                short sinomega = SCMath.rsin(omega);

                int to = (t * omega) >> 12;
                endScale = (short) ((SCMath.rsin(to) << 12) / sinomega);
                startScale = (short) (SCMath.rcos(to) - ((cosOmega * endScale) >> 12));
            } else {
                // Ends very close
                startScale = (short) (0x1000 - t);
                endScale = t;
            }

            if (bflip)
                endScale = (short) -endScale;

            destC = (short) ((startScale * startq.c + endScale * endq.c) >> 12);
            destX = (short) ((startScale * startq.x + endScale * endq.x) >> 12);
            destY = (short) ((startScale * startq.y + endScale * endq.y) >> 12);
            destZ = (short) ((startScale * startq.z + endScale * endq.z) >> 12);
        }

        // Now, MR_QUAT_TO_MAT/MRQuaternionToMatrix
        SCMath.quatToMatrix(destC, destX, destY, destZ, result.getMatrix());
    }
}
