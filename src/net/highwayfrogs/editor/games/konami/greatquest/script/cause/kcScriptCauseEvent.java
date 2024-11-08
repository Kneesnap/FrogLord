package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;
import java.util.logging.Logger;

/**
 * Allows a script event to trigger for an event.
 * This may appear like it should only fire when the event hash in here matches the event which has been fired.
 * However, it seems they forgot to add verification check to that, so actually the event hash specified here is unused.
 * ProcessGlobalScript() tests the scriptType and the causeType, but none of the additional params.
 * This cause appears unused in the game, so that's probably why this slipped into the final game.
 * It would be feasible to fix the feature, but it may not matter.
 * Created by Kneesnap on 8/17/2023.
 */
@Getter
public class kcScriptCauseEvent extends kcScriptCause {
    private int eventNameHash;

    public kcScriptCauseEvent(kcScript script) {
        super(script, kcScriptCauseType.EVENT, 1, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        if (subCauseType != 0)
            throw new RuntimeException("The subCauseType for kcScriptCauseEvent is expected to always be zero, but was " + subCauseType + ".");

        this.eventNameHash = extraValues.get(0);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(0);
        output.add(this.eventNameHash);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.eventNameHash = GreatQuestUtils.getAsHash(arguments.useNext(), 0);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        kcScriptDisplaySettings.applyGqsSyntaxHashDisplay(arguments.createNext(), settings, this.eventNameHash);
    }

    @Override
    public void printWarnings(Logger logger) {
        super.printWarnings(logger);
        printWarning(logger, "is not supported by the game.");
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ this.eventNameHash;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((kcScriptCauseEvent) obj).getEventNameHash() == this.eventNameHash;
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When any event is triggered. (Should be for just '");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.eventNameHash, true));
        builder.append("', but the game is broken)");
    }
}