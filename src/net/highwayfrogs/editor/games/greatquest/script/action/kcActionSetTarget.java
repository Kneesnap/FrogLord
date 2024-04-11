package net.highwayfrogs.editor.games.greatquest.script.action;

import net.highwayfrogs.editor.games.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.greatquest.script.kcParamType;

/**
 * Represents the kcAction for setting an actor's target.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetTarget extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH, "newTarget");

    public kcActionSetTarget() {
        super(kcActionID.SET_TARGET);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}