package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * A kcAction which sets the speed of the actor.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetSpeed extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "speed");

    public kcActionSetSpeed(GreatQuestChunkedFile chunkedFile) {
        super(chunkedFile, kcActionID.SET_SPEED);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }
}