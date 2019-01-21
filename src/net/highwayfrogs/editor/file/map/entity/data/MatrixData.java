package net.highwayfrogs.editor.file.map.entity.data;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

import java.util.Arrays;

/**
 * Entity data which involves a matrix.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
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
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Matrix Position", Arrays.toString(matrix.getTransform())));
        for (int i = 0; i < matrix.getMatrix().length; i++)
            table.getItems().add(new NameValuePair("Matrix[" + i + "]", Arrays.toString(matrix.getMatrix()[i])));
    }
}
