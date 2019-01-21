package net.highwayfrogs.editor.file.map.entity.data.desert;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityThermal extends EntityData {
    private int rotateTime;

    @Override
    public void load(DataReader reader) {
        this.rotateTime = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.rotateTime);
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Rotate Time", String.valueOf(getRotateTime())));
    }
}
