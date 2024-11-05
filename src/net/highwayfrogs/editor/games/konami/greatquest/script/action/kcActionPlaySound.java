package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Implements the play sound command.
 * TODO: Unless the sfxId & 0xFFF < 64, it won't be deleted for some reason. I think we should hack the executable to not have that limit?
 * Created by Kneesnap on 10/29/2024.
 */
public class kcActionPlaySound extends kcActionTemplate {
    public static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.SOUND, "sound");
    public static final int BITMASK_STOP_SOUND = 0xFFFF0000;

    private static final String ARGUMENT_STOP_SOUND = "StopSound";

    public kcActionPlaySound(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        super.loadArguments(arguments);
        if (arguments.use(ARGUMENT_STOP_SOUND) != null) {
            kcParam sfxIdParam = getOrCreateParam(0);
            sfxIdParam.setValue(sfxIdParam.getAsInteger() | BITMASK_STOP_SOUND);
        }
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        super.saveArguments(arguments, settings);
        if ((getParamOrError(0).getAsInteger() & BITMASK_STOP_SOUND) != 0)
            arguments.getOrCreate(ARGUMENT_STOP_SOUND);
    }
}
