package net.highwayfrogs.editor.games.sony.shared.sound.body;

import javafx.scene.control.Alert.AlertType;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCWindowsPreReleaseSoundBankBody.SCWindowsPreReleaseSoundBodyEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCWindowsRetailSoundBankBody.SCWindowsSoundBodyEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCWindowsSoundBankHeader.SCWindowsSoundBankHeaderEntry;
import net.highwayfrogs.editor.utils.AudioUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Represents a Windows sound bank body in pre-release form.
 * This format is seen from June 1997 to September 1997 in Frogger prototypes.
 * Builds starting with the retail PC build of Frogger (November 1997) use "SCWindowsRetailSoundBankBody".
 * Created by Kneesnap on 5/13/2024.
 */
public class SCWindowsPreReleaseSoundBankBody extends SCWindowsSoundBankBody<SCWindowsPreReleaseSoundBodyEntry> {
    private static final byte[] RIFF_SIGNATURE = {0x52, 0x49, 0x46, 0x46};

    public SCWindowsPreReleaseSoundBankBody(SCGameInstance instance, String fileName) {
        super(instance, fileName);
    }

    @Override
    public SCWindowsPreReleaseSoundBodyEntry createNewEntry(SCWindowsSoundBankHeaderEntry entry, int id) {
        return new SCWindowsPreReleaseSoundBodyEntry(this, entry, id);
    }

    public static class SCWindowsPreReleaseSoundBodyEntry extends SCWindowsSoundBodyEntry {
        private byte[] cachedRawAudio;

        public SCWindowsPreReleaseSoundBodyEntry(SCWindowsPreReleaseSoundBankBody body, SCWindowsSoundBankHeaderEntry headerEntry, int vanillaTrackId) {
            super(body, headerEntry, vanillaTrackId);
        }

        @Override
        public void load(DataReader reader) {
            if (reader.hasMore()) { // The last entry is null in the prototype.
                super.load(reader);

                AudioFormat appliedFormat = getAudioFormatFromRawData();
                if (appliedFormat != null)
                    getAudioFormat().applyAudioFormat(appliedFormat); // Use the audio info from the wav file.
            }
        }

        @Override
        public byte[] getRawAudioPlaybackData() {
            if (this.cachedRawAudio != null)
                return this.cachedRawAudio;

            return this.cachedRawAudio = AudioUtils.getRawAudioDataConvertedFromWavFile(getAudioFormat(), this.rawAudioData);
        }

        @Override
        public void saveToImportableFile(File saveTo) throws IOException {
            Files.write(saveTo.toPath(), this.rawAudioData);
        }

        @Override
        public void importSoundFromFile(File file) throws IOException {
            byte[] newWavBytes = Files.readAllBytes(file.toPath());
            if (newWavBytes == null || newWavBytes.length == 0) {
                FXUtils.makePopUp("The file is empty.", AlertType.ERROR);
                return;
            }

            // Basic header test.
            byte[] header = new byte[RIFF_SIGNATURE.length];
            System.arraycopy(newWavBytes, 0, header, 0, RIFF_SIGNATURE.length);
            Utils.verify(Arrays.equals(RIFF_SIGNATURE, header), "INVALID RIFF SIGNATURE: %s!", new String(header));

            // Import the file.
            byte[] oldWavBytes = this.rawAudioData;
            this.rawAudioData = newWavBytes; // Keep the audio data.

            // Update audio format, and revert if it can't be applied.
            String errorMessage;
            if ((errorMessage = getAudioFormat().applyAudioFormat(getAudioFormatFromRawData())) != null) {
                this.rawAudioData = oldWavBytes;
                FXUtils.makePopUp(errorMessage, AlertType.ERROR);
            } else {
                // Reset cache.
                this.cachedRawAudio = null;
            }
        }

        @SneakyThrows
        private AudioFormat getAudioFormatFromRawData() {

            return this.rawAudioData != null && this.rawAudioData.length > 0 ? AudioUtils.getAudioFormatFromWavFile(this.rawAudioData) : null;
        }
    }
}