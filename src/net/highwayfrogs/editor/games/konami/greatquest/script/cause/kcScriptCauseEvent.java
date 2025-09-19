package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;
import java.util.Objects;

/**
 * Allows an event trigger to cause a script function to run.
 * This may appear like it should only fire when the event hash in here matches the event which has been fired.
 * However, it seems they forgot to add verification check to that, so actually the event hash specified here is unused.
 * ProcessGlobalScript() tests the scriptType and the causeType, but none of the additional params.
 * This cause appears unused in the game, so that's probably why this slipped into the final game.
 * It would be feasible to fix the feature, but it may not matter.
 * Created by Kneesnap on 8/17/2023.
 */
@Getter
public class kcScriptCauseEvent extends kcScriptCause {
    private final GreatQuestHash<?> eventRef = new GreatQuestHash<>();

    public kcScriptCauseEvent(kcScript script) {
        super(script, kcScriptCauseType.EVENT, 1, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        if (subCauseType != 0)
            throw new RuntimeException("The subCauseType for kcScriptCauseEvent is expected to always be zero, but was " + subCauseType + ".");

        int eventHash = extraValues.get(0);
        String eventName = GreatQuestUtils.getEventName(eventHash);
        if (eventName != null) {
            this.eventRef.setHash(eventName);
        } else {
            this.eventRef.setHash(eventHash);
        }
    }

    @Override
    public void save(List<Integer> output) {
        output.add(0);
        output.add(this.eventRef.getHashNumber());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        String eventName = arguments.useNext().getAsString(); // We can't resolve the sequence by the hash of the string normally since these seem to use randomized hash values.
        if (NumberUtils.isHexInteger(eventName)) {
            this.eventRef.setHash(NumberUtils.parseHexInteger(eventName));
        } else {
            this.eventRef.setHash(eventName);
        }
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.eventRef.applyGqsString(arguments.createNext(), settings);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        printWarning(logger, "is not supported by the game.");
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ this.eventRef.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && Objects.equals(((kcScriptCauseEvent) obj).getEventRef(), this.eventRef);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When any event is triggered. (Should be for just '");
        builder.append(this.eventRef.getDisplayString(false));
        builder.append("', but the game is broken)");
    }
}