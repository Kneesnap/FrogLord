package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.NameValuePair;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.utils.FileUtils3D;
import net.highwayfrogs.editor.utils.Utils;

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

        entryList.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                this.selectedEntry = entryList.getSelectionModel().getSelectedItem();
                this.updateEntry();
                this.editFile();
            }
        });

        entryList.getSelectionModel().select(0);
    }

    private void updateEntryText() {
        entryList.setCellFactory(null);
        entryList.setCellFactory(param -> new WADEntryListCell());
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
        File selectedFile = Utils.promptFileOpenExtensions(getGameInstance(), "Select the WAD Entry to import...", "Usable Files", "mm3d", "VLO", "XAR", "XMR", "*");
        if (selectedFile == null)
            return; // Cancelled.

        String fileName = selectedFile.getName().toLowerCase();
        byte[] newBytes = Files.readAllBytes(selectedFile.toPath());

        if (fileName.endsWith(".mm3d")) {
            SCGameFile<?> selectFile = this.selectedEntry.getFile();
            if (!(selectFile instanceof MOFHolder)) {
                Utils.makePopUp("You cannot import a model over a " + (selectFile != null ? selectFile.getClass().getSimpleName() : "null") + ".", AlertType.ERROR);
                return;
            }

            MOFHolder mofHolder = (MOFHolder) selectFile;
            MisfitModel3DObject newObject = new MisfitModel3DObject();
            DataReader reader = new DataReader(new ArraySource(newBytes));

            try {
                newObject.load(reader);
            } catch (Exception ex) {
                Utils.makeErrorPopUp("There was an error loading the mm3d model.", ex, true);
                return;
            }

            try {
                FileUtils3D.importMofFromModel(newObject, mofHolder);
            } catch (Exception ex) {
                Utils.makeErrorPopUp("An error occurred when importing the model.", ex, true);
                return;
            }
        } else if (fileName.endsWith(".vlo") || fileName.endsWith(".xar") || fileName.endsWith(".xmr")) {
            this.selectedEntry.setFile(getFile().getArchive().replaceFile(newBytes, this.selectedEntry.getFileEntry(), this.selectedEntry.getFile(), true));
        } else {
            Utils.makePopUp("Don't know how to import this file type. Aborted.", AlertType.WARNING);
            return;
        }

        getLogger().info("Imported WAD Entry.");
        updateEntry(); // Update the display.
        updateEntryText();
    }

    @FXML
    @SneakyThrows
    private void exportEntry(ActionEvent event) {
        File selectedFile = Utils.promptFileSave(getGameInstance(), "Specify the file to export this entry as...", this.selectedEntry.getFileEntry().getDisplayName(), null, null);
        if (selectedFile == null)
            return; // Cancelled.

        DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
        this.selectedEntry.getFile().save(writer);
        writer.closeReceiver();
    }

    @FXML
    @SneakyThrows
    private void exportAll(ActionEvent event) {
        File selectedFolder = Utils.promptChooseDirectory(getGameInstance(), "Select the directory to export WAD contents to.", true);
        if (selectedFolder == null)
            return; // Cancelled.

        for (WADEntry wadEntry : getFile().getFiles()) {
            MWIResourceEntry resourceEntry = wadEntry.getFileEntry();

            File save = Utils.getNonExistantFile(new File(selectedFolder, resourceEntry.getDisplayName()));
            getLogger().info("Saving: " + resourceEntry.getDisplayName());

            DataWriter writer = new DataWriter(new FileReceiver(save));
            wadEntry.getFile().save(writer);
            writer.closeReceiver();
        }
    }

    @FXML
    @SneakyThrows
    private void exportAlternate(ActionEvent event) {
        this.selectedEntry.getFile().exportAlternateFormat();
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
        @Override
        public void updateItem(WADEntry wadEntry, boolean empty) {
            super.updateItem(wadEntry, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
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

            // Update text.
            setStyle(wadEntryFile.getCollectionViewDisplayStyle());
            setText("[" + getIndex() + "/" + wadEntry.getResourceId() + "] " + wadEntry.getDisplayName());
        }
    }
}