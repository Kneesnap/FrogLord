package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.Arrays;

/**
 * Entity data which involves a matrix.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
@Setter
public class MatrixData extends EntityData {
    private PSXMatrix matrix = new PSXMatrix();

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        float[] translation = new float[3];
        float[] matrixRow = new float[3];

        // Position information is in fixed point format, hence conversion to float representation.
        for (int i = 0; i < matrix.getTransform().length; i++) {
            translation[i] = Utils.unsignedIntToFloat(matrix.getTransform()[i]);
        }
        editor.addNormalLabel("Matrix Position");
        editor.addVector3D(translation, 30.0);

        // Transform information is in fixed point format, hence conversion to float representation.
        editor.addNormalLabel("Matrix Transformation");
        for (int i = 0; i < matrix.getMatrix().length; i++) {
            for (int j = 0; j < matrix.getMatrix().length; j++) {
                matrixRow[j] = Utils.unsignedShortToFloatNBits(matrix.getMatrix()[i][j], 12);
            }
            editor.addVector3D(matrixRow, 25.0 + ((i == (matrix.getMatrix().length - 1)) ? (5.0) : (0.0)));
        }
    }
}
