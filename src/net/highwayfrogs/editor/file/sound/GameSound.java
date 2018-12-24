package net.highwayfrogs.editor.file.sound;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a game sound.
 * Created by Kneesnap on 9/24/2018.
 */
@Getter
public abstract class GameSound {
    private int vanillaTrackId;
    private List<Integer> audioData = new ArrayList<>();

    private static final Map<String, List<String>> SOUND_NAME_MAP = new HashMap<>();
    private static final List<String> SOUND_NAME_BY_TRACK_ID = new ArrayList<>();

    public GameSound(int vanillaTrackId) {
        this.vanillaTrackId = vanillaTrackId;
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
    public abstract int getChannelCount();

    /**
     * Set the amount of channels.
     * @param channelCount New channel count.
     */
    public abstract void setChannelCount(int channelCount);

    /**
     * Gets the sample rate of this sound.
     * @return sampleRate
     */
    public abstract int getSampleRate();

    /**
     * Sets the sample rate for this sound.
     * @param newSampleRate The sample rate to set.
     */
    public abstract void setSampleRate(int newSampleRate);

    /**
     * Gets the bit width for this sound.
     * @return bitWidth
     */
    public abstract int getBitWidth();


    /**
     * Sets the bit width for this sound.
     * @param newBitWidth The bit width to set.
     */
    public abstract void setBitWidth(int newBitWidth);

    /**
     * Gets the byte width for this sound.
     * @return byteWidth
     */
    public int getByteWidth() {
        return getBitWidth() / Constants.BITS_PER_BYTE;
    }

    /**
     * Sets the byte-size of this sound's audio data.
     * @param newSize The audio data's new size.
     */
    public abstract void setDataSize(int newSize);

    /**
     * Get the name of this sound.
     * @return soundName
     */
    public String getSoundName() {
        return getVanillaTrackId() >= 0 && getVanillaTrackId() < SOUND_NAME_BY_TRACK_ID.size()
                ? SOUND_NAME_BY_TRACK_ID.get(getVanillaTrackId())
                : "???????";
    }

    static {
        InputStreamReader isr = new InputStreamReader(Utils.getResourceStream("sounds.cfg"));
        BufferedReader reader = new BufferedReader(isr);

        List<String> lines = reader.lines().collect(Collectors.toList());

        String tempBank = null;
        List<String> tempNames = new ArrayList<>();

        for (String line : lines) {
            line = line.split("#")[0].trim(); // Remove comments.

            if (line.isEmpty())
                continue; // Ignore blank lines.

            if (line.startsWith("[") && line.endsWith("]")) { // New section.
                if (tempBank != null)
                    SOUND_NAME_MAP.put(tempBank, tempNames);
                tempBank = line.substring(1, line.length() - 1);
                tempNames = new ArrayList<>();
            } else {
                SOUND_NAME_BY_TRACK_ID.add(line);
                tempNames.add(line);
            }
        }

        SOUND_NAME_MAP.put(tempBank, tempNames); // Add the final bank.
    }
}
