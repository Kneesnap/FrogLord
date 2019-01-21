package net.highwayfrogs.editor.file.map.entity.script.volcano;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Holds helicopter script data.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class ScriptHelicopterData extends EntityScriptData {
    private int destination;
    private int delta;

    @Override
    public void load(DataReader reader) {
        this.destination = reader.readInt();
        this.delta = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.destination);
        writer.writeInt(this.delta);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Target", String.valueOf(destination)));
        table.getItems().add(new NameValuePair("Delta", String.valueOf(delta)));
    }
}
