package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Implements the 'SET_POSITION_XYZ' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetPositionXyz extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");

    public kcActionSetPositionXyz(kcActionExecutor executor) {
        super(executor, kcActionID.SET_POSITION_XYZ);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}