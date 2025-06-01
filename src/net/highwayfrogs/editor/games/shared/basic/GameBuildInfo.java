package net.highwayfrogs.editor.games.shared.basic;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.system.Config;

/**
 * Contains configuration data about modded files. This can be embedded in different ways, and is up to each individual game implementation if we'd like to use this, and how.
 * Created by Kneesnap on 5/4/2025.
 */
@Getter
public class GameBuildInfo<TGameInstance extends GameInstance> {
    private String gameType;
    private String gameVersion;
    private int frogLordVersion;

    public static final String CONFIG_KEY_ROOT_NAME = "BuildInfo";
    private static final String CONFIG_KEY_GAME_TYPE = "gameType";
    private static final String CONFIG_KEY_GAME_VERSION = "gameVersion";
    private static final String CONFIG_KEY_FROGLORD_VERSION = "frogLordVersion";

    public GameBuildInfo(@NonNull TGameInstance gameInstance) {
        applyFromGameInstance(gameInstance);
    }

    public GameBuildInfo(@NonNull Config config) {
        fromConfig(config);
    }

    /**
     * Applies the data from a given game instance to this object.
     * @param gameInstance the game instance to apply data from
     */
    public void applyFromGameInstance(TGameInstance gameInstance) {
        if (gameInstance == null)
            throw new NullPointerException("gameInstance");

        this.gameType = gameInstance.getGameType().getIdentifier();
        this.gameVersion = gameInstance.getVersionConfig().getInternalName();
        this.frogLordVersion = Constants.UPDATE_VERSION;
    }

    /**
     * Loads the data tracked in this object from a config object
     * @param config the config object to read from
     */
    public void fromConfig(Config config) {
        if (config == null)
            throw new NullPointerException("config");

        this.gameType = config.getKeyValueNodeOrError(CONFIG_KEY_GAME_TYPE).getAsString();
        this.gameVersion = config.getKeyValueNodeOrError(CONFIG_KEY_GAME_VERSION).getAsString();
        this.frogLordVersion = config.getKeyValueNodeOrError(CONFIG_KEY_FROGLORD_VERSION).getAsInteger();
    }

    /**
     * Gets the GameBuildInfo as a config object.
     * @return configObject
     */
    public Config toConfig() {
        Config newConfigNode = new Config(CONFIG_KEY_ROOT_NAME);
        newConfigNode.getOrCreateKeyValueNode(CONFIG_KEY_GAME_TYPE).setAsString(this.gameType);
        newConfigNode.getOrCreateKeyValueNode(CONFIG_KEY_GAME_VERSION).setAsString(this.gameVersion);
        newConfigNode.getOrCreateKeyValueNode(CONFIG_KEY_FROGLORD_VERSION).setAsInteger(this.frogLordVersion);
        return newConfigNode;
    }
}
