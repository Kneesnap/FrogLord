package net.highwayfrogs.editor.games.sony.shared.sound.body;

import javafx.scene.control.Alert.AlertType;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBodyEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCWindowsRetailSoundBankBody.SCWindowsSoundBodyEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCWindowsSoundBankHeader.SCWindowsSoundBankHeaderEntry;
import net.highwayfrogs.editor.utils.AudioUtils;
import net.highwayfrogs.editor.utils.FXUtils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Builds starting with the retail PC build of Frogger (November 1997) use this.
 * This is pretty much just raw pcm data.
 * Created by Kneesnap on 5/13/2024.
 */
public class SCWindowsRetailSoundBankBody extends SCWindowsSoundBankBody<SCWindowsSoundBodyEntry> {
    public SCWindowsRetailSoundBankBody(SCGameInstance instance, String fileName) {
        super(instance, fileName);
    }

    @Override
    public SCWindowsSoundBodyEntry createNewEntry(SCWindowsSoundBankHeaderEntry entry, int id) {
        return new SCWindowsSoundBodyEntry(this, entry, id);
    }

    @Getter
    public static class SCWindowsSoundBodyEntry extends SCSplitSoundBankBodyEntry {
        protected byte[] rawAudioData;

        public SCWindowsSoundBodyEntry(SCWindowsRetailSoundBankBody body, SCWindowsSoundBankHeaderEntry headerEntry, int vanillaTrackId) {
            super(body, headerEntry, headerEntry.getHeader(), headerEntry.getAudioFormat(), vanillaTrackId);
        }

        protected SCWindowsSoundBodyEntry(SCWindowsPreReleaseSoundBankBody body, SCWindowsSoundBankHeaderEntry headerEntry, int vanillaTrackId) {
            super(body, headerEntry, headerEntry.getHeader(), headerEntry.getAudioFormat(), vanillaTrackId);
        }

        @Override
        public SCWindowsSoundBankHeaderEntry getHeaderEntry() {
            return (SCWindowsSoundBankHeaderEntry) super.getHeaderEntry();
        }

        @Override
        public void load(DataReader reader) {
            this.rawAudioData = reader.readBytes(getHeaderEntry().getDataSize());
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeBytes(this.rawAudioData);
        }

        @Override
        public boolean isClipSupported() {
            return true;
        }

        @Override
        public byte[] getRawAudioPlaybackData() {
            return this.rawAudioData;
        }

        @Override
        public void saveToImportableFile(File saveTo) throws IOException, LineUnavailableException {
            AudioUtils.saveRawAudioDataToWavFile(saveTo, getAudioClip(), getRawAudioPlaybackData());
        }

        @Override
        public void importSoundFromFile(File file) throws IOException, UnsupportedAudioFileException {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);

            String errorMessage;
            if ((errorMessage = getAudioFormat().applyAudioFormat(inputStream.getFormat())) != null) {
                FXUtils.makePopUp(errorMessage, AlertType.ERROR);
                return; // Import failed.
            }

            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter writer = new DataWriter(receiver);

            // Read audio data.
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1)
                writer.writeBytes(buffer, 0, bytesRead);

            // Apply new audio data.
            this.rawAudioData = receiver.toArray();
        }
    }
}