package net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.DataUtils;

import java.util.Arrays;

/**
 * Represents the 'MR_MAT34B' struct for the 'MR_ANIM_FILE_ID_BYTE_TRANSFORMS' animated MOF transform type.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MRAnimatedMofTransformMatrixByte extends MRAnimatedMofTransform {
    private final byte[][] matrix = new byte[3][3];
    private final short[] translation = new short[3];
    private byte padding;

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readByte();

        this.padding = reader.readByte(); // Believed to be garbage data.
        for (int i = 0; i < this.translation.length; i++)
            this.translation[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                writer.writeByte(this.matrix[i][j]);

        writer.writeByte(this.padding);
        for (int i = 0; i < this.translation.length; i++)
            writer.writeShort(this.translation[i]);
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) {
        this.translation[0] = (short) matrix.getTransform()[0];
        this.translation[1] = (short) matrix.getTransform()[1];
        this.translation[2] = (short) matrix.getTransform()[2];

        for (int i = 0; i < this.matrix.length; i++) {
            for (int j = 0; j < this.matrix[i].length; j++) {
                float floatVal = DataUtils.fixedPointShortToFloat12Bit(matrix.getMatrix()[i][j]) * 128.0F;
                this.matrix[i][j] = (byte) (Math.max(-128.0F, Math.min(127.0F, floatVal)));
            }
        }
    }

    @Override
    public PSXMatrix createMatrix() {
        PSXMatrix matrix = new PSXMatrix();
        matrix.getMatrix()[0][0] = (short) (((short) this.matrix[0][0]) << 5);
        matrix.getMatrix()[0][1] = (short) (((short) this.matrix[0][1]) << 5);
        matrix.getMatrix()[0][2] = (short) (((short) this.matrix[0][2]) << 5);
        matrix.getMatrix()[1][0] = (short) (((short) this.matrix[1][0]) << 5);
        matrix.getMatrix()[1][1] = (short) (((short) this.matrix[1][1]) << 5);
        matrix.getMatrix()[1][2] = (short) (((short) this.matrix[1][2]) << 5);
        matrix.getMatrix()[2][0] = (short) (((short) this.matrix[2][0]) << 5);
        matrix.getMatrix()[2][1] = (short) (((short) this.matrix[2][1]) << 5);
        matrix.getMatrix()[2][2] = (short) (((short) this.matrix[2][2]) << 5);

        // Copy translation
        matrix.getTransform()[0] = this.translation[0];
        matrix.getTransform()[1] = this.translation[1];
        matrix.getTransform()[2] = this.translation[2];
        return matrix;
    }

    @Override
    public int hashCode() {
        return ((this.translation[0] & 0xFF0) << 12) | ((this.translation[2] & 0xFF0) << 20)
                | ((this.matrix[0][1] & 0x3C) << 10) | (this.matrix[1][2] << 4) | ((this.matrix[2][0] & 0x3C) >> 2);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MRAnimatedMofTransformMatrixByte))
            return false;

        MRAnimatedMofTransformMatrixByte other = (MRAnimatedMofTransformMatrixByte) obj;
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                if (this.matrix[i][j] != other.matrix[i][j])
                    return false;

        return Arrays.equals(this.translation, other.translation);
    }
}