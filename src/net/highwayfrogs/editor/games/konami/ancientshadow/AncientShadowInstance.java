package net.highwayfrogs.editor.games.konami.ancientshadow;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.konami.ancientshadow.ui.AncientShadowMainMenuUIController;
import net.highwayfrogs.editor.games.konami.hudson.HudsonFileUserFSDefinition;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Represents an instance of Frogger Ancient Shadow.
 * Created by Kneesnap on 8/4/2024.
 */
@Getter
public class AncientShadowInstance extends GameInstance {
    private HFSFile mainHfs;
    private File mainHfsFile;

    public AncientShadowInstance() {
        super(AncientShadowGameType.INSTANCE);
    }

    /**
     * Load and setup all instance data relating to the game such as version configuration and game files.
     * @param gameVersionConfigName The name of the version configuration file to load.
     * @param mainFile the main archive file to read
     * @param progressBar the progress bar to display load progress on, if it exists
     */
    public void loadGame(String gameVersionConfigName, File mainFile, ProgressBarComponent progressBar) {
        if (this.mainHfs != null)
            throw new RuntimeException("The game instance has already been loaded.");

        if (mainFile == null || !mainFile.exists())
            throw new RuntimeException("The main archive file '" + mainFile + "' does not exist.");

        this.mainHfsFile = mainFile;
        loadGameConfig(gameVersionConfigName);

        // Load the main file.
        try {
            DataReader reader = new DataReader(new FileSource(mainFile));
            this.mainHfs = new HFSFile(new HudsonFileUserFSDefinition(this, mainFile));
            this.mainHfs.load(reader, progressBar);
        } catch (IOException ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to load the bin file.");
        }
    }

    @Override
    protected MainMenuController<?, ?> makeMainMenuController() {
        return new AncientShadowMainMenuUIController(this);
    }

    @Override
    public File getMainGameFolder() {
        if (this.mainHfsFile != null) {
            return this.mainHfsFile.getParentFile();
        } else {
            throw new IllegalStateException("The folder is not known since the game has not been loaded yet.");
        }
    }

    /**
     * Gets all game files tracked for the game instance.
     */
    public List<AncientShadowGameFile> getAllGameFiles() {
        return this.mainHfs.getGameFiles();
    }
}