package net.highwayfrogs.editor.file.map.entity.data.jungle;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * The frog on the plinth.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class EntityPlinthFrog extends MatrixData {
    private int plinthId;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.plinthId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.plinthId);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Plinth ID", String.valueOf(getPlinthId())));
    }
}
