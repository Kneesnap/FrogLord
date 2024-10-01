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
public class kcActionAttachSensor extends kcActionTemplate {
    private static final kcArgument[] BASE_ARGUMENTS = kcArgument.make(kcParamType.ATTACH_ID, "type");
    private static final kcArgument[] ATTACH_PARTICLE = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.HASH, "hEffect", kcParamType.UNSIGNED_INT, "tag");
    private static final kcArgument[] ATTACH_LAUNCHER = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.UNSIGNED_INT, "tag", kcParamType.HASH, "hLaunchData");
    private static final kcArgument[] ATTACH_ATTACK_OR_BUMP = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.UNSIGNED_INT, "boneID", kcParamType.FLOAT, "radius", kcParamType.UNSIGNED_INT, "focus");


    public kcActionAttachSensor(GreatQuestChunkedFile chunkedFile, kcActionID action) {
        super(chunkedFile, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        if (arguments == null || arguments.length == 0 || arguments[0] == null)
            return BASE_ARGUMENTS;

        int firstArg = arguments[0].getAsInteger();

        // CCharacter::OnCommand
        if (firstArg == kcAttachID.PARTICLE_EMITTER.ordinal()) {
            return ATTACH_PARTICLE;
        } else if (firstArg == kcAttachID.LAUNCHER.ordinal()) {
            return ATTACH_LAUNCHER;
        } else if (firstArg == kcAttachID.ATTACK_SENSOR.ordinal() || firstArg == kcAttachID.BUMP_SENSOR.ordinal()) {
            return ATTACH_ATTACK_OR_BUMP;
        } else {
            return BASE_ARGUMENTS;
        }
    }
}