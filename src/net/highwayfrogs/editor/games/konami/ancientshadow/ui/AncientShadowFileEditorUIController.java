package net.highwayfrogs.editor.games.konami.ancientshadow.ui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowGameFile;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent;

/**
 * Represents editor UI for an Ancient Shadow game file.
 * Created by Kneesnap on 8/4/2024.
 */
@Getter
public class AncientShadowFileEditorUIController<TGameFile extends AncientShadowGameFile> extends GameUIController<AncientShadowInstance> {
    @FXML private HBox contentBox;
    private TGameFile file;
    private final PropertyListViewerComponent<AncientShadowInstance> propertyListViewer;

    public AncientShadowFileEditorUIController(AncientShadowInstance instance) {
        super(instance);
        this.propertyListViewer = new PropertyListViewerComponent<>(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        if (this.contentBox != null) {
            Node propertyListViewRootNode = this.propertyListViewer.getRootNode();
            HBox.setHgrow(propertyListViewRootNode, Priority.ALWAYS);
            this.contentBox.getChildren().add(propertyListViewRootNode);
            addController(this.propertyListViewer);
        }
    }

    /**
     * Setup this window, by loading a GameFile to edit.
     * @param file The file to load and edit.
     */
    public void setTargetFile(TGameFile file) {
        TGameFile oldFile = this.file;
        if (oldFile != file) {
            this.file = file;
            this.propertyListViewer.showProperties(file != null ? file.createPropertyList() : null);
        }
    }
}