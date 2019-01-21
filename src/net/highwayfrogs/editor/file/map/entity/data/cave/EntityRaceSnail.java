package net.highwayfrogs.editor.file.map.entity.data.cave;

import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityRaceSnail extends PathData {
    private int forwardDistance;
    private int backwardDistance;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.forwardDistance = reader.readUnsignedShortAsInt();
        this.backwardDistance = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.forwardDistance);
        writer.writeUnsignedShort(this.backwardDistance);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Forward Distance", String.valueOf(getForwardDistance())));
        table.getItems().add(new NameValuePair("Backward Distance", String.valueOf(getBackwardDistance())));
    }
}
