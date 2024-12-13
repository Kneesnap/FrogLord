package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCause;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Contains information used to validate script data.
 * Created by Kneesnap on 11/25/2024.
 */
@Getter
@RequiredArgsConstructor
public class kcScriptValidationData {
    private final kcCResourceEntityInst entity;
    private final ILogger logger;
    private final Map<kcActionID, List<kcAction>> actionsByType = new HashMap<>();
    private final Map<kcScriptCauseType, List<kcScriptCause>> causesByType = new HashMap<>();

    /**
     * Returns true if there are any actions matching the given type.
     * @param actionID the action ID to search for actions by
     * @return true/false based on if any action was found of the given type.
     */
    public <TAction extends kcAction> boolean anyActionsMatch(kcActionID actionID) {
        List<kcAction> actions = this.actionsByType.get(actionID);
        return actions != null && !actions.isEmpty();
    }

    /**
     * Returns true if there are any actions matching the given criteria running as the entity.
     * @param actionID the action ID to search for actions by
     * @param action the action to check for each action
     * @return true/false based on if any action was found validated by the given data.
     * @param <TAction> the type of action to execute for
     */
    @SuppressWarnings("unchecked")
    public <TAction extends kcAction> boolean anyActionsMatch(kcActionID actionID, Predicate<TAction> action) {
        List<kcAction> actions = this.actionsByType.get(actionID);
        if (actions == null || actions.isEmpty())
            return false;

        for (int i = 0; i < actions.size(); i++)
            if (action.test((TAction) actions.get(i)))
                return true;

        return false;
    }

    /**
     * Returns true if there are any causes matching the given criteria running as the entity.
     * @param causeType the causeType search for causes by
     * @param action the action to check for each cause
     * @return true/false based on if any action was found validated by the given data.
     * @param <TCause> the type of cause to execute for
     */
    @SuppressWarnings("unchecked")
    public <TCause extends kcScriptCause> boolean anyCausesMatch(kcScriptCauseType causeType, Predicate<TCause> action) {
        List<kcScriptCause> causes = this.causesByType.get(causeType);
        if (causes == null || causes.isEmpty())
            return false;

        for (int i = 0; i < causes.size(); i++)
            if (action.test((TCause) causes.get(i)))
                return true;

        return false;
    }

    /**
     * Gets the name of the entity, if it can be found.
     */
    public String getEntityName() {
        return getEntityName("the script owner");
    }

    /**
     * Gets the entity name, or a fallback.
     * @param fallbackName the fallback name
     * @return entityName
     */
    public String getEntityName(String fallbackName) {
        String entityName = this.entity != null ? this.entity.getName() : null;
        return StringUtils.isNullOrWhiteSpace(entityName) ? fallbackName : entityName;
    }
}
