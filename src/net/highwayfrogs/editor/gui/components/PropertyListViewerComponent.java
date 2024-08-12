package net.highwayfrogs.editor.gui.components;

import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Allows viewing a list of properties.
 * Created by Kneesnap on 4/15/2024.
 */
public class PropertyListViewerComponent<TGameInstance extends GameInstance> extends GameUIController<TGameInstance> {
    private TableColumn<NameValuePair, String> tableColumnKey;
    private TableColumn<NameValuePair, String> tableColumnValue;

    private static final int PIXEL_WIDTH_OFFSET = 5; // Pixels included in the table which aren't part of a column.

    public PropertyListViewerComponent(TGameInstance instance) {
        super(instance);
        loadController(new TableView<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public TableView<NameValuePair> getRootNode() {
        return (TableView<NameValuePair>) super.getRootNode();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        AnchorPane.setTopAnchor(rootNode, 8D);
        AnchorPane.setBottomAnchor(rootNode, 8D);
        AnchorPane.setLeftAnchor(rootNode, 8D);
        AnchorPane.setRightAnchor(rootNode, 8D);

        getRootNode().getColumns().add(this.tableColumnKey = new TableColumn<>("Name"));
        getRootNode().getColumns().add(this.tableColumnValue = new TableColumn<>("Value"));
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

        clear();
        this.tableColumnKey.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        if (getRootNode().getRowFactory() == null) {
            getRootNode().setRowFactory(tableView -> {
                TableRow<NameValuePair> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() != 2)
                        return;

                    if (!(row.getItem() instanceof PropertyListPair))
                        return;

                    PropertyListPair pair = (PropertyListPair) row.getItem();
                    if (pair.getOnClickBehavior() != null) {
                        try {
                            Object result = pair.getOnClickBehavior().get();
                            if (result != null) {
                                pair.setValue(String.valueOf(result));
                                showProperties(propertyList); // Refresh the UI.
                            }
                        } catch (Throwable th) {
                            Utils.handleError(getGameInstance().getLogger(), th, true, "Failed to run click handler for '%s'.", pair.getName());
                        }
                    }
                });

                return row;
            });
        }

        List<NameValuePair> properties = propertyList.getEntries();
        if (properties != null && properties.size() > 0)
            for (NameValuePair pair : properties)
                getRootNode().getItems().add(pair);
    }

    /**
     * Binds the column widths to the table size.
     */
    public void bindSize() {
        double oneThirdWidth = getRootNode().getWidth() / 3D;
        this.tableColumnKey.setPrefWidth(oneThirdWidth);
        this.tableColumnValue.setPrefWidth(oneThirdWidth * 2);
        this.tableColumnKey.maxWidthProperty().bind(getRootNode().widthProperty()); // Could restrict this to be widthProperty() - PIXEL_WIDTH_OFFSET

        // Only allow resizing the key.
        this.tableColumnKey.setResizable(true);
        this.tableColumnValue.setResizable(false);

        this.tableColumnKey.widthProperty().addListener((observable, oldValue, newValue) -> {
            double tableWidth = getRootNode().getWidth();
            double columnWidth = newValue.doubleValue();
            if (!Double.isFinite(tableWidth) || Math.abs(tableWidth) <= .001)
                return;

            double newWidth = Math.max(0D, Math.min(tableWidth - PIXEL_WIDTH_OFFSET, columnWidth));
            this.tableColumnValue.setPrefWidth(tableWidth - newWidth);
        });

        getRootNode().widthProperty().addListener((observable, oldValue, newValue) -> {
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
     * Clears the properties seen here.
     */
    public void clear() {
        getRootNode().getItems().clear();
    }

    /**
     * Contains a list of properties to display in a UI.
     */
    @Getter
    public static class PropertyList {
        private final List<NameValuePair> entries = new ArrayList<>();

        /**
         * Adds a new property to the property list.
         * @param key the key to add
         * @param value the value to add
         */
        public void add(String key, Object value) {
            this.entries.add(new NameValuePair(key, String.valueOf(value)));
        }

        /**
         * Adds a new property to the property list.
         * @param key the key to add
         * @param value the value to add
         * @param onClickBehavior behavior to run when the property is clicked
         */
        public <TObject> void add(String key, TObject value, Supplier<TObject> onClickBehavior) {
            this.entries.add(new PropertyListPair(key, String.valueOf(value), onClickBehavior));
        }

        /**
         * Apply the property list to the given table.
         * @param tableFileData the table to apply to
         */
        public void apply(TableView<NameValuePair> tableFileData) {
            tableFileData.getItems().clear();
            tableFileData.getItems().addAll(this.entries);
        }
    }

    @Getter
    public static class PropertyListPair extends NameValuePair {
        private final Supplier<?> onClickBehavior;

        public PropertyListPair(String name, String value, Supplier<?> onClickBehavior) {
            super(name, value);
            this.onClickBehavior = onClickBehavior;
        }
    }

    public interface IPropertyListCreator {
        /**
         * Creates and populates the property list.
         */
        default PropertyList createPropertyList() {
            return addToPropertyList(new PropertyList());
        }

        /**
         * Adds properties to the property list.
         * @param propertyList the property list to add properties to
         * @return propertyList
         */
        PropertyList addToPropertyList(PropertyList propertyList);
    }
}