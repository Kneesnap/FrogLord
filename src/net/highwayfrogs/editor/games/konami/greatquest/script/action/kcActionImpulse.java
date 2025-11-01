package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
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
        if (!data.doesEntityEverHaveFlagSet(kcEntityInstanceFlag.ENABLE_PHYSICS))
            printWarning(data.getLogger(), data.getEntityName() + " never has the " + kcEntityInstanceFlag.ENABLE_PHYSICS.getDisplayName() + " flag set.");
        if (data.doesEntityAlwaysHaveFlagSet(kcEntityInstanceFlag.ENABLE_TERRAIN_TRACKING))
            printWarning(data.getLogger(), data.getEntityName() + " always has the " + kcEntityInstanceFlag.ENABLE_TERRAIN_TRACKING.getDisplayName() + " flag set.");
    }
}
