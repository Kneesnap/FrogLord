package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionFlag;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionNumber;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionNumber.NumberOperation;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCause;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Contains information used to validate script data.
 * Created by Kneesnap on 11/25/2024.
 */
@RequiredArgsConstructor
public class kcScriptValidationData {
    @Getter private final kcCResourceEntityInst entity;
    @Getter private final ILogger logger;
    private final Map<kcActionID, List<kcAction>> actionsByType = new HashMap<>();
    private final Map<kcScriptCauseType, List<kcScriptCause>> causesByType = new HashMap<>();
    private int sentVariables;

    /**
     * Adds an action to the tracker.
     * @param action the action to add
     */
    public void addAction(kcAction action) {
        if (action == null)
            throw new NullPointerException("action");

        this.actionsByType.computeIfAbsent(action.getActionID(), key -> new ArrayList<>()).add(action);
    }

    /**
     * Adds an action to the tracker.
     * @param cause the action to add
     */
    public void addCause(kcScriptCause cause) {
        if (cause == null)
            throw new NullPointerException("cause");

        this.causesByType.computeIfAbsent(cause.getType(), key -> new ArrayList<>()).add(cause);
    }

    /**
     * If the provided action is for sending a number, track it separately, to track which variables have been used.
     * This needs a special case because SendNumber has a special-case for handling --AsEntity, where it will grab variable data from the sending entity.
     * @param action the action to try adding
     */
    public void addSendNumberToOwner(kcAction action) {
        // Track variables which have been sent.
        if (action.getActionID() == kcActionID.NUMBER && ((kcActionNumber) action).getOperation() == NumberOperation.ENTITY_VARIABLE)
            markVariableSent(((kcActionNumber) action).getNumber());
    }

    /**
     * Marks a particular variable ID as having been used/sent.
     * @param variableId the variable ID to mark as used/sent.
     */
    private void markVariableSent(int variableId) {
        if (variableId < 0 || variableId >= kcActionNumber.ENTITY_VARIABLE_SLOTS)
            return;

        this.sentVariables |= (1 << variableId);
    }

    /**
     * Test if a variable was sent
     * @param variableId the variable ID to test.
     * @return true iff a variable was sent by the given entity.
     */
    public boolean wasVariableSent(int variableId) {
        return (variableId >= 0) && (variableId < kcActionNumber.ENTITY_VARIABLE_SLOTS) && (this.sentVariables & (1 << variableId)) != 0;
    }

    /**
     * Returns true if there are any actions matching the given type.
     * @param actionID the action ID to search for actions by
     * @return true/false based on if any action was found of the given type.
     */
    public boolean anyActionsMatch(kcActionID actionID) {
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
        if (action == null)
            return true;

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
        if (action == null)
            return true;

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

    /**
     * Returns true iff the entity ever has the specified flag applied.
     * @param flag the flag to test
     * @return true iff the entity ever has the flag set
     */
    public boolean doesEntityEverHaveFlagSet(kcEntityInstanceFlag flag) {
        kcEntityInst entityInst = this.entity != null ? this.entity.getInstance() : null;
        return (entityInst instanceof kcEntity3DInst) && (
                ((kcEntity3DInst) entityInst).hasFlag(flag)
                || anyActionsMatch(kcActionID.SET_FLAGS, action -> ((kcActionFlag) action).hasFlagPresent(flag))
                || anyActionsMatch(kcActionID.INIT_FLAGS, action -> ((kcActionFlag) action).hasFlagPresent(flag)));
    }

    /**
     * Returns true iff the entity ever has the specified flag applied.
     * @param flag the flag to test
     * @return true iff the entity ever has the flag set
     */
    public boolean doesEntityAlwaysHaveFlagSet(kcEntityInstanceFlag flag) {
        kcEntityInst entityInst = this.entity != null ? this.entity.getInstance() : null;
        return (entityInst instanceof kcEntity3DInst) && ((kcEntity3DInst) entityInst).hasFlag(flag)
                && !anyActionsMatch(kcActionID.CLEAR_FLAGS, action -> ((kcActionFlag) action).hasFlagPresent(flag));
    }
}
