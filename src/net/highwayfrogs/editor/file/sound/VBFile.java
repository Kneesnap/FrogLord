package net.highwayfrogs.editor.file.sound;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VHFile.AudioHeader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses VB files and allows exporting to WAV, and importing audio files.
 * TODO: Add support for importing audio files.
 * Created by rdrpenguin04 on 8/22/2018.
 */
@Getter
public class VBFile extends GameFile {
    private VHFile header;
    private List<GameSound> audioEntries = new ArrayList<>();
    private DataReader cachedReader;

    public static int SOUND_ID;

    /**
     * Load the VB file, with the mandatory VH file.
     * @param file The VHFile to load information from.
     */
    public void load(VHFile file) {
        Utils.verify(this.cachedReader != null, "Tried to load VB without a reader.");
        this.header = file;
        load(this.cachedReader);
        this.cachedReader = null;
    }

    @Override
    public void load(DataReader reader) {
        if (getHeader() == null) {
            this.cachedReader = reader;
            return;
        }

        while (header.getEntries().size() > SOUND_ID) {
            AudioHeader vhEntry = header.getEntries().get(SOUND_ID);
            GameSound audioEntry = new GameSound(SOUND_ID, vhEntry);

            int byteSize = vhEntry.getDataSize();
            int readLength = byteSize / audioEntry.getByteWidth();
            reader.jumpTemp(vhEntry.getDataStartOffset());

            if (!reader.hasMore() || reader.getIndex() + byteSize > reader.getSize())
                return; // For some reason, the .VH files have way more entries than the VB has files. It looks to me like the VH has entries for all audio files, not just ones present in the VB.

            for (int i = 0; i < readLength; i++)
                audioEntry.getAudioData().add(reader.readInt(audioEntry.getByteWidth()));
            reader.jumpReturn();

            this.audioEntries.add(audioEntry);
            SOUND_ID++;
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (GameSound entry : getAudioEntries())
            for (int toWrite : entry.getAudioData())
                writer.writeNumber(toWrite, entry.getByteWidth());
    }

    @Getter
    public static class GameSound {
        private AudioHeader header;
        private int vanillaTrackId;
        private List<Integer> audioData = new ArrayList<>();

        public GameSound(int vanillaTrackId, AudioHeader vhEntry) {
            this.vanillaTrackId = vanillaTrackId;
            this.header = vhEntry;
        }

        /**
         * Export this audio entry as a standard audio clip.
         * @return audioClip
         */
        public Clip toStandardAudio() throws LineUnavailableException {
            byte[] byteData = toRawAudio();

            Clip result = AudioSystem.getClip();
            result.open(getAudioFormat(), byteData, 0, byteData.length);
            return result;
        }

        /**
         * Export this audio clip to a file.
         * @param saveTo The audio file to export.
         */
        public void exportToFile(File saveTo) throws IOException, LineUnavailableException {
            Clip clip = toStandardAudio();
            AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(toRawAudio()), clip.getFormat(), clip.getFrameLength());
            AudioSystem.write(inputStream, Type.WAVE, saveTo);
        }

        /**
         * Import a sound file to override
         * @param file The file to replace this sound with.
         */
        public void replaceWithFile(File file) {

        }

        /**
         * Gets the audio format used by this GameSound.
         * @return audioFormat
         */
        public AudioFormat getAudioFormat() {
            return new AudioFormat(getSampleRate(), getBitWidth(), getChannelCount(), true, false);
        }

        /**
         * Return the audioentry as a raw audio byte array.
         * @return byteData
         */
        public byte[] toRawAudio() {
            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter writer = new DataWriter(receiver);

            for (int i = 0; i < getAudioData().size(); i++)
                writer.writeNumber(getAudioData().get(i), getByteWidth());
            return receiver.toArray();
        }

        /**
         * Get the number of channels for this entry.
         * @return channelCount
         */
        public int getChannelCount() {
            return header.getChannels();
        }

        /**
         * Set the number of channels for this entry.
         * @param channelCount The new channel amount.
         */
        public void setChannelCount(int channelCount) {
            header.setChannels(channelCount);
        }

        /**
         * Gets the sample rate of this audio entry.
         * @return sampleRate
         */
        public int getSampleRate() {
            return header.getSampleRate();
        }

        /**
         * Set the sample rate for this audio entry.
         * @param newSampleRate The new sample rate.
         */
        public void setSampleRate(int newSampleRate) {
            header.setSampleRate(newSampleRate);
        }

        /**
         * Gets the bit width for this audio entry.
         * @return bitWidth
         */
        public int getBitWidth() {
            return header.getBitWidth();
        }

        /**
         * Gets the byte width for this audio entry.
         * @return byteWidth
         */
        public int getByteWidth() {
            return getBitWidth() / Constants.BITS_PER_BYTE;
        }

        /**
         * Sets the bit width for this audio entry.
         * @param newBitWidth The new bit width.
         */
        public void setBitWidth(int newBitWidth) {
            header.setBitWidth(newBitWidth);
        }
    }
}
