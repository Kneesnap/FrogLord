package net.highwayfrogs.editor.games.generic.data;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.utils.Utils;

import java.util.logging.Logger;

/**
 * Represents an object used by a game supported by FrogLord.
 * @param <TGameInstance> The type of game instance this object corresponds to.
 * Created by Kneesnap on 4/10/2024.
 */
@Getter
public abstract class GameObject<TGameInstance extends GameInstance> implements IGameObject {
    private final TGameInstance gameInstance;

    public GameObject(TGameInstance instance) {
        this.gameInstance = instance;
    }

    /**
     * Gets the game version config.
     */
    public GameConfig getConfig() {
        GameInstance instance = getGameInstance();
        return instance != null ? instance.getConfig() : null;
    }

    /**
     * Gets the logger for this class type.
     */
    public Logger getLogger() {
        return Logger.getLogger(Utils.getSimpleName(this));
    }

    /**
     * Represents a GameObject which can be used by any GameInstance.
     */
    public static abstract class SharedGameObject extends GameObject<GameInstance> {
        public SharedGameObject(GameInstance instance) {
            super(instance);
        }
    }
}