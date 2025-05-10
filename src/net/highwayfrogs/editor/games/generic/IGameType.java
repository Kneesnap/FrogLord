package net.highwayfrogs.editor.games.generic;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.TimeUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
     * @param instanceConfig the config which the instance stores its user-configuration data within
     * @param progressBar the progress bar to display load progress on (optional)
     */
    void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, Config instanceConfig, ProgressBarComponent progressBar);

    /**
     * Creates a new config for this game type.
     * @param configName the name of the config to create
     */
    GameConfig createConfig(String configName);

    /**
     * Creates config UI to configure the load of a potential game instance.
     * @param controller the UI controller to create the UI under
     */
    GameConfigUIController<?> setupConfigUI(GameConfigController controller);

    /**
     * Gets the FrogLord logo associated with this game type.
     */
    default ImageResource getFrogLordLogo() {
        return ImageResource.FROGLORD_LOGO_MAIN_LARGE;
    }

    /**
     * Gets an InputStream to files included for this specific game.
     * @param localPath the local path of the file to load
     * @return embeddedResourceStream
     */
    default String getEmbeddedResourcePath(String localPath) {
        return "games/" + getIdentifier() + "/" + localPath;
    }

    /**
     * Gets an InputStream to files included for this specific game.
     * @param localPath the local path of the file to load
     * @return embeddedResourceStream
     */
    default InputStream getEmbeddedResourceStream(String localPath) {
        return FileUtils.getResourceStream(getEmbeddedResourcePath(localPath));
    }

    /**
     * Gets a URL to files included for this specific game.
     * @param localPath the local path of the file to load
     * @return embeddedResourceURL
     */
    default URL getEmbeddedResourceURL(String localPath) {
        return FileUtils.getResourceURL(getEmbeddedResourcePath(localPath));
    }

    /**
     * Load a config file from a resource path.
     * @param localPath the local resource path to load the config from
     * @return loadedConfig
     */
    default Config loadConfigFromEmbeddedResourcePath(String localPath, boolean allowNull) {
        String embeddedResourcePath = getEmbeddedResourcePath(localPath);
        InputStream inputStream = getEmbeddedResourceStream(localPath);
        if (inputStream == null) {
            if (allowNull)
                return null;

            throw new IllegalArgumentException("Local resource path '" + localPath + "' (" + embeddedResourcePath + ") could not be resolved to a config file.");
        }

        try {
            return Config.loadTextConfigFromInputStream(inputStream, embeddedResourcePath);
        } catch (Throwable th) {
            throw new RuntimeException("Failed to read config file resource '" + embeddedResourcePath + "'.", th);
        }
    }

    /**
     * Resolve a version config by its name.
     * @param internalName the name to resolve
     * @return gameConfig or null
     */
    default GameConfig getVersionConfigByName(String internalName) {
        if (internalName == null)
            return null;

        List<GameConfig> versionConfigs = getVersionConfigs();
        for (int i = 0; i < versionConfigs.size(); i++) {
            GameConfig testGameConfig = versionConfigs.get(i);
            if (internalName.equals(testGameConfig.getInternalName()))
                return testGameConfig;
        }

        return null;
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
        ILogger logger = ClassNameLogger.getLogger(null, getClass());
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