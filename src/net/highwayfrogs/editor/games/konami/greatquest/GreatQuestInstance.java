package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestMainMenuUIController;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Represents an instance of 'Frogger: The Great Quest'.
 * Created by Kneesnap on 4/13/2024.
 */
@Getter
public class GreatQuestInstance extends GameInstance {
    private GreatQuestAssetBinFile mainArchive;
    private File mainArchiveBinFile;

    public static final float JUMP_SLOPE_THRESHOLD = .8F;

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

        // Load the main file.
        try {
            DataReader reader = new DataReader(new FileSource(binFile));
            this.mainArchive = new GreatQuestAssetBinFile(this);
            this.mainArchive.load(reader, progressBar);
        } catch (IOException ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to load the bin file.");
        }
    }

    @Override
    protected MainMenuController<?, ?> makeMainMenuController() {
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