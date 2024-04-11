package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;

/**
 * Represents a kcScriptEffect which runs a kcAction.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
public abstract class kcScriptEffectAction extends kcScriptEffect {
    private final int effectID;

    public kcScriptEffectAction(kcScriptEffectType effectType, int effectID) {
        super(effectType);
        this.effectID = effectID;
    }

    /**
     * Get the action to use for this effect.
     */
    public abstract kcAction getAction();

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        super.toString(builder, settings);

        kcAction action = getAction();
        if (action != null)
            action.toString(builder, settings);
    }
}