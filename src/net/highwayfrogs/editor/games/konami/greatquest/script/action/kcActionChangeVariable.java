package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.*;

/**
 * Represents an action for changing a variable.
 * Created by Kneesnap on 10/25/2025.
 */
public class kcActionChangeVariable extends kcActionTemplate {
    private static final kcArgument[] VARIABLE_CHANGE_ARGUMENTS = kcArgument.make(kcParamType.VARIABLE_ID, "variableId", kcParamType.INT16, "value");

    public kcActionChangeVariable(kcActionExecutor executor, kcActionID actionID) {
        super(executor, actionID);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return VARIABLE_CHANGE_ARGUMENTS;
    }

    @Override
    public void printAdvancedWarnings(kcScriptValidationData data) {
        super.printAdvancedWarnings(data);
        int variableId = getVariableID();
        if (!data.wasVariableSent(variableId))
            printWarning(data.getLogger(), "variable ID " + variableId + " is never sent from " + data.getEntityName() + " with " + kcActionID.NUMBER.getFrogLordName());
    }

    /**
     * Gets the variable ID.
     */
    public int getVariableID() {
        return getParamOrError(0).getAsInteger();
    }
}
