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
    private byte x; // Presumably delta?
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

        short wx = (short) (((int) matrix.getMatrix()[1][2] - (int) matrix.getMatrix()[2][1]) / 2);
        short wy = (short) (((int) matrix.getMatrix()[2][0] - (int) matrix.getMatrix()[0][2]) / 2);
        short wz = (short) (((int) matrix.getMatrix()[0][1] - (int) matrix.getMatrix()[1][0]) / 2);

        PSXMatrix tempMatrix = new PSXMatrix();
        float bestDistance = Float.MAX_VALUE;
        for (byte c = Byte.MIN_VALUE; c < Byte.MAX_VALUE; c++) {
            byte calcX = (byte) (c != (byte) 0 ? ((wx / c) >> 1) : (byte) 0); //TODO: Make sure this works well.
            byte calcY = (byte) (c != (byte) 0 ? ((wy / c) >> 1) : (byte) 0);
            byte calcZ = (byte) (c != (byte) 0 ? ((wz / c) >> 1) : (byte) 0);

            applyToMatrix(tempMatrix, c, calcX, calcY, calcZ);

            float tempDistance = 0;
            for (int i = 0; i < matrix.getMatrix().length; i++)
                for (int j = 0; j < matrix.getMatrix()[i].length; j++)
                    tempDistance += Math.abs(Utils.fixedPointShortToFloat12Bit(matrix.getMatrix()[i][j]) - Utils.fixedPointShortToFloat12Bit(tempMatrix.getMatrix()[i][j]));

            if (tempDistance < bestDistance) {
                bestDistance = tempDistance;
                this.c = c;
                this.x = calcX;
                this.y = calcY;
                this.z = calcZ;
                if (bestDistance == 0F)
                    break; // Found the exact results, stop looking for more.
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
}
