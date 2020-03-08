package net.highwayfrogs.editor.file.mof.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents MR_MAT34.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MR_MAT34 extends TransformObject {
    private short[][] matrix = new short[3][3]; // Rotation matrix.
    private short[] transform = new short[3];

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readShort();

        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        for (short[] aMatrix : this.matrix)
            for (short aShort : aMatrix)
                writer.writeShort(aShort);

        for (short aTransfer : this.transform)
            writer.writeShort(aTransfer);
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) {
        this.transform[0] = (short) matrix.getTransform()[0];
        this.transform[1] = (short) matrix.getTransform()[1];
        this.transform[2] = (short) matrix.getTransform()[2];
        for (int i = 0; i < getMatrix().length; i++)
            System.arraycopy(matrix.getMatrix()[i], 0, getMatrix()[i], 0, getMatrix()[i].length);
    }


    @Override
    public PSXMatrix createMatrix() {
        PSXMatrix newMatrix = new PSXMatrix();
        for (int i = 0; i < getMatrix().length; i++)
            System.arraycopy(getMatrix()[i], 0, newMatrix.getMatrix()[i], 0, getMatrix()[i].length);

        for (int i = 0; i < getTransform().length; i++)
            newMatrix.getTransform()[i] = getTransform()[i];
        return newMatrix;
    }

    @Override
    public int hashCode() {
        return ((this.transform[2] & 0xFF0) << 20) | ((this.transform[0] & 0xFF0) << 12)
                | ((this.matrix[0][1] & 0xFF0) << 4) | ((this.matrix[1][2] & 0xFF0) >> 4);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MR_MAT34))
            return false;

        MR_MAT34 other = (MR_MAT34) obj;
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                if (this.matrix[i][j] != other.matrix[i][j])
                    return false;

        return this.transform[0] == other.transform[0] && this.transform[1] == other.transform[1] && this.transform[2] == other.transform[2];
    }
}