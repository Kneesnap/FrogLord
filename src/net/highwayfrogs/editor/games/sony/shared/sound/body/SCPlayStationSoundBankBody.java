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
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationMinimalSoundBankHeader.SCPlayStationMinimalSoundBankHeaderEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationVabSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationVabSoundBankHeader.SCPlayStationVabHeaderEntry;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;

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
            int audioSize = i >= addresses.length - 1 || addresses[i + 1] == 0 ? reader.getRemaining() : addresses[i + 1]; // Where the reading ends.
            if (!reader.hasMore())
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
        private byte[] vagAudioData;
        private static final int EMPTY_BYTE_PADDING = 16;

        public SCPlayStationVabSound(SCSplitSoundBankBody<?, ?> body, SCPlayStationVabSoundBankHeader header, int internalTrackId, int expectedReadLength) {
            super(body, null, header, new EditableAudioFormat(11025, 16, 1, true, false), makeGlobalId(body, internalTrackId));
            this.expectedReadLength = expectedReadLength;
            // TODO: Prevent editing everything except sample rate.
        }

        public SCPlayStationVabSound(SCSplitSoundBankBody<?, ?> body, SCPlayStationMinimalSoundBankHeaderEntry headerEntry, int internalTrackId, int expectedReadLength) {
            super(body, headerEntry, headerEntry.getHeader(), new EditableAudioFormat(11025, 16, 1, true, false), makeGlobalId(body, internalTrackId));
            this.expectedReadLength = expectedReadLength;
            // TODO: Prevent editing everything except sample rate.
        }

        @Override
        public boolean isClipSupported() {
            return true;
        }

        @Override
        public byte[] getRawAudioPlaybackData() {
            return VAGUtil.rawVagToWav(this.vagAudioData);
        }

        @Override
        public void saveToImportableFile(File saveTo) throws IOException {
            Files.write(saveTo.toPath(), VAGUtil.rawVagToWav(this.vagAudioData, (int) getAudioFormat().getSampleRate()));
        }

        @Override
        public void importSoundFromFile(File file) {
            byte[] wavBytes;

            try {
                wavBytes = Files.readAllBytes(file.toPath());
            } catch (Exception ex) {
                FXUtils.makeErrorPopUp("There was an error reading the wav file.", ex, true);
                return;
            }

            this.vagAudioData = VAGUtil.wavToVag(wavBytes);
        }

        @Override
        public void load(DataReader reader) {
            reader.skipBytesRequireEmpty(EMPTY_BYTE_PADDING);
            this.vagAudioData = reader.readBytes(this.expectedReadLength - EMPTY_BYTE_PADDING);
        }

        @Override
        public void save(DataWriter writer) {
            if (getHeaderEntry() instanceof SCPlayStationMinimalSoundBankHeaderEntry) {
                ((SCPlayStationMinimalSoundBankHeaderEntry) getHeaderEntry()).setDataStartAddress(writer.getIndex());
            } else if (getHeader() instanceof SCPlayStationVabSoundBankHeader) {
                ((SCPlayStationVabSoundBankHeader) getHeader()).getLoadedSampleAddresses()[getInternalTrackId() + 1] = (this.vagAudioData != null ? this.vagAudioData.length : 0) + EMPTY_BYTE_PADDING;
            }

            writer.writeNull(EMPTY_BYTE_PADDING);
            if (this.vagAudioData != null)
                writer.writeBytes(this.vagAudioData);
        }

        private static int makeGlobalId(SCSplitSoundBankBody<?, ?> body, int internalTrackId) {
            // Generate name.
            SCGameConfig config = body.getGameInstance().getVersionConfig();
            String bankName = FileUtils.stripExtension(body.getFileName());
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