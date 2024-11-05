package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * Caused by the player responding to a prompt.
 * This functionality is unused in unmodified gameplay.
 * It does not appear to be possible to actually show a prompt to the player, attempting to do so will instead immediately trigger this cause with a hash of zero.
 * Created by Kneesnap on 8/17/2023.
 */
public class kcScriptCausePrompt extends kcScriptCause {
    private int promptHash;

    public kcScriptCausePrompt(kcScript script) {
        super(script, kcScriptCauseType.PROMPT, 1, 1);
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
    protected void loadArguments(OptionalArguments arguments) {
        this.promptHash = GreatQuestUtils.getAsHash(arguments.useNext(), 0);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        kcScriptDisplaySettings.applyGqsSyntaxHashDisplay(arguments.createNext(), settings, this.promptHash);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("The player responds to the dialog prompt ");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.promptHash, true));
        builder.append(" (This feature was not finished, so it will not work properly)");
    }
}