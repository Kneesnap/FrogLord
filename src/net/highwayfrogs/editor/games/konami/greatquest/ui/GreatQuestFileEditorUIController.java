package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestConfig;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent;

/**
 * Represents editor UI for a GreatQuestArchiveFile.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestFileEditorUIController<TGameFile extends GreatQuestArchiveFile> extends GameUIController<GreatQuestInstance> {
    @FXML private HBox contentBox;
    private TGameFile file;
    private Class<? extends TGameFile> fileClass;
    private final PropertyListViewerComponent<GreatQuestInstance> propertyListViewer;

    public GreatQuestFileEditorUIController(GreatQuestInstance instance) {
        super(instance);
        this.propertyListViewer = new PropertyListViewerComponent<>(instance);
    }

    @Override
    public GreatQuestConfig getConfig() {
        return (GreatQuestConfig) super.getConfig();
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
    @SuppressWarnings("unchecked")
    public void setTargetFile(TGameFile file) {
        TGameFile oldFile = this.file;
        if (oldFile != file) {
            if (file != null && (this.fileClass == null || file.getClass().isAssignableFrom(this.fileClass)))
                this.fileClass = (Class<? extends TGameFile>) file.getClass();

            this.file = file;
            this.propertyListViewer.showProperties(file != null ? file.createPropertyList() : null);
        }
    }
}