package net.highwayfrogs.editor.games.generic;

import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.logging.Logger;

/**
 * Represents an object used by a game supported by FrogLord.
 * @param <TGameInstance> The type of game instance this object corresponds to.
 * Created by Kneesnap on 4/10/2024.
 */
@Getter
public abstract class GameObject<TGameInstance extends GameInstance> {
    private final TGameInstance gameInstance;

    public GameObject(TGameInstance instance) {
        this.gameInstance = instance;
    }

    /**
     * Gets the game version config.
     */
    public GameConfig getConfig() {
        return getGameInstance().getConfig();
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
    public static abstract class GenericGameObject extends GameObject<GameInstance> {
        public GenericGameObject(GameInstance instance) {
            super(instance);
        }
    }
}