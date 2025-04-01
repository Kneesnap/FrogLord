package net.highwayfrogs.editor.file.map.entity.script.jungle;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents floating tree data.
 * Created by Kneesnap on 2/7/2023.
 */
@Getter
@Setter
public class ScriptFloatingTreeData extends EntityScriptData {
    private int delayBeforeMoving;

    @Override
    public void load(DataReader reader) {
        this.delayBeforeMoving = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.delayBeforeMoving);
    }
}