package net.highwayfrogs.editor.utils;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.Clip;

/**
 * Represents the contents of a .wav file.
 * Created by Kneesnap on 9/30/2025.
 */
public class WavFile implements IBinarySerializable, IPropertyListCreator {
    @Getter @Setter private short formatTag; // 1 - PCM
    @Getter @Setter private short channelCount;
    @Getter @Setter private int sampleRate; // Samples per second.
    private int averageBytesPerSecond;
    private short blockAlign;
    private short bitDepth;
    @Getter @Setter private byte[] extraHeaderBytes;
    @Getter @Setter private byte[] rawAudioData;
    @Getter @Setter private byte[] extraBytesAtEnd;
    @Setter private boolean ignoreCbSizeField;

    // Modelled by the 'WAVEFORMATEX' struct. https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatex
    // The function at 004494b0 (kcCAudioManagerPC::PlayPCMStream as I've dubbed it) reads a hardcoded 0x12 bytes for the header, suggesting it does not support larger headers.
    public static final int DEFAULT_HEADER_SIZE = 18;
    public static final int WAVE_EXTRA_DATA_SIZE = 7 * Constants.INTEGER_SIZE;

    public static final String RIFF_SIGNATURE = "RIFF";
    public static final String WAVE_SIGNATURE = "WAVE";
    public static final String FORMAT_SIGNATURE = "fmt ";
    public static final String DATA_SIGNATURE = "data";

    // See Windows mmreg.h for a full list.
    public static final short WAVE_FORMAT_PCM = 1; // bitDepth is 8 or 16.
    public static final short WAVE_FORMAT_ADPCM = 2;
    public static final short WAVE_FORMAT_IEEE_FLOAT = 3; // bitDepth: 32
    public static final short WAVE_FORMAT_ALAW = 6;
    public static final short WAVE_FORMAT_MULAW = 7;

    @Override
    public void load(DataReader reader) {
        int dataStartIndex = reader.getIndex();

        reader.verifyString(RIFF_SIGNATURE);
        int fileSize = reader.readInt();
        reader.verifyString(WAVE_SIGNATURE);
        reader.verifyString(FORMAT_SIGNATURE);
        int headerSize = reader.readInt();

        // Read header.
        int headerStartIndex = reader.getIndex();
        loadHeader(reader);
        int readHeaderSize = (reader.getIndex() - headerStartIndex);
        if (readHeaderSize != headerSize)
            throw new RuntimeException("Header size mismatch! Expected the header to be " + headerSize + " bytes large, but " + readHeaderSize + " bytes were read instead.");

        reader.verifyString(DATA_SIGNATURE);
        int dataSize = reader.readInt();
        loadData(reader.readBytes(dataSize));

        int readDataSize = reader.getIndex() - dataStartIndex - (2 * Constants.INTEGER_SIZE);
        if (readDataSize > fileSize)
            throw new IllegalStateException("The .wav file was " + fileSize + " bytes large, but " + readDataSize + " bytes were read!");

        this.extraBytesAtEnd = (fileSize > readDataSize) ? reader.readBytes(fileSize - readDataSize) : null;
    }

    /**
     * Loads the WAVEFORMATEX header from the reader
     * Info: <a href="https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatex"/>
     * @param reader the reader to read the data from
     */
    public void loadHeader(DataReader reader) {
        this.formatTag = reader.readShort();
        this.channelCount = reader.readShort();
        this.sampleRate = reader.readInt();
        this.averageBytesPerSecond = reader.readInt();
        this.blockAlign = reader.readShort();
        this.bitDepth = reader.readShort();
        int cbSize = reader.readShort();

        // From https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatex:
        // WAVEFORMATEX is nearly identical to the PCMWAVEFORMAT structure, which is an obsolete structure used to specify PCM formats.
        // The only difference is that WAVEFORMATEX contains a cbSize member and PCMWAVEFORMAT does not.
        // By convention, cbSize should be ignored when wFormatTag = WAVE_FORMAT_PCM (because cbSize is implicitly zero).
        // Also, note that Frogger: The Great Quest was likely using PCMWAVEFORMAT for the PS2 version, and WAVEFORMATEX for PC.
        if (this.formatTag == WAVE_FORMAT_PCM && cbSize != 0) { // cbSize is ALWAYS zero for WAVE_FORMAT_PCM according to the above documentation.
            this.ignoreCbSizeField = true;
            reader.setIndex(reader.getIndex() - Constants.SHORT_SIZE);
        } else {
            this.ignoreCbSizeField = false;
            this.extraHeaderBytes = cbSize > 0 ? reader.readBytes(cbSize) : null;
        }
    }

    /**
     * Loads the audio data body from the byte array.
     * @param audioData the audio data body
     */
    public void loadData(byte[] audioData) {
        if (audioData == null)
            throw new NullPointerException("audioData");

        this.rawAudioData = audioData;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(RIFF_SIGNATURE);
        int fileSizeAddress = writer.writeNullPointer();
        writer.writeStringBytes(WAVE_SIGNATURE);
        writer.writeStringBytes(FORMAT_SIGNATURE);
        int headerSizeAddress = writer.writeNullPointer();

        // Read header.
        int headerStartIndex = writer.getIndex();
        saveHeader(writer);
        int headerSize = (writer.getIndex() - headerStartIndex);
        writer.writeIntAtPos(headerSizeAddress, headerSize);

        // Write data body.
        writer.writeStringBytes(DATA_SIGNATURE);
        writer.writeInt(this.rawAudioData.length);
        saveData(writer);
        writer.writeIntAtPos(fileSizeAddress, writer.getIndex() - fileSizeAddress - Constants.INTEGER_SIZE);

        // Write extra bytes at the end of the file.
        if (this.extraBytesAtEnd != null)
            writer.writeBytes(this.extraBytesAtEnd);
    }

    /**
     * Saves the WAVEFORMATEX header to the writer
     * Info: <a href="https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatex"/>
     * @param writer the writer to write the data to
     */
    public void saveHeader(DataWriter writer) {
        writer.writeShort(this.formatTag);
        writer.writeShort(this.channelCount);
        writer.writeInt(this.sampleRate);
        writer.writeInt(getAverageBytesPerSecond());
        writer.writeShort(getBlockAlign());
        writer.writeShort(this.bitDepth);

        // See above for information.
        if (!this.ignoreCbSizeField || this.formatTag != WAVE_FORMAT_PCM) {
            writer.writeShort((short) (this.extraHeaderBytes != null ? this.extraHeaderBytes.length : 0));
            if (this.extraHeaderBytes != null)
                writer.writeBytes(this.extraHeaderBytes);
        }
    }

    /**
     * Saves the audio data body to the writer.
     * @param writer the writer to write the audio body data to
     */
    public void saveData(DataWriter writer) {
        writer.writeBytes(this.rawAudioData);
    }

    /**
     * Returns the average bytes per second.
     */
    @SuppressWarnings({"LombokGetterMayBeUsed", "RedundantSuppression"})
    public int getAverageBytesPerSecond() {
        if (this.formatTag == WAVE_FORMAT_PCM || this.formatTag == WAVE_FORMAT_IEEE_FLOAT) {
            return ((this.channelCount * this.sampleRate * this.bitDepth) / Constants.BITS_PER_BYTE); // This value doesn't appear to be used in most cases.
        } else {
            return this.averageBytesPerSecond;
        }
    }

    /**
     * Gets the block alignment of the audio data.
     */
    public short getBlockAlign() {
        if (this.formatTag == WAVE_FORMAT_PCM || this.formatTag == WAVE_FORMAT_IEEE_FLOAT) {
            return (short) ((this.channelCount * this.bitDepth) / Constants.BITS_PER_BYTE);
        } else {
            return this.blockAlign;
        }
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        int fileSizeInBytes = getFileSizeInBytes();
        propertyList.add("Sound File Size", fileSizeInBytes + " (" + DataSizeUnit.formatSize(fileSizeInBytes) + ")");
        propertyList.add("Audio Format", this.formatTag);
        propertyList.add("Sample Rate", getSampleRate());
        propertyList.add("Channel Count", getChannelCount());
        propertyList.add("Bit Depth", this.bitDepth);
        if (this.extraHeaderBytes != null && this.extraHeaderBytes.length > 0)
            propertyList.add("Extra Header Bytes", this.extraHeaderBytes.length);
        if (this.extraBytesAtEnd != null && this.extraBytesAtEnd.length > 0)
            propertyList.add("EOF Bytes", this.extraBytesAtEnd.length);
    }

    /**
     * Gets the size of the wav file in bytes.
     */
    public int getFileSizeInBytes() {
        return DEFAULT_HEADER_SIZE + WAVE_EXTRA_DATA_SIZE
                + (this.extraHeaderBytes != null ? this.extraHeaderBytes.length : 0)
                + (this.rawAudioData != null ? this.rawAudioData.length : 0)
                + (this.extraBytesAtEnd != null ? this.extraBytesAtEnd.length : 0);
    }

    /**
     * Creates an audio format object for this file.
     */
    public AudioFormat createAudioFormat() {
        Encoding audioType;
        switch (this.formatTag) {
            case WAVE_FORMAT_PCM:
            case WAVE_FORMAT_ADPCM:
                audioType = (this.bitDepth <= 8) ? Encoding.PCM_UNSIGNED : Encoding.PCM_SIGNED;
                break;
            case WAVE_FORMAT_IEEE_FLOAT:
                audioType = Encoding.PCM_FLOAT;
                break;
            case WAVE_FORMAT_MULAW:
                audioType = Encoding.ULAW;
                break;
            case WAVE_FORMAT_ALAW:
                audioType = Encoding.ALAW;
                break;
            default:
                audioType = Encoding.PCM_SIGNED; // If unsure, use PCM.
                break;
        }

        // Correct parameters determined by looking at com.sun.media.sound.WaveFileReader.getFMT()
        return new AudioFormat(audioType, this.sampleRate, this.bitDepth, this.channelCount,
                calculatePCMFrameSize(this.bitDepth, this.channelCount), this.sampleRate, false);
    }

    /**
     * Gets this wave file as a new playable audio clip
     */
    public Clip createClip(boolean showErrorWindow) {
        return AudioUtils.getClipFromWavFile(writeDataToByteArray(), showErrorWindow);
    }

    /**
     * Applies the settings seen in the provided audio format to this wav file
     * @param format the format to apply from
     */
    public void applyAudioFormat(AudioFormat format) {
        if (format == null)
            throw new NullPointerException("format");

        Encoding encoding = format.getEncoding();
        if (encoding.equals(Encoding.ULAW)) {
            this.formatTag = WAVE_FORMAT_MULAW;
        } else if (encoding.equals(Encoding.ALAW)) {
            this.formatTag = WAVE_FORMAT_ALAW;
        } else if (encoding.equals(Encoding.PCM_FLOAT)) {
            this.formatTag = WAVE_FORMAT_IEEE_FLOAT;
        } else {
            this.formatTag = WAVE_FORMAT_PCM;
        }

        this.bitDepth = (short) format.getSampleSizeInBits();
        this.sampleRate = (int) format.getSampleRate();
        this.channelCount = (short) format.getChannels();
        this.averageBytesPerSecond = 0;
    }

    /**
     * Converts the wav file to use the provided AudioFormat.
     * @param newFormat the new format to convert to
     */
    public void convertToAudioFormat(AudioFormat newFormat) {
        if (newFormat == null)
            throw new NullPointerException("newFormat");

        this.rawAudioData = AudioUtils.getRawAudioDataConvertedFromWavFile(newFormat, writeDataToByteArray());
        applyAudioFormat(newFormat);
    }

    /**
     * This ensures the wav file is at least a given number of seconds long.
     * This was just made to allow using voice clips with ElevenLabs, which has a 5 second minimum clip length.
     * @param seconds the minimum number of seconds the audio should be
     */
    public void padToAtLeast(double seconds) {
        int bytesPerSecond = (this.channelCount * this.sampleRate * this.bitDepth);
        int totalBytes = (int) ((seconds * bytesPerSecond) / Constants.BITS_PER_BYTE);
        if (this.rawAudioData.length >= totalBytes)
            return;

        byte[] newAudioData = new byte[totalBytes];
        System.arraycopy(this.rawAudioData, 0, newAudioData, 0, this.rawAudioData.length);
        this.rawAudioData = newAudioData;
    }

    /**
     * Calculates the frame size for PCM audio.
     * @param bitsPerSample the number of bits per sample
     * @param channelCount the number of audio channels
     * @return pcmFrameSize
     */
    public static int calculatePCMFrameSize(int bitsPerSample, int channelCount) {
        return (bitsPerSample + Constants.BITS_PER_BYTE - 1) / Constants.BITS_PER_BYTE * channelCount;
    }
}
