package net.highwayfrogs.editor.games.tgq.script.action;

import net.highwayfrogs.editor.games.tgq.script.kcArgument;
import net.highwayfrogs.editor.games.tgq.script.kcParam;
import net.highwayfrogs.editor.games.tgq.script.kcParamType;

/**
 * Represents the "SET_ANIMATION" kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetAnimation extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH, "track", kcParamType.UNSIGNED_INT, "mode", kcParamType.INT, "startTick", kcParamType.INT, "transTime");

    public kcActionSetAnimation() {
        super(kcActionID.SET_ANIMATION);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}
