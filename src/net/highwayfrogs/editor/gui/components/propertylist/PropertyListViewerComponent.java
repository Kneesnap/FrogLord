package net.highwayfrogs.editor.gui.components.propertylist;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Displays PropertyList data in a JavaFX TreeTableView.
 * Reference: <a href="https://examples.javacodegeeks.com/java-development/desktop-java/javafx/javafx-treetableview-example/"/>
 * TODO:
 *  -> Fix the issue where often times PropertyListViewerComponents don't properly use the space available (Eg: Improper resizing, etc)
 *  -> Allow setting the graphic node to go along with the text of a TreeItem.
 * Created by Kneesnap on 11/8/2025.
 */
public class PropertyListViewerComponent<TGameInstance extends GameInstance> extends GameUIController<TGameInstance> {
    private final Map<PropertyListNode, PropertyListUINode> nodeMappings = new HashMap<>();
    private final EventHandler<? super MouseEvent> rowMouseClickHandler = this::onRowMouseClicked;
    private TreeTableColumn<PropertyListNode, String> tableColumnKey;
    private TreeTableColumn<PropertyListNode, String> tableColumnValue;

    private static final int PIXEL_WIDTH_OFFSET = 5; // Pixels included in the table which aren't part of a column.

    public PropertyListViewerComponent(TGameInstance instance) {
        this(instance, null);
    }

    public PropertyListViewerComponent(TGameInstance instance, TreeTableView<PropertyListNode> tableView) {
        super(instance);
        loadController(tableView != null ? tableView : new TreeTableView<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeTableView<PropertyListNode> getRootNode() {
        return (TreeTableView<PropertyListNode>) super.getRootNode();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onControllerLoad(Node rootNode) {
        TreeTableView<PropertyListNode> tableView = (TreeTableView<PropertyListNode>) rootNode;
        tableView.setShowRoot(false); // Hide the root node, so instead of all properties being collapsable, only sub-properties are collapsable.
        tableView.setEditable(true); // Required to allow the use of the edit() method. TODO: Consider if we don't need this.

        // TODO: I don't think this belongs here.
        AnchorPane.setTopAnchor(rootNode, 8D);
        AnchorPane.setBottomAnchor(rootNode, 8D);
        AnchorPane.setLeftAnchor(rootNode, 8D);
        AnchorPane.setRightAnchor(rootNode, 8D);

        // Setup columns.
        if (tableView.getColumns().size() == 2) {
            this.tableColumnKey = (TreeTableColumn<PropertyListNode, String>) tableView.getColumns().get(0);
            this.tableColumnValue = (TreeTableColumn<PropertyListNode, String>) tableView.getColumns().get(1);
        } else {
            tableView.getColumns().clear();
            tableView.getColumns().add(this.tableColumnKey = new TreeTableColumn<>("Name")); // "Name" is the column display name, shown above all the entries.
            tableView.getColumns().add(this.tableColumnValue = new TreeTableColumn<>("Value")); // "Value" is the column display name, shown above all the entries.
        }

        // Setup display property.
        this.tableColumnKey.setEditable(false);
        this.tableColumnKey.setCellValueFactory(data -> {
            PropertyListNode node = data.getValue().getValue();
            return node instanceof PropertyListEntry ? ((PropertyListEntry) node).nameProperty() : null;
        });
        this.tableColumnValue.setEditable(true); // Must be editable for individual rows to be editable.
        this.tableColumnValue.setCellValueFactory(data -> {
            PropertyListNode node = data.getValue().getValue();
            return node instanceof PropertyListEntry ? ((PropertyListEntry) node).valueProperty() : null;
        });

        tableView.setRowFactory(this::createPropertyListEntryTableRow);

        // Setup the default width.
        double oneThirdWidth = getRootNode().getWidth() / 3D;
        this.tableColumnKey.setPrefWidth(oneThirdWidth);
        this.tableColumnValue.setPrefWidth(oneThirdWidth * 2);
        // TODO: TOSS this.tableColumnKey.maxWidthProperty().bind(getRootNode().widthProperty()); // Could restrict this to be widthProperty() - PIXEL_WIDTH_OFFSET

        // Only allow resizing the key.
        this.tableColumnKey.setResizable(true);
        this.tableColumnValue.setResizable(false);

        // Bind the sizes of each column to ensure
        this.tableColumnKey.widthProperty().addListener((observable, oldValue, newValue) -> {
            double tableWidth = getRootNode().getWidth();
            double columnWidth = newValue.doubleValue();
            if (!Double.isFinite(tableWidth) || Math.abs(tableWidth) <= .001)
                return;

            double newWidth = Math.max(0D, Math.min(tableWidth - PIXEL_WIDTH_OFFSET, columnWidth));
            this.tableColumnValue.setPrefWidth(tableWidth - newWidth);
        });

        tableView.widthProperty().addListener((observable, oldValue, newValue) -> {
            double newWidth = newValue.doubleValue();
            if (!Double.isFinite(newWidth) || Math.abs(newWidth) <= .001 || Math.abs(this.tableColumnKey.getWidth()) <= .001)
                return;

            double oldWidth = oldValue.doubleValue();
            double oldKeyPercent = Double.isFinite(oldWidth) && Math.abs(oldWidth) > .001 ? Math.max(0D, Math.min(1D, this.tableColumnKey.getWidth() / oldWidth)) : (1 / 3D);
            this.tableColumnKey.setPrefWidth(oldKeyPercent * newWidth);
            this.tableColumnValue.setPrefWidth(Math.max(0, ((1D - oldKeyPercent) * newWidth) - PIXEL_WIDTH_OFFSET));
        });
    }

    /**
     * Hides any properties which are currently shown/active.
     */
    public void clear() {
        showProperties((PropertyList) null);
    }

    /**
     * Show the provided property list
     * @param propertyListCreator the property list creator to show
     */
    public void showProperties(IPropertyListCreator propertyListCreator) {
        showProperties(propertyListCreator != null ? propertyListCreator.createPropertyList() : null);
    }

    /**
     * Show the provided property list
     * @param propertyList the property list to show
     */
    public void showProperties(PropertyList propertyList) {
        // Setup and initialise the table view
        boolean hasEntry = (propertyList != null);
        getRootNode().setVisible(hasEntry);
        if (!hasEntry)
            return;

        // Remove previous entries.
        if (getRootNode().getRoot() != null && getRootNode().getRoot().getValue() != null && getRootNode().getRoot() != null)
            getRootNode().getRoot().getValue().clearChildEntries();

        if (getRootNode().getRoot() != null && getRootNode().getRoot().getValue() == propertyList) {
            // If it's the same property list as before, so just update the entries.
            propertyList.updateChildEntries();
        } else {
            // This is a new property list, so generate entries and create nodes.
            propertyList.populateChildEntriesIfNecessary();
            getRootNode().setRoot(getOrCreateNodeUI(propertyList).getTreeItem());
        }
    }

    /**
     * Binds the column widths to the table size.
     * TODO: Review.
     */
    public void bindSize() {
        getRootNode().maxWidth(Double.POSITIVE_INFINITY);
        getRootNode().prefWidth(Double.POSITIVE_INFINITY);
    }

    private PropertyListUINode getOrCreateNodeUI(PropertyListNode node) {
        PropertyListUINode nodeUI = this.nodeMappings.get(node);
        if (nodeUI == null) {
            this.nodeMappings.put(node, nodeUI = new PropertyListUINode(this, new TreeItem<>(node)));

            // If the node has potential for properties:
            if (node.canHaveProperties()) {
                // If the entries have been populated, add em now.
                for (int i = 0; i < node.getChildEntries().size(); i++)
                    nodeUI.getTreeItem().getChildren().add(getOrCreateNodeUI(node.getChildEntries().get(i)).getTreeItem());

                // Always have at least one child node, so it's possible to expand.
                if (node.getChildEntries().isEmpty())
                    nodeUI.getTreeItem().getChildren().add(new TreeItem<>());

                nodeUI.setup(); // Setup the listeners.
            }
        }

        return nodeUI;
    }

    private void onTreeItemRemove(TreeItem<PropertyListNode> treeItem) {
        PropertyListNode node = treeItem.getValue();
        PropertyListUINode uiNode = this.nodeMappings.remove(node);
        if (uiNode != null)
            uiNode.remove();

        // Recursively remove child nodes.
        List<TreeItem<PropertyListNode>> children = treeItem.getChildren();
        for (int i = 0; i < children.size(); i++)
            onTreeItemRemove(children.get(i));
    }

    private void onRowMouseClicked(MouseEvent event) {
        if (event.getClickCount() != 2)
            return;

        if (!(event.getSource() instanceof TreeTableRow))
            throw new ClassCastException("Expected the EventSource to be TreeTableRow, but was actually " + Utils.getSimpleName(event.getSource()) + ".");

        @SuppressWarnings("unchecked")
        TreeTableRow<PropertyListNode> tableRow = (TreeTableRow<PropertyListNode>) event.getSource();
        PropertyListNode propertyListNode = tableRow.getItem(); // The root node shouldn't be clickable.
        if (!(propertyListNode instanceof PropertyListEntry))
            return; // If null, OR the root node, there's nothing to handle.

        // TODO: If the node does not support editing, abort!

        PropertyListUINode uiNode = this.nodeMappings.get(propertyListNode);

        try {
            uiNode.currentRowUnsafe = tableRow; // TODO: TOSS?
            // TODO: Call method.
            // TODO: Catch error?
        } finally {
            uiNode.currentRowUnsafe = null;
        }
    }

    private TreeTableRow<PropertyListNode> createPropertyListEntryTableRow(TreeTableView<PropertyListNode> tableView) {
        TreeTableRow<PropertyListNode> newRow = new TreeTableRow<>();
        newRow.setOnMouseClicked(this.rowMouseClickHandler);
        return newRow;
    }

    private static class PropertyListUINode implements IPropertyListEntryUI {
        @Getter private final PropertyListViewerComponent<?> component;
        @Getter private final TreeItem<PropertyListNode> treeItem;
        private final ListChangeListener<PropertyListNode> listListener;
        private TreeTableRow<PropertyListNode> currentRowUnsafe;

        @SuppressWarnings({"unchecked"})
        private static final ChangeListener<? super Boolean> EXPANSION_LISTENER = (observable, oldValue, newValue) -> {
            if (!newValue)
                return;

            TreeItem<PropertyListNode> treeItem = ((TreeItem<PropertyListNode>) ((BooleanProperty) observable).getBean());
            if (treeItem.getChildren().size() == 1 && treeItem.getChildren().get(0).getValue() == null)
                treeItem.getChildren().clear(); // If this is a placeholder so the expand icon is visible, clear it and replace it with the real properties.
            treeItem.getValue().populateChildEntriesIfNecessary();
        };

        private PropertyListUINode(PropertyListViewerComponent<?> component, TreeItem<PropertyListNode> treeItem) {
            this.treeItem = treeItem;
            this.component = component;
            this.listListener = new ChildEntryListChangeListener<>(this);
        }

        private PropertyListNode getNode() {
            return this.treeItem.getValue();
        }

        @Override
        public PropertyListEntry getEntry() {
            PropertyListNode node = this.treeItem.getValue();
            return node instanceof PropertyListEntry ? (PropertyListEntry) node : null;
        }

        @Override
        public void updateEntry(PropertyListNode node) {
            node.updateChildEntries();
            // TODO: Ensure removed stuff is properly updated. (I think this works, but actually test)
            // TODO: Actually, we should ensure the same TreeItem depths are expanded as before, and the same node is selected.
            // TODO: This method should be in the main class, so that when changing the root we can call this method too.
        }

        @Override
        public void edit(String startValue, Predicate<String> validator, Consumer<String> newValueHandler) {
            if (this.currentRowUnsafe == null) // TODO: Perhaps fallback to the default InputMenu method instead? ORR
                throw new IllegalStateException("Cannot use edit() while the PropertyListUINode doesn't know what row it should make editable.");

            // TODO: Perhaps this will work.
            /*int rowIndex = this.component.getRootNode().getRow(this.treeItem);
            if (rowIndex >= 0)
                this.component.getRootNode().edit(rowIndex, this.component.tableColumnValue);*/ // TODO: RESUME?

            this.currentRowUnsafe.editingProperty().addListener((observable, oldValue, newValue) -> {
                getNode().getLogger().info("EDIT STATE CHANGE! OLD: %s%nNEW: %s%nSOURCE: %s", oldValue, newValue, observable);
            }); // TODO: Handle change?

            this.currentRowUnsafe.addEventHandler(TreeTableView.editCancelEvent(), event -> {
                getNode().getLogger().info("CANCEL EVENT!%nTree Item: %s%nOld/New Values: %s/%s%nSource: %s", event.getTreeItem(), event.getOldValue(), event.getNewValue(), event.getSource());
            }); // TODO: Handle!
            this.currentRowUnsafe.addEventHandler(TreeTableView.editCommitEvent(), event -> {
                getNode().getLogger().info("COMMIT EVENT!%nTree Item: %s%nOld/New Values: %s/%s%nSource: %s", event.getTreeItem(), event.getOldValue(), event.getNewValue(), event.getSource());
            }); // TODO: Handle!
            this.currentRowUnsafe.textProperty().addListener((observable, oldValue, newValue) -> {
                getNode().getLogger().info("TEXT CHANGE! OLD: %s%nNEW: %s%nSOURCE: %s", oldValue, newValue, observable);
            }); // TODO: Handle change?
            this.currentRowUnsafe.startEdit();
        }

        private void setup() {
            if (!getNode().canHaveProperties())
                return;

            getNode().getChildEntries().addListener(this.listListener);
            this.treeItem.expandedProperty().addListener(EXPANSION_LISTENER);
        }

        private void remove() {
            if (!getNode().canHaveProperties())
                return;

            getNode().getChildEntries().removeListener(this.listListener);
            this.treeItem.expandedProperty().removeListener(EXPANSION_LISTENER);
        }
    }

    private static class ChildEntryListChangeListener<T> implements ListChangeListener<PropertyListNode> {
        private final WeakReference<PropertyListUINode> uiNode;

        public ChildEntryListChangeListener(PropertyListUINode uiNode) {
            this.uiNode = new WeakReference<>(uiNode);
        }

        @Override
        public void onChanged(Change<? extends PropertyListNode> change) {
            PropertyListUINode node = this.uiNode.get();
            if (node == null) {
                change.getList().removeListener(this);
                return;
            }

            final PropertyListViewerComponent<?> component = node.component;
            final TreeItem<PropertyListNode> treeItem = node.getTreeItem();
            final List<TreeItem<PropertyListNode>> list = treeItem.getChildren();

            while (change.next()) {
                if (change.wasPermutated()) { // The area between [from, to) has been permuted, so we just need to reorder the nodes.
                    for (int i = change.getFrom(); i < change.getTo(); i++) // Note this assumes the list has been kept perfectly updated.
                        list.set(i, component.nodeMappings.get(change.getList().get(i)).treeItem);
                    continue;
                }

                if (change.wasRemoved()) {
                    // Equivalent to list.subList(change.getFrom(), change.getFrom() + change.getRemovedSize()).clear();
                    // But, we need to handle proper removals of the properties.
                    int fromIndex = change.getFrom();
                    int addEndIndex = fromIndex + change.getAddedSize();
                    for (int i = fromIndex + change.getRemovedSize() - 1; i >= fromIndex; i--) {
                        TreeItem<PropertyListNode> temp = list.remove(i);
                        if (!change.wasAdded() || !contains(change.getList(), fromIndex, addEndIndex, temp.getValue()))
                            component.onTreeItemRemove(temp);
                    }
                }


                if (change.wasAdded()) {
                    // Version of list.addAll(change.getFrom(), change.getAddedSubList());
                    // But, it needs transformation to a TreeItem before adding.
                    for (int i = change.getFrom(); i < change.getTo(); i++)
                        list.add(i, component.getOrCreateNodeUI(change.getList().get(i)).getTreeItem());
                }
            }
        }

        private static boolean contains(List<? extends PropertyListNode> list, int fromIndex, int toIndex, PropertyListNode node) {
            for (int i = fromIndex; i < toIndex; i++)
                if (Objects.equals(list.get(i), node))
                    return true;

            return false;
        }
    }
}
