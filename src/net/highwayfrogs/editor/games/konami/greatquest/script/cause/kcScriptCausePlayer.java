package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Represents a cause of a player action.
 * Created by Kneesnap on 8/16/2023.
 */
@Getter
public class kcScriptCausePlayer extends kcScriptCause {
    private kcScriptCauseEntityAction action;

    public kcScriptCausePlayer() {
        super(kcScriptCauseType.PLAYER, 0);
    }

    @Override
    public void load(int primaryValue, List<Integer> extraValues) {
        this.action = kcScriptCauseEntityAction.getAction(primaryValue, false);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.action.getValue());
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append(this.action.getPlayerDescription());
    }
}