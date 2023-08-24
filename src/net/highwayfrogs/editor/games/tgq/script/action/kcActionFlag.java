package net.highwayfrogs.editor.games.tgq.script.action;

import net.highwayfrogs.editor.games.tgq.script.kcArgument;
import net.highwayfrogs.editor.games.tgq.script.kcParam;
import net.highwayfrogs.editor.games.tgq.script.kcParamType;

/**
 * Represents kcActions which run commands on flags.
 * TODO: Label each of the flag bits, so we don't have magic numbers.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionFlag extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.ANY, "flagId");

    public kcActionFlag(kcActionID action) {
        super(action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}
