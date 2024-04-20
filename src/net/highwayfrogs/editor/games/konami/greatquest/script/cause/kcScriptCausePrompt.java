package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Caused by the player responding to a prompt.
 * This functionality is unused in unmodified gameplay.
 * Created by Kneesnap on 8/17/2023.
 */
public class kcScriptCausePrompt extends kcScriptCause {
    private int promptHash;

    public kcScriptCausePrompt(GreatQuestInstance gameInstance) {
        super(gameInstance, kcScriptCauseType.PROMPT, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        if (subCauseType != 1)
            throw new RuntimeException("The subCauseType for kcScriptCauseEvent is expected to always be one, but was " + subCauseType + ".");

        this.promptHash = extraValues.get(0);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(1);
        output.add(this.promptHash);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("The player responds to the dialog prompt ");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.promptHash, true));
        builder.append(" (This feature appears unfinished/not working)");
    }
}