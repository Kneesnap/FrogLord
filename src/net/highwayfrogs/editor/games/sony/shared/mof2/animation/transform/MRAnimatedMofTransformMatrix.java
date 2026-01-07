package net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.PSXMatrix;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Arrays;

/**
 * Represents the 'MR_MAT34' struct for the 'MR_ANIM_FILE_ID_NORMAL' animated MOF transform type.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MRAnimatedMofTransformMatrix extends MRAnimatedMofTransform {
    private final short[][] matrix = new short[3][3]; // Rotation matrix.
    private final short[] translation = new short[3];

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readShort();

        for (int i = 0; i < this.translation.length; i++)
            this.translation[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                writer.writeShort(this.matrix[i][j]);

        for (int i = 0; i < this.translation.length; i++)
            writer.writeShort(this.translation[i]);
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) {
        this.translation[0] = (short) matrix.getTransform()[0];
        this.translation[1] = (short) matrix.getTransform()[1];
        this.translation[2] = (short) matrix.getTransform()[2];
        for (int i = 0; i < getMatrix().length; i++)
            System.arraycopy(matrix.getMatrix()[i], 0, getMatrix()[i], 0, getMatrix()[i].length);
    }


    @Override
    public PSXMatrix createMatrix() {
        PSXMatrix newMatrix = new PSXMatrix();
        for (int i = 0; i < getMatrix().length; i++)
            System.arraycopy(getMatrix()[i], 0, newMatrix.getMatrix()[i], 0, getMatrix()[i].length);

        for (int i = 0; i < this.translation.length; i++)
            newMatrix.getTransform()[i] = this.translation[i];
        return newMatrix;
    }

    @Override
    public int hashCode() {
        // TODO: This hashcode sucks.
        return ((this.translation[2] & 0xFF0) << 20) | ((this.translation[0] & 0xFF0) << 12)
                | ((this.matrix[0][1] & 0xFF0) << 4) | ((this.matrix[1][2] & 0xFF0) >> 4);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MRAnimatedMofTransformMatrix))
            return false;

        MRAnimatedMofTransformMatrix other = (MRAnimatedMofTransformMatrix) obj;
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                if (this.matrix[i][j] != other.matrix[i][j])
                    return false;

        return Arrays.equals(this.translation, other.translation);
    }
}