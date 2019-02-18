package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

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
        // Still overwritten by children.
    }

    @Override
    public void addData(MapUIController controller, GUIEditorGrid editor) {
        float[] translation = new float[3];
        float[] matrixRow = new float[3];

        // Position information is in fixed point format, hence conversion to float representation.
        for (int i = 0; i < matrix.getTransform().length; i++)
            translation[i] = Utils.fixedPointIntToFloat20Bit(matrix.getTransform()[i]);

        editor.addNormalLabel("Position");
        editor.addVector3D(translation, 30D, (index, newValue) -> {
            matrix.getTransform()[index] = Utils.floatToFixedPointInt20Bit(newValue);
            controller.getController().resetEntities();
        });

        // Transform information is in fixed point format, hence conversion to float representation.
        editor.addNormalLabel("Rotation Matrix");
        for (int i = 0; i < matrix.getMatrix().length; i++) {
            for (int j = 0; j < matrix.getMatrix().length; j++)
                matrixRow[j] = Utils.fixedPointShortToFloat12Bit(matrix.getMatrix()[i][j]);

            final int tempRow = i;
            editor.addVector3D(matrixRow, 25D + ((i == (matrix.getMatrix().length - 1)) ? 5D : 0D),
                    (index, newValue) -> matrix.getMatrix()[tempRow][index] = Utils.floatToFixedPointShort12Bit(newValue));
        }

        super.addData(controller, editor);
    }
}
