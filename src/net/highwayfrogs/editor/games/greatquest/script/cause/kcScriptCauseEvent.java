package net.highwayfrogs.editor.games.greatquest.script.cause;

import net.highwayfrogs.editor.games.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Allows a script event to trigger for an event.
 * Created by Kneesnap on 8/17/2023.
 */
public class kcScriptCauseEvent extends kcScriptCause {
    private int eventNameHash;

    public kcScriptCauseEvent() {
        super(kcScriptCauseType.EVENT, 1);
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
        builder.append("When the event ");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.eventNameHash, true));
        builder.append(" occurs");
    }
}