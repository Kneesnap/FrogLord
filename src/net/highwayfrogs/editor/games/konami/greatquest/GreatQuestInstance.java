package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SoundChunkFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestGameFile;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestMainMenuUIController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents an instance of 'Frogger: The Great Quest'.
 * Created by Kneesnap on 4/13/2024.
 */
@Getter
public class GreatQuestInstance extends GameInstance {
    private final List<GreatQuestGameFile> allFiles = new ArrayList<>();
    private final List<GreatQuestGameFile> looseFiles = new ArrayList<>();
    private GreatQuestAssetBinFile mainArchive;
    private SoundChunkFile soundChunkFile;
    private File mainArchiveBinFile;
    private final Map<Integer, String> soundPathsById = new HashMap<>();
    private final Map<String, Integer> soundIdsByPath = new HashMap<>();

    public static final float JUMP_SLOPE_THRESHOLD = .8F;

    // Padding data.
    public static final byte PADDING_BYTE_DEFAULT = (byte) 0xCC;
    public static final byte PADDING_BYTE_CD = (byte) 0xCD;
    private static final byte[] PADDING_DEFAULT_INT_BYTES = {PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT};
    public static final int PADDING_DEFAULT_INT = Utils.readIntFromBytes(PADDING_DEFAULT_INT_BYTES, 0);
    private static final byte[] PADDING_CD_INT_BYTES = {PADDING_BYTE_CD, PADDING_BYTE_CD, PADDING_BYTE_CD, PADDING_BYTE_CD};
    public static final int PADDING_CD_INT = Utils.readIntFromBytes(PADDING_CD_INT_BYTES, 0);

    public GreatQuestInstance() {
        super(GreatQuestGameType.INSTANCE);
    }

    /**
     * Load and setup all instance data relating to the game such as version configuration and game files.
     * @param gameVersionConfigName The name of the version configuration file to load.
     * @param binFile the main archive file to read
     * @param progressBar the progress bar to display load progress on, if it exists
     */
    public void loadGame(String gameVersionConfigName, File binFile, ProgressBarComponent progressBar) {
        if (this.mainArchive != null)
            throw new RuntimeException("The game instance has already been loaded.");

        if (binFile == null || !binFile.exists())
            throw new RuntimeException("The main archive file '" + binFile + "' does not exist.");

        this.mainArchiveBinFile = binFile;
        loadGameConfig(gameVersionConfigName);
        loadSoundFilePaths();

        // Load the sound files.
        loadSoundFolder();

        // Load the main file.
        try {
            DataReader reader = new DataReader(new FileSource(binFile));
            this.mainArchive = new GreatQuestAssetBinFile(this);
            this.mainArchive.load(reader, progressBar);
            this.allFiles.addAll(this.mainArchive.getFiles());
        } catch (IOException ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to load the bin file.");
        }
    }

    private void loadSoundFilePaths() {
        Config config = Config.loadTextConfigFromInputStream(getGameType().getEmbeddedResourceStream("sound-list.cfg"), "sound-list");

        this.soundIdsByPath.clear();
        this.soundPathsById.clear();
        for (Entry<String, ConfigValueNode> keyValuePair : config.getKeyValuePairs().entrySet()) {
            if (!Utils.isInteger(keyValuePair.getKey())) {
                getLogger().warning("sound-list key '" + keyValuePair.getKey() + "' is not an integer, skipping!");
                continue;
            }

            int soundId = Integer.parseInt(keyValuePair.getKey());
            String soundPath = keyValuePair.getValue().getAsString();
            if (soundPath == null || soundPath.trim().isEmpty()) {
                getLogger().warning("sound-list key '" + keyValuePair.getKey() + "' has not associated value, skipping!");
                continue;
            }

            this.soundPathsById.put(soundId, soundPath);
            Integer oldSoundId = this.soundIdsByPath.put(soundPath, soundId);

            // Warn if there are any duplicate file paths.
            if (oldSoundId != null)
                getLogger().warning("Both SFX ID " + oldSoundId + " & " + soundId + " share the path '" + soundPath + "'! This will cause issues trying to export all sounds at once!");
        }
    }

    private void loadSoundFolder() {
        this.soundChunkFile = null;
        File soundFolder = new File(getMainGameFolder(), "SOUND");
        if (!soundFolder.exists() || !soundFolder.isDirectory())
            return;

        File idxFile = null, sckFile = null;
        for (File sndFile : soundFolder.listFiles()) {
            String soundFileName = sndFile.getName().toLowerCase();

            GreatQuestGameFile gameFile;
            if (soundFileName.endsWith(".sbr")) {
                gameFile = new SBRFile(this, sndFile);
            } else if (soundFileName.endsWith(".idx")) {
                if (idxFile != null) {
                    getLogger().warning("There was more than one index file! '" + idxFile + "', '" + sndFile.getName() + "'");
                    continue;
                }

                if (sckFile != null) {
                    gameFile = this.soundChunkFile = new SoundChunkFile(this, sndFile, sckFile);
                } else {
                    idxFile = sndFile;
                    continue;
                }
            } else if (soundFileName.endsWith("sck")) {
                if (sckFile != null) {
                    getLogger().warning("There was more than sound chunk file! '" + sckFile + "', '" + sndFile.getName() + "'");
                    continue;
                }

                if (idxFile != null) {
                    gameFile = this.soundChunkFile = new SoundChunkFile(this, idxFile, sndFile);
                } else {
                    sckFile = sndFile;
                    continue;
                }
            } else { // Unrecognized file.
                if (sndFile.isFile())
                    getLogger().warning("Skipping unrecognized file in the sound folder: '" + sndFile.getName() + "'");
                continue;
            }

            try {
                DataReader reader = new DataReader(new FileSource(sndFile));
                this.looseFiles.add(gameFile);
                this.allFiles.add(gameFile);
                gameFile.load(reader);
            } catch (IOException ex) {
                Utils.handleError(getLogger(), ex, true, "Failed to load the file in the sound folder '%s'.", sndFile.getName());
            }
        }
    }

    @Override
    public GreatQuestMainMenuUIController getMainMenuController() {
        return (GreatQuestMainMenuUIController) super.getMainMenuController();
    }

    @Override
    protected GreatQuestMainMenuUIController makeMainMenuController() {
        return new GreatQuestMainMenuUIController(this);
    }

    @Override
    public File getMainGameFolder() {
        if (this.mainArchiveBinFile != null) {
            return this.mainArchiveBinFile.getParentFile();
        } else {
            throw new IllegalStateException("The folder is not known since the game has not been loaded yet.");
        }
    }

    /**
     * Gets the full sound file path for the given sound ID.
     * @param soundId the sound ID to resolve.
     * @return fullSoundPath, or the ID as a string if there is none.
     */
    public String getFullSoundPath(int soundId) {
        String soundPath = this.soundPathsById.get(soundId);
        if (soundPath != null)
            return soundPath;

        return String.valueOf(soundId);
    }

    /**
     * Gets the shorted sound file path for the given sound ID.
     * @param soundId the sound ID to resolve.
     * @param includeId if true, the sound id will be guaranteed to be included as part of the name
     * @return shortenedSoundPath
     */
    public String getShortenedSoundPath(int soundId, boolean includeId) {
        String soundPath = this.soundPathsById.get(soundId);
        if (soundPath == null)
            return Utils.padNumberString(soundId, 4);

        int lastSlashFound = soundPath.lastIndexOf('/');
        if (lastSlashFound >= 0) {
            int secondToLastSlashFound = soundPath.lastIndexOf('/', lastSlashFound - 1);
            if (secondToLastSlashFound >= 0)
                soundPath = soundPath.substring(secondToLastSlashFound + 1);
        }

        return (includeId ? "[" + Utils.padNumberString(soundId, 4) + "] " : "") + soundPath;
    }

    /**
     * Gets the sound file name for the given sound ID.
     * @param soundId the sound ID to resolve.
     * @param includeId if true, the sound id will be guaranteed to be included as part of the name
     * @return soundFileName
     */
    public String getSoundFileName(int soundId, boolean includeId) {
        String soundPath = this.soundPathsById.get(soundId);
        if (soundPath == null)
            return Utils.padNumberString(soundId, 4);

        int lastSlashFound = soundPath.lastIndexOf('/');
        if (lastSlashFound >= 0)
            return soundPath.substring(lastSlashFound + 1);

        return (includeId ? "[" + Utils.padNumberString(soundId, 4) + "] " : "") + soundPath;
    }
}