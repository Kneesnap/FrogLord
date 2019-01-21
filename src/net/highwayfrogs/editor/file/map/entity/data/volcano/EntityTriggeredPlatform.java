package net.highwayfrogs.editor.file.map.entity.data.volcano;

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
public class EntityTriggeredPlatform extends PathData {
    private int initialMovement;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.initialMovement = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.initialMovement);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Initial Movement", String.valueOf(getInitialMovement())));
    }
}
