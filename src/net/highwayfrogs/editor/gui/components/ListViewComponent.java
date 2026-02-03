package net.highwayfrogs.editor.gui.components;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Implements a CollectionView using a basic ListView.
 * This assumes that the provided List contains no duplicate entries. Null entries will be skipped/ignored.
 * The order of the ListView is consistent with the order of the underlying collection.
 * Created by Kneesnap on 9/10/2024.
 */
@Getter
public abstract class ListViewComponent<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends CollectionViewComponent<TGameInstance, TViewEntry> {
    private final ObservableList<TViewEntry> entries = FXCollections.observableArrayList();

    public ListViewComponent(TGameInstance instance) {
        super(instance);
        loadController(createListView());
    }

    @Override
    public abstract List<? extends TViewEntry> getViewEntries();

    /**
     * Gets the view entry list as a strictly-typed list.
     */
    @SuppressWarnings("unchecked")
    public List<TViewEntry> getStrictlyTypedList() {
        return (List<TViewEntry>) getViewEntries();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ListView<TViewEntry> getRootNode() {
        return (ListView<TViewEntry>) super.getRootNode();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        setAnchorPaneStretch(rootNode);
    }

    @Override
    public void refreshDisplay() {
        TViewEntry oldSelectedEntry = getSelectedViewEntry();

        // Update the list while also maintaining consistent ordering.
        // This implementation is very simple, but more than performant enough.
        boolean containsOldSelectedEntry = false;
        Collection<? extends TViewEntry> viewEntries = getViewEntries();
        List<TViewEntry> activeViewEntries = new ArrayList<>();
        for (TViewEntry viewEntry : viewEntries) {
            if (viewEntry != null && matchesSearchQuery(viewEntry)) {
                activeViewEntries.add(viewEntry);
                if (oldSelectedEntry == viewEntry)
                    containsOldSelectedEntry = true;
            }
        }

        if (containsOldSelectedEntry) {
            setSelectedViewEntryInUI(oldSelectedEntry);
        } else {
            getRootNode().getSelectionModel().clearSelection();
        }

        this.entries.setAll(activeViewEntries); // Do this regardless of if there's a change in values, as to update the icons/display names/etc.
        boolean didChangeOccur = !activeViewEntries.equals(this.entries);
        if (didChangeOccur)
            getRootNode().requestFocus();

        // Re-select the old value.
        if (oldSelectedEntry != null)
            setSelectedViewEntryInUI(oldSelectedEntry);
    }

    @Override
    public void setSelectedViewEntryInUI(TViewEntry viewEntry) {
        getRootNode().getSelectionModel().select(viewEntry);
        getRootNode().scrollTo(viewEntry);
    }

    /**
     * Removes the view entry from the underlying collection, if possible and supported.
     * Throws exceptions if unsupported, so call isAddOperationSupported() first.
     * @param viewEntry the entry to remove
     */
    @SuppressWarnings("unchecked")
    public <TUnsafeEntry extends ICollectionViewEntry> void addViewEntry(@NonNull TUnsafeEntry viewEntry, int insertionOffset) {
        addViewEntry(getSelectedViewEntry(), (TViewEntry) viewEntry, insertionOffset);
    }

    /**
     * Removes the view entry from the underlying collection, if possible and supported.
     * Throws exceptions if unsupported, so call isAddOperationSupported() first.
     * @param selectedEntry the entry currently selected, which the target entry is added in relation to. Can be null.
     * @param viewEntry the entry to remove
     */
    public void addViewEntry(TViewEntry selectedEntry, @NonNull TViewEntry viewEntry, int insertionOffset) {
        List<TViewEntry> list = getStrictlyTypedList();
        if (selectedEntry == null) {
            int insertionIndex = this.entries.size();
            list.add(viewEntry);
            this.entries.add(viewEntry);
            getRootNode().getSelectionModel().select(insertionIndex);
            return;
        }

        int existingValueIndex = list.indexOf(selectedEntry);
        if (existingValueIndex < 0)
            throw new IllegalStateException("The selectedEntry '" + selectedEntry + "' was not found as part of the viewEntryList. So, the newEntry cannot be added in relation to it.");

        if (list.contains(viewEntry))
            throw new IllegalArgumentException("The viewEntry '" + viewEntry + "' is already found within the list!");

        int insertionIndex = Math.max(0, Math.min(list.size(), existingValueIndex + insertionOffset));
        list.add(insertionIndex, viewEntry);
        this.entries.add(insertionIndex, viewEntry);
        getRootNode().getSelectionModel().select(insertionIndex);
    }

    /**
     * Removes the view entry from the underlying collection, if possible and supported.
     * Throws exceptions if unsupported, so call isRemoveOperationSupported() first.
     * @param viewEntry the entry to remove
     * @param updateUI if true, the UI will be updated
     */
    public void removeViewEntry(@NonNull TViewEntry viewEntry, boolean updateUI) {
        Collection<? extends TViewEntry> collection = getViewEntries();
        if (!collection.remove(viewEntry))
            throw new IllegalArgumentException("The viewEntry '" + viewEntry + "' could not be removed from the collection!");

        if (getRootNode().getSelectionModel().getSelectedItem() == viewEntry)
            getRootNode().getSelectionModel().clearSelection();

        if (this.entries.remove(viewEntry) && updateUI)
            refreshDisplay();
    }

    /**
     * Moves the view entry in the given direction in the underlying collection, if possible and supported.
     * Throws exceptions if unsupported, so call isMoveOperationSupported() first.
     * @param viewEntry the entry to move
     * @param moveOffset the amount of indices to move the view entry by.
     */
    public void moveViewEntry(@NonNull TViewEntry viewEntry, int moveOffset) {
        List<TViewEntry> list = getStrictlyTypedList();
        int oldIndex = list.indexOf(viewEntry);
        if (oldIndex < 0)
            throw new IllegalStateException("The value '" + viewEntry + "' was not found as part of the viewEntryList. So it cannot be moved.");

        boolean wasSelected = getRootNode().getSelectionModel().isSelected(oldIndex);

        int newIndex = Math.max(0, Math.min(list.size() - 1, oldIndex + moveOffset));
        if (oldIndex == newIndex) {
            return;
        } else if (newIndex > oldIndex) {
            for (int i = oldIndex; i < newIndex; i++)
                list.set(i, list.get(i + 1));
            list.set(newIndex, viewEntry);
        } else {
            for (int i = oldIndex; i > newIndex; i--)
                list.set(i, list.get(i - 1));
            list.set(newIndex, viewEntry);
        }

        refreshDisplay();
        if (wasSelected)
            getRootNode().getSelectionModel().select(newIndex);
    }

    /**
     * Applies the default editor behavior for a List to the given editor component.
     * @param editorComponent the editor component to apply the behavior to
     */
    @SuppressWarnings("unchecked")
    public void applyDefaultEditor(CollectionEditorComponent<TGameInstance, ?> editorComponent) {
        if (editorComponent.getRemoveButtonLogic() == null)
            editorComponent.setRemoveButtonLogic(viewEntry -> removeViewEntry((TViewEntry) viewEntry, true));
        if (editorComponent.getMoveButtonLogic() == null)
            editorComponent.setMoveButtonLogic((viewEntry, direction) -> moveViewEntry((TViewEntry) viewEntry, direction.getOffset()));
    }

    /**
     * Creates the ListView.
     */
    private ListView<TViewEntry> createListView() {
        ListView<TViewEntry> listView = new ListView<>(this.entries);
        listView.setCellFactory(param -> new CollectionListViewComponentEntryListCell<>(this));

        // Selecting an item should update.
        listView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
        return listView;
    }

    private void onSelectionChange(ObservableValue<? extends TViewEntry> observableValue, TViewEntry oldViewEntry, TViewEntry newViewEntry) {
        if (!Objects.equals(oldViewEntry, newViewEntry))
            setSelectedViewEntry(newViewEntry);
    }

    private static class CollectionListViewComponentEntryListCell<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends CollectionViewEntryListCell<TViewEntry> {
        private final ListViewComponent<TGameInstance, TViewEntry> component;
        private final EventHandler<? super MouseEvent> doubleClickHandler;

        @SuppressWarnings("unchecked")
        private CollectionListViewComponentEntryListCell(ListViewComponent<TGameInstance, TViewEntry> component) {
            this.component = component;
            this.doubleClickHandler = event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    event.consume();
                    this.component.onDoubleClick(((ListCell<TViewEntry>) event.getSource()).getItem());
                }
            };
        }

        @Override
        public void updateItem(TViewEntry viewEntry, boolean empty) {
            super.updateItem(viewEntry, empty);
            if (empty) {
                setOnMouseClicked(null);
            } else {
                setOnMouseClicked(this.doubleClickHandler);
            }
        }
    }
}