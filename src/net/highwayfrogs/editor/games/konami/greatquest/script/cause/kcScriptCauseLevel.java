package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * A cause relating to levels.
 * Triggers: EvLevelBegin, EvLevelEnd
 * Created by Kneesnap on 8/16/2023.
 */
public class kcScriptCauseLevel extends kcScriptCause {
    private boolean levelCompleted;

    public kcScriptCauseLevel(GreatQuestInstance gameInstance) {
        super(gameInstance, kcScriptCauseType.LEVEL, 0);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.levelCompleted = readBoolean(subCauseType, "kcScriptCauseLevel");
    }

    @Override
    public void save(List<Integer> output) {
        writeBoolean(output, this.levelCompleted);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        if (this.levelCompleted) {
            builder.append("When the level is completed");
        } else {
            builder.append("When the level is loaded");
        }
    }
}