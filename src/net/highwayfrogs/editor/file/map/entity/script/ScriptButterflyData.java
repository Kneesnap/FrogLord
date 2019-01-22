package net.highwayfrogs.editor.file.map.entity.script;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Holds onto butterfly data.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
public class ScriptButterflyData extends EntityScriptData {
    private FlyScoreType type = FlyScoreType.SCORE_10;

    @Override
    public void load(DataReader reader) {
        this.type = FlyScoreType.values()[reader.readInt()];
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(type.ordinal());
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        editor.addEnumSelector("Score Type", type, FlyScoreType.values(), false, this::setType);
    }
}
