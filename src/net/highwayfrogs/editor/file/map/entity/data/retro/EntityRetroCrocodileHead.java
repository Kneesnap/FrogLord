package net.highwayfrogs.editor.file.map.entity.data.retro;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityRetroCrocodileHead extends MatrixData {
    private int type;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.type);
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Type", String.valueOf(getType())));
    }
}
