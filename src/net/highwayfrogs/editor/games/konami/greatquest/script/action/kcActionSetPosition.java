package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Implements the 'SET_POSITION' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetPosition extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis", kcParamType.FLOAT, "coordinate");

    public kcActionSetPosition(kcActionExecutor executor) {
        super(executor, kcActionID.SET_POSITION);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}