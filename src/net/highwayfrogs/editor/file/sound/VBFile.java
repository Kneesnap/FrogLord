package net.highwayfrogs.editor.file.sound;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VHFile.AudioHeader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses VB files and allows exporting to WAV, and importing audio files.
 * Created by rdrpenguin04 on 8/22/2018.
 */
@Getter
public class VBFile extends PCVBFile {

    @Override
    public GameSound makeSound(AudioHeader entry, int id, int readLength) {
        return new PCSound(entry, id, readLength);
    }

    @Getter
    public static class PCSound extends GameSound {
        private List<Integer> audioData = new ArrayList<>();

        public PCSound(AudioHeader vhEntry, int vanillaTrackId, int readLength) {
            super(vhEntry, vanillaTrackId, readLength / vhEntry.getByteWidth());
        }

        @Override
        public void load(DataReader reader) {
            for (int i = 0; i < getReadLength(); i++)
                getAudioData().add(reader.readInt(getHeader().getByteWidth()));
        }

        @Override
        public void save(DataWriter writer) {
            for (int toWrite : getAudioData())
                writer.writeNumber(toWrite, getHeader().getByteWidth());
        }

        /**
         * Return the audio as a raw audio byte array.
         * @return byteData
         */
        public byte[] toRawAudio() {
            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter writer = new DataWriter(receiver);

            for (int i = 0; i < getAudioData().size(); i++)
                writer.writeNumber(getAudioData().get(i), getByteWidth());
            return receiver.toArray();
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
            Clip clip = getClip();
            AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(toRawAudio()), clip.getFormat(), clip.getFrameLength());
            AudioSystem.write(inputStream, Type.WAVE, saveTo);
        }

        @Override
        public void replaceWithFile(File file) throws IOException, UnsupportedAudioFileException {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
            getAudioData().clear();

            AudioFormat format = inputStream.getFormat();
            Utils.verify(!format.isBigEndian(), "Big Endian audio files are not accepted.");
            Utils.verify(format.getEncoding() == Encoding.PCM_SIGNED, "Unsigned audio files are not supported. (%s)", format.getEncoding());
            Utils.verify(format.getChannels() == getChannelCount(), "%d-channel audio is not supported!", format.getChannels());

            setBitWidth(format.getSampleSizeInBits());
            setSampleRate((int) format.getSampleRate());

            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter writer = new DataWriter(receiver);
            int byteLength = getByteWidth();

            byte[] buffer = new byte[byteLength];
            while (inputStream.read(buffer) != -1)
                writer.writeBytes(buffer);

            byte[] data = receiver.toArray();
            setDataSize(data.length);

            DataReader reader = new DataReader(new ArraySource(data));
            while (reader.hasMore())
                this.audioData.add(reader.readInt(byteLength));

            onImport();
        }
    }
}
