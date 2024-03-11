package net.highwayfrogs.editor.gui.editor;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents a side panel, commonly used in MeshViewController.
 * Created by Kneesnap on 1/23/2024.
 */
public class UISidePanel {
    @Getter private final Accordion parent;
    @Getter private final boolean insertFirst;
    @Getter private final VBox elementRoot;
    @Getter private final AnchorPane anchorPane;
    @Getter private final ScrollPane scrollPane;
    @Getter private final TitledPane titledPane;
    @Getter private boolean visible;

    public UISidePanel(Accordion parent, boolean insertFirst, String title) {
        this.parent = parent;
        this.insertFirst = insertFirst;
        this.elementRoot = new VBox();

        this.anchorPane = new AnchorPane(this.elementRoot);
        AnchorPane.setBottomAnchor(this.elementRoot, 0D);
        AnchorPane.setLeftAnchor(this.elementRoot, 0D);
        AnchorPane.setRightAnchor(this.elementRoot, 0D);
        AnchorPane.setTopAnchor(this.elementRoot, 0D);

        this.scrollPane = new ScrollPane(this.anchorPane);
        this.scrollPane.setFitToWidth(true);
        this.scrollPane.setPadding(new Insets(8, 8, 8, 8));

        this.titledPane = new TitledPane(title, this.scrollPane);
        this.titledPane.setAnimated(false);
    }

    /**
     * Adds a Node to the side panel.
     * @param node the node to add
     */
    public void add(Node node) {
        this.elementRoot.getChildren().add(node);
    }

    /**
     * Creates an editor grid, adding it to the side panel.
     * @return editorGrid
     */
    public GUIEditorGrid makeEditorGrid() {
        GridPane gridPane = GUIEditorGrid.createDefaultPane();
        this.elementRoot.getChildren().add(gridPane);
        return new GUIEditorGrid(gridPane);
    }

    /**
     * Sets the visibility state of the UISidePanel.
     * @param newState New visibility state.
     */
    public void setVisible(boolean newState) {
        if (this.visible == newState)
            return;

        this.visible = newState;
        if (newState) {
            if (this.insertFirst) {
                this.parent.getPanes().add(0, this.titledPane);
            } else {
                this.parent.getPanes().add(this.titledPane);
            }
        } else {
            this.parent.getPanes().remove(this.titledPane);
        }
    }

    /**
     * Request the panel be expanded and focused.
     */
    public void requestFocus() {
        if (!isVisible())
            setVisible(true);

        this.titledPane.requestFocus();
        this.parent.setExpandedPane(this.titledPane);
    }
}
