package net.highwayfrogs.editor.file;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.VHFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses VB files and allows exporting to WAV, and importing audio files.
 * TODO: Add support for importing audio files.
 * TODO: Test exporting audio files.
 * TODO: Move into a sound folder.
 * Created by rdrpenguin04 on 8/22/2018.
 */
@Getter
public class VBFile extends GameFile {
    private VHFile header;
    private List<AudioEntry> audioEntries = new ArrayList<>();
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

        for (FileEntry vhEntry : header.getEntries()) {
            AudioEntry audioEntry = new AudioEntry(SOUND_ID++, vhEntry);

            reader.jumpTemp(vhEntry.getDataStartOffset());
            int byteSize = vhEntry.getDataSize() / Constants.BITS_PER_BYTE;
            int readLength = byteSize / audioEntry.getByteWidth();

            if (!reader.hasMore() || reader.getIndex() + byteSize > reader.getSize())
                return; // For some reason, the .VH files have way more entries than the VB has files. It looks to me like the VH has entries for all audio files, not just ones present in the VB.

            for (int i = 0; i < readLength; i++)
                audioEntry.getAudioData().add(reader.readInt(audioEntry.getByteWidth()));
            reader.jumpReturn();

            this.audioEntries.add(audioEntry);
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (AudioEntry entry : getAudioEntries())
            for (int toWrite : entry.getAudioData())
                writer.writeNumber(toWrite, entry.getByteWidth());
    }

    @Getter
    private static class AudioEntry {
        private FileEntry vhEntry;
        private int vanillaTrackId;
        private List<Integer> audioData = new ArrayList<>();

        public AudioEntry(int vanillaTrackId, FileEntry vhEntry) {
            this.vanillaTrackId = vanillaTrackId;
            this.vhEntry = vhEntry;
        }

        /**
         * Export this audio entry as a standard audio clip.
         * @return audioClip
         */
        public Clip toStandardAudio() throws LineUnavailableException {
            ArrayReceiver receiver = new ArrayReceiver();
            DataWriter writer = new DataWriter(receiver);

            for (int i = 0; i < getAudioData().size(); i++)
                writer.writeNumber(getAudioData().get(i), getByteWidth());
            byte[] byteData = receiver.toArray();

            Clip result = AudioSystem.getClip();
            result.open(new AudioFormat(getSampleRate(), getBitWidth(), getChannelCount(), true, false),
                    byteData, 0, byteData.length);

            return result;
        }

        /**
         * Get the number of channels for this entry.
         * @return channelCount
         */
        public int getChannelCount() {
            return vhEntry.getChannels();
        }

        /**
         * Set the number of channels for this entry.
         * @param channelCount The new channel amount.
         */
        public void setChannelCount(int channelCount) {
            vhEntry.setChannels(channelCount);
        }

        /**
         * Gets the sample rate of this audio entry.
         * @return sampleRate
         */
        public int getSampleRate() {
            return vhEntry.getSampleRate();
        }

        /**
         * Set the sample rate for this audio entry.
         * @param newSampleRate The new sample rate.
         */
        public void setSampleRate(int newSampleRate) {
            vhEntry.setSampleRate(newSampleRate);
        }

        /**
         * Gets the bit width for this audio entry.
         * @return bitWidth
         */
        public int getBitWidth() {
            return vhEntry.getBitWidth();
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
            vhEntry.setBitWidth(newBitWidth);
        }
    }
}
