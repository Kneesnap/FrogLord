package net.highwayfrogs.editor.gui.components;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.Collection;
import java.util.Objects;

/**
 * Allows viewing a list of entries.
 * Created by Kneesnap on 4/12/2024.
 */
@Getter
public abstract class CollectionViewComponent<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends GameUIController<TGameInstance> {
    private TViewEntry selectedViewEntry;
    private String searchQuery;

    public CollectionViewComponent(TGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        refreshDisplay();
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        refreshDisplay();
    }

    /**
     * Sets the currently active search query.
     * @param searchQuery the search query to update
     */
    public void setSearchQuery(String searchQuery) {
        if (Objects.equals(this.searchQuery, searchQuery))
            return;

        this.searchQuery = searchQuery;
        refreshDisplay();
    }

    /**
     * Called when a view entry has been selected
     * @param viewEntry the view entry which was selected
     */
    protected abstract void onSelect(TViewEntry viewEntry);

    /**
     * Called when a view entry has been selected
     * @param viewEntry the view entry which was selected
     */
    protected abstract void onDoubleClick(TViewEntry viewEntry);

    /**
     * Called when a view entry has been selected
     * @param viewEntry the view entry which was selected
     */
    public void setSelectedViewEntry(TViewEntry viewEntry) {
        TViewEntry oldEntry = this.selectedViewEntry;
        this.selectedViewEntry = viewEntry;
        if (!Objects.equals(oldEntry, viewEntry))
            onSelect(viewEntry);
    }

    /**
     * Sets the selected view entry in the UI.
     * @param viewEntry The view entry to select
     */
    public abstract void setSelectedViewEntryInUI(TViewEntry viewEntry);

    /**
     * Test if the viewEntry matches the search query
     * @param viewEntry the view query to test
     * @return true if the search query is matched
     */
    public final boolean matchesSearchQuery(TViewEntry viewEntry) {
        return matchesSearchQuery(viewEntry, getSearchQuery());
    }

    /**
     * Test if the viewEntry matches the search query
     * @param viewEntry the view query to test
     * @param searchQuery the search query to test
     * @return true if the search query is matched
     */
    public boolean matchesSearchQuery(TViewEntry viewEntry, String searchQuery) {
        if (searchQuery == null || searchQuery.isEmpty())
            return true;

        String viewEntryDisplayName = viewEntry != null ? viewEntry.getCollectionViewDisplayName() : null;
        return viewEntryDisplayName != null && StringUtils.containsIgnoreCase(viewEntryDisplayName, searchQuery);
    }

    /**
     * Refreshes the list of displayed stuff.
     */
    public abstract void refreshDisplay();

    /**
     * Gets the view entries to be visible in the collection view
     */
    public abstract Collection<? extends TViewEntry> getViewEntries();

    public interface ICollectionViewEntry {
        /**
         * Returns the name to display in the viewer.
         */
        String getCollectionViewDisplayName();

        /**
         * Gets any style specification for how the display should look.
         */
        String getCollectionViewDisplayStyle();

        /**
         * Gets the icon (if there is one) to display next to the entry name.
         */
        Image getCollectionViewIcon();

        /**
         * Sets up right-click menu items when clicked.
         * @param contextMenu the menu to add the right-click menu items to.
         */
        default void setupRightClickMenuItems(ContextMenu contextMenu) {
            // Setup nothing by default.
        }
    }
}