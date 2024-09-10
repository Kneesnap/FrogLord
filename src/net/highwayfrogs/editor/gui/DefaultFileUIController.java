package net.highwayfrogs.editor.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents editor UI for a GameFile.
 * This is the standard editor by which most file editor UIs can extend from.
 * Created by Kneesnap on 5/8/2024.
 */
@Getter
public class DefaultFileUIController<TGameInstance extends GameInstance, TGameFile extends GameObject<?> & IPropertyListCreator> extends GameUIController<TGameInstance> {
    private final Image icon;
    private final String fileNameText;
    @FXML private VBox leftSidePanelFreeArea;
    @FXML private HBox leftSidePanelTopBox;
    @FXML private HBox contentBox;
    @FXML private ImageView iconImageView;
    @FXML private Label fileNameLabel;
    private TGameFile file;
    private Class<? extends TGameFile> fileClass;
    private final PropertyListViewerComponent<TGameInstance> propertyListViewer;

    private static final String TEMPLATE_URL = "edit-file-default-template";

    public DefaultFileUIController(TGameInstance instance, String fileNameText, Image icon) {
        super(instance);
        this.icon = icon;
        this.fileNameText = fileNameText;
        this.propertyListViewer = new PropertyListViewerComponent<>(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        if (this.iconImageView != null)
            this.iconImageView.setImage(this.icon != null ? this.icon : ImageResource.QUESTION_MARK_32.getFxImage());
        if (this.fileNameLabel != null)
            this.fileNameLabel.setText(this.fileNameText != null ? this.fileNameText : "Unnamed File Type");

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

    @Override
    @SuppressWarnings("unchecked")
    public boolean trySetTargetFile(GameObject<?> file) {
        if ((this.fileClass != null && this.fileClass.isInstance(file))) {
            setTargetFile((TGameFile) file);
            return true;
        }

        return super.trySetTargetFile(file);
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameInstance extends GameInstance, TGameFile extends GameObject<?> & IPropertyListCreator, TUIController extends DefaultFileUIController<TGameInstance, TGameFile>> TUIController loadEditor(TGameInstance gameInstance, TUIController controller, TGameFile fileToEdit) {
        return loadEditor(gameInstance, TEMPLATE_URL, controller, fileToEdit);
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param template the gui layout template
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameInstance extends GameInstance, TGameFile extends GameObject<?> & IPropertyListCreator, TUIController extends DefaultFileUIController<TGameInstance, TGameFile>> TUIController loadEditor(TGameInstance gameInstance, String template, TUIController controller, TGameFile fileToEdit) {
        try {
            FXMLLoader templateLoader = Utils.getFXMLTemplateLoader(gameInstance, template);
            GameUIController.loadController(gameInstance, templateLoader, controller);
            controller.setTargetFile(fileToEdit);
        } catch (Throwable th) {
            Utils.handleError(fileToEdit.getLogger(), th, true, "Failed to create editor UI.");
        }

        return controller;
    }

}