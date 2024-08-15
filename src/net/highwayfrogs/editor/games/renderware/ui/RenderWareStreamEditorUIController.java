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
 * Created by Kneesnap on 8/11/2024.
 */
public class RenderWareStreamEditorUIController<TGameInstance extends GameInstance> extends GameUIController<TGameInstance> {
    private final PropertyListViewerComponent<TGameInstance> propertyListViewer;
    @FXML private VBox leftSidePanelFreeArea;
    @FXML private HBox leftSidePanelTopBox;
    @FXML private HBox contentBox;
    @FXML private ImageView iconImageView;
    @FXML private Label fileNameLabel;
    @FXML private TreeView<IRwStreamSectionUIEntry> treeView;
    @FXML private HBox extraOptionsBox; // This is where we should put UI for stuff like a button to view the World.
    @FXML private VBox rightSidePanelFreeArea;
    private Separator rightSideSeparator;
    private GameUIController<?> sectionUiController;
    private final RwStreamFile streamFile;
    private IRwStreamSectionUIEntry shownChunk;

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

        TreeItem<IRwStreamSectionUIEntry> treeRootNode = new TreeItem<>(null);
        treeRootNode.setExpanded(true);
        this.treeView.setRoot(treeRootNode);
        this.treeView.setFixedCellSize(Constants.RECOMMENDED_TREE_VIEW_FIXED_CELL_SIZE);
        this.treeView.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChange);
        createFxTreeItems(this.streamFile);

        if (this.rightSidePanelFreeArea != null) {
            TableView<NameValuePair> propertyListViewRootNode = this.propertyListViewer.getRootNode();
            HBox.setHgrow(propertyListViewRootNode, Priority.ALWAYS);
            this.propertyListViewer.bindSize();

            this.rightSidePanelFreeArea.getChildren().add(propertyListViewRootNode);
            addController(this.propertyListViewer);
        }
    }

    private void createFxTreeItems(RwStreamFile file) {
        this.treeView.getRoot().getChildren().clear();

        List<TreeItem<IRwStreamSectionUIEntry>> queue = new ArrayList<>();
        queue.add(this.treeView.getRoot());

        while (queue.size() > 0) {
            TreeItem<IRwStreamSectionUIEntry> node = queue.remove(0);
            IRwStreamSectionUIEntry chunk = node.getValue();

            List<? extends IRwStreamSectionUIEntry> childChunks = chunk != null ? chunk.getChildUISections() : file.getChunks();
            ObservableList<TreeItem<IRwStreamSectionUIEntry>> fxChildNodes = node.getChildren();
            for (int i = 0; i < childChunks.size(); i++) {
                IRwStreamSectionUIEntry childChunk = childChunks.get(i);
                TreeItem<IRwStreamSectionUIEntry> newTreeItem = new TreeItem<>(childChunk);
                fxChildNodes.add(newTreeItem);
                if (!childChunk.getChildUISections().isEmpty())
                    queue.add(newTreeItem);
            }
        }
    }

    private void onSelectionChange(ObservableValue<? extends TreeItem<IRwStreamSectionUIEntry>> observableValue, TreeItem<IRwStreamSectionUIEntry> oldChunk, TreeItem<IRwStreamSectionUIEntry> newChunk) {
        showChunkEditor(newChunk != null ? newChunk.getValue() : null);
    }

    /**
     * Shows the editor UI for a particular chunk.
     * @param chunk The chunk to display.
     */
    public void showChunkEditor(IRwStreamSectionUIEntry chunk) {
        if (chunk == this.shownChunk)
            return;

        if (chunk != null && chunk.getStreamFile() != this.streamFile)
            throw new RuntimeException("The provided chunk is for a different RwStreamFile! (" + (chunk.getStreamFile() != null ? chunk.getStreamFile().getLocationName() : null) + ")");

        this.propertyListViewer.showProperties(chunk != null ? chunk.createPropertyList() : null);
        this.shownChunk = chunk;

        GameUIController<?> newUiController = chunk != null ? chunk.makeEditorUI() : null;

        // Setup UI.
        if (this.sectionUiController != null) {
            this.rightSidePanelFreeArea.getChildren().remove(this.sectionUiController.getRootNode());
            this.removeController(this.sectionUiController);
            this.sectionUiController = null;
        }

        if (this.rightSideSeparator != null && newUiController == null) {
            this.rightSidePanelFreeArea.getChildren().remove(this.rightSideSeparator);
            this.rightSideSeparator = null;
        } else if (this.rightSideSeparator == null && newUiController != null) {
            this.rightSideSeparator = new Separator(Orientation.HORIZONTAL);
            this.rightSidePanelFreeArea.getChildren().add(this.rightSideSeparator);
        }

        if (newUiController != null) {
            this.sectionUiController = newUiController;
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

    /**
     * Applies collection view entry display properties to a JavaFX TreeCell.
     * Created by Kneesnap on 8/9/2024.
     */
    private static class ContextMenuTreeCell extends CollectionViewEntryTreeCell<IRwStreamSectionUIEntry> {
        @Override
        public void updateItem(IRwStreamSectionUIEntry streamChunk, boolean empty) {
            super.updateItem(streamChunk, empty);
            if (empty) {
                setOnContextMenuRequested(null);
                return;
            }

            // NOTE: Make a more capable context menu system later.
            setOnContextMenuRequested(evt -> {
                ContextMenu contextMenu = new ContextMenu();

                if (streamChunk instanceof RwStreamChunk) {
                    RwStreamChunk streamSection = (RwStreamChunk) streamChunk;
                    MenuItem menuItem = new MenuItem("Export Raw Chunk Data");
                    contextMenu.getItems().add(menuItem);
                    menuItem.setOnAction(event -> {
                        File outputFile = Utils.promptFileSave(streamSection.getGameInstance(), "Specify the file to save the chunk data as...", "raw-chunk-data", "Raw RenderWare Stream", "rawrws");
                        if (outputFile != null) {
                            try {
                                Files.write(outputFile.toPath(), streamSection.getRawReadData());
                            } catch (IOException ex) {
                                Utils.handleError(streamSection.getLogger(), ex, true, "Failed to save file '%s'.", outputFile);
                            }
                        }
                    });
                }

                if (!contextMenu.getItems().isEmpty())
                    contextMenu.show((Node) evt.getSource(), evt.getScreenX(), evt.getScreenY());
            });
        }
    }
}