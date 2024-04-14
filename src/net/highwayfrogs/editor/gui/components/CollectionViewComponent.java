package net.highwayfrogs.editor.gui.components;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;

/**
 * Allows viewing a list of entries.
 * TODO: Finish.
 * TODO: Right click -> Rename, Import Raw, Export Raw, Import .png, Export .png, Delete.
 * Created by Kneesnap on 4/12/2024.
 */
@Getter
public abstract class CollectionViewComponent<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends GameUIController<TGameInstance> {
    private TViewEntry selectedViewEntry;

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
     * Called when a view entry has been selected
     * @param viewEntry the view entry which was selected
     */
    protected abstract void onSelect(TViewEntry viewEntry);

    /**
     * Called when a view entry has been selected
     * @param viewEntry the view entry which was selected
     */
    public void setSelectedViewEntry(TViewEntry viewEntry) {
        this.selectedViewEntry = viewEntry;
        onSelect(viewEntry);
    }

    /**
     * Refreshes the list of displayed stuff.
     */
    public abstract void refreshDisplay();

    /**
     * Gets the view entries to be visible in the collection view
     */
    public abstract Iterable<TViewEntry> getViewEntries();

    public interface ICollectionViewEntry {
        /**
         * Gets a parent entry representing the category.
         */
        ICollectionViewEntry getCollectionViewParentEntry();

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
    }
}