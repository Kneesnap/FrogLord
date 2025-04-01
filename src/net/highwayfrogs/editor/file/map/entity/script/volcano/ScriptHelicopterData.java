package net.highwayfrogs.editor.file.map.entity.script.volcano;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Holds helicopter script data.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
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
}
