package net.highwayfrogs.editor.file.map.entity.script.desert;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
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
}
