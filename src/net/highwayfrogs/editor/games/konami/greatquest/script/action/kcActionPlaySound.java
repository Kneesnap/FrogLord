package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntry;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Implements the play sound command.
 * Created by Kneesnap on 10/29/2024.
 */
public class kcActionPlaySound extends kcAction {
    private String soundPath;
    private int soundId;

    public static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.SOUND, "sound");
    public static final int BITMASK_STOP_SOUND = 0xFFFF0000;

    public kcActionPlaySound(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.soundId = reader.next().getAsSfxId();
        this.soundPath = getGameInstance().getFullSoundPath(this.soundId);
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.soundId);
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        this.soundPath = arguments.useNext().getAsString();
        this.soundId = getGameInstance().getSfxIdFromFullSoundPath(this.soundPath);
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsString(this.soundPath, this.soundPath != null && !NumberUtils.isInteger(this.soundPath));
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);

        // Validate SFX ID is valid.
        if (!getGameInstance().hasFullSoundPathFor(this.soundId)) {
            if (this.soundPath != null) {
                printWarning(logger, "no sound could be found named '" + this.soundPath + "'. (Sound ID: " + this.soundId + ")");
            } else {
                printWarning(logger, "no sound was provided. (Sound ID: " + this.soundId + ")");
            }
            return;
        }

        // Validate SFX found in sound bank.
        GreatQuestChunkedFile chunkedFile = getExecutor() != null ? getExecutor().getChunkedFile() : null;
        if (chunkedFile != null) {
            SBRFile soundFile = chunkedFile.getSoundBankFile();
            SfxEntry sfxEntry = soundFile != null ? soundFile.getEntryByID(this.soundId) : null;
            if (sfxEntry == null) {
                String shortenedSfxFilePath = getGameInstance().getShortenedSoundPath(this.soundId, false);
                printWarning(logger, "the sound '" + (shortenedSfxFilePath != null ? shortenedSfxFilePath : this.soundPath) + "' does not appear to be registered in the level's sound bank!");
            }
        } else {
            printWarning(logger, "FrogLord is unable to validate the sound '" + this.soundPath + "' is present in the level's sound bank!");
        }
    }
}
