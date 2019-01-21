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
public class EntitySnake extends MatrixData {
    private short logId;
    private int speed;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.logId = reader.readShort();
        this.speed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.logId);
        writer.writeUnsignedShort(this.speed);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Log ID", String.valueOf(getLogId())));
        table.getItems().add(new NameValuePair("Speed", String.valueOf(getSpeed())));
    }
}
