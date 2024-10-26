package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Contains static utilities for working with audio.
 * Created by Kneesnap on 10/25/2024.
 */
public class AudioUtils {
    public static final BrowserFileType BROWSER_WAV_FILE_TYPE = new BrowserFileType("Audio File", "wav");
    public static final SavedFilePath EXPORT_WAV_FILE_PATH = new SavedFilePath("wavFileExportPath", "Specify the file to save the sound as...", BROWSER_WAV_FILE_TYPE);
    public static final SavedFilePath IMPORT_WAV_FILE_PATH = new SavedFilePath("wavFileImportPath", "Specify the sound file to load...", BROWSER_WAV_FILE_TYPE);

    /**
     * Creates a wav file with the given data.
     * @param formatHeaderSource a byte array containing ONLY a wave format header
     * @param rawAudioDataSource a byte array containing ONLY the raw audio data
     * @return wavFile
     */
    public static byte[] createWavFile(byte[] formatHeaderSource, byte[] rawAudioDataSource) {
        if (formatHeaderSource == null)
            throw new NullPointerException("formatHeaderSource");
        if (rawAudioDataSource == null)
            throw new NullPointerException("rawAudioDataSource");

        return createWavFile(formatHeaderSource, 0, formatHeaderSource.length, rawAudioDataSource, 0, rawAudioDataSource.length);
    }

    /**
     * Creates a wav file with the given data.
     * @param formatHeaderSource a byte array containing a wave format header
     * @param formatStartIndex the index where the data to the wave data header starts
     * @param formatLength the amount of bytes in the format header
     * @param rawAudioDataSource a byte array containing the raw audio data
     * @param audioDataStartIndex the index where the raw audio data starts
     * @param audioDataLength the amount of bytes to read of raw audio data
     * @return wavFile
     */
    public static byte[] createWavFile(byte[] formatHeaderSource, int formatStartIndex, int formatLength, byte[] rawAudioDataSource, int audioDataStartIndex, int audioDataLength) {
        if (formatHeaderSource == null)
            throw new NullPointerException("formatHeaderSource");
        if (rawAudioDataSource == null)
            throw new NullPointerException("rawAudioDataSource");

        if (formatLength < 16)
            throw new IllegalArgumentException("The size of a wave audio header format must be at least 16 bytes! (Was: " + formatLength + ")");
        if (formatStartIndex < 0)
            throw new IllegalArgumentException("The offset into the wave audio header byte array must be at least 0! (Was: " + formatStartIndex + ")");
        if (formatLength > formatHeaderSource.length - formatStartIndex)
            throw new IllegalArgumentException("The format size (" + formatLength + ") was greater than the total number of bytes available! (" + (formatHeaderSource.length - formatStartIndex) + ")");

        if (audioDataStartIndex < 0)
            throw new IllegalArgumentException("The offset into the raw audio data byte array must be at least 0! (Was: " + audioDataStartIndex + ")");
        if (audioDataLength < 0)
            throw new IllegalArgumentException("The size of the raw audio data must be at least 0 bytes! (Was: " + audioDataLength + ")");
        if (audioDataLength > rawAudioDataSource.length - audioDataStartIndex)
            throw new IllegalArgumentException("The format size (" + formatLength + ") was greater than the total number of bytes available! (" + (rawAudioDataSource.length - audioDataStartIndex) + ")");

        // Read the audio format header.
        byte[] waveFormatEx;
        if (formatStartIndex == 0 && formatHeaderSource.length == formatLength) {
            waveFormatEx = formatHeaderSource;
        } else {
            waveFormatEx = new byte[formatLength];
            System.arraycopy(formatHeaderSource, formatStartIndex, waveFormatEx, 0, formatLength);
        }

        // Read the raw audio data.
        byte[] rawAudioData;
        if (audioDataStartIndex == 0 && rawAudioDataSource.length == audioDataLength) {
            rawAudioData = rawAudioDataSource;
        } else {
            rawAudioData = new byte[audioDataLength];
            System.arraycopy(rawAudioDataSource, audioDataStartIndex, rawAudioData, 0, audioDataLength);
        }

        // Write the WAV file.
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter writer = new DataWriter(receiver);

        writer.writeStringBytes("RIFF");
        int fileSizeAddress = writer.writeNullPointer();
        writer.writeStringBytes("WAVE");
        writer.writeStringBytes("fmt ");
        writer.writeInt(waveFormatEx.length); // Write chunk 1 size.
        writer.writeBytes(waveFormatEx);
        writer.writeStringBytes("data");
        writer.writeInt(rawAudioData.length);
        writer.writeBytes(rawAudioData);
        writer.writeIntAtPos(fileSizeAddress, writer.getIndex() - (fileSizeAddress + Constants.INTEGER_SIZE));

        writer.closeReceiver();
        return receiver.toArray();
    }

    /**
     * Gets the AudioFormat from file bytes representing a supported audio file type (Usually .wav)
     * @param wavFileContents the file contents to load.
     * @return rawAudioFormat
     */
    public static AudioFormat getAudioFormatFromWavFile(byte[] wavFileContents) {
        if (wavFileContents == null)
            throw new NullPointerException("wavFileContents");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(wavFileContents);
        AudioInputStream audioInputStream = null;

        try {
            audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            return audioInputStream.getFormat();
        } catch (UnsupportedAudioFileException | IOException ex) {
            Utils.handleError(Utils.getLogger(), ex, true, "Couldn't read the audio data.");
            return null;
        } finally {
            try {
                if (audioInputStream != null)
                    audioInputStream.close();
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "Failed to close AudioInputStream.");
            }
        }
    }

    /**
     * Gets an audio clip from the raw audio data.
     * @param format the format to create the clip with
     * @param rawAudioData the raw audio data to supply to the clip
     * @return audioClip, if successful
     */
    public static Clip getClipFromRawAudioData(AudioFormat format, byte[] rawAudioData) {
        if (format == null)
            throw new NullPointerException("format");
        if (rawAudioData == null)
            throw new NullPointerException("rawAudioData");

        Clip audioClip = null;
        try {
            audioClip = AudioSystem.getClip();
            audioClip.open(format, rawAudioData, 0, rawAudioData.length);
            return audioClip;
        } catch (LineUnavailableException ex) {
            Utils.handleError(null, ex, false, "Failed to create an audio Clip.");
            if (audioClip != null)
                audioClip.close();

            return null;
        }
    }

    /**
     * Gets an audio Clip from file bytes representing a supported audio file type (Usually .wav)
     * @param wavFileContents the file contents to load.
     * @return audioClip
     */
    public static Clip getClipFromWavFile(byte[] wavFileContents, boolean showErrorWindow) {
        if (wavFileContents == null)
            throw new NullPointerException("wavFileContents");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(wavFileContents);
        AudioInputStream audioInputStream = null;

        try {
            audioInputStream = AudioSystem.getAudioInputStream(inputStream);

            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            return clip;
        } catch (UnsupportedAudioFileException ex) {
            Utils.handleError(Utils.getLogger(), ex, showErrorWindow, "The audio file format appears to be unsupported.\nThe file may only be playable using an external audio player such as VLC.");
            return null;
        } catch (IOException ex) {
            Utils.handleError(Utils.getLogger(), ex, showErrorWindow, "Problems with IO when reading the audio data.");
            return null;
        } catch (LineUnavailableException ex) {
            Utils.handleError(Utils.getLogger(), ex, showErrorWindow, "Couldn't get an audio line.");
            return null;
        } finally {
            try {
                if (audioInputStream != null)
                    audioInputStream.close();
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "Failed to close AudioInputStream.");
            }
        }
    }

    /**
     * Gets raw audio data from file bytes representing a supported audio file type (Usually .wav)
     * @param wavFileContents the file contents to load.
     * @return rawAudioDataInBytes
     */
    public static byte[] getRawAudioDataFromWavFile(byte[] wavFileContents) {
        return getRawAudioDataConvertedFromWavFile(null, wavFileContents);
    }

    /**
     * Gets raw audio data from file bytes representing a supported audio file type (Usually .wav)
     * @param targetFormat the target format to convert the audio to. Null indicates no conversion should occur.
     * @param wavFileContents the file contents to load.
     * @return rawAudioDataInBytes
     */
    public static byte[] getRawAudioDataConvertedFromWavFile(AudioFormat targetFormat, byte[] wavFileContents) {
        if (wavFileContents == null)
            throw new NullPointerException("wavFileContents");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(wavFileContents);
        AudioInputStream audioInputStream;
        AudioInputStream convertedInputStream;

        try {
            audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            if (targetFormat == null)
                targetFormat = audioInputStream.getFormat();

            convertedInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            audioInputStream.close();
        } catch (UnsupportedAudioFileException | IOException ex) {
            Utils.handleError(Utils.getLogger(), ex, true, "Couldn't read the audio data. The audio will still play, but it will have a pop.");
            return wavFileContents;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) (audioInputStream.getFrameLength() * targetFormat.getFrameSize()));
        FileUtils.copyInputStreamData(convertedInputStream, byteArrayOutputStream, true);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Saves the raw audio clip provided to the given wav file.
     * @param file the file to save the audio data to
     * @param format the format to get data from
     * @param rawAudioData the raw (header-less) audio data.
     */
    public static void saveRawAudioDataToWavFile(File file, AudioFormat format, byte[] rawAudioData) {
        saveRawAudioDataToFile(file, AudioFileFormat.Type.WAVE, format, rawAudioData);
    }

    /**
     * Saves the raw audio clip provided to the given file.
     * @param file the file to save the audio data to
     * @param fileType the format (usually .wav) to save the file contents as
     * @param format the format to get data from
     * @param rawAudioData the raw (header-less) audio data.
     */
    public static void saveRawAudioDataToFile(File file, AudioFileFormat.Type fileType, AudioFormat format, byte[] rawAudioData) {
        if (file == null)
            throw new NullPointerException("file");
        if (format == null)
            throw new NullPointerException("format");

        Clip audioClip = getClipFromRawAudioData(format, rawAudioData);
        if (audioClip == null)
            return;

        saveRawAudioDataToFile(file, fileType, audioClip, rawAudioData);
        audioClip.close();
    }

    /**
     * Saves the raw audio clip provided to the given wav file.
     * @param file the file to save the audio data to
     * @param audioClip the audio clip to source format and information from
     * @param rawAudioData the raw (header-less) audio data.
     */
    public static void saveRawAudioDataToWavFile(File file, Clip audioClip, byte[] rawAudioData) {
        saveRawAudioDataToFile(file, AudioFileFormat.Type.WAVE, audioClip, rawAudioData);
    }

    /**
     * Saves the raw audio clip provided to the given file.
     * @param file the file to save the audio data to
     * @param fileType the format (usually .wav) to save the file contents as
     * @param audioClip the audio clip to source format and information from
     * @param rawAudioData the raw (header-less) audio data.
     */
    public static void saveRawAudioDataToFile(File file, AudioFileFormat.Type fileType, Clip audioClip, byte[] rawAudioData) {
        if (file == null)
            throw new NullPointerException("file");
        if (fileType == null)
            throw new NullPointerException("fileType");
        if (audioClip == null)
            throw new NullPointerException("audioClip");
        if (rawAudioData == null)
            throw new NullPointerException("rawAudioData");
        if (DataUtils.testSignature(rawAudioData, "RIFF"))
            throw new IllegalArgumentException("The 'rawAudioData' appears to have a RIFF header, making it already already directly savable as a .wav file!");

        AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(rawAudioData), audioClip.getFormat(), audioClip.getFrameLength());
        try {
            AudioSystem.write(inputStream, fileType, file);
        } catch (IOException ex) {
            Utils.handleError(null, ex, false, "Failed to save sound to file '%s'.", file.getName());
        }
    }
}
