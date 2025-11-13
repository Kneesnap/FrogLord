package net.highwayfrogs.editor.games.renderware.ui;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewEntryTreeCell;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListViewerComponent;
import net.highwayfrogs.editor.utils.FileUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows the exploration of a RenderWare stream file.
 * Created by Kneesnap on 8/11/2024.
 */
public class RenderWareStreamEditorUIController<TGameInstance extends GameInstance> extends GameUIController<TGameInstance> {
    private final PropertyListViewerComponent<TGameInstance> propertyListViewer;
    @FXML private VBox leftSidePanelFreeArea;
    @FXML private HBox leftSidePanelTopBox;
    @FXML private HBox contentBox;
    @FXML private ImageView iconImageView;
    @FXML private Label fileNameLabel;
    @FXML private TreeView<IRwStreamChunkUIEntry> treeView;
    @FXML private HBox extraOptionsBox; // This is where we should put UI for stuff like a button to view the World.
    @FXML private VBox rightSidePanelFreeArea;
    private Separator rightSideSeparator;
    private GameUIController<?> activeUiController;
    private final RwStreamFile streamFile;
    private IRwStreamChunkUIEntry shownChunk;

    private static final URL FXML_TEMPLATE_URL = FileUtils.getResourceURL("fxml/edit-file-renderware-stream.fxml");
    private static final FXMLLoader FXML_TEMPLATE_LOADER = new FXMLLoader(FXML_TEMPLATE_URL);

    @SuppressWarnings("unchecked")
    public RenderWareStreamEditorUIController(RwStreamFile streamFile) {
        super((TGameInstance) streamFile.getGameInstance());
        this.propertyListViewer = new PropertyListViewerComponent<>(getGameInstance());
        this.streamFile = streamFile;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.treeView.setCellFactory(treeViewParam -> new CollectionViewEntryTreeCell<>());

        TreeItem<IRwStreamChunkUIEntry> treeRootNode = new TreeItem<>(null);
        treeRootNode.setExpanded(true);
        this.treeView.setRoot(treeRootNode);
        this.treeView.setFixedCellSize(Constants.RECOMMENDED_TREE_VIEW_FIXED_CELL_SIZE);
        this.treeView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
        createFxTreeItems(this.streamFile);

        if (this.rightSidePanelFreeArea != null) {
            TreeTableView<PropertyListNode> propertyListViewRootNode = this.propertyListViewer.getRootNode();
            HBox.setHgrow(propertyListViewRootNode, Priority.ALWAYS);

            this.rightSidePanelFreeArea.getChildren().add(propertyListViewRootNode);
            addController(this.propertyListViewer);
        }
    }

    private void createFxTreeItems(RwStreamFile file) {
        this.treeView.getRoot().getChildren().clear();

        List<TreeItem<IRwStreamChunkUIEntry>> queue = new ArrayList<>();
        queue.add(this.treeView.getRoot());

        while (queue.size() > 0) {
            TreeItem<IRwStreamChunkUIEntry> node = queue.remove(0);
            IRwStreamChunkUIEntry chunk = node.getValue();

            List<? extends IRwStreamChunkUIEntry> childChunks = chunk != null ? chunk.getChildUISections() : file.getChunks();
            ObservableList<TreeItem<IRwStreamChunkUIEntry>> fxChildNodes = node.getChildren();
            for (int i = 0; i < childChunks.size(); i++) {
                IRwStreamChunkUIEntry childChunk = childChunks.get(i);
                TreeItem<IRwStreamChunkUIEntry> newTreeItem = new TreeItem<>(childChunk);
                fxChildNodes.add(newTreeItem);
                if (!childChunk.getChildUISections().isEmpty())
                    queue.add(newTreeItem);
            }
        }
    }

    private void onSelectionChange(ObservableValue<? extends TreeItem<IRwStreamChunkUIEntry>> observableValue, TreeItem<IRwStreamChunkUIEntry> oldChunk, TreeItem<IRwStreamChunkUIEntry> newChunk) {
        showChunkEditor(newChunk != null ? newChunk.getValue() : null);
    }

    /**
     * Shows the editor UI for a particular chunk.
     * @param chunk The chunk to display.
     */
    public void showChunkEditor(IRwStreamChunkUIEntry chunk) {
        if (chunk == this.shownChunk)
            return;

        if (chunk != null && chunk.getStreamFile() != this.streamFile)
            throw new RuntimeException("The provided chunk is for a different RwStreamFile! (" + (chunk.getStreamFile() != null ? chunk.getStreamFile().getLocationName() : null) + ")");

        this.propertyListViewer.showProperties(chunk != null ? chunk.createPropertyList() : null);
        this.shownChunk = chunk;

        GameUIController<?> newUiController = chunk != null ? chunk.makeEditorUI() : null;

        // Setup UI.
        if (this.activeUiController != null) {
            this.rightSidePanelFreeArea.getChildren().remove(this.activeUiController.getRootNode());
            this.removeController(this.activeUiController);
            this.activeUiController = null;
        }

        if (this.rightSideSeparator != null && newUiController == null) {
            this.rightSidePanelFreeArea.getChildren().remove(this.rightSideSeparator);
            this.rightSideSeparator = null;
        } else if (this.rightSideSeparator == null && newUiController != null) {
            this.rightSideSeparator = new Separator(Orientation.HORIZONTAL);
            this.rightSidePanelFreeArea.getChildren().add(this.rightSideSeparator);
        }

        if (newUiController != null) {
            this.activeUiController = newUiController;
            this.rightSidePanelFreeArea.getChildren().add(newUiController.getRootNode());
            this.addController(newUiController);
        }
    }

    /**
     * Loads the UI controller for the given file.
     * @param instance the game instance to create the ui template from
     * @param streamFile the stream file to display
     * @param <TGameInstance> the type of game instance
     * @return controller
     */
    public static <TGameInstance extends GameInstance> RenderWareStreamEditorUIController<TGameInstance> loadController(TGameInstance instance, RwStreamFile streamFile) {
        if (instance != streamFile.getGameInstance())
            throw new RuntimeException("The provided game instance did not match the stream file's game instance.");

        return loadController(instance, FXML_TEMPLATE_LOADER, new RenderWareStreamEditorUIController<>(streamFile));
    }
}