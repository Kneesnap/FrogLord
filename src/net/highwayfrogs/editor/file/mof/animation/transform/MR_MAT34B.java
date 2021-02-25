package net.highwayfrogs.editor.file.mof.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the "MR_MAT34B" transformation struct.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MR_MAT34B extends TransformObject {
    private byte[][] matrix = new byte[3][3];
    private short[] transform = new short[3];

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readByte();

        reader.skipByte(); // Padding.
        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        for (byte[] aMatrix : this.matrix)
            for (byte aByte : aMatrix)
                writer.writeByte(aByte);
        writer.writeByte(Constants.NULL_BYTE); // Padding

        for (short aTransfer : this.transform)
            writer.writeShort(aTransfer);
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) {
        this.transform[0] = (short) matrix.getTransform()[0];
        this.transform[1] = (short) matrix.getTransform()[1];
        this.transform[2] = (short) matrix.getTransform()[2];

        for (int i = 0; i < getMatrix().length; i++) {
            for (int j = 0; j < getMatrix()[i].length; j++) {
                float floatVal = Utils.fixedPointShortToFloat12Bit(matrix.getMatrix()[i][j]) * 128.0F;
                getMatrix()[i][j] = (byte) (Math.max(-128.0F, Math.min(127.0F, floatVal)));
            }
        }
    }

    @Override
    public PSXMatrix createMatrix() {
        PSXMatrix matrix = new PSXMatrix();
        matrix.getMatrix()[0][0] = (short) (((short) getMatrix()[0][0]) << 5);
        matrix.getMatrix()[0][1] = (short) (((short) getMatrix()[0][1]) << 5);
        matrix.getMatrix()[0][2] = (short) (((short) getMatrix()[0][2]) << 5);
        matrix.getMatrix()[1][0] = (short) (((short) getMatrix()[1][0]) << 5);
        matrix.getMatrix()[1][1] = (short) (((short) getMatrix()[1][1]) << 5);
        matrix.getMatrix()[1][2] = (short) (((short) getMatrix()[1][2]) << 5);
        matrix.getMatrix()[2][0] = (short) (((short) getMatrix()[2][0]) << 5);
        matrix.getMatrix()[2][1] = (short) (((short) getMatrix()[2][1]) << 5);
        matrix.getMatrix()[2][2] = (short) (((short) getMatrix()[2][2]) << 5);

        // Copy translation
        matrix.getTransform()[0] = getTransform()[0];
        matrix.getTransform()[1] = getTransform()[1];
        matrix.getTransform()[2] = getTransform()[2];
        return matrix;
    }

    @Override
    public int hashCode() {
        return ((this.transform[0] & 0xFF0) << 12) | ((this.transform[2] & 0xFF0) << 20)
                | ((this.matrix[0][1] & 0x3C) << 10) | (this.matrix[1][2] << 4) | ((this.matrix[2][0] & 0x3C) >> 2);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MR_MAT34B))
            return false;

        MR_MAT34B other = (MR_MAT34B) obj;
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                if (this.matrix[i][j] != other.matrix[i][j])
                    return false;

        return this.transform[0] == other.transform[0] && this.transform[1] == other.transform[1] && this.transform[2] == other.transform[2];
    }
}
