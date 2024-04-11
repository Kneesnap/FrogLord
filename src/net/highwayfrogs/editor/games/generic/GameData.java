package net.highwayfrogs.editor.games.generic;

import net.highwayfrogs.editor.utils.IBinarySerializable;

/**
 * Represents a game object which has the capability of saving / loading data.
 * @param <TGameInstance> The type of game instance this object corresponds to.
 * Created by Kneesnap on 4/10/2024.
 */
public abstract class GameData<TGameInstance extends GameInstance> extends GameObject<TGameInstance> implements IBinarySerializable {
    public GameData(TGameInstance instance) {
        super(instance);
    }

    /**
     * Represents GameData which can be used by any GameInstance.
     */
    public static abstract class GenericGameData extends GameData<GameInstance> {
        public GenericGameData(GameInstance instance) {
            super(instance);
        }
    }
}