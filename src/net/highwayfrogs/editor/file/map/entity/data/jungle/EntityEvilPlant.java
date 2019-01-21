package net.highwayfrogs.editor.file.map.entity.data.jungle;

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
public class EntityEvilPlant extends MatrixData {
    private short snapTime;
    private short snapDelay;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.snapTime = reader.readShort();
        this.snapDelay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.snapTime);
        writer.writeShort(this.snapDelay);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Snap Time", String.valueOf(getSnapTime())));
        table.getItems().add(new NameValuePair("Snap Delay", String.valueOf(getSnapDelay())));
    }
}
