package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

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
            translation[i] = Utils.fixedPointIntToFloatNBits(matrix.getTransform()[i], 20);
        }
        editor.addNormalLabel("Rotation Matrix");
        editor.addVector3D(translation, 30D, (index, newValue) -> {
            matrix.getTransform()[index] = Utils.floatToFixedPointInt(newValue, 20);
            //TODO: Update entity position in 3d space.
        });

        // Transform information is in fixed point format, hence conversion to float representation.
        editor.addNormalLabel("Matrix Transformation");
        for (int i = 0; i < matrix.getMatrix().length; i++) {
            for (int j = 0; j < matrix.getMatrix().length; j++)
                matrixRow[j] = Utils.fixedPointShortToFloatNBits(matrix.getMatrix()[i][j], 12);

            final int tempRow = i;
            editor.addVector3D(matrixRow, 25D + ((i == (matrix.getMatrix().length - 1)) ? 5D : 0D),
                    (index, newValue) -> matrix.getMatrix()[tempRow][index] = Utils.floatToFixedPointShort(newValue, 12));
        }
    }
}
