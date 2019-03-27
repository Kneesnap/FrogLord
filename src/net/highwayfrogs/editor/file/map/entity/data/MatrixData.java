package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
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
        editor.addMatrix(matrix, controller.getController()::resetEntities);
        super.addData(controller, editor);
    }
}
