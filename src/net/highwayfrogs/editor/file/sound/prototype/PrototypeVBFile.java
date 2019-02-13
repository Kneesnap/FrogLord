package net.highwayfrogs.editor.file.sound.prototype;

import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.GameSound;
import net.highwayfrogs.editor.file.sound.PCVBFile;
import net.highwayfrogs.editor.file.sound.VHFile.AudioHeader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Represents prototype audio body data.
 * Created by Kneesnap on 2/13/2019.
 */
public class PrototypeVBFile extends PCVBFile {

    @Override
    public GameSound makeSound(AudioHeader entry, int id, int readLength) {
        return new PrototypeSound(entry, id, readLength);
    }

    public static class PrototypeSound extends GameSound {
        private AudioFormat cachedFormat;
        private byte[] wavBytes;

        public PrototypeSound(AudioHeader header, int vanillaTrackId, int readLength) {
            super(header, vanillaTrackId, readLength);
        }

        @Override
        public void load(DataReader reader) {
            //TODO: Can we have a better reading system?
            this.wavBytes = reader.readBytes(Math.min(reader.getRemaining(), getReadLength()));
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeBytes(wavBytes);
        }

        @Override
        public Clip toStandardAudio() throws LineUnavailableException {
            Clip result = AudioSystem.getClip();
            result.open(getAudioFormat(), wavBytes, 0, wavBytes.length);
            return result;
        }

        @Override
        public void exportToFile(File saveTo) throws IOException {
            Files.write(saveTo.toPath(), wavBytes);
        }

        @Override
        public void replaceWithFile(File file) {
            //TODO

            onImport();
        }

        @Override
        public void onImport() {
            super.onImport();
            this.cachedFormat = null;
        }

        @Override
        @SneakyThrows
        public AudioFormat getAudioFormat() {
            if (this.cachedFormat == null) {
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(this.wavBytes)));
                this.cachedFormat = inputStream.getFormat();
            }

            return this.cachedFormat;
        }
    }
}
