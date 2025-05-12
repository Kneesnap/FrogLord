package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.games.sony.shared.utils.FileUtils3D;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;

import java.io.File;
import java.nio.file.Files;

/**
 * A temporary WAD Controller. This is temporary.
 * Created by Kneesnap on 9/30/2018.
 */
public class WADController extends SCFileEditorUIController<SCGameInstance, WADFile> {
    @FXML private TableView<NameValuePair> tableFileData;
    @FXML private TableColumn<Object, Object> tableColumnFileDataName;
    @FXML private TableColumn<Object, Object> tableColumnFileDataValue;
    @FXML private ListView<WADEntry> entryList;
    private WADEntry selectedEntry;

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

    @FXML
    @SneakyThrows
    private void importEntry(ActionEvent event) {
        File selectedFile = FXUtils.promptFileOpenExtensions(getGameInstance(), "Select the WAD Entry to import...", "Usable Files", "mm3d", "VLO", "XAR", "XMR", "*");
        if (selectedFile == null)
            return; // Cancelled.

        String fileName = selectedFile.getName().toLowerCase();
        byte[] newBytes = Files.readAllBytes(selectedFile.toPath());

        if (fileName.endsWith(".mm3d")) {
            SCGameFile<?> selectFile = this.selectedEntry.getFile();
            if (!(selectFile instanceof MRModel)) {
                FXUtils.makePopUp("You cannot import a model over a " + (selectFile != null ? selectFile.getClass().getSimpleName() : "null") + ".", AlertType.ERROR);
                return;
            }

            MRModel model = (MRModel) selectFile;
            MisfitModel3DObject newObject = new MisfitModel3DObject();
            DataReader reader = new DataReader(new ArraySource(newBytes));

            try {
                newObject.load(reader);
            } catch (Exception ex) {
                FXUtils.makeErrorPopUp("There was an error loading the mm3d model.", ex, true);
                return;
            }

            try {
                FileUtils3D.importMofFromModel(newObject, model);
            } catch (Exception ex) {
                FXUtils.makeErrorPopUp("An error occurred when importing the model.", ex, true);
                return;
            }
        } else if (fileName.endsWith(".vlo") || fileName.endsWith(".xar") || fileName.endsWith(".xmr")) {
            getFile().getArchive().replaceFile(fileName, newBytes, this.selectedEntry.getFileEntry(), this.selectedEntry.getFile(), true);
        } else {
            FXUtils.makePopUp("Don't know how to import this file type. Aborted.", AlertType.WARNING);
            return;
        }

        getLogger().info("Imported WAD Entry.");
        updateEntry(); // Update the display.
        updateEntryText();
    }

    @FXML
    @SneakyThrows
    private void exportEntry(ActionEvent event) {
        File selectedFile = FXUtils.promptFileSave(getGameInstance(), "Specify the file to export this entry as...", this.selectedEntry.getFileEntry().getDisplayName(), null, null);
        if (selectedFile == null)
            return; // Cancelled.

        DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
        this.selectedEntry.getFile().save(writer);
        writer.closeReceiver();
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
        selectedEntry.getFile().handleWadEdit(getFile());
    }

    private void updateEntry() {
        updateProperties();
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
                            gameFile.handleWadEdit(this.controller.getFile());
                    }
                }
            };

            this.rightClickHandler = event -> {
                ContextMenu contextMenu = new ContextMenu();
                WADEntry wadEntry = ((ListCell<WADEntry>) event.getSource()).getItem();
                if (wadEntry != null && wadEntry.getFile() != null) {
                    wadEntry.getFile().setupRightClickMenuItems(contextMenu);
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