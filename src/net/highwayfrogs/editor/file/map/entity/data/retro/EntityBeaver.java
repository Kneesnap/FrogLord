package net.highwayfrogs.editor.file.map.entity.data.retro;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityBeaver extends PathData {
    private short delay;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.delay = reader.readShort();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.delay);
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Delay", String.valueOf(getDelay())));
    }
}
