package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;
import java.util.Objects;

/**
 * Caused by the player responding to a prompt.
 * This functionality is unused in unmodified gameplay.
 * It does not appear to be possible to actually show a prompt to the player, attempting to do so will instead immediately trigger this cause with a hash of zero.
 * Created by Kneesnap on 8/17/2023.
 */
@Getter
public class kcScriptCausePrompt extends kcScriptCause {
    private final GreatQuestHash<?> promptRef = new GreatQuestHash<>();

    public kcScriptCausePrompt(kcScript script) {
        super(script, kcScriptCauseType.PROMPT, 1, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        if (subCauseType != 1)
            throw new RuntimeException("The subCauseType for kcScriptCauseEvent is expected to always be one, but was " + subCauseType + ".");

        this.promptRef.setHash(extraValues.get(0));
    }

    @Override
    public void save(List<Integer> output) {
        output.add(1);
        output.add(this.promptRef.getHashNumber());
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        String promptName = arguments.useNext().getAsString();
        if (NumberUtils.isPrefixedHexInteger(promptName)) {
            this.promptRef.setHash(NumberUtils.parseIntegerAllowHex(promptName));
        } else {
            this.promptRef.setHash(promptName);
        }
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.promptRef.applyGqsString(arguments.createNext(), settings);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        printWarning(logger, "is not supported by the game.");
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ this.promptRef.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && Objects.equals(((kcScriptCausePrompt) obj).getPromptRef(), this.promptRef);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("The player responds to the dialog prompt ");
        builder.append(this.promptRef.getDisplayString(false));
        builder.append(". (This feature was not finished, so it will not work properly)");
    }
}