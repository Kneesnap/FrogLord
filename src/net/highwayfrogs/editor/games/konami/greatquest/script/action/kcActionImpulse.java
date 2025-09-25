package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;

/**
 * Implements the 'IMPULSE' command.
 * Created by Kneesnap on 8/18/2025.
 */
public class kcActionImpulse extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");

    public kcActionImpulse(kcActionExecutor executor) {
        super(executor, kcActionID.IMPULSE);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void printAdvancedWarnings(kcScriptValidationData data) {
        super.printAdvancedWarnings(data);

        kcEntityInst entityInst = data.getEntity() != null ? data.getEntity().getInstance() : null;
        if (entityInst instanceof kcEntity3DInst && !((kcEntity3DInst) entityInst).hasFlag(kcEntityInstanceFlag.ENABLE_PHYSICS)
                && !data.anyActionsMatch(kcActionID.SET_FLAGS, action -> ((kcActionFlag) action).hasFlagPresent(kcEntityInstanceFlag.ENABLE_PHYSICS)))
            printWarning(data.getLogger(), data.getEntityName() + " never has the " + kcEntityInstanceFlag.ENABLE_PHYSICS.getDisplayName() + " flag set.");
    }
}
