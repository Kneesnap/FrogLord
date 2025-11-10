package net.highwayfrogs.editor.games.sony.shared.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListViewerComponent;

/**
 * Represents a file editor with a property list showing the properties of the file.
 * Created by Kneesnap on 7/22/2024.
 */
public class SCFileEditorPropertyListUIController<TGameInstance extends SCGameInstance, TGameFile extends SCGameFile<?>> extends SCFileEditorUIController<TGameInstance, TGameFile> {
    protected final PropertyListViewerComponent<TGameInstance> propertyListViewer;
    @Getter private final String fileTypeName;
    @FXML protected HBox contentBox;
    @Getter @FXML protected VBox fileDataBox;
    @FXML protected ImageView fileIconView;
    @FXML protected Label fileTypeLabel;
    @FXML protected Button backToWadButton;

    public SCFileEditorPropertyListUIController(TGameInstance instance, String fileTypeName) {
        super(instance);
        this.propertyListViewer = new PropertyListViewerComponent<>(instance);
        this.fileTypeName = fileTypeName;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        this.fileTypeLabel.setText(this.fileTypeName != null ? this.fileTypeName : "File");
        this.backToWadButton.setVisible(getParentWadFile() != null);
        if (this.contentBox != null) {
            Node propertyListViewRootNode = this.propertyListViewer.getRootNode();
            HBox.setHgrow(propertyListViewRootNode, Priority.ALWAYS);
            this.contentBox.getChildren().add(propertyListViewRootNode);
            addController(this.propertyListViewer);
        }
    }

    @Override
    public void setTargetFile(TGameFile languageFile) {
        super.setTargetFile(languageFile);

        // Clear display.
        this.propertyListViewer.clear();

        // If there's no file, abort!
        if (languageFile == null)
            return;

        this.fileIconView.setImage(languageFile.getCollectionViewIcon());
        this.propertyListViewer.showProperties(languageFile.createPropertyList());
    }

    @FXML
    private void onBackToWadButtonClick(ActionEvent event) {
        tryReturnToParentWadFile();
    }

    @Override
    public void setParentWadFile(WADFile parentWadFile) {
        super.setParentWadFile(parentWadFile);
        if (this.backToWadButton != null)
            this.backToWadButton.setVisible(parentWadFile != null);
    }
}

