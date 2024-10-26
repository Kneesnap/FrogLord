package net.highwayfrogs.editor.games.generic;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.gui.MainMenuController;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents the definition of a game instance.
 * Created by Kneesnap on 8/12/2024.
 */
public interface IGameInstance {
    /**
     * Gets the game type represented by the instance.
     */
    IGameType getGameType();

    /**
     * Gets the configuration used by the instance.
     */
    GameConfig getVersionConfig();

    /**
     * Gets the main menu controller currently active for the instance.
     */
    MainMenuController<?, ?> getMainMenuController();

    /**
     * Adds a console log entry to the console window.
     * @param message the message to add.
     */
    void addConsoleLogEntry(String message);

    /**
     * Gets and clears the logging cache for the main menu UI.
     */
    String getAndClearQueuedLogMessages();

    /**
     * Sets up the main menu controller for this game instance
     */
    MainMenuController<?, ?> setupMainMenuWindow();

    /**
     * Gets the logger for this game instance.
     */
    Logger getLogger();

    /**
     * Gets the main menu stage available for this game instance.
     */
    Stage getMainStage();

    /**
     * Gets the folder where the main game files are located.
     * @return gameFolder
     */
    File getMainGameFolder();

    /**
     * Loads the game configuration from the provided config.
     * @param configName the name of the configuration game data is loaded from
     * @param config the config object to load data from
     * @param instanceConfig the config containing configuration for this specific instance
     */
    void loadGameConfig(String configName, Config config, net.highwayfrogs.editor.system.Config instanceConfig);

    /**
     * Get the target platform this game version runs on.
     */
    default GamePlatform getPlatform() {
        GameConfig config = getVersionConfig();
        return config != null ? config.getPlatform() : null;
    }

    /**
     * Test if this is a game version intended for Windows.
     * @return isPCRelease
     */
    default boolean isPC() {
        return getPlatform() == GamePlatform.WINDOWS;
    }

    /**
     * Test if this is a game version intended for the PlayStation.
     * @return isPSXRelease
     */
    default boolean isPSX() {
        return getPlatform() == GamePlatform.PLAYSTATION;
    }

    /**
     * Test if this is a game version intended for the PlayStation 2.
     * @return isPS2Release
     */
    default boolean isPS2() {
        return getPlatform() == GamePlatform.PLAYSTATION_2;
    }

    /**
     * Tests if a given unsigned 32-bit number passed as a long looks like a valid pointer to memory present in the executable.
     * @param testPointer The pointer to test.
     * @return If it looks good or not.
     */
    default boolean isValidLookingPointer(long testPointer) {
        return GameUtils.isValidLookingPointer(getPlatform(), testPointer);
    }

    /**
     * Gets the FXMLLoader by its name.
     * @param template The template name.
     * @return loader
     */
    default URL getFXMLTemplateURL(String template) {
        return getGameType().getEmbeddedResourceURL("fxml/" + template + ".fxml");
    }

    /**
     * Gets the fxml template URL by its name.
     * @param template The template name.
     * @return fxmlTemplateUrl
     */
    default FXMLLoader getFXMLTemplateLoader(String template) {
        Map<String, FXMLLoader> resourcePaths = GameInstance.knownResourcePaths.computeIfAbsent(getGameType(), key -> new HashMap<>());
        FXMLLoader fxmlLoader = resourcePaths.get(template);
        if (!resourcePaths.containsKey(template)) {
            URL url = getFXMLTemplateURL(template);
            if (url != null)
                fxmlLoader = new FXMLLoader(url);
            resourcePaths.put(template, fxmlLoader);
        }

        return fxmlLoader;
    }

}