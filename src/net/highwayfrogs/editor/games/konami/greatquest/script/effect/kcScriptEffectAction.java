package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents a kcScriptEffect which runs a kcAction.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
public abstract class kcScriptEffectAction extends kcScriptEffect implements kcActionExecutor {
    private final int effectID;

    public kcScriptEffectAction(kcScriptFunction parentFunction, kcScriptEffectType effectType, int effectID) {
        super(parentFunction, effectType);
        this.effectID = effectID;
    }

    /**
     * Get the action to use for this effect.
     */
    public abstract kcAction getAction();

    @Override
    protected void printLoadWarnings(OptionalArguments arguments, ILogger logger) {
        kcAction action = getAction();
        if (action != null)
            action.printWarnings(logger);

        super.printLoadWarnings(arguments, logger);
    }
    @Override
    public boolean isActionApplicableToTarget() {
        kcCResourceEntityInst entity = getTargetEntityRef().getResource();
        kcAction action = getAction();
        return action != null && entity != null && action.getActionID().getActionTargetType() != null
                && action.getActionID().getActionTargetType().isApplicable(entity.getInstance());
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        super.toString(builder, settings);

        kcAction action = getAction();
        if (action != null)
            action.toString(builder, settings);
    }

    @Override
    public String getEndOfLineComment() {
        kcAction action = getAction();
        return action != null ? action.getEndOfLineComment() : null;
    }
}