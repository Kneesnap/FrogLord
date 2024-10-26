package net.highwayfrogs.editor.games.generic;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.TimeUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a unique game.
 * Provides a way to work with game types which are not currently loaded.
 * Created by Kneesnap on 4/10/2024.
 */
public interface IGameType {
    /**
     * Gets the display name of the game.
     */
    String getDisplayName();

    /**
     * Gets an identifier which can be used to identify the game uniquely.
     * Should be alphanumeric, since it may be used for a file name.
     */
    String getIdentifier();

    /**
     * Creates a new instance of the game.
     */
    GameInstance createGameInstance();

    /**
     * Loads the game instance with the provided config.
     * @param instance the game instance to setup from configuration
     * @param gameVersionConfigName the name of the version config file to use
     * @param gameSetupConfig the configuration data to load the game instance with
     * @param progressBar the progress bar to display load progress on (optional)
     */
    void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, ProgressBarComponent progressBar);

    /**
     * Creates a new config for this game type.
     * @param configName the name of the config to create
     */
    GameConfig createConfig(String configName);

    /**
     * Creates config UI to configure the load of a potential game instance.
     * @param controller the UI controller to create the UI under
     * @param config the config used to load data
     */
    GameConfigUIController setupConfigUI(GameConfigController controller, GameConfig gameConfig, Config config);

    /**
     * Gets an InputStream to files included for this specific game.
     * @param localPath the local path of the file to load
     * @return embeddedResourceStream
     */
    default InputStream getEmbeddedResourceStream(String localPath) {
        return FileUtils.getResourceStream("games/" + getIdentifier() + "/" + localPath);
    }

    /**
     * Gets a URL to files included for this specific game.
     * @param localPath the local path of the file to load
     * @return embeddedResourceURL
     */
    default URL getEmbeddedResourceURL(String localPath) {
        return FileUtils.getResourceURL("games/" + getIdentifier() + "/" + localPath);
    }

    /**
     * Gets a list of version configs available for this game type.
     */
    default List<GameConfig> getVersionConfigs() {
        List<GameConfig> versionConfigs = Constants.getCachedConfigsByGameType().get(this);
        if (versionConfigs != null)
            return versionConfigs;

        // Load configs.
        versionConfigs = new ArrayList<>();
        Logger logger = Logger.getLogger(getClass().getSimpleName());
        List<URL> versionConfigFiles = FileUtils.getInternalResourceFilesInDirectory(getEmbeddedResourceURL("versions"), true);
        if (versionConfigFiles.isEmpty())
            logger.severe("Did not find any version configs for the game type " + getIdentifier() + "/'" + getDisplayName() + "'. This seems like a bug.");

        for (URL url : versionConfigFiles) {
            String versionConfigName = FileUtils.getFileNameWithoutExtension(url);

            net.highwayfrogs.editor.file.config.Config versionConfig;
            try {
                versionConfig = new net.highwayfrogs.editor.file.config.Config(url.openStream(), versionConfigName);
            } catch (IOException ex) {
                Utils.handleError(logger, ex, false, "Failed to load configuration file '%s'. (%s)", url, versionConfigName);
                continue;
            }

            // Loads the config.
            GameConfig newConfig = createConfig(versionConfigName);
            try {
                newConfig.loadData(versionConfig, this);
            } catch (Throwable th) {
                Utils.handleError(logger, th, false, "Failed to parse configuration data in file '%s'.", url);
                continue;
            }

            versionConfigs.add(newConfig);
        }

        versionConfigs.sort((o1, o2) -> TimeUtils.compare(o1.getBuildTime(), o2.getBuildTime()));

        // Store configs in cache.
        Constants.getCachedConfigsByGameType().put(this, versionConfigs);
        return versionConfigs;
    }
}