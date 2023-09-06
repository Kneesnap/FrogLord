package net.highwayfrogs.editor.file.sound.prototype;

import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.GameSound;
import net.highwayfrogs.editor.file.sound.PCVBFile;
import net.highwayfrogs.editor.file.sound.VHFile.AudioHeader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Represents prototype audio body data.
 * Created by Kneesnap on 2/13/2019.
 */
public class PrototypeVBFile extends PCVBFile {
    private static final byte[] RIFF_SIGNATURE = {0x52, 0x49, 0x46, 0x46};

    @Override
    public GameSound makeSound(AudioHeader entry, int id, int readLength) {
        return new PrototypeSound(entry, id, readLength);
    }

    public static class PrototypeSound extends GameSound {
        private AudioFormat cachedFormat;
        private byte[] wavBytes;
        private byte[] cachedRawAudio;

        public PrototypeSound(AudioHeader header, int vanillaTrackId, int readLength) {
            super(header, vanillaTrackId, readLength);
        }

        @Override
        public byte[] toRawAudio() {
            if (this.cachedRawAudio != null)
                return this.cachedRawAudio;

            ByteArrayInputStream inputStream = new ByteArrayInputStream(this.wavBytes);
            AudioInputStream audioInputStream;
            AudioInputStream convertedInputStream;

            try {
                audioInputStream = AudioSystem.getAudioInputStream(inputStream);
                convertedInputStream = AudioSystem.getAudioInputStream(getAudioFormat(), audioInputStream);
                audioInputStream.close();
            } catch (UnsupportedAudioFileException | IOException ex) {
                System.out.println("Couldn't read the audio data. The audio will still play, but it will have a pop.");
                ex.printStackTrace();
                return this.wavBytes;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
            Utils.copyInputStreamData(convertedInputStream, baos, true);
            return this.cachedRawAudio = baos.toByteArray();
        }

        @Override
        public void load(DataReader reader) {
            if (reader.hasMore()) // The last entry is null in the prototype.
                this.wavBytes = reader.readBytes(getReadLength());
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeBytes(wavBytes);
        }

        @Override
        public Clip toStandardAudio() throws LineUnavailableException {
            Clip result = AudioSystem.getClip();
            byte[] pcmData = toRawAudio();
            result.open(getAudioFormat(), pcmData, 0, pcmData.length);
            return result;
        }

        @Override
        public void exportToFile(File saveTo) throws IOException {
            Files.write(saveTo.toPath(), wavBytes);
        }

        @Override
        @SneakyThrows
        public void replaceWithFile(File file) {
            byte[] newBytes = Files.readAllBytes(file.toPath());

            // Basic header test.
            byte[] header = new byte[RIFF_SIGNATURE.length];
            System.arraycopy(newBytes, 0, header, 0, RIFF_SIGNATURE.length);
            Utils.verify(Arrays.equals(RIFF_SIGNATURE, header), "INVALID RIFF SIGNATURE: %s!", new String(header));

            //Import the file.
            if (this.importFormat(AudioSystem.getAudioInputStream(file).getFormat())) {
                this.wavBytes = newBytes; // Keep the audio data.
                onImport(); // Call onImport hook.
            }
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
