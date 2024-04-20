package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Represents the kcAction for setting an actor's target.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetTarget extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH, "newTarget");

    public kcActionSetTarget(GreatQuestChunkedFile chunkedFile) {
        super(chunkedFile, kcActionID.SET_TARGET);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}