package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Represents the 'ACTIVATE_SPECIAL' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionActivateSpecial extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.BOOLEAN, "activate", kcParamType.UNSIGNED_INT, "activateMask");

    public kcActionActivateSpecial() {
        super(kcActionID.ACTIVATE_SPECIAL);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}