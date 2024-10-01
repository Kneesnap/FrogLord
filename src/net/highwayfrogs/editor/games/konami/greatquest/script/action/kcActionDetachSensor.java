package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcAttachID;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Represents the 'ATTACH' and 'ATTACH_SENSOR' types.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionDetachSensor extends kcActionTemplate {
    private static final kcArgument[] BASE_ARGUMENTS = kcArgument.make(kcParamType.ATTACH_ID, "type");
    private static final kcArgument[] DETACH_PARTICLE = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.HASH, "hOwner");

    public kcActionDetachSensor(GreatQuestChunkedFile chunkedFile, kcActionID action) {
        super(chunkedFile, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        if (arguments == null || arguments.length == 0 || arguments[0] == null)
            return BASE_ARGUMENTS;

        int firstArg = arguments[0].getAsInteger();

        // CCharacter::OnCommand
        if (firstArg == kcAttachID.PARTICLE_EMITTER.ordinal()) {
            return DETACH_PARTICLE;
        } else {
            return BASE_ARGUMENTS;
        }
    }
}