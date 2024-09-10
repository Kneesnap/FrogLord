package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SoundChunkFile;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestMainMenuUIController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public static final float JUMP_SLOPE_THRESHOLD = .8F;

    // Padding data.
    public static final byte PADDING_BYTE_DEFAULT = (byte) 0xCC;
    private static final byte[] PADDING_DEFAULT_INT_BYTES = {PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT};
    public static final int PADDING_DEFAULT_INT = Utils.readIntFromBytes(PADDING_DEFAULT_INT_BYTES, 0);

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
}