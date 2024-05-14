package net.highwayfrogs.editor.games.shared.sound;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * The {@see javax.sound.sampled.AudioFormat} class represents configuration for how raw audio data is stored.
 * That class is immutable since most situations where audio is used do not need to change the audio format.
 * However, we need a way to change audio format settings, whether for playback purposes or whatnot.
 * This class manages the formatting to make it easier to modify these settings.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public class EditableAudioFormat extends AudioFormat {
    private boolean frameAutomaticallyCalculated;

    /**
     * Constructs an <code>AudioFormat</code> with the given parameters.
     * The encoding specifies the convention used to represent the data.
     * The other parameters are further explained in the {@link AudioFormat
     * class description}.
     * @param encoding                  the audio encoding technique
     * @param sampleRate                the number of samples per second
     * @param sampleSizeInBits  the number of bits in each sample
     * @param channels                  the number of channels (1 for mono, 2 for stereo, and so on)
     * @param frameSize                 the number of bytes in each frame
     * @param frameRate                 the number of frames per second
     * @param bigEndian                 indicates whether the data for a single sample
     *                                                  is stored in big-endian byte order (<code>false</code>
     *                                                  means little-endian)
     */
    public EditableAudioFormat(Encoding encoding, float sampleRate, int sampleSizeInBits,
                               int channels, int frameSize, float frameRate, boolean bigEndian) {
        super (encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        this.frameAutomaticallyCalculated = false;
    }

    /**
     * Constructs an <code>AudioFormat</code> with the given parameters.
     * The encoding specifies the convention used to represent the data.
     * The other parameters are further explained in the {@link AudioFormat
     * class description}.
     * @param encoding         the audio encoding technique
     * @param sampleRate       the number of samples per second
     * @param sampleSizeInBits the number of bits in each sample
     * @param channels         the number of channels (1 for mono, 2 for
     *                         stereo, and so on)
     * @param frameSize        the number of bytes in each frame
     * @param frameRate        the number of frames per second
     * @param bigEndian        indicates whether the data for a single sample
     *                         is stored in big-endian byte order
     *                         (<code>false</code> means little-endian)
     * @param properties       a <code>Map&lt;String,Object&gt;</code> object
     *                         containing format properties
     *
     * @since 1.5
     */
    public EditableAudioFormat(Encoding encoding, float sampleRate,
                               int sampleSizeInBits, int channels,
                               int frameSize, float frameRate,
                               boolean bigEndian, Map<String, Object> properties) {
        super (encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian, properties);
        this.frameAutomaticallyCalculated = false;
    }


    /**
     * Constructs an <code>AudioFormat</code> with a linear PCM encoding and
     * the given parameters.  The frame size is set to the number of bytes
     * required to contain one sample from each channel, and the frame rate
     * is set to the sample rate.
     *
     * @param sampleRate                the number of samples per second
     * @param sampleSizeInBits  the number of bits in each sample
     * @param channels                  the number of channels (1 for mono, 2 for stereo, and so on)
     * @param signed                    indicates whether the data is signed or unsigned
     * @param bigEndian                 indicates whether the data for a single sample
     *                                                  is stored in big-endian byte order (<code>false</code>
     *                                                  means little-endian)
     */
    public EditableAudioFormat(float sampleRate, int sampleSizeInBits,
                               int channels, boolean signed, boolean bigEndian) {
        super(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        this.frameAutomaticallyCalculated = true;
    }

    private static int calculateFrameSize(int channels, int sampleSizeInBits) {
        // This came from the constructor of AudioFormat.
        return (channels == AudioSystem.NOT_SPECIFIED || sampleSizeInBits == AudioSystem.NOT_SPECIFIED)
                ? AudioSystem.NOT_SPECIFIED
                : ((sampleSizeInBits + (Constants.BITS_PER_BYTE - 1)) / Constants.BITS_PER_BYTE) * channels;
    }

    /**
     * Sets whether the frame size is automatically calculated.
     * @param frameAutomaticallyCalculated Whether the frame size should be automatically calculated.
     * @return null iff the setting was successfully applied, otherwise a message describing why it failed.
     */
    public String setFrameAutomaticallyCalculated(boolean frameAutomaticallyCalculated) {
        boolean wasAutomaticallyCalculated = this.frameAutomaticallyCalculated;
        this.frameAutomaticallyCalculated = frameAutomaticallyCalculated;
        if (!wasAutomaticallyCalculated && frameAutomaticallyCalculated) {
            this.frameRate = this.sampleRate;
            this.frameSize = calculateFrameSize(this.channels, this.sampleSizeInBits);
        }

        return null;
    }

    /**
     * Applies the settings from another AudioFormat to this.
     * @param newFormat The new AudioFormat to import settings from.
     * @return null iff the setting was successfully applied, otherwise a message describing why it failed.
     */
    public String applyAudioFormat(AudioFormat newFormat) {
        if (newFormat == null)
            throw new NullPointerException("newFormat");

        String failMessage = null;
        int propertiesChanged = 0;

        // Apply new byte order.
        boolean wasBigEndian = this.bigEndian;
        boolean isBigEndian = newFormat.isBigEndian();
        if (wasBigEndian != isBigEndian) {
            failMessage = setBigEndian(isBigEndian);
            propertiesChanged++;
        }

        // Encoding.
        Encoding oldEncoding = getEncoding();
        Encoding newEncoding = newFormat.getEncoding();
        if (failMessage == null && oldEncoding != newEncoding) {
            failMessage = setEncoding(newEncoding);
            propertiesChanged++;
        }

        // Automatic calculation.
        // This should occur before the properties which will auto-calculate the values.
        boolean wasFrameCalculated = this.frameAutomaticallyCalculated;
        boolean nowFrameCalculated = isFrameAutomaticallyCalculated(newFormat);
        if (failMessage == null && wasFrameCalculated != nowFrameCalculated) {
            failMessage = setFrameAutomaticallyCalculated(nowFrameCalculated);
            propertiesChanged++;
        }

        // Sample rate.
        float oldSampleRate = getSampleRate();
        float newSampleRate = newFormat.getSampleRate();
        if (failMessage == null && oldSampleRate != newSampleRate) {
            failMessage = setSampleRate(newSampleRate);
            propertiesChanged++;
        }

        // Sample size.
        int oldSampleSizeInBits = getSampleSizeInBits();
        int newSampleSizeInBits = newFormat.getSampleSizeInBits();
        if (failMessage == null && oldSampleSizeInBits != newSampleSizeInBits) {
            failMessage = setSampleSizeInBits(newSampleSizeInBits);
            propertiesChanged++;
        }

        // Channels
        int oldChannels = getChannels();
        int newChannels = newFormat.getChannels();
        if (failMessage == null && oldChannels != newChannels) {
            failMessage = setChannels(newChannels);
            propertiesChanged++;
        }

        // Set frame size (if it's not automatically calculated.)
        int oldFrameSize = getFrameSize();
        int newFrameSize = newFormat.getFrameSize();
        if (failMessage == null && !nowFrameCalculated && oldFrameSize != newFrameSize) {
            failMessage = setFrameSize(newFrameSize);
            propertiesChanged++;
        }

        // Set frame rate (if it's not automatically calculated.)
        float oldFrameRate = getFrameRate();
        float newFrameRate = newFormat.getFrameRate();
        if (failMessage == null && !nowFrameCalculated && oldFrameRate != newFrameRate) {
            failMessage = setFrameRate(newFrameRate);
            propertiesChanged++;
        }

        // Restore old state if failed.
        if (failMessage != null) {
            if (wasBigEndian != isBigEndian && propertiesChanged-- > 0)
                setBigEndian(wasBigEndian);
            if (oldEncoding != newEncoding && propertiesChanged-- > 0)
                setEncoding(oldEncoding);
            if (wasFrameCalculated != nowFrameCalculated && propertiesChanged-- > 0)
                setFrameAutomaticallyCalculated(wasFrameCalculated);
            if (oldSampleRate != newSampleRate && propertiesChanged-- > 0)
                setSampleRate(oldSampleRate);
            if (oldSampleSizeInBits != newSampleSizeInBits && propertiesChanged-- > 0)
                setSampleSizeInBits(oldSampleSizeInBits);
            if (oldChannels != newChannels && propertiesChanged-- > 0)
                setChannels(oldChannels);
            if (!nowFrameCalculated && oldFrameSize != newFrameSize && propertiesChanged-- > 0)
                setFrameSize(oldFrameSize);
            if (!nowFrameCalculated && oldFrameRate != newFrameRate && propertiesChanged > 0)
                setFrameRate(oldFrameRate);
        }

        return failMessage;
    }

    private static boolean isFrameAutomaticallyCalculated(AudioFormat audioFormat) {
        if (audioFormat instanceof EditableAudioFormat)
            return ((EditableAudioFormat) audioFormat).isFrameAutomaticallyCalculated();

        // Guess based on what we see.
        return (audioFormat.getFrameRate() == audioFormat.getSampleRate())
                && calculateFrameSize(audioFormat.getChannels(), audioFormat.getSampleSizeInBits()) == audioFormat.getFrameSize();
    }

    /**
     * Sets the sample rate for this sound sample.
     * @param newSampleRate The sample rate to apply.
     * @return null iff the sample rate was successfully applied, otherwise a message describing why it failed.
     */
    public String setSampleRate(float newSampleRate) {
        this.sampleRate = newSampleRate;
        if (this.frameAutomaticallyCalculated)
            this.frameRate = newSampleRate;

        return null;
    }

    /**
     * Sets the frame rate for this sound sample.
     * @param newFrameRate The frame rate to apply.
     * @return null iff the frame rate was successfully applied, otherwise a message describing why it failed.
     */
    public String setFrameRate(float newFrameRate) {
        if (this.frameAutomaticallyCalculated)
            return "Cannot change frame rate, because automatic frame calculation is currently enabled.";

        this.frameRate = newFrameRate;
        return null;
    }

    /**
     * Sets the frame size for this sound sample.
     * @param newFrameSize The frame size to apply.
     * @return null iff the frame size was successfully applied, otherwise a message describing why it failed.
     */
    public String setFrameSize(int newFrameSize) {
        if (this.frameAutomaticallyCalculated)
            return "Cannot change frame rate, because automatic frame calculation is currently enabled.";

        this.frameSize = newFrameSize;
        return null;
    }

    /**
     * Sets the bits per sample.
     * @param bitsPerSample The bits per sample to apply.
     * @return null iff the bit width was successfully applied, otherwise a message describing why it failed.
     */
    public String setSampleSizeInBits(int bitsPerSample) {
        this.sampleSizeInBits = bitsPerSample;
        if (this.frameAutomaticallyCalculated && this.channels != AudioSystem.NOT_SPECIFIED)
            this.frameSize = calculateFrameSize(this.channels, bitsPerSample);

        return null;
    }

    /**
     * Gets the sample byte width by calculating it from the sample bit width.
     * @return byteWidth
     */
    public int getSampleSizeInBytes() {
        return getSampleSizeInBits() / Constants.BITS_PER_BYTE;
    }

    /**
     * Sets the byte width for this sound sample.
     * This is a shortcut to setSampleSizeInBits() with an automated conversion from bytes.
     * @param newByteWidth The byte width to apply.
     * @return null iff the bit width was successfully applied, otherwise a message describing why it failed.
     */
    public String setSampleSizeInBytes(int newByteWidth) {
        return setSampleSizeInBits(newByteWidth * Constants.BITS_PER_BYTE);
    }

    /**
     * Sets the number of channels used for this sound sample.
     * @param newChannelCount The channel count to apply.
     * @return null iff the bit width was successfully applied, otherwise a message describing why it failed.
     */
    public String setChannels(int newChannelCount) {
        this.channels = newChannelCount;
        if (this.frameAutomaticallyCalculated && this.sampleSizeInBits != AudioSystem.NOT_SPECIFIED)
            this.frameSize = calculateFrameSize(newChannelCount, this.sampleSizeInBits);

        return null;
    }

    /**
     * Sets the audio data encoding.
     * @param newEncoding The new encoding to apply.
     * @return null iff the encoding was successfully applied, otherwise a message describing why it failed.
     */
    public String setEncoding(Encoding newEncoding) {
        if (newEncoding == null)
            throw new NullPointerException("newEncoding");

        this.encoding = newEncoding;
        return null;
    }

    /**
     * Gets the byte order (endian) for this sound sample.
     * @return byteOrder
     */
    public ByteOrder getByteOrder() {
        return isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    /**
     * Sets the byte order (endian) for this sound sample.
     * @param bigEndian If the byte order is big endian.
     * @return null iff the byte order was successfully applied, otherwise a message describing why it failed.
     */
    public String setBigEndian(boolean bigEndian) {
        this.bigEndian = bigEndian;
        return null;
    }

    /**
     * Sets the byte order (endian) for this sound sample.
     * @param newByteOrder The byte order to apply.
     * @return null iff the byte order was successfully applied, otherwise a message describing why it failed.
     */
    public String setByteOrder(ByteOrder newByteOrder) {
        if (newByteOrder == null)
            throw new NullPointerException("newByteOrder");

        return setBigEndian(newByteOrder == ByteOrder.BIG_ENDIAN);
    }
}