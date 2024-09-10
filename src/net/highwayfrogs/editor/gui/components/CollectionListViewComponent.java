package net.highwayfrogs.editor.gui.components;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Implements a CollectionView using a basic ListView.
 * Created by Kneesnap on 9/10/2024.
 */
@Getter
public abstract class CollectionListViewComponent<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends CollectionViewComponent<TGameInstance, TViewEntry> {
    private final List<TViewEntry> entries = new ArrayList<>();
    private final Predicate<? super TViewEntry> searchEntryRemover = ((Predicate<? super TViewEntry>) this::matchesSearchQuery).negate();
    private final Predicate<? super TViewEntry> valueExistsRemover = entry -> !getViewEntries().contains(entry);

    public CollectionListViewComponent(TGameInstance instance) {
        super(instance);
        loadController(createListView());
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
        // Add files to the list.
        this.entries.removeIf(this.searchEntryRemover);
        this.entries.removeIf(this.valueExistsRemover);
        for (TViewEntry viewEntry : getViewEntries())
            if (!this.entries.contains(viewEntry) && matchesSearchQuery(viewEntry))
                this.entries.add(viewEntry);

        // Update UI visibility.
        updateEntryList();
    }

    /**
     * Update the entry list.
     */
    public void updateEntryList() {
        // Update existing pane to show updated entry list.
        getRootNode().setItems(FXCollections.observableArrayList(this.entries));
        // TODO: We might need to re-select the previously selected value.
    }

    /**
     * Creates the ListView.
     */
    private ListView<TViewEntry> createListView() {
        ObservableList<TViewEntry> fxFilesList = FXCollections.observableArrayList(this.entries);
        ListView<TViewEntry> listView = new ListView<>(fxFilesList);
        listView.setCellFactory(param -> new CollectionViewEntryListCell<>());
        listView.setItems(fxFilesList);

        // Selecting an item should update.
        listView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
        return listView;
    }

    private void onSelectionChange(ObservableValue<? extends TViewEntry> observableValue, TViewEntry oldViewEntry, TViewEntry newViewEntry) {
        if (newViewEntry != null)
            setSelectedViewEntry(newViewEntry);
    }
}