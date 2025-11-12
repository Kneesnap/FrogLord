package net.highwayfrogs.editor.gui.components.propertylist;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Displays PropertyList data in a JavaFX TreeTableView.
 * Reference: <a href="https://examples.javacodegeeks.com/java-development/desktop-java/javafx/javafx-treetableview-example/"/>
 * Created by Kneesnap on 11/8/2025.
 */
public class PropertyListViewerComponent<TGameInstance extends GameInstance> extends GameUIController<TGameInstance> {
    private final Map<PropertyListNode, PropertyListUINode> nodeMappings = new HashMap<>();
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
        tableView.setEditable(true); // Required to allow editing.

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
        this.tableColumnValue.setEditable(true); // Required for editing to work.
        this.tableColumnValue.setCellFactory(list -> new FXPropertyListNodeTreeTableCell(this)); // Responsible for editing behavior.
        this.tableColumnValue.setCellValueFactory(data -> {
            PropertyListNode node = data.getValue().getValue();
            return node instanceof PropertyListEntry ? ((PropertyListEntry) node).valueProperty() : null;
        });

        tableView.setRowFactory(this::createPropertyListEntryTableRow);

        // Setup the default width.
        double oneThirdWidth = getRootNode().getWidth() / 3D;
        this.tableColumnKey.setPrefWidth(oneThirdWidth);
        this.tableColumnValue.setPrefWidth(oneThirdWidth * 2);

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

        if (getRootNode().getRoot() != null && getRootNode().getRoot().getValue() == propertyList) {
            // If it's the same property list as before, so just update the entries.
            updateNode(propertyList);
        } else {
            // Remove previous entries.
            if (getRootNode().getRoot() != null && getRootNode().getRoot().getValue() != null && getRootNode().getRoot() != null)
                getRootNode().getRoot().getValue().clearChildEntries();

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

    PropertyListUINode getNodeUI(PropertyListNode node) {
        return this.nodeMappings.get(node);
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

    private void updateNode(PropertyListNode node) {
        if (!node.canHaveProperties())
            return; // Not much to do.

        PropertyListUINode nodeUI = getNodeUI(node);
        if (nodeUI == null)
            return; // Can't refresh nodes without a UI node.

        List<TreeItem<PropertyListNode>> oldTreeItems = new ArrayList<>(nodeUI.getTreeItem().getChildren());

        // Doing this will clear the tree, we'll need to update the UI to re-expand and re-select everything which should be expanded.
        node.updateChildEntries();

        tryExpandNewTreeItems(oldTreeItems, nodeUI.getTreeItem());
        selectTreeItem(nodeUI.getTreeItem(), false);
    }

    private void tryExpandNewTreeItems(List<TreeItem<PropertyListNode>> oldTreeItems, TreeItem<PropertyListNode> treeItem) {
        int lastMatchIndex = 0;
        List<TreeItem<PropertyListNode>> newTreeItems = treeItem.getChildren();
        for (int i = 0; i< oldTreeItems.size(); i++) {
            TreeItem<PropertyListNode> oldItem = oldTreeItems.get(i);
            if (!oldItem.isExpanded() || oldItem.isLeaf())
                continue;

            for (int j = lastMatchIndex; j < newTreeItems.size(); j++) {
                TreeItem<PropertyListNode> newItem = newTreeItems.get(j);
                if (Objects.equals(((PropertyListEntry) oldItem.getValue()).getName(), ((PropertyListEntry) newItem.getValue()).getName())) {
                    lastMatchIndex = j + 1;
                    newItem.setExpanded(true);
                    tryExpandNewTreeItems(oldItem.getChildren(), newItem);
                    break;
                }
            }
        }
    }

    private void selectTreeItem(TreeItem<PropertyListNode> treeItem, boolean focus) {
        int newRowIndex = getRootNode().getRow(treeItem);
        getRootNode().scrollTo(newRowIndex);

        // Put the first column in editing mode
        getRootNode().getSelectionModel().select(treeItem);
        if (focus)
            getRootNode().getFocusModel().focus(newRowIndex, this.tableColumnKey);
    }

    @SuppressWarnings("unchecked")
    private TreeTableRow<PropertyListNode> createPropertyListEntryTableRow(TreeTableView<PropertyListNode> tableView) {
        TreeTableRow<PropertyListNode> newRow = new TreeTableRow<>();

        // My goal is to make the rows alternate in colors, similarly to ListView or TableView.
        // The most sensible thing is to apply the same property as applied to table-row-cell:odd, but to tree-table-row-cell in modena.css.
        // But I've not figured out how to actually do that. So this is the next best thing.
        newRow.indexProperty().addListener((observable, oldIndex, newIndex) ->
                ((TreeTableRow<PropertyListNode>) ((ReadOnlyIntegerProperty) observable).getBean()).setStyle((newIndex.intValue() % 2) > 0 ? "-fx-background: -fx-control-inner-background-alt;" : null));
        newRow.selectedProperty().addListener((observable, wasSelected, isSelected) -> {
            TreeTableRow<PropertyListNode> tableRow = ((TreeTableRow<PropertyListNode>) ((ReadOnlyBooleanProperty) observable).getBean());
            tableRow.setStyle(!isSelected && (tableRow.getIndex() % 2) > 0 ? "-fx-background: -fx-control-inner-background-alt;" : null);
        });

        return newRow;
    }

    protected static class PropertyListUINode implements IPropertyListEntryUI {
        @Getter private final PropertyListViewerComponent<?> component;
        @Getter private final TreeItem<PropertyListNode> treeItem;
        private final ListChangeListener<PropertyListNode> listListener;
        @Getter private Predicate<String> validator;
        @Getter private BiConsumer<IPropertyListEntryUI, String> newValueHandler;
        @Getter private boolean editorDataSetup;

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
            this.component.updateNode(node);
        }

        @Override
        public void edit(String startValue, Predicate<String> validator, BiConsumer<IPropertyListEntryUI, String> newValueHandler) {
            if (newValueHandler == null)
                throw new NullPointerException("newValueHandler");

            this.validator = validator;
            this.newValueHandler = newValueHandler;
            this.editorDataSetup = true;

            // Start editing.
            int rowIndex = this.component.getRootNode().getRow(this.treeItem);
            if (rowIndex >= 0) {
                this.component.selectTreeItem(this.treeItem, true);
                this.component.getRootNode().edit(rowIndex, this.component.tableColumnValue);
            }
        }

        void setupEditor() {
            PropertyListEntry entry = getEntry();
            if (entry == null)
                return;

            try {
                entry.setupEditor(this);
            } catch (Throwable th) {
                Utils.handleError(entry.getLogger(), th, true, "Failed to setup editor for property named '%s'. (Value: %s)", entry.getName(), entry.getValue());
                onEditorShutdown();
            }
        }

        boolean validate(String data) {
            try {
                return this.validator == null || this.validator.test(data);
            } catch (Throwable th) {
                return false;
            }
        }

        void onEditorShutdown() {
            this.validator = null;
            this.newValueHandler = null;
            this.editorDataSetup = false;
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
