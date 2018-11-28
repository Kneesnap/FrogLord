package net.highwayfrogs.editor.file.map.entity.script.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

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
}
