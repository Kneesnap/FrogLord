package net.highwayfrogs.editor.file.map.entity.script.swamp;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Holds data for the SCRIPT_SWP_BOBBING_WASTE_BARREL script.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class ScriptBobbingWasteData extends EntityScriptData {
    private int delay;

    @Override
    public void load(DataReader reader) {
        this.delay = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.delay);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Delay", String.valueOf(delay)));
    }
}
