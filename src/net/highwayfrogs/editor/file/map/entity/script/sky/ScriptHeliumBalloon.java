package net.highwayfrogs.editor.file.map.entity.script.sky;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * It is unknown what these values are for.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
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

    @Override
    public void addData(GUIEditorGrid editor) {
        editor.addIntegerField("Unknown 1", getUnknown1(), this::setUnknown1, null);
        editor.addIntegerField("Unknown 2", getUnknown2(), this::setUnknown2, null);
    }
}
