package net.highwayfrogs.editor.games.sony.shared.sound;

import lombok.Getter;
import net.highwayfrogs.editor.games.shared.coupled.CoupledDataEntry;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a header in a split sound bank.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public abstract class SCSplitSoundBankHeader<THeaderEntry extends SCSplitSoundBankHeaderEntry, TBodyEntry extends SCSplitSoundBankBodyEntry> extends CoupledDataEntry<SCSplitSoundBankBody<THeaderEntry, TBodyEntry>> {
    protected final List<THeaderEntry> entries = new ArrayList<>();

    public SCSplitSoundBankHeader(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public SCGameInstance getGameInstance() {
        return (SCGameInstance) super.getGameInstance();
    }
}