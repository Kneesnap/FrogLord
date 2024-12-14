package net.highwayfrogs.editor.games.sony;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;

/**
 * Represents an object used by a Sony Cambridge / Millennium Interactive game.
 * @param <TGameInstance> The type of game instance this object corresponds to.
 * Created by Kneesnap on 9/8/2023.
 */
@Getter
public abstract class SCGameObject<TGameInstance extends SCGameInstance> extends GameObject<TGameInstance> {
    public SCGameObject(TGameInstance instance) {
        super(instance);
    }

    /**
     * Get the file archive associated with the game instance.
     * @return fileArchive
     */
    public MWDFile getArchive() {
        return getGameInstance() != null ? getGameInstance().getMainArchive() : null;
    }

    @Override
    public SCGameConfig getConfig() {
        return getGameInstance().getVersionConfig();
    }

    /**
     * Represents an SCGameObject which can be used by any SCGameInstance.
     */
    public static abstract class SCSharedGameObject extends SCGameObject<SCGameInstance> {
        public SCSharedGameObject(SCGameInstance instance) {
            super(instance);
        }
    }
}