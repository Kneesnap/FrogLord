package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntry;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Implements the play sound command.
 * Created by Kneesnap on 10/29/2024.
 */
public class kcActionPlaySound extends kcActionTemplate {
    public static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.SOUND, "sound");
    public static final int BITMASK_STOP_SOUND = 0xFFFF0000;

    public kcActionPlaySound(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    /**
     * Gets the kcParam representing the sound effect id.
     */
    public kcParam getSoundId() {
        return getParamOrError(0);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);

        GreatQuestChunkedFile chunkedFile = getExecutor() != null ? getExecutor().getChunkedFile() : null;
        if (chunkedFile != null) {
            SBRFile soundFile = chunkedFile.getSoundBankFile();
            int sfxId = getSoundId().getAsSfxId();
            if (getGameInstance().hasFullSoundPathFor(sfxId)) {
                SfxEntry sfxEntry = soundFile != null ? soundFile.getEntryByID(sfxId) : null;
                if (sfxEntry == null) {
                    String sfxFilePath = getGameInstance().getShortenedSoundPath(sfxId, false);
                    printWarning(logger, "the sound '" + sfxFilePath + "' does not appear to be registered in the level's sound bank!");
                }
            }
        }
    }
}
