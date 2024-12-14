package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Represents the 'TRIGGER_EVENT' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionTriggerEvent extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH, "eventHash");

    public kcActionTriggerEvent(kcActionExecutor executor) {
        super(executor, kcActionID.TRIGGER_EVENT);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}