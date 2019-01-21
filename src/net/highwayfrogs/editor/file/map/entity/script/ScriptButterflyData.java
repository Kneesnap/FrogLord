package net.highwayfrogs.editor.file.map.entity.script;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Holds onto butterfly data.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class ScriptButterflyData extends EntityScriptData {
    private FlyScoreType type;

    @Override
    public void load(DataReader reader) {
        this.type = FlyScoreType.values()[reader.readInt()];
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(type.ordinal());
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Type", type.name()));
    }
}
