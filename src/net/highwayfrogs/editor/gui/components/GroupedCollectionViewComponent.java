package net.highwayfrogs.editor.gui.components;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a collection view component which groups the entries into categories.
 * Created by Kneesnap on 4/12/2024.
 */
public abstract class GroupedCollectionViewComponent<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends CollectionViewComponent<TGameInstance, TViewEntry> {
    private final List<CollectionViewGroup<TViewEntry>> groups = new ArrayList<>();
    private boolean hasExpansionOccurred;

    public GroupedCollectionViewComponent(TGameInstance instance) {
        super(instance);
        loadController(new Accordion());
    }

    @Override
    public Accordion getRootNode() {
        return (Accordion) super.getRootNode();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.hasExpansionOccurred = false;
        super.onControllerLoad(rootNode);
        setAnchorPaneStretch(rootNode);
    }

    @Override
    public void refreshDisplay() {
        // Create the list of groups to add files amongst.
        if (this.groups.isEmpty())
            setupViewEntryGroups();

        // Clear the entry list of each group, we're about to regenerate it.
        for (int i = 0; i < this.groups.size(); i++)
            this.groups.get(i).getEntries().clear();

        // Add files to the different groups.
        for (TViewEntry viewEntry : getViewEntries()) {
            // Check each file group to see if the file belongs there.
            boolean addedSuccessfully = false;
            for (int i = 0; i < this.groups.size(); i++) {
                CollectionViewGroup<TViewEntry> fileGroup = this.groups.get(i);
                if (fileGroup.isPartOfGroup(viewEntry) && fileGroup.getEntries().add(viewEntry)) {
                    addedSuccessfully = true;
                    break;
                }
            }

            // If no group was found, add a new one which covers this.
            if (!addedSuccessfully) {
                CollectionViewGroup<TViewEntry> newGroup = createMissingEntryGroup(viewEntry);
                addGroup(newGroup);
                newGroup.getEntries().add(viewEntry);
            }
        }

        // Update UI visibility.
        for (int i = 0; i < this.groups.size(); i++)
            this.groups.get(i).updateEntryList();
    }

    /**
     * Registers a new collection view group.
     * @param collectionViewGroup the collection view group to add
     */
    public void addGroup(CollectionViewGroup<TViewEntry> collectionViewGroup) {
        if (collectionViewGroup == null)
            throw new NullPointerException("collectionViewGroup");

        collectionViewGroup.viewComponent = this;
        this.groups.add(collectionViewGroup);
    }

    /**
     * Registers the basic collection view groups.
     */
    protected abstract void setupViewEntryGroups();

    /**
     * Creates an entry group for an entry which wasn't placed into any pre-existing group.
     * @param viewEntry the view entry to create a group for
     * @return entryGroupToHold
     */
    protected CollectionViewGroup<TViewEntry> createMissingEntryGroup(TViewEntry viewEntry) {
        return new LazyCollectionViewGroup<>("Uncategorized", safeViewEntry -> true);
    }

    /**
     * Represents a group of view entries.
     * @param <TViewEntry>
     */
    @Getter
    public abstract static class CollectionViewGroup<TViewEntry extends ICollectionViewEntry> implements ICollectionViewEntry {
        private GroupedCollectionViewComponent<?, TViewEntry> viewComponent;
        private final List<TViewEntry> entries = new ArrayList<>();
        private final String name;
        private TitledPane titledPane;

        public CollectionViewGroup(String name) {
            this.name = name;
        }

        /**
         * Test if the given view entry is part of this group.
         * @param viewEntry The view entry to test.
         * @return true iff the view entry is part of the group.
         */
        public abstract boolean isPartOfGroup(TViewEntry viewEntry);

        /**
         * Update the entry list.
         */
        public void updateEntryList() {
            if (this.entries.size() > 0) {
                if (this.titledPane != null) {
                    // Update existing pane to show updated entry list.
                    getListView().setItems(FXCollections.observableArrayList(this.entries));
                    // TODO: We might need to re-select the previously selected value.
                    this.titledPane.setText(getCollectionViewDisplayName());
                } else {
                    createTitlePane();
                }

                if (this.viewComponent.isLoadingComplete() && !this.viewComponent.hasExpansionOccurred)
                    tryExpandAndSelectCurrent();
            } else if (this.titledPane != null) {
                removeTitlePane();
            }
        }

        /**
         * Gets the ListView displaying the group's entries.
         */
        @SuppressWarnings("unchecked")
        public ListView<TViewEntry> getListView() {
            return this.titledPane != null ? (ListView<TViewEntry>) this.titledPane.getContent() : null;
        }

        /**
         * Removes the titled pane from existence.
         */
        public void removeTitlePane() {
            if (this.titledPane == null)
                return;

            // Prevent memory leak.
            this.titledPane.prefWidthProperty().unbind();
            this.titledPane.prefHeightProperty().unbind();
            this.viewComponent.getRootNode().getPanes().remove(this.titledPane);
            this.titledPane = null;
        }

        /**
         * Creates the UI accordion pane.
         */
        public TitledPane createTitlePane() {
            TitledPane pane = new TitledPane();
            pane.prefWidthProperty().bind(this.viewComponent.getRootNode().widthProperty());
            pane.prefHeightProperty().bind(this.viewComponent.getRootNode().heightProperty());
            pane.setAnimated(false);

            ObservableList<TViewEntry> fxFilesList = FXCollections.observableArrayList(this.entries);
            ListView<TViewEntry> listView = new ListView<>(fxFilesList);
            listView.setCellFactory(param -> new AttachmentListCell<>());
            listView.setItems(fxFilesList);

            pane.setContent(listView);
            pane.setText(getCollectionViewDisplayName());
            this.viewComponent.getRootNode().getPanes().add(pane);

            // Selecting an item should
            listView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
            this.titledPane = pane;
            tryExpandAndSelectCurrent();
            return this.titledPane;
        }

        private void tryExpandAndSelectCurrent() {
            if (this.titledPane == null || this.viewComponent.hasExpansionOccurred || !this.viewComponent.isLoadingComplete())
                return;

            // If this is the first titled pane seen with a value, expand the pane & select the first value.
            this.viewComponent.getRootNode().setExpandedPane(this.titledPane);
            getListView().getSelectionModel().selectFirst();
            this.viewComponent.hasExpansionOccurred = true;
        }

        private void onSelectionChange(ObservableValue<? extends TViewEntry> observableValue, TViewEntry oldViewEntry, TViewEntry newViewEntry) {
            if (newViewEntry == null)
                return;

            // Clear selection of all other titled panes.
            for (CollectionViewGroup<TViewEntry> group : viewComponent.groups)
                if (group.titledPane != null && this.titledPane != group.titledPane)
                    group.getListView().getSelectionModel().clearSelection();

            // Select it in the view component.
            this.viewComponent.setSelectedViewEntry(newViewEntry);
        }

        @Override
        public ICollectionViewEntry getCollectionViewParentEntry() {
            return null;
        }

        @Override
        public String getCollectionViewDisplayName() {
            return this.name + " (" + this.entries.size() + " Entries)";
        }

        @Override
        public String getCollectionViewDisplayStyle() {
            return null;
        }

        @Override
        public Image getCollectionViewIcon() {
            return null;
        }
    }

    public static class LazyCollectionViewGroup<TViewEntry extends ICollectionViewEntry> extends CollectionViewGroup<TViewEntry>  {
        private final Predicate<TViewEntry> predicate;

        public LazyCollectionViewGroup(String name, Predicate<TViewEntry> predicate) {
            super(name);
            this.predicate = predicate;
        }

        @Override
        public boolean isPartOfGroup(TViewEntry viewEntry) {
            return this.predicate != null && this.predicate.test(viewEntry);
        }
    }

    private static class AttachmentListCell<TViewEntry extends ICollectionViewEntry> extends ListCell<TViewEntry> {
        @Override
        public void updateItem(TViewEntry view, boolean empty) {
            super.updateItem(view, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            // Apply icon.
            Image iconImage = view.getCollectionViewIcon();
            setGraphic(iconImage != null ? new ImageView(iconImage) : null);

            // Update text.
            setStyle(view.getCollectionViewDisplayStyle());
            setText(view.getCollectionViewDisplayName());
        }
    }
}