package net.highwayfrogs.editor.file.map.entity.script;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;

/**
 * Holds onto butterfly data.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
public class ScriptButterflyData extends EntityScriptData {
    private FroggerFlyScoreType type = FroggerFlyScoreType.SCORE_10;

    @Override
    public void load(DataReader reader) {
        int scoreType = reader.readInt();
        if (scoreType >= 0 && scoreType < FroggerFlyScoreType.values().length) // JUN1.MAP has corrupted data here in certain mwds. This just makes it not crash from it.
            this.type = FroggerFlyScoreType.values()[scoreType];
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(type.ordinal());
    }
}
