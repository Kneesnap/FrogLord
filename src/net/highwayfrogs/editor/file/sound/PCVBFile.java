package net.highwayfrogs.editor.file.sound;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VHFile.AudioHeader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

/**
 * Represents a VB file with PC VH headers.
 * Created by Kneesnap on 2/13/2019.
 */
public abstract class PCVBFile extends VBAudioBody<VHFile> {
    public PCVBFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader, VHFile header) {
        for (int id = 0; id < header.getEntries().size(); id++) {
            AudioHeader vhEntry = header.getEntries().get(id);
            if (!vhEntry.isAudioPresent()) { // If we don't have the audio for this entry...
                if (getAudioEntries().isEmpty()) {
                    continue; // and we haven't loaded any entries yet, keep going.
                } else {
                    return; // and we've already loaded at least one entry, we're done reading entries.
                }
            }

            reader.jumpTemp(vhEntry.getDataStartOffset());
            GameSound gameSound = makeSound(vhEntry, id, vhEntry.getDataSize());
            gameSound.load(reader);
            reader.jumpReturn();

            getAudioEntries().add(gameSound);
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (GameSound entry : getAudioEntries())
            entry.save(writer);
    }

    /**
     * Make the sound this class will use.
     * @param entry The entry for the file.
     * @param id    The sound id.
     * @return newGameSound
     */
    public abstract GameSound makeSound(AudioHeader entry, int id, int readLength);
}