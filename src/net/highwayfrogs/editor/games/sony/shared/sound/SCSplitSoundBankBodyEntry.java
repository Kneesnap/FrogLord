package net.highwayfrogs.editor.games.sony.shared.sound;

import lombok.Getter;
import net.highwayfrogs.editor.games.shared.sound.EditableAudioFormat;
import net.highwayfrogs.editor.games.shared.sound.ISoundSample;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;

/**
 * Represents a body entry in the sound bank body.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public abstract class SCSplitSoundBankBodyEntry extends SCSharedGameData implements ISoundSample {
    private final SCSplitSoundBankBody<?, ?> body;
    private final SCSplitSoundBankHeaderEntry headerEntry;
    private final SCSplitSoundBankHeader<?, ?> header;
    private final EditableAudioFormat audioFormat;
    private final int internalTrackId;
    private final int globalTrackId;

    public SCSplitSoundBankBodyEntry(SCSplitSoundBankBody<?, ?> body, SCSplitSoundBankHeaderEntry headerEntry, SCSplitSoundBankHeader<?, ?> header, EditableAudioFormat audioFormat, int internalTrackId, int globalTrackId) {
        super(body.getGameInstance());
        this.body = body;
        this.headerEntry = headerEntry;
        this.header = header;
        this.audioFormat = audioFormat;
        this.internalTrackId = internalTrackId;
        this.globalTrackId = globalTrackId;
    }

    @Override
    public String getSoundName() {
        return getConfig().getSoundBank().getName(getGlobalTrackId());
    }
}