package net.highwayfrogs.editor.games.shared.basic.ui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.IBasicGameFile;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListViewerComponent;

/**
 * Represents editor UI for a basic game file.
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public class BasicFileEditorUIController<TGameFile extends IBasicGameFile, TGameInstance extends BasicGameInstance> extends GameUIController<TGameInstance> {
    @FXML private HBox contentBox;
    private TGameFile file;
    private final PropertyListViewerComponent<TGameInstance> propertyListViewer;

    public BasicFileEditorUIController(TGameInstance instance) {
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