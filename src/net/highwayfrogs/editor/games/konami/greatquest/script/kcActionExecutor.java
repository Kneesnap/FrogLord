package net.highwayfrogs.editor.games.konami.greatquest.script;

import net.highwayfrogs.editor.games.generic.data.IGameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;

/**
 * Represents an object capable of holding kcAction objects.
 * Created by Kneesnap on 10/31/2024.
 */
public interface kcActionExecutor extends IGameObject {
    /**
     * Gets the game instance which the game object belongs to.
     */
    GreatQuestInstance getGameInstance();

    /**
     * Gets the chunked file which holds this action.
     */
    GreatQuestChunkedFile getChunkedFile();

    /**
     * Gets the description of the executing entities, if known.
     */
    kcEntity3DDesc getExecutingEntityDescription();

    /**
     * Gets the description of the executing entities, if known.
     */
    default kcActorBaseDesc getExecutingActorBaseDescription() {
        kcEntity3DDesc entity3DDesc = getExecutingEntityDescription();
        return (entity3DDesc instanceof kcActorBaseDesc) ? (kcActorBaseDesc) entity3DDesc : null;
    }
}
