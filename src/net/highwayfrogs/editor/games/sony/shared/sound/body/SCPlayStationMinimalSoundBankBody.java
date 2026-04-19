package net.highwayfrogs.editor.games.sony.shared.sound.body;

import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationSoundBankBody.SCPlayStationVabSound;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationMinimalSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationMinimalSoundBankHeader.SCPlayStationMinimalSoundBankHeaderEntry;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.*;

/**
 * Contains VAG data, compatible with the minimal header (MediEvil II, C-12 Final Resistance).
 * NOTE: This is a heavily simplified format, not similar to the .VH formats seen in previous games.
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

        // Generate a mapping of the header entries to their audio sizes.
        Map<SCPlayStationMinimalSoundBankHeaderEntry, Integer> audioSizesByEntry = getAudioSizeMap(other.getEntries(), reader.getSize());

        // Load audio entries.
        this.entries.clear();
        for (int i = 0; i < other.getEntries().size(); i++) {
            SCPlayStationMinimalSoundBankHeaderEntry entry = other.getEntries().get(i);
            Integer audioSize = audioSizesByEntry.get(entry); // Where the reading ends.
            if (audioSize == null || audioSize == 0)
                break;

            SCPlayStationVabSound newSound = new SCPlayStationVabSound(this, entry, i, audioSize);
            newSound.load(reader);
            this.entries.add(newSound);
        }

        return true;
    }

    @Override
    public void save(DataWriter writer, SCSplitSoundBankHeader<SCPlayStationMinimalSoundBankHeaderEntry, SCPlayStationVabSound> other) {
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).save(writer);
    }

    private static Map<SCPlayStationMinimalSoundBankHeaderEntry, Integer> getAudioSizeMap(List<SCPlayStationMinimalSoundBankHeaderEntry> entries, int fileSize) {
        List<SCPlayStationMinimalSoundBankHeaderEntry> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort(Comparator.comparingInt(SCPlayStationMinimalSoundBankHeaderEntry::getDataStartAddress));

        // Use the sorted option.
        Map<SCPlayStationMinimalSoundBankHeaderEntry, Integer> audioSizesByEntry = new HashMap<>(sortedEntries.size());
        SCPlayStationMinimalSoundBankHeaderEntry lastEntry = null;
        for (int i = 0; i < sortedEntries.size(); i++) {
            SCPlayStationMinimalSoundBankHeaderEntry currentEntry = sortedEntries.get(i);
            if (lastEntry != null)
                audioSizesByEntry.put(lastEntry, currentEntry.getDataStartAddress() - lastEntry.getDataStartAddress());

            lastEntry = currentEntry;
        }

        if (lastEntry != null)
            audioSizesByEntry.put(lastEntry, fileSize - lastEntry.getDataStartAddress());

        return audioSizesByEntry;
    }
}