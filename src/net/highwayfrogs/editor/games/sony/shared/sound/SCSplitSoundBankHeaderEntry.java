package net.highwayfrogs.editor.games.sony.shared.sound;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;

/**
 * Represents a header entry in the sound bank header.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public abstract class SCSplitSoundBankHeaderEntry extends SCSharedGameData {
    private final SCSplitSoundBankHeader<?, ?> header;
    private final int internalId;

    public SCSplitSoundBankHeaderEntry(SCSplitSoundBankHeader<?, ?> header, int internalId) {
        super(header.getGameInstance());
        this.header = header;
        this.internalId = internalId;
    }
}