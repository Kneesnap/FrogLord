package net.highwayfrogs.editor.file.map.entity.data.general;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Data for a entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class BonusFlyEntity extends MatrixData {
    private int type; // Score type.

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.type);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Type", String.valueOf(getType())));
    }
}
