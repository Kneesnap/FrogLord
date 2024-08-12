package net.highwayfrogs.editor.games.renderware.ui;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewEntryTreeCell;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows the exploration of a RenderWare stream file.
 * TODO: I'd like to make it so the RwStreamChunk can optionally provide a UI controller. When that happens, we'll add a separator on the right-side, and add the provided UI controller under the property list.
 * Created by Kneesnap on 8/11/2024.
 */
public class RenderWareStreamEditorUIController<TGameInstance extends GameInstance> extends GameUIController<TGameInstance> {
    private final PropertyListViewerComponent<TGameInstance> propertyListViewer;
    @FXML private VBox leftSidePanelFreeArea;
    @FXML private HBox leftSidePanelTopBox;
    @FXML private HBox contentBox;
    @FXML private ImageView iconImageView;
    @FXML private Label fileNameLabel;
    @FXML private TreeView<RwStreamChunk> treeView;
    @FXML private HBox extraOptionsBox; // This is where we should put UI for stuff like a button to view the World.
    private final RwStreamFile streamFile;
    private RwStreamChunk shownChunk;

    private static final URL FXML_TEMPLATE_URL = Utils.getResourceURL("fxml/edit-file-renderware-stream.fxml");
    private static final FXMLLoader FXML_TEMPLATE_LOADER = new FXMLLoader(FXML_TEMPLATE_URL);

    @SuppressWarnings("unchecked")
    public RenderWareStreamEditorUIController(RwStreamFile streamFile) {
        super((TGameInstance) streamFile.getGameInstance());
        this.propertyListViewer = new PropertyListViewerComponent<>(getGameInstance());
        this.streamFile = streamFile;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.treeView.setCellFactory(treeViewParam -> new ContextMenuTreeCell());

        TreeItem<RwStreamChunk> treeRootNode = new TreeItem<>(null);
        treeRootNode.setExpanded(true);
        this.treeView.setRoot(treeRootNode);
        this.treeView.setFixedCellSize(Constants.RECOMMENDED_TREE_VIEW_FIXED_CELL_SIZE);
        this.treeView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
        createFxTreeItems(this.streamFile);

        if (this.contentBox != null) {
            TableView<NameValuePair> propertyListViewRootNode = this.propertyListViewer.getRootNode();
            HBox.setHgrow(propertyListViewRootNode, Priority.ALWAYS);
            this.propertyListViewer.bindSize();

            this.contentBox.getChildren().add(propertyListViewRootNode);
            addController(this.propertyListViewer);
        }
    }

    private void createFxTreeItems(RwStreamFile file) {
        this.treeView.getRoot().getChildren().clear();

        List<TreeItem<RwStreamChunk>> queue = new ArrayList<>();
        queue.add(this.treeView.getRoot());

        while (queue.size() > 0) {
            TreeItem<RwStreamChunk> node = queue.remove(0);
            RwStreamChunk chunk = node.getValue();

            List<RwStreamChunk> childChunks = chunk != null ? chunk.getChildChunks() : file.getChunks();
            ObservableList<TreeItem<RwStreamChunk>> fxChildNodes = node.getChildren();
            for (int i = 0; i < childChunks.size(); i++) {
                RwStreamChunk childChunk = childChunks.get(i);
                TreeItem<RwStreamChunk> newTreeItem = new TreeItem<>(childChunk);
                fxChildNodes.add(newTreeItem);
                if (!childChunk.getChildChunks().isEmpty())
                    queue.add(newTreeItem);
            }
        }
    }

    private void onSelectionChange(ObservableValue<? extends TreeItem<RwStreamChunk>> observableValue, TreeItem<RwStreamChunk> oldChunk, TreeItem<RwStreamChunk> newChunk) {
        showChunkEditor(newChunk != null ? newChunk.getValue() : null);
    }

    /**
     * Shows the editor UI for a particular chunk.
     * @param chunk The chunk to display.
     */
    public void showChunkEditor(RwStreamChunk chunk) {
        if (chunk == this.shownChunk)
            return;

        if (chunk != null && chunk.getStreamFile() != this.streamFile)
            throw new RuntimeException("The provided chunk is for a different RwStreamFile! (" + (chunk.getStreamFile() != null ? chunk.getStreamFile().getLocationName() : null) + ")");

        this.propertyListViewer.showProperties(chunk != null ? chunk.createPropertyList() : null);
        this.shownChunk = chunk;
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

    /**
     * Applies collection view entry display properties to a JavaFX TreeCell.
     * Created by Kneesnap on 8/9/2024.
     */
    private static class ContextMenuTreeCell extends CollectionViewEntryTreeCell<RwStreamChunk> {
        @Override
        public void updateItem(RwStreamChunk streamChunk, boolean empty) {
            super.updateItem(streamChunk, empty);
            if (empty) {
                setOnContextMenuRequested(null);
                return;
            }

            // NOTE: Make a more capable context menu system later.
            setOnContextMenuRequested(evt -> {
                ContextMenu contextMenu = new ContextMenu();

                MenuItem menuItem = new MenuItem("Export Raw Chunk Data");
                contextMenu.getItems().add(menuItem);
                menuItem.setOnAction(event -> {
                    File outputFile = Utils.promptFileSave(streamChunk.getGameInstance(), "Specify the file to save the chunk data as...", "raw-chunk-data", "Raw RenderWare Stream", "rawrws");
                    if (outputFile != null) {
                        try {
                            Files.write(outputFile.toPath(), streamChunk.getRawReadData());
                        } catch (IOException ex) {
                            Utils.handleError(streamChunk.getLogger(), ex, true, "Failed to save file '%s'.", outputFile);
                        }
                    }
                });

                contextMenu.show((Node) evt.getSource(), evt.getScreenX(), evt.getScreenY());
            });
        }
    }
}