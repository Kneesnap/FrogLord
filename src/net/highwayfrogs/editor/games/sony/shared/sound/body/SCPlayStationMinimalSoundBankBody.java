package net.highwayfrogs.editor.games.sony.shared.sound.body;

import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationSoundBankBody.SCPlayStationVabSound;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationMinimalSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationMinimalSoundBankHeader.SCPlayStationMinimalSoundBankHeaderEntry;

/**
 * Contains VAG data, compatible with the minimal header.
 * Created by Kneesnap on 5/13/2024.
 */
public class SCPlayStationMinimalSoundBankBody  extends SCSplitSoundBankBody<SCPlayStationMinimalSoundBankHeaderEntry, SCPlayStationVabSound> {
    public SCPlayStationMinimalSoundBankBody(SCGameInstance instance, String fileName) {
        super(instance, fileName);
    }

    @Override
    public boolean load(DataReader reader, SCSplitSoundBankHeader<SCPlayStationMinimalSoundBankHeaderEntry, SCPlayStationVabSound> other) {
        if (!(other instanceof SCPlayStationMinimalSoundBankHeader) || other.getEntries().isEmpty())
            return false; // Can't read without header.

        getEntries().clear();
        for (int i = 0; i < other.getEntries().size(); i++) {
            int audioSize = i >= other.getEntries().size() - 1 ? reader.getRemaining() : other.getEntries().get(i + 1).getDataStartAddress(); // Where the reading ends.
            if (audioSize == 0)
                break;

            SCPlayStationVabSound newSound = new SCPlayStationVabSound(this, other.getEntries().get(i), i, audioSize);
            newSound.load(reader);
            getEntries().add(newSound);
        }

        return true;
    }

    @Override
    public void save(DataWriter writer, SCSplitSoundBankHeader<SCPlayStationMinimalSoundBankHeaderEntry, SCPlayStationVabSound> other) {
        for (int i = 0; i < getEntries().size(); i++)
            getEntries().get(i).save(writer);
    }
}