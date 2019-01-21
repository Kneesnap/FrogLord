package net.highwayfrogs.editor.gui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import net.highwayfrogs.editor.Constants;

/**
 * Creates an editor grid.
 * Created by Kneesnap on 1/20/2019.
 */
public class GUIEditorGrid {
    private GridPane gridPane;
    private int rowIndex;

    public GUIEditorGrid(GridPane pane) {
        this.gridPane = pane;
    }

    /**
     * Clear everything from this editor.
     */
    public void clearEditor() {
        this.rowIndex = 0;
        gridPane.getChildren().clear();
        gridPane.getRowConstraints().clear();
    }

    /**
     * Add a label.
     * @param label The label to add.
     */
    public void addLabel(String label) {
        Label labelText = new Label(label);
        GridPane.setColumnSpan(labelText, 2);
        labelText.setFont(Constants.SYSTEM_BOLD_FONT);

        gridPane.getChildren().add(setupNode(labelText));
        addRow(15);
    }

    /**
     * Add a text field.
     * @param label The field description.
     * @param value The field value.
     */
    public void addTextField(String label, String value) {
        Label labelText = new Label(label);
        TextField field = new TextField(value);
        GridPane.setColumnIndex(field, 1);

        gridPane.getChildren().addAll(setupNode(labelText), setupNode(field));
        addRow(25);
    }

    private Node setupNode(Node node) {
        GridPane.setRowIndex(node, this.rowIndex);
        return node;
    }

    private void addRow(double height) {
        RowConstraints newRow = new RowConstraints(height + 1);
        gridPane.getRowConstraints().add(newRow);
        this.rowIndex++;
    }
}
