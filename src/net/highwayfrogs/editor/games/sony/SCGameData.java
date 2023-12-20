package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.utils.IBinarySerializable;

/**
 * Represents a game object which has the capability of saving / loading data.
 * @param <TGameInstance> The type of game instance this object corresponds to.
 * Created by Kneesnap on 9/8/2023.
 */
public abstract class SCGameData<TGameInstance extends SCGameInstance> extends SCGameObject<TGameInstance> implements IBinarySerializable {
    public SCGameData(TGameInstance instance) {
        super(instance);
    }

    /**
     * Represents an SCGameData which can be used by any SCGameInstance.
     */
    public static abstract class SCSharedGameData extends SCGameData<SCGameInstance> {
        public SCSharedGameData(SCGameInstance instance) {
            super(instance);
        }
    }
}