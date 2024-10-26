package net.highwayfrogs.editor.games.generic;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents an instance of a game. For example, a folder containing the files for a single version of a game.
 * TODO: UI Plans?
 *  - Standardize 3D UI.
 *  TODO: I think there's some kind of caching bug with shading. It happened on "Time Device", where none of the shading in the world was right. Then, after I toggled shading off/on, it was fine. I suspect there's probably some tracking issue then.
 * Created by Kneesnap on 4/10/2024.
 */
public abstract class GameInstance implements IGameInstance {
    @Getter private final IGameType gameType;
    @Getter private NoodleScriptEngine scriptEngine;
    @Getter private GameConfig config;
    @Getter private MainMenuController<?, ?> mainMenuController;
    private Logger cachedLogger;
    private StringBuilder cachedLogging;

    static final Map<IGameType, Map<String, FXMLLoader>> knownResourcePaths = new HashMap<>();

    public GameInstance(IGameType gameType) {
        if (gameType == null)
            throw new NullPointerException("gameType");

        this.gameType = gameType;
    }

    /**
     * Adds a console log entry to the console window.
     * @param message the message to add.
     */
    public void addConsoleLogEntry(String message) {
        if (this.mainMenuController != null) {
            this.mainMenuController.addConsoleEntry(message);
        } else {
            if (this.cachedLogging == null)
                this.cachedLogging = new StringBuilder();

            this.cachedLogging.append(message).append(System.lineSeparator());
        }
    }

    /**
     * Gets and clears the logging cache for the main menu UI.
     */
    public String getAndClearQueuedLogMessages() {
        if (this.cachedLogging != null) {
            String cachedLogging = this.cachedLogging.toString();
            this.cachedLogging = null;
            return cachedLogging;
        }

        return null;
    }

    /**
     * Sets up the main menu controller for this game instance
     */
    public MainMenuController<?, ?> setupMainMenuWindow() {
        if (this.mainMenuController != null)
            throw new IllegalStateException("The main menu controller already exists for this game instance.");

        this.mainMenuController = makeMainMenuController();
        if (this.mainMenuController != null) {
            String versionName = (this.config.getDisplayName() != null ? this.config.getDisplayName() : this.config.getInternalName());
            GameUIController.loadController(this, MainMenuController.MAIN_MENU_FXML_TEMPLATE_LOADER, this.mainMenuController);
            Stage stage = GameUIController.openWindow(this.mainMenuController, "FrogLord " + Constants.VERSION + " -- " + this.gameType.getDisplayName() + " " + versionName, false);
            stage.setResizable(true);
        }

        return this.mainMenuController;
    }

    /**
     * Creates a new main menu controller.
     */
    protected abstract MainMenuController<?, ?> makeMainMenuController();

    /**
     * Gets the logger for this game instance.
     */
    public Logger getLogger() {
        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = Logger.getLogger(Utils.getSimpleName(this));
    }

    /**
     * Gets the main menu stage available for this game instance.
     */
    public Stage getMainStage() {
        Stage stage = this.mainMenuController != null ? this.mainMenuController.getStage() : null;
        if (stage == null) {
            FXUtils.makePopUp("There was no stage available to override.", AlertType.ERROR);
            return null;
        }

        return stage;
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

        // Register to GUIMain and log.
        GUIMain.getActiveGameInstances().add(this);
        getLogger().info("Hello! FrogLord is loading config '" + configName + "'.");

        this.scriptEngine = new NoodleScriptEngine(this, configName + "@" + Utils.getSimpleName(this));

        // Create & load config.
        this.config = this.gameType.createConfig(configName);
        this.config.loadData(config, this.gameType);
        this.onConfigLoad(config);

        // Setup script engine.
        setupScriptEngine(this.scriptEngine);
        this.scriptEngine.seal();
    }

    /**
     * Sets up the script engine for use with this game instance.
     */
    protected void setupScriptEngine(NoodleScriptEngine engine) {
        engine.addWrapperTemplates(GameUtils.class, GameConfig.class);
    }

    /**
     * Load and setup game config data relating to the game such as version configuration and game files.
     * @param gameVersionConfigName the name of the version config file to load
     */
    protected void loadGameConfig(String gameVersionConfigName) {
        if (this.config != null)
            throw new IllegalStateException("Cannot load the game configuration '" + gameVersionConfigName + "' because it has already been loaded.");

        // Load config.
        net.highwayfrogs.editor.file.config.Config gameConfig = new net.highwayfrogs.editor.file.config.Config(this.gameType.getEmbeddedResourceStream("versions/" + gameVersionConfigName + ".cfg"));
        loadGameConfig(gameVersionConfigName, gameConfig);
    }

    /**
     * Load and setup game config data relating to the game such as version configuration and game files.
     * @param config the already loaded config file
     */
    protected void loadGameConfig(GameConfig config) {
        if (config == null)
            throw new NullPointerException("config");
        if (this.config != null)
            throw new IllegalStateException("Cannot load the game configuration '" + config.getInternalName() + "' because it has already been loaded.");

        // Register to GUIMain and log.
        GUIMain.getActiveGameInstances().add(this);
        getLogger().info("Hello! FrogLord is loading config '" + config.getInternalName() + "'.");

        // Create & load config.
        this.config = config;
        this.onConfigLoad(config.getConfig());
    }

    /**
     * Called when configuration data is loaded.
     * @param configObj The config object which data is loaded from.
     */
    protected void onConfigLoad(Config configObj) {
        // Does nothing by default.
    }
}