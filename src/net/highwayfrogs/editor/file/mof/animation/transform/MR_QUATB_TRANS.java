package net.highwayfrogs.editor.file.mof.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents 'MR_QUATB_TRANS'.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MR_QUATB_TRANS extends TransformObject {
    private byte c; // 'real'.
    private byte x; // Angle.
    private byte y;
    private byte z;
    private short[] transform = new short[3];

    @Override
    public void load(DataReader reader) {
        this.c = reader.readByte();
        this.x = reader.readByte();
        this.y = reader.readByte();
        this.z = reader.readByte();

        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.c);
        writer.writeByte(this.x);
        writer.writeByte(this.y);
        writer.writeByte(this.z);

        for (short aTransfer : this.transform)
            writer.writeShort(aTransfer);
    }

    @Override
    public int hashCode() {
        return ((this.c & 0xFF) << 24) | ((this.x & 0xFF) << 16) | ((this.y & 0xFF) << 8) | (this.z & 0xFF);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MR_QUATB_TRANS))
            return false;

        MR_QUATB_TRANS other = (MR_QUATB_TRANS) obj;
        return this.c == other.c && this.x == other.x && this.y == other.y && this.z == other.z && this.transform[0] == other.transform[0]
                && this.transform[1] == other.transform[1] && this.transform[2] == other.transform[2];
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) {
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
        return "MR_QUATB_TRANS<c=" + this.c + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ",tx=" + this.transform[0] + ",ty=" + this.transform[1] + ",tz=" + this.transform[2] + ">";
    }
}
