package net.highwayfrogs.editor.gui.components;

import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.NameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows viewing a list of properties.
 * Created by Kneesnap on 4/15/2024.
 */
public class PropertyListViewerComponent<TGameInstance extends GameInstance> extends GameUIController<TGameInstance> {
    private TableColumn<NameValuePair, String> tableColumnKey;
    private TableColumn<NameValuePair, String> tableColumnValue;

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

        getRootNode().getItems().clear();
        this.tableColumnKey.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnValue.setCellValueFactory(new PropertyValueFactory<>("value"));

        List<NameValuePair> properties = propertyList.getEntries();
        if (properties != null && properties.size() > 0)
            for (NameValuePair pair : properties)
                getRootNode().getItems().add(pair);
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
         * Apply the property list to the given table.
         * @param tableFileData the table to apply to
         */
        public void apply(TableView<NameValuePair> tableFileData) {
            tableFileData.getItems().clear();
            tableFileData.getItems().addAll(this.entries);
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