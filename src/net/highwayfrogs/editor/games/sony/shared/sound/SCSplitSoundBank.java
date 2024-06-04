package net.highwayfrogs.editor.games.sony.shared.sound;

import net.highwayfrogs.editor.games.shared.coupled.CoupledDataParent;
import net.highwayfrogs.editor.games.shared.sound.ISoundBank;
import net.highwayfrogs.editor.games.shared.sound.ISoundSample;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

import java.util.List;

/**
 * Represents a sound bank which is split into having a header and a body.
 * Created by Kneesnap on 5/13/2024.
 */
public class SCSplitSoundBank extends CoupledDataParent implements ISoundBank {
    public SCSplitSoundBank(SCGameInstance instance, SCSplitSoundBankHeader<? extends SCSplitSoundBankHeaderEntry, ? extends SCSplitSoundBankBodyEntry> header, SCSplitSoundBankBody<? extends SCSplitSoundBankHeaderEntry, ? extends SCSplitSoundBankBodyEntry> body) {
        super(instance, header, body);
    }

    @Override
    public SCSplitSoundBankHeader<? extends SCSplitSoundBankHeaderEntry, ? extends SCSplitSoundBankBodyEntry> getFirstData() {
        return (SCSplitSoundBankHeader<? extends SCSplitSoundBankHeaderEntry, ? extends SCSplitSoundBankBodyEntry>) super.getFirstData();
    }

    @Override
    public SCSplitSoundBankBody<? extends SCSplitSoundBankHeaderEntry, ? extends SCSplitSoundBankBodyEntry> getSecondData() {
        return (SCSplitSoundBankBody<? extends SCSplitSoundBankHeaderEntry, ? extends SCSplitSoundBankBodyEntry>) super.getSecondData();
    }

    @Override
    public List<? extends ISoundSample> getSounds() {
        return getSecondData().getEntries();
    }
}