package net.highwayfrogs.editor.games.generic;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Represents an instance of a game. For example, a folder containing the files for a single version of a game.
 * TODO: UI Plans?
 *  - 1) Standard UI for opening games.
 *  - 2) Standardize Main UI Layout. (But let games optionally roll their own system) -> Also, change who's in charge of UI.
 *  - 3) Standardize 3D UI.
 * Created by Kneesnap on 4/10/2024.
 */
public abstract class GameInstance {
    @Getter private final IGameType gameType;
    @Getter private GameConfig config;
    private Logger cachedLogger;

    public GameInstance(IGameType gameType) {
        if (gameType == null)
            throw new NullPointerException("gameType");

        this.gameType = gameType;
    }

    /**
     * Gets the logger for this game instance.
     */
    public Logger getLogger() {
        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = Logger.getLogger(Utils.getSimpleName(this));
    }

    /**
     * Gets the folder where the main game files are located.
     * @return gameFolder
     */
    public abstract File getMainGameFolder();

    /**
     * Loads the game configuration from the provided config.
     * @param configName the name of the configuration game data is loaded from
     * @param config the config object to load data from
     */
    public void loadGameConfig(String configName, Config config) {
        if (this.config != null)
            throw new IllegalStateException("Cannot load the game configuration '" + configName + "' because it has already been loaded.");

        this.config = makeConfig(configName);
        this.config.loadData(config, this.gameType);
        this.onConfigLoad(config);
    }

    /**
     * Called when configuration data is loaded.
     * @param configObj The config object which data is loaded from.
     */
    protected void onConfigLoad(Config configObj) {
        // Does nothing by default.
    }

    /**
     * Makes a new game config instance for this game.
     */
    protected abstract GameConfig makeConfig(String internalName);

    /**
     * Get the target platform this game version runs on.
     */
    public GamePlatform getPlatform() {
        return this.config != null ? this.config.getPlatform() : null;
    }

    /**
     * Test if this is a game version intended for Windows.
     * @return isPCRelease
     */
    public boolean isPC() {
        return getPlatform() == GamePlatform.WINDOWS;
    }

    /**
     * Test if this is a game version intended for the PlayStation.
     * @return isPSXRelease
     */
    public boolean isPSX() {
        return getPlatform() == GamePlatform.PLAYSTATION;
    }

    /**
     * Tests if a given unsigned 32-bit number passed as a long looks like a valid pointer to memory present in the executable.
     * @param testPointer The pointer to test.
     * @return If it looks good or not.
     */
    public boolean isValidLookingPointer(long testPointer) {
        return GameUtils.isValidLookingPointer(getPlatform(), testPointer);
    }

    /**
     * Gets the FXMLLoader by its name.
     * @param template The template name.
     * @return loader
     */
    public URL getFXMLTemplateURL(String template) {
        return this.gameType.getEmbeddedResourceURL("fxml/" + template + ".fxml");
    }
}