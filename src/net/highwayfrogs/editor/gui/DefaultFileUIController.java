package net.highwayfrogs.editor.gui;

import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.utils.FXUtils;
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
    @FXML private SplitPane mainSplitPane;
    @FXML private AnchorPane leftSideAnchorPane;
    @FXML private VBox leftSidePanelFreeArea;
    @FXML private HBox leftSidePanelTopBox;
    @FXML private HBox contentBox;
    @FXML private VBox rightSidePanelFreeArea;
    @FXML private ImageView iconImageView;
    @FXML private Label fileNameLabel;
    private TGameFile file;
    private Class<? extends TGameFile> fileClass;
    private final PropertyListViewerComponent<TGameInstance> propertyListViewer;
    private GameUIController<?> extraUIController;

    public static final String TEMPLATE_URL = "edit-file-default-template";

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

        if (this.mainSplitPane.getDividers().size() > 0) {
            DoubleProperty position = this.mainSplitPane.getDividers().get(0).positionProperty();
            position.set(.4D);
            this.contentBox.prefWidthProperty().bind(position);
            this.leftSidePanelFreeArea.prefWidthProperty().bind(position);
            this.leftSideAnchorPane.prefWidthProperty().bind(position);
            this.leftSidePanelTopBox.prefWidthProperty().bind(position);
        }

        if (this.rightSidePanelFreeArea != null) {
            HBox.setHgrow(this.rightSidePanelFreeArea, Priority.ALWAYS);

            Node propertyListViewRootNode = this.propertyListViewer.getRootNode();
            VBox.setVgrow(propertyListViewRootNode, Priority.ALWAYS);
            this.propertyListViewer.bindSize();
            getRightSidePanelFreeArea().getChildren().add(propertyListViewRootNode);
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
            onSelectedFileChange(oldFile, file);
        }
    }

    /**
     * Called when the selected file changes.
     * @param oldFile the previous file
     * @param newFile the new file
     */
    protected void onSelectedFileChange(TGameFile oldFile, TGameFile newFile) {
        this.propertyListViewer.showProperties(newFile != null ? newFile.createPropertyList() : null);
        setExtraUI(newFile instanceof IExtraUISupplier ? ((IExtraUISupplier) newFile).createExtraUIController() : null);
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
     * Sets the extra UI to display under the property list.
     * @param uiController the UI controller to apply as the extra UI.
     */
    public void setExtraUI(GameUIController<?> uiController) {
        if (this.extraUIController == uiController)
            return;

        // Remove existing extra UI controller.
        if (this.extraUIController != null) {
            getRightSidePanelFreeArea().getChildren().remove(this.extraUIController.getRootNode());
            removeController(this.extraUIController);
        }

        // Setup new extra UI controller.
        this.extraUIController = uiController;
        if (this.extraUIController != null && isActive()) {
            getRightSidePanelFreeArea().getChildren().add(this.extraUIController.getRootNode());
            addController(this.extraUIController);
        }
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
            FXMLLoader templateLoader = FXUtils.getFXMLTemplateLoader(gameInstance, template);
            GameUIController.loadController(gameInstance, templateLoader, controller);
            controller.setTargetFile(fileToEdit);
        } catch (Throwable th) {
            Utils.handleError(fileToEdit.getLogger(), th, true, "Failed to create editor UI.");
        }

        return controller;
    }

    /**
     * Supplies an extra UI, usually under a property list.
     */
    public interface IExtraUISupplier {
        /**
         * Creates a UI controller for the extra UI display.
         */
        GameUIController<?> createExtraUIController();
    }
}