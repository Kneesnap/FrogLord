package net.highwayfrogs.editor.games.tgq.script.action;

import net.highwayfrogs.editor.games.tgq.script.kcArgument;
import net.highwayfrogs.editor.games.tgq.script.kcParam;
import net.highwayfrogs.editor.games.tgq.script.kcParamType;

/**
 * Represents the 'TRIGGER_EVENT' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionTriggerEvent extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH, "eventHash");

    public kcActionTriggerEvent() {
        super(kcActionID.TRIGGER_EVENT);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}
