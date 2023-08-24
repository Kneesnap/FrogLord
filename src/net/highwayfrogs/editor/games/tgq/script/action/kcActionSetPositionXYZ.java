package net.highwayfrogs.editor.games.tgq.script.action;

import net.highwayfrogs.editor.games.tgq.script.kcArgument;
import net.highwayfrogs.editor.games.tgq.script.kcParam;
import net.highwayfrogs.editor.games.tgq.script.kcParamType;

/**
 * Implements the 'SET_POSITION_XYZ' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetPositionXYZ extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");

    public kcActionSetPositionXYZ() {
        super(kcActionID.SET_POSITION);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}
