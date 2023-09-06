package net.highwayfrogs.editor.file.sound.psx;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.AbstractVBFile;
import net.highwayfrogs.editor.file.sound.GameSound;
import net.highwayfrogs.editor.file.sound.VHFile.AudioHeader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A file which contains PSX audio data.
 * Created by Kneesnap on 2/17/2020.
 */
@Getter
public class PSXVBFile extends AbstractVBFile<PSXVHFile> {
    private transient int savedTotalSize;

    @Override
    public void load(DataReader reader) {
        if (this.cachedReader == null) {
            this.cachedReader = reader;
            return;
        }

        int[] addresses = getHeader().getLoadedSampleAddresses();
        for (int i = 0; i < addresses.length; i++) {
            int nextAddress = (addresses.length > i + 1 ? addresses[i + 1] : 0);
            int audioSize = nextAddress > 0 ? nextAddress : reader.getSize() - reader.getIndex(); // Where the reading ends.
            if (audioSize == 0)
                break;

            NameBank bank = getConfig().getSoundBank().getChildBank(Utils.stripExtension(getFileEntry().getDisplayName()));
            PSXSound newSound = new PSXSound(bank != null ? getConfig().getSoundBank().getNames().indexOf(bank.getName(i)) : -1, audioSize);
            newSound.load(reader);
            getAudioEntries().add(newSound);
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (GameSound sound : getAudioEntries())
            sound.save(writer);

        this.savedTotalSize = writer.getIndex();
    }

    @Getter
    @Setter
    public static class PSXSound extends GameSound {
        private byte[] audioData;
        private int sampleRate = 11025;
        private transient int addressWrittenTo;

        public PSXSound(int vanillaTrackId, int readLength) {
            super(null, vanillaTrackId, readLength);
        }

        @Override
        public byte[] toRawAudio() {
            return VAGUtil.rawVagToWav(this.audioData);
        }

        @Override
        public Clip toStandardAudio() throws LineUnavailableException {
            byte[] byteData = toRawAudio();
            Clip result = AudioSystem.getClip();
            result.open(getAudioFormat(), byteData, 0, byteData.length);
            return result;
        }

        @Override
        public void exportToFile(File saveTo) throws IOException {
            Files.write(saveTo.toPath(), VAGUtil.rawVagToWav(this.audioData, this.sampleRate));
        }

        @Override
        public void replaceWithFile(File file) {
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
            this.audioData = reader.readBytes(getReadLength() - 16);
        }

        @Override
        public void save(DataWriter writer) {
            this.addressWrittenTo = writer.getIndex();
            writer.writeNull(16);
            writer.writeBytes(this.audioData);
        }

        @Override
        public int getSampleRate() {
            return this.sampleRate;
        }

        @Override
        public void setSampleRate(int newSampleRate) {
            this.sampleRate = newSampleRate;
        }

        @Override
        public int getBitWidth() {
            return 16;
        }

        @Override
        public AudioHeader getHeader() {
            throw new RuntimeException("Cannot get AudioHeader for PSXSound, since it has none.");
        }
    }
}