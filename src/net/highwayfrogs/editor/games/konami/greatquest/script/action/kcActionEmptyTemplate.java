package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;

/**
 * Represents a template accepting no arguments.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionEmptyTemplate extends kcActionTemplate {
    private static final kcArgument[] EMPTY_ARGUMENTS_ARRAY = new kcArgument[0];

    public kcActionEmptyTemplate(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return EMPTY_ARGUMENTS_ARRAY;
    }
}