package net.highwayfrogs.editor.file.map.entity.script.sky;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * It is unknown what these values are for.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class ScriptHeliumBalloon extends EntityScriptData {
    private int unknown1;
    private int unknown2;

    @Override
    public void load(DataReader reader) {
        this.unknown1 = reader.readInt();
        this.unknown2 = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.unknown1);
        writer.writeInt(this.unknown2);
    }
}
