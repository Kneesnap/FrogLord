package net.highwayfrogs.editor.gui.components.propertylist;

import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.KeyCode;
import lombok.NonNull;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListViewerComponent.PropertyListUINode;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;

/**
 * This implements a tree table cell fit for editing PropertyListNodes.
 * IMPORTANT FOR MAKING TABLE CELLS EDITABLE.
 * By default, when a table cell is created, it's pretty much just a Label.
 * But, in addition to the label text, there is a "Graphic" JavaFX node which can override the label/display in place of it.
 * However, there are different kinds of TreeTableCells available in javafx.scene.control.cell.
 * For example, TextFieldTreeTableCell.forTreeTableColumn() creates a TreeTableCell which sets the graphic to a TextField when clicked, so it can be edited.
 *  - tableColumnValue.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
 * However, none of the options built into JavaFX work quite right for us, since we want:
 *  A) The ability to have different types of UI in cells for different properties.
 *  TODO: Implement this later.
 *   - Booleans as checkboxes
 *   - Enums and bit flags as combo boxes.
 *   - Colors as color pickers.
 *   - Great quest resources should have a search bar
 *  B) The ability to validate user input in real-time as it is typed.
 *  C) To only enable editing for property list nodes that have editing behavior setup.
 * Created by Kneesnap on 11/10/2025.
 */
public class FXPropertyListNodeTreeTableCell extends TreeTableCell<PropertyListNode, String> {
    @NonNull private final PropertyListViewerComponent<?> propertyListViewer;

    private TextField textField;

    public FXPropertyListNodeTreeTableCell(PropertyListViewerComponent<?> propertyListViewer) {
        this.propertyListViewer = propertyListViewer;
    }

    private PropertyListNode getPropertyListNode() {
        TreeTableRow<PropertyListNode> treeTableRow = getTreeTableRow();
        return treeTableRow != null ? treeTableRow.getItem() : null;
    }

    private PropertyListEntry getPropertyListEntry() {
        PropertyListNode propertyListNode = getPropertyListNode();
        return propertyListNode instanceof PropertyListEntry ? (PropertyListEntry) propertyListNode : null;
    }

    private PropertyListUINode getUINode() {
        return this.propertyListViewer.getNodeUI(getPropertyListNode());
    }

    private String getItemText() {
        PropertyListEntry propertyListEntry = getPropertyListEntry();
        return propertyListEntry != null ? propertyListEntry.getValue() : null;
    }

    @Override
    public void startEdit() {
        if (!isEditable()
                || !getTreeTableView().isEditable()
                || !getTableColumn().isEditable()) {
            return; // Regular JavaFX checks.
        }

        // If editing is not allowed on the property list entry, don't allow editing!
        PropertyListEntry propertyListEntry = getPropertyListEntry();
        if (propertyListEntry == null || !propertyListEntry.isEditingAllowed())
            return;

        // If the editing callbacks and data aren't ready yet, that means call the hook to set things up.
        PropertyListUINode uiNode = getUINode();
        if (uiNode != null && !uiNode.isEditorDataSetup()) {
            uiNode.setupEditor();
            return;
        }

        super.startEdit(); // Start editing the usual way.

        if (isEditing()) {
            if (this.textField == null)
                this.textField = createTextField();

            this.textField.setStyle(null);
            this.textField.setText(getItemText());
            this.setText(null);

            this.setGraphic(this.textField);
            this.textField.selectAll();

            // requesting focus so that key input can immediately go into the
            // TextField (see RT-28132)
            this.textField.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        this.setText(getItemText());
        this.setGraphic(null);
        PropertyListUINode uiNode = getUINode();
        if (uiNode != null)
            uiNode.onEditorShutdown();
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (this.isEmpty()) {
            this.setText(null);
            this.setGraphic(null);
        } else if (this.isEditing()) {
            if (this.textField != null) {
                this.textField.setText(getItemText());
                this.textField.setStyle(null);
            }

            this.setText(null);
            this.setGraphic(this.textField);
        } else {
            this.setText(getItemText());
            this.setGraphic(null);
        }
    }

    private TextField createTextField() {
        final TextField textField = new TextField(getItemText());

        // Use onAction here rather than onKeyReleased (with check for Enter),
        // as otherwise we encounter RT-34685
        textField.setOnAction(event -> {
            TextField field = (TextField) event.getSource();
            String text = field.getText();
            PropertyListUINode uiNode = getUINode();
            if (uiNode == null)
                return;

            event.consume();

            if (!uiNode.validate(text)) {
                field.setStyle(FXUtils.STYLE_LIST_CELL_RED_BACKGROUND);
                return;
            }

            try {
                uiNode.getNewValueHandler().accept(uiNode, text);
                this.commitEdit(text); // Commit the edit.
                uiNode.onEditorShutdown();
            } catch (Throwable th) {
                field.setStyle(FXUtils.STYLE_LIST_CELL_RED_BACKGROUND);
                Utils.handleError(uiNode.getEntry().getLogger(), th, true, "Failed to apply '%s' to the property.", text);
            }
        });

        textField.textProperty().addListener((observable, oldText, newText) -> {
            PropertyListUINode uiNode = getUINode();
            if (uiNode != null)
                this.textField.setStyle(uiNode.validate(newText) ? null : FXUtils.STYLE_LIST_CELL_RED_BACKGROUND);
        });

        textField.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                this.cancelEdit();
                event.consume();
            }
        });

        return textField;
    }
}
