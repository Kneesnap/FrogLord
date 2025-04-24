package net.highwayfrogs.editor.games.sony.shared.sound.body;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.shared.sound.ISoundSample;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBodyEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCWindowsSoundBankHeader.SCWindowsSoundBankHeaderEntry;

/**
 * Represents the audio body as implemented for Windows.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public abstract class SCWindowsSoundBankBody<TBodyEntry extends SCSplitSoundBankBodyEntry & ISoundSample> extends SCSplitSoundBankBody<SCWindowsSoundBankHeaderEntry, TBodyEntry> {
    public SCWindowsSoundBankBody(SCGameInstance instance, String fileName) {
        super(instance, fileName);
    }

    @Override
    public boolean load(DataReader reader, SCSplitSoundBankHeader<SCWindowsSoundBankHeaderEntry, TBodyEntry> other) {
        if (other == null || other.getEntries().isEmpty())
            return false; // Can't read any data without the header.

        getEntries().clear();
        for (int id = 0; id < other.getEntries().size(); id++) {
            SCWindowsSoundBankHeaderEntry headerEntry = other.getEntries().get(id);
            if (!headerEntry.isAudioPresent()) { // If we don't have the audio for this entry...
                if (getEntries().isEmpty()) {
                    continue; // and we haven't loaded any entries yet, keep going.
                } else {
                    break; // and we've already loaded at least one entry, we're done reading entries.
                }
            }

            reader.jumpTemp(headerEntry.getDataStartOffset());
            TBodyEntry loadedEntry = createNewEntry(headerEntry, id);
            loadedEntry.load(reader);
            reader.jumpReturn();

            getEntries().add(loadedEntry);
        }

        return true;
    }

    @Override
    public void save(DataWriter writer, SCSplitSoundBankHeader<SCWindowsSoundBankHeaderEntry, TBodyEntry> header) {
        for (int i = 0; i < getEntries().size(); i++)
            getEntries().get(i).save(writer);
    }

    /**
     * Make the sound this class will use.
     * @param entry The entry for the file.
     * @param id    The sound id.
     * @return newGameSound
     */
    public abstract TBodyEntry createNewEntry(SCWindowsSoundBankHeaderEntry entry, int id);
}