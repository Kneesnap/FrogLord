package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntry;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.logging.Logger;

/**
 * Implements the play sound command.
 * Created by Kneesnap on 10/29/2024.
 */
public class kcActionPlaySound extends kcActionTemplate {
    public static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.SOUND, "sound");
    public static final int BITMASK_STOP_SOUND = 0xFFFF0000;

    private static final String ARGUMENT_STOP_SOUND = "StopSoundMask"; // This may exist, but isn't very helpful, as we have no way of knowing the correct ID to pass in order to disable the sound.

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
            kcParam sfxIdParam = getSoundId();
            sfxIdParam.setValue(sfxIdParam.getAsInteger() | BITMASK_STOP_SOUND);
        }
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        super.saveArguments(arguments, settings);
        if ((getSoundId().getAsInteger() & BITMASK_STOP_SOUND) != 0)
            arguments.getOrCreate(ARGUMENT_STOP_SOUND);
    }

    /**
     * Gets the kcParam representing the sound effect id.
     */
    public kcParam getSoundId() {
        return getParamOrError(0);
    }

    @Override
    public void printWarnings(Logger logger) {
        super.printWarnings(logger);

        GreatQuestChunkedFile chunkedFile = getExecutor() != null ? getExecutor().getChunkedFile() : null;
        if (chunkedFile != null) {
            SBRFile soundFile = chunkedFile.getSoundBankFile();
            int sfxId = getSoundId().getAsInteger();
            SfxEntry sfxEntry = soundFile != null ? soundFile.getEntryByID(sfxId) : null;
            if (sfxEntry == null) {
                String sfxFilePath = getGameInstance().getShortenedSoundPath(sfxId, false);
                printWarning(logger, "the sound '" + sfxFilePath + "' does not appear to be registered in the level's sound bank!");
            }
        }
    }
}
