package net.highwayfrogs.editor.games.sony.shared.sound;

import lombok.Getter;
import net.highwayfrogs.editor.games.shared.coupled.CoupledDataEntry;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a body in a split sound bank.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public abstract class SCSplitSoundBankBody<THeaderEntry extends SCSplitSoundBankHeaderEntry, TBodyEntry extends SCSplitSoundBankBodyEntry> extends CoupledDataEntry<SCSplitSoundBankHeader<THeaderEntry, TBodyEntry>> {
    private final String fileName;
    private final List<TBodyEntry> entries = new ArrayList<>();

    public SCSplitSoundBankBody(SCGameInstance instance, String fileName) {
        super(instance);
        this.fileName = fileName;
    }

    @Override
    public SCGameInstance getGameInstance() {
        return (SCGameInstance) super.getGameInstance();
    }
}