package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Allows a script event to trigger for an event.
 * This may appear like it should only fire when the event hash in here matches the event which has been fired.
 * However, it seems they forgot to add verification check to that, so actually the event hash specified here is unused.
 * This cause appears unused in the game, so that's probably why this slipped into the final game.
 * Perhaps it would be feasible to fix the feature, but it may not matter.
 * Created by Kneesnap on 8/17/2023.
 */
public class kcScriptCauseEvent extends kcScriptCause {
    private int eventNameHash;

    public kcScriptCauseEvent(GreatQuestInstance gameInstance) {
        super(gameInstance, kcScriptCauseType.EVENT, 1);
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
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When the any event occurs (Bugged, should be for just '");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.eventNameHash, true));
        builder.append("'.)");
    }
}