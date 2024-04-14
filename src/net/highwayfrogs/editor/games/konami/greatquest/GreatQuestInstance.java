package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestMainMenuUIController;
import net.highwayfrogs.editor.gui.MainMenuController;

import java.io.File;

/**
 * Represents an instance of 'Frogger: The Great Quest'.
 * Created by Kneesnap on 4/13/2024.
 */
@Getter
public class GreatQuestInstance extends GameInstance {
    private GreatQuestAssetBinFile mainArchive;
    private File mainArchiveBinFile;

    public GreatQuestInstance() {
        super(GreatQuestGameType.INSTANCE);
    }

    /**
     * Load and setup all instance data relating to the game such as version configuration and game files.
     * @param configName The name of the version configuration to load.
     * @param config The config to load.
     * @param binFile the main archive file to read
     */
    public void loadGame(String configName, Config config, File binFile) {
        if (getConfig() != null || this.mainArchive != null)
            throw new RuntimeException("The game instance has already been loaded.");

        if (binFile == null || !binFile.exists())
            throw new RuntimeException("The main archive file '" + binFile + "' does not exist.");

        this.mainArchiveBinFile = binFile;
        loadGameConfig(configName, config);
        this.mainArchive = new GreatQuestAssetBinFile(this);
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

    @Override
    protected GreatQuestConfig makeConfig(String internalName) {
        return new GreatQuestConfig(internalName);
    }
}