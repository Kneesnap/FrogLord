package net.highwayfrogs.editor.games.shared.sound;

import net.highwayfrogs.editor.games.generic.data.IGameObject;
import net.highwayfrogs.editor.utils.AudioUtils;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Represents audio data approximating a sample.
 * Created by Kneesnap on 5/13/2024.
 */
public interface ISoundSample extends IGameObject {

    /**
     * Returns true iff the sample supports creating a javax Clip object.
     */
    boolean isClipSupported();

    /**
     * Gets the sample as a Clip.
     * @return clip
     */
    default Clip getAudioClip() throws LineUnavailableException {
        return AudioUtils.getClipFromRawAudioData(getAudioFormat(), getRawAudioPlaybackData());
    }

    /**
     * Gets the audio format used by this sample.
     * @return audioFormat
     */
    EditableAudioFormat getAudioFormat();

    /**
     * Gets the raw audio data as bytes.
     * These bytes should contain only the bytes passed to whichever playback system is used to play the sound.
     * For Clip, this means no header data is supported, just raw adpcm.
     */
    byte[] getRawAudioPlaybackData();

    /**
     * Export the sound to a file.
     * The file is one expected to contain just this sound.
     * The format is expected to be possible to re-import, and ideally there are external tools which can support it.
     * @param saveTo The audio file to export.
     */
    void saveToImportableFile(File saveTo) throws IOException, LineUnavailableException, UnsupportedAudioFileException;

    /**
     * Import sound data from the file.
     * Failures should be thrown as exceptions.
     * @param file The file to load sound data from.
     */
    void importSoundFromFile(File file) throws IOException, UnsupportedAudioFileException;

    /**
     * Get the name of this sound.
     * TODO: I'd like a little more variety.
     * @return soundName
     */
    public String getSoundName();
}