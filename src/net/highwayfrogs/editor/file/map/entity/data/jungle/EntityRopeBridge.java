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
public class EntityRopeBridge extends MatrixData {
    private int fallDelay;
    private int hopsBefore;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fallDelay = reader.readUnsignedShortAsInt();
        this.hopsBefore = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.fallDelay);
        writer.writeUnsignedShort(this.hopsBefore);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Fall Delay", String.valueOf(getFallDelay())));
        table.getItems().add(new NameValuePair("Hops Before", String.valueOf(getHopsBefore())));
    }
}
