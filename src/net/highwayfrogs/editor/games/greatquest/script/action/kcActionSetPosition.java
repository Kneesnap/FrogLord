package net.highwayfrogs.editor.games.greatquest.script.action;

import net.highwayfrogs.editor.games.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.greatquest.script.kcParamType;

/**
 * Implements the 'SET_POSITION' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetPosition extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis", kcParamType.FLOAT, "pos");

    public kcActionSetPosition() {
        super(kcActionID.SET_POSITION);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}