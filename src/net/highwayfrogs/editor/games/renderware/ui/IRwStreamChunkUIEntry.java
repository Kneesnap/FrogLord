package net.highwayfrogs.editor.games.renderware.ui;

import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.utils.IGameObject;

import java.util.List;

/**
 * Represents a target for display in the RwStream tree user interface.
 * Created by Kneesnap on 8/14/2024.
 */
public interface IRwStreamChunkUIEntry extends IGameObject, IPropertyListCreator, ICollectionViewEntry {
    /**
     * Gets child chunks (child tree nodes).
     */
    List<? extends IRwStreamChunkUIEntry> getChildUISections();

    /**
     * Gets the stream file holding this chunk.
     */
    RwStreamFile getStreamFile();

    /**
     * Create any special UI to go along with this chunk.
     */
    GameUIController<?> makeEditorUI();
}