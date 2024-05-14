package net.highwayfrogs.editor.games.sony.shared.sound;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameData.SharedGameData;
import net.highwayfrogs.editor.games.shared.sound.EditableAudioFormat;
import net.highwayfrogs.editor.games.shared.sound.ISoundSample;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

/**
 * Represents a body entry in the sound bank body.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public abstract class SCSplitSoundBankBodyEntry extends SharedGameData implements ISoundSample {
    private final SCSplitSoundBankBody<?, ?> body;
    private final SCSplitSoundBankHeaderEntry headerEntry;
    private final SCSplitSoundBankHeader<?, ?> header;
    private final EditableAudioFormat audioFormat;
    private final int internalTrackId;

    public SCSplitSoundBankBodyEntry(SCSplitSoundBankBody<?, ?> body, SCSplitSoundBankHeaderEntry headerEntry, SCSplitSoundBankHeader<?, ?> header, EditableAudioFormat audioFormat, int internalTrackId) {
        super(body.getGameInstance());
        this.body = body;
        this.headerEntry = headerEntry;
        this.header = header;
        this.audioFormat = audioFormat;
        this.internalTrackId = internalTrackId;
    }

    @Override
    public SCGameInstance getGameInstance() {
        return (SCGameInstance) super.getGameInstance();
    }

    @Override
    public SCGameConfig getConfig() {
        return (SCGameConfig) super.getConfig();
    }

    @Override
    public String getSoundName() {
        return getConfig().getSoundBank().getName(getInternalTrackId());
    }
}