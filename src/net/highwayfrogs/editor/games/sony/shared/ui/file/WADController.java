package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.DefaultFileUIController.IExtraUISupplier;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;

import java.io.File;

/**
 * A temporary WAD Controller. This is temporary.
 * Created by Kneesnap on 9/30/2018.
 */
public class WADController extends SCFileEditorUIController<SCGameInstance, WADFile> {
    @FXML private TableView<NameValuePair> tableFileData;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private ListView<WADEntry> entryList;
    @FXML private VBox rightSidePanelFreeArea;
    private WADEntry selectedEntry;
    private GameUIController<?> extraUIController;

    public WADController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(WADFile file) {
        super.setTargetFile(file);

        entryList.setItems(FXCollections.observableArrayList(file.getFiles()));
        updateEntryText();

        entryList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedEntry = newValue;
            this.updateEntry();
        });

        entryList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                event.consume();
                this.selectedEntry = entryList.getSelectionModel().getSelectedItem();
                this.updateEntry();
                this.editFile();
            }
        });

        entryList.getSelectionModel().select(0);
    }

    private void updateEntryText() {
        entryList.setCellFactory(null);
        entryList.setCellFactory(param -> new WADEntryListCell(this));
    }

    /**
     * Select the currently highlighted file in the entry list.
     * Silently fails if the file is not found.
     * @param file the file to select.
     */
    public void selectFile(SCGameFile<?> file) {
        int entryIndex = -1;
        for (int i = 0; i < getFile().getFiles().size(); i++)
            if (file == getFile().getFiles().get(i).getFile())
                entryIndex = i;

        if (entryIndex != -1) {
            this.entryList.getSelectionModel().select(entryIndex);
            this.entryList.scrollTo(entryIndex);
        }
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
            this.rightSidePanelFreeArea.getChildren().remove(this.extraUIController.getRootNode());
            removeController(this.extraUIController);
        }

        // Setup new extra UI controller.
        this.extraUIController = uiController;
        if (this.extraUIController != null && isActive()) {
            this.rightSidePanelFreeArea.getChildren().add(this.extraUIController.getRootNode());
            addController(this.extraUIController);
        }
    }

    @FXML
    @SneakyThrows
    private void importEntry(ActionEvent event) {
        this.selectedEntry.getFile().askUserToImportFile();
        updateEntry(); // Update the display.
        updateEntryText();
    }

    @FXML
    @SneakyThrows
    private void exportEntry(ActionEvent event) {
        this.selectedEntry.getFile().askUserToSaveToFile(false);
    }

    @FXML
    @SneakyThrows
    private void exportAll(ActionEvent event) {
        File selectedFolder = FXUtils.promptChooseDirectory(getGameInstance(), "Select the directory to export WAD contents to.", true);
        if (selectedFolder == null)
            return; // Cancelled.

        for (WADEntry wadEntry : getFile().getFiles()) {
            MWIResourceEntry resourceEntry = wadEntry.getFileEntry();

            File save = FileUtils.getNonExistantFile(new File(selectedFolder, resourceEntry.getDisplayName()));
            getLogger().info("Saving: %s", resourceEntry.getDisplayName());

            DataWriter writer = new DataWriter(new FileReceiver(save));
            wadEntry.getFile().save(writer);
            writer.closeReceiver();
        }
    }

    @FXML
    private void editSelectedFile(ActionEvent event) {
        this.editFile();
    }

    private void editFile() {
        selectedEntry.getFile().performDefaultUIAction();
    }

    private void updateEntry() {
        updateProperties();

        // Update extra UI.
        SCGameFile<?> selectedFile = this.selectedEntry != null ? this.selectedEntry.getFile() : null;
        setExtraUI(selectedFile instanceof IExtraUISupplier ? ((IExtraUISupplier) selectedFile).createExtraUIController() : null);
    }

    private void updateProperties() {
        // Setup and initialise the table view
        boolean hasEntry = (this.selectedEntry != null);
        this.tableFileData.setVisible(hasEntry);
        if (!hasEntry)
            return;

        this.tableFileData.getItems().clear();
        this.tableColumnFileDataName.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.tableColumnFileDataValue.setCellValueFactory(new PropertyValueFactory<>("value"));

        PropertyList properties = this.selectedEntry.getFile().createPropertyList();
        if (properties != null)
            properties.apply(this.tableFileData);
    }

    private static class WADEntryListCell extends ListCell<WADEntry> {
        private final WADController controller;
        private final EventHandler<? super MouseEvent> doubleClickHandler;
        private final EventHandler<? super ContextMenuEvent> rightClickHandler;

        @SuppressWarnings("unchecked")
        private WADEntryListCell(WADController controller) {
            this.controller = controller;
            this.doubleClickHandler = event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    event.consume();
                    WADEntry wadEntry = ((ListCell<WADEntry>) event.getSource()).getItem();
                    if (wadEntry != null) {
                        SCGameFile<?> gameFile = wadEntry.getFile();
                        if (gameFile != null)
                            gameFile.performDefaultUIAction();
                    }
                }
            };

            this.rightClickHandler = event -> {
                ContextMenu contextMenu = new ContextMenu();
                WADEntry wadEntry = ((ListCell<WADEntry>) event.getSource()).getItem();
                if (wadEntry != null && wadEntry.getFile() != null) {
                    wadEntry.getFile().setupRightClickMenuItems(contextMenu);
                    FXUtils.disableMnemonicParsing(contextMenu);
                    if (!contextMenu.getItems().isEmpty())
                        contextMenu.show((Node) event.getSource(), event.getScreenX(), event.getScreenY());
                }
            };
        }

        @Override
        public void updateItem(WADEntry wadEntry, boolean empty) {
            super.updateItem(wadEntry, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                setOnMouseClicked(null);
                setOnContextMenuRequested(null);
                return;
            }

            // Apply icon.
            SCGameFile<?> wadEntryFile = wadEntry.getFile();
            Image iconImage = wadEntryFile.getCollectionViewIcon();
            ImageView iconView = iconImage != null ? new ImageView(iconImage) : null;
            if (iconView != null) {
                iconView.setFitWidth(15);
                iconView.setFitHeight(15);
            }

            setGraphic(iconView);
            setOnMouseClicked(this.doubleClickHandler);
            setOnContextMenuRequested(this.rightClickHandler);

            // Update text.
            setStyle(wadEntryFile.getCollectionViewDisplayStyle());
            setText("[" + getIndex() + "/" + wadEntry.getResourceId() + "] " + wadEntry.getDisplayName());
        }
    }
}