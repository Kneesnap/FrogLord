package net.highwayfrogs.editor.file.map.entity.script.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Holds script data for nuclear barrels.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class ScriptNuclearBarrelData extends EntityScriptData {
    private int flags;
    private int distance;

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        this.distance = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.flags);
        writer.writeInt(this.distance);
    }
}
