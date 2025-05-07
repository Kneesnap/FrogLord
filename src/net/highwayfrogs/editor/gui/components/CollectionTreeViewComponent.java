package net.highwayfrogs.editor.gui.components;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;

import java.util.*;

/**
 * Allows displaying a collection of values as a tree.
 * TODO: Searching still seems semi-broken.
 * Created by Kneesnap on 8/9/2024.
 */
public abstract class CollectionTreeViewComponent<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends CollectionViewComponent<TGameInstance, TViewEntry> implements Comparator<TViewEntry> {
    private final Set<TViewEntry> activeEntries = new HashSet<>();
    private CollectionViewTreeNode<TViewEntry> rootNode;

    public CollectionTreeViewComponent(TGameInstance instance) {
        super(instance);
        loadController(new TreeView<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeView<CollectionViewTreeNode<TViewEntry>> getRootNode() {
        return (TreeView<CollectionViewTreeNode<TViewEntry>>) super.getRootNode();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onControllerLoad(Node rootNode) {
        TreeView<CollectionViewTreeNode<TViewEntry>> treeView = (TreeView<CollectionViewTreeNode<TViewEntry>>) rootNode;
        treeView.setEditable(false);
        treeView.setShowRoot(false); // Hide the root node.
        treeView.setFixedCellSize(Constants.RECOMMENDED_TREE_VIEW_FIXED_CELL_SIZE); // Fixes performance issues. Recommended by https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/TreeView.html#fixedCellSizeProperty--
        treeView.setCellFactory(treeViewParam -> new CollectionTreeViewEntryTreeCell<>(this));

        this.rootNode = new CollectionViewTreeNode<>(null, this, "root");
        TreeItem<CollectionViewTreeNode<TViewEntry>> rootTreeItem = this.rootNode.createFxTreeItem();
        treeView.setRoot(rootTreeItem);
        treeView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);

        super.onControllerLoad(treeView);
        setAnchorPaneStretch(treeView);

        // Expand nodes until reaching files.
        // This is not only a nice QoL feature, but also makes FrogLord much easier to navigate for a first-time user.
        CollectionViewTreeNode<TViewEntry> tempNode = this.rootNode;
        do {
            tempNode.getFxTreeItem().setExpanded(true);
            tempNode = tempNode.getChildNodes().size() == 1 ? tempNode.getChildNodes().get(0) : null;
        } while (tempNode != null);
    }

    @Override
    public void setSelectedViewEntryInUI(TViewEntry viewEntry) {
        CollectionViewTreeNode<TViewEntry> treeNodeEntry = getOrCreateTreePath(this.rootNode, viewEntry);
        if (treeNodeEntry == null || treeNodeEntry.getFxTreeItem() == null)
            return;

        CollectionViewTreeNode<TViewEntry> temp = treeNodeEntry;
        List<CollectionViewTreeNode<TViewEntry>> nodeTraversal = new ArrayList<>();
        while (temp != null) {
            if (temp.getFxTreeItem() != null)
                nodeTraversal.add(temp);
            temp = temp.getParent();
        }

        // Expand the tree to reach the UI.
        for (int i = nodeTraversal.size() - 1; i >= 0; i--)
            nodeTraversal.get(i).getFxTreeItem().setExpanded(true);

        getRootNode().getSelectionModel().select(treeNodeEntry.getFxTreeItem());
    }

    private void onSelectionChange(ObservableValue<? extends TreeItem<CollectionViewTreeNode<TViewEntry>>> observableValue, TreeItem<CollectionViewTreeNode<TViewEntry>> oldViewEntry, TreeItem<CollectionViewTreeNode<TViewEntry>> newViewEntry) {
        TViewEntry viewEntry = newViewEntry != null ? (newViewEntry.getValue() != null ? newViewEntry.getValue().getValue() : null) : null;
        setSelectedViewEntry(viewEntry);
    }

    @Override
    public void refreshDisplay() {
        Collection<? extends TViewEntry> sourceViewEntries = getViewEntries();

        // Remove existing expired entries.
        Iterator<TViewEntry> iterator = this.activeEntries.iterator();
        while (iterator.hasNext()) {
            TViewEntry viewEntry = iterator.next();
            if (!sourceViewEntries.contains(viewEntry) || !matchesSearchQuery(viewEntry)) {
                iterator.remove();

                // Update the UI.
                CollectionViewTreeNode<TViewEntry> treeNodeEntry = getOrCreateTreePath(this.rootNode, viewEntry);
                if (treeNodeEntry == null)
                    throw new IllegalStateException("No tree node was returned for the view entry '" + viewEntry + "'.");

                if (treeNodeEntry.getParent().removeChildNode(viewEntry) == null)
                    getLogger().severe("Failed to remove the child node for view entry '" + viewEntry + "'.");
            }
        }

        // Add new entries.
        for (TViewEntry viewEntry : sourceViewEntries) {
            if (!matchesSearchQuery(viewEntry) || !this.activeEntries.add(viewEntry))
                continue;

            CollectionViewTreeNode<TViewEntry> treeNodeEntry = getOrCreateTreePath(this.rootNode, viewEntry);
            if (treeNodeEntry == null)
                throw new IllegalStateException("No tree node was returned for the view entry '" + viewEntry + "'.");
        }
    }

    /**
     * Gets or creates the tree path up to the value. This should not add the value to the tree, just create nodes up until the leaf.
     * @param rootNode the tree root node to find the path from
     * @param viewEntry the value to find the path for
     * @return treeNode
     */
    protected abstract CollectionViewTreeNode<TViewEntry> getOrCreateTreePath(CollectionViewTreeNode<TViewEntry> rootNode, TViewEntry viewEntry);

    /**
     * Represents a tree node.
     * @param <TViewEntry>
     */
    public static class CollectionViewTreeNode<TViewEntry extends ICollectionViewEntry> implements ICollectionViewEntry, Comparable<CollectionViewTreeNode<TViewEntry>> {
        @Getter private final CollectionViewTreeNode<TViewEntry> parent;
        @Getter private final CollectionTreeViewComponent<?, TViewEntry> viewComponent;
        @Getter private final List<CollectionViewTreeNode<TViewEntry>> childNodes = new ArrayList<>();
        private final Map<String, CollectionViewTreeNode<TViewEntry>> childNodesByName = new HashMap<>();
        private final Map<TViewEntry, CollectionViewTreeNode<TViewEntry>> childNodesByValue = new HashMap<>();
        @Getter private final String name;
        @Getter private final TViewEntry value;
        @Getter private TreeItem<CollectionViewTreeNode<TViewEntry>> fxTreeItem;

        public CollectionViewTreeNode(CollectionViewTreeNode<TViewEntry> parent, CollectionTreeViewComponent<?, TViewEntry> viewComponent, String name) {
            this.parent = parent;
            this.viewComponent = viewComponent;
            this.name = name;
            this.value = null;
        }

        public CollectionViewTreeNode(CollectionViewTreeNode<TViewEntry> parent, CollectionTreeViewComponent<?, TViewEntry> viewComponent, TViewEntry value) {
            this.parent = parent;
            this.viewComponent = viewComponent;
            this.name = null;
            this.value = value;
        }

        /**
         * Returns true if the component is active. A component becomes inactive when it is unregistered and is no longer valid to keep cached.
         */
        public boolean isActive() {
            return this.fxTreeItem != null;
        }

        /**
         * Adds a child node for a particular view entry.
         * @param value the view entry to add
         * @return newTreeNode
         */
        public CollectionViewTreeNode<TViewEntry> addChildNode(TViewEntry value) {
            if (value == null)
                throw new NullPointerException("value");

            CollectionViewTreeNode<TViewEntry> childNode = this.childNodesByValue.get(value);
            if (childNode != null)
                return childNode;

            childNode = new CollectionViewTreeNode<>(this, this.viewComponent, value);
            int insertionIndex = -(Collections.binarySearch(this.childNodes, childNode) + 1);
            this.childNodes.add(insertionIndex, childNode);
            this.childNodesByValue.put(value, childNode);
            if (this.fxTreeItem != null)
                this.fxTreeItem.getChildren().add(insertionIndex, childNode.createFxTreeItem());

            return childNode;
        }

        /**
         * Adds a child node for a particular view entry.
         * @param value the view entry to add
         * @return newTreeNode
         */
        private CollectionViewTreeNode<TViewEntry> removeChildNode(TViewEntry value) {
            if (value == null)
                throw new NullPointerException("value");

            CollectionViewTreeNode<TViewEntry> childNode = this.childNodesByValue.remove(value);
            if (childNode == null)
                return null;

            int foundIndex = Collections.binarySearch(this.childNodes, childNode);
            this.childNodes.remove(foundIndex);
            if (this.fxTreeItem != null)
                this.fxTreeItem.getChildren().remove(foundIndex);
            childNode.onRemove();

            tryRemoveIfEmpty();
            return childNode;
        }

        private void onRemove() {
            this.fxTreeItem = null;
        }

        private void tryRemoveIfEmpty() {
            if (!this.childNodes.isEmpty())
                return;

            if (this.parent != null) {
                if (this.name != null) {
                    this.parent.removeChildNode(this.name);
                } else if (this.value != null) {
                    this.parent.removeChildNode(this.value);
                }
            }
        }

        /**
         * Gets a named child node if it exists, creating it if it does not
         * @param name the name of the child node to obtain
         * @return childNode or null
         */
        public CollectionViewTreeNode<TViewEntry> getOrCreateChildNode(String name) {
            return getChildNode(name, true);
        }

        /**
         * Gets a named child node if it exists.
         * @param name the name of the child node to obtain
         * @param createIfMissing if no such named child node exists, do we create it?
         * @return childNode or null
         */
        public CollectionViewTreeNode<TViewEntry> getChildNode(String name, boolean createIfMissing) {
            CollectionViewTreeNode<TViewEntry> childNode = this.childNodesByName.get(name);
            if (childNode == null && createIfMissing) {
                if (name == null)
                    throw new NullPointerException("name");

                childNode = new CollectionViewTreeNode<>(this, this.viewComponent, name);
                int insertionIndex = -(Collections.binarySearch(this.childNodes, childNode) + 1);
                this.childNodes.add(insertionIndex, childNode);
                this.childNodesByName.put(name, childNode);
                if (this.fxTreeItem != null)
                    this.fxTreeItem.getChildren().add(insertionIndex, childNode.createFxTreeItem());
            }

            return childNode;
        }

        /**
         * Removes a named child node.
         * @param name the name to remove
         * @return removedTreeNode
         */
        private CollectionViewTreeNode<TViewEntry> removeChildNode(String name) {
            if (name == null)
                return null;

            CollectionViewTreeNode<TViewEntry> childNode = this.childNodesByName.remove(name);
            if (childNode == null)
                return null;

            int foundIndex = Collections.binarySearch(this.childNodes, childNode);
            this.childNodes.remove(foundIndex);
            if (this.fxTreeItem != null)
                this.fxTreeItem.getChildren().remove(foundIndex);
            childNode.onRemove();

            tryRemoveIfEmpty();
            return childNode;
        }

        /**
         * Creates the UI tree item.
         */
        private TreeItem<CollectionViewTreeNode<TViewEntry>> createFxTreeItem() {
            if (this.fxTreeItem != null)
                return this.fxTreeItem;

            TreeItem<CollectionViewTreeNode<TViewEntry>> newTreeItem = new TreeItem<>(this);
            this.fxTreeItem = newTreeItem;

            // Register child nodes.
            ObservableList<TreeItem<CollectionViewTreeNode<TViewEntry>>> newNodeItems = newTreeItem.getChildren();
            for (int i = 0; i < this.childNodes.size(); i++)
                newNodeItems.add(this.childNodes.get(i).createFxTreeItem());

            return newTreeItem;
        }

        @Override
        public String getCollectionViewDisplayName() {
            if (this.value != null) {
                return this.value.getCollectionViewDisplayName();
            } else if (this.name != null) {
                return this.name;
            } else {
                return "{unnamed}";
            }
        }

        @Override
        public String getCollectionViewDisplayStyle() {
            return this.value != null ? this.value.getCollectionViewDisplayStyle() : null;
        }

        @Override
        public Image getCollectionViewIcon() {
            return this.value != null ? this.value.getCollectionViewIcon() : ImageResource.GHIDRA_ICON_OPEN_FOLDER_16.getFxImage();
        }

        @Override
        public int compareTo(CollectionViewTreeNode<TViewEntry> other) {
            if (this.name == null && other.name != null) {
                return 1; // Nodes with names are ordered before ones with. (Folders come first, files come second)
            } else if (this.name != null && other.name == null) {
                return -1; // Nodes without names are ordered after ones without. (Files come after folders)
            } else if (this.name != null) {
                // Both names aren't null, so compare names alphabetically.
                return this.name.compareTo(other.name);
            } else if (this.value != null && other.value != null) {
                TViewEntry o1 = this.value;
                TViewEntry o2 = other.value;

                // Both names are null, so compare values.
                int result = this.viewComponent.compare(o1, o2);
                if (result != 0)
                    return result;

                // Even the custom tiebreaker matched, so try to sort using default string sorting behavior.
                String o1Name = o1.getCollectionViewDisplayName();
                String o2Name = o2.getCollectionViewDisplayName();
                if (o1Name == null)
                    o1Name = "{unnamed}";
                if (o2Name == null)
                    o2Name = "{unnamed}";

                result = o1Name.compareTo(o2Name);
                if (result != 0)
                    return result;

                // If all else fails, we will compare their hash codes.
                return Integer.compare(o1.hashCode(), o2.hashCode());
            } else {
                throw new NullPointerException("Either this.value or other.value was null! This should not be possible!");
            }
        }
    }

    private static class CollectionTreeViewEntryTreeCell<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends CollectionViewEntryTreeCell<CollectionViewTreeNode<TViewEntry>> {
        private final CollectionTreeViewComponent<TGameInstance, TViewEntry> component;
        private final EventHandler<? super MouseEvent> doubleClickHandler;

        @SuppressWarnings("unchecked")
        private CollectionTreeViewEntryTreeCell(CollectionTreeViewComponent<TGameInstance, TViewEntry> component) {
            this.component = component;
            this.doubleClickHandler = event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    event.consume();
                    this.component.onDoubleClick(((TreeCell<CollectionViewTreeNode<TViewEntry>>) event.getSource()).getItem().getValue());
                }
            };
        }

        @Override
        public void updateItem(CollectionViewTreeNode<TViewEntry> viewEntry, boolean empty) {
            super.updateItem(viewEntry, empty);
            if (empty) {
                setOnMouseClicked(null);
                return;
            }

            setOnMouseClicked(this.doubleClickHandler);
        }
    }
}