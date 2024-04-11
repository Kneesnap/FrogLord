package net.highwayfrogs.editor.games.greatquest.script.action;

import net.highwayfrogs.editor.games.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.greatquest.script.kcParamType;

/**
 * A kcAction which sets the speed of the actor.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetSpeed extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "speed");

    public kcActionSetSpeed() {
        super(kcActionID.SET_SPEED);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}