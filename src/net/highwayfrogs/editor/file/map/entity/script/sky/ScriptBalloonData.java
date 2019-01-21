package net.highwayfrogs.editor.file.map.entity.script.sky;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * It is unknown what these values are for.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class ScriptBalloonData extends EntityScriptData {
    private int unknown1;
    private int unknown2;
    private int unknown3;

    @Override
    public void load(DataReader reader) {
        this.unknown1 = reader.readInt();
        this.unknown2 = reader.readInt();
        this.unknown3 = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.unknown1);
        writer.writeInt(this.unknown2);
        writer.writeInt(this.unknown3);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Unknown 1", String.valueOf(unknown1)));
        table.getItems().add(new NameValuePair("Unknown 2", String.valueOf(unknown2)));
        table.getItems().add(new NameValuePair("Unknown 3", String.valueOf(unknown3)));
    }
}
