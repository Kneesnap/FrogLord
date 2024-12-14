package net.highwayfrogs.editor.games.konami.greatquest.generic;

import javafx.scene.control.ContextMenu;
import net.highwayfrogs.editor.games.generic.data.IGameData;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;

/**
 * Represents the data for a generic resource.
 * Created by Kneesnap on 10/26/2024.
 */
public interface kcIGenericResourceData extends IGameData {
    /**
     * Gets the generic resource which holds this data.
     * @return resource
     */
    kcCResourceGeneric getResource();

    /**
     * Gets the generic resource type.
     */
    kcCResourceGenericType getResourceType();

    /**
     * Gets the parent file holding the resource chunk.
     */
    default GreatQuestChunkedFile getParentFile() {
        kcCResource resource = getResource();
        return resource != null ? resource.getParentFile() : null;
    }

    /**
     * Called when the entry is double-clicked in the UI.
     */
    default void handleDoubleClick() {
        // Do nothing.
    }

    /**
     * Called to setup right-click context menu items.
     * @param contextMenu The context menu to add right-click items to.
     */
    default void setupRightClickMenuItems(ContextMenu contextMenu) {
        // Do nothing.
    }
}
