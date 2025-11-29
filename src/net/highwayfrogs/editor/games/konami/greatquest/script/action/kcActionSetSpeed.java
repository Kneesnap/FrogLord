package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * A kcAction which sets the speed of the actor.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetSpeed extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "speed");

    public kcActionSetSpeed(kcActionExecutor executor) {
        super(executor, kcActionID.SET_SPEED);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);

        if (getExecutor() instanceof kcCActionSequence) {
            kcCActionSequence actionSequence = (kcCActionSequence) getExecutor();
            int selfIndex = actionSequence.getActions().indexOf(this);
            if (selfIndex < 0)
                throw new RuntimeException(this + " was not found in the parent action sequence.");
            if (selfIndex == 0 || actionSequence.getActions().get(selfIndex - 1).getActionID() != kcActionID.SET_ANIMATION)
                printWarning(logger, "the action on the previous line was not '" + kcActionID.SET_ANIMATION.getFrogLordName() + "'. (THIS MAY CRASH THE GAME!)");
        }
    }
}