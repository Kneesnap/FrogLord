package net.highwayfrogs.editor.games.sony.shared.sound.body;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.sound.VAGUtil;
import net.highwayfrogs.editor.games.shared.sound.EditableAudioFormat;
import net.highwayfrogs.editor.games.shared.sound.ISoundSample;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBodyEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationSoundBankBody.SCPlayStationVabSound;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationVabSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationVabSoundBankHeader.SCPlayStationVabHeaderEntry;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Implements the VAB audio data body.
 * Created by Kneesnap on 5/13/2024.
 */
public class SCPlayStationSoundBankBody extends SCSplitSoundBankBody<SCPlayStationVabHeaderEntry, SCPlayStationVabSound> {
    public SCPlayStationSoundBankBody(SCGameInstance instance, String fileName) {
        super(instance, fileName);
    }

    @Override
    public boolean load(DataReader reader, SCSplitSoundBankHeader<SCPlayStationVabHeaderEntry, SCPlayStationVabSound> other) {
        if (!(other instanceof SCPlayStationVabSoundBankHeader))
            return false; // Can't read without header.

        SCPlayStationVabSoundBankHeader typedHeader = (SCPlayStationVabSoundBankHeader) other;
        int[] addresses = typedHeader.getLoadedSampleAddresses();
        if (addresses == null)
            return false; // Can't read yet since the header hasn't read its own data yet.

        getEntries().clear();
        for (int i = 0; i < addresses.length; i++) {
            int audioSize = i >= addresses.length - 1 ? reader.getRemaining() : addresses[i + 1]; // Where the reading ends.
            if (audioSize == 0)
                break;

            SCPlayStationVabSound newSound = new SCPlayStationVabSound(this, typedHeader, i, audioSize);
            newSound.load(reader);
            getEntries().add(newSound);
        }

        return true;
    }

    @Override
    public void save(DataWriter writer, SCSplitSoundBankHeader<SCPlayStationVabHeaderEntry, SCPlayStationVabSound> other) {
        int dataStartIndex = writer.getIndex();
        for (int i = 0; i < getEntries().size(); i++)
            getEntries().get(i).save(writer);

        // Apply to the header.
        if (other instanceof SCPlayStationVabSoundBankHeader)
            ((SCPlayStationVabSoundBankHeader) other).setSavedBodyTotalSize(writer.getIndex() - dataStartIndex);
    }

    @Getter
    public static class SCPlayStationVabSound extends SCSplitSoundBankBodyEntry implements ISoundSample {
        private final int expectedReadLength;
        private byte[] audioData;

        public SCPlayStationVabSound(SCSplitSoundBankBody<?, ?> body, SCPlayStationVabSoundBankHeader header, int internalTrackId, int expectedReadLength) {
            super(body, null, header, new EditableAudioFormat(11025, 16, 1, true, false), makeGlobalId(body, internalTrackId));
            this.expectedReadLength = expectedReadLength;
            // TODO: Prevent editing everything except sample rate.
        }

        @Override
        public SCPlayStationVabSoundBankHeader getHeader() {
            return (SCPlayStationVabSoundBankHeader) super.getHeader();
        }

        @Override
        public boolean isClipSupported() {
            return true;
        }

        @Override
        public byte[] getRawAudioPlaybackData() {
            return VAGUtil.rawVagToWav(this.audioData);
        }

        @Override
        public void saveToImportableFile(File saveTo) throws IOException {
            Files.write(saveTo.toPath(), VAGUtil.rawVagToWav(this.audioData, (int) getAudioFormat().getSampleRate()));
        }

        @Override
        public void importSoundFromFile(File file) {
            byte[] wavBytes;

            try {
                wavBytes = Files.readAllBytes(file.toPath());
            } catch (Exception ex) {
                Utils.makeErrorPopUp("There was an error reading the wav file.", ex, true);
                return;
            }

            this.audioData = VAGUtil.wavToVag(wavBytes);
        }

        @Override
        public void load(DataReader reader) {
            reader.skipBytes(16);
            this.audioData = reader.readBytes(this.expectedReadLength - 16);
        }

        @Override
        public void save(DataWriter writer) {
            getHeader().getLoadedSampleAddresses()[getInternalTrackId()] = writer.getIndex();
            writer.writeNull(16);
            writer.writeBytes(this.audioData);
        }

        private static int makeGlobalId(SCSplitSoundBankBody<?, ?> body, int internalTrackId) {
            // Generate name.
            SCGameConfig config = body.getGameInstance().getConfig();
            String bankName = Utils.stripExtension(body.getFileName());
            NameBank bank = config.getSoundBank().getChildBank(bankName);
            if (bank != null) {
                String soundName = bank.getName(internalTrackId);
                return config.getSoundBank().getNames().indexOf(soundName);
            } else {
                return -1;
            }
        }
    }
}