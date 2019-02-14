package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;

import java.io.File;
import java.nio.file.Files;

/**
 * A temporary WAD Controller. This is temporary.
 * Created by Kneesnap on 9/30/2018.
 */
public class WADController extends EditorController<WADFile> {
    @FXML private ListView<WADEntry> entryList;

    private WADEntry selectedEntry;

    @Override
    public void loadFile(WADFile file) {
        super.loadFile(file);

        ObservableList<WADEntry> wadEntries = FXCollections.observableArrayList(file.getFiles());
        entryList.setItems(wadEntries);
        entryList.setCellFactory(param -> new AttachmentListCell());

        entryList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedEntry = newValue;
            this.updateEntry();
        });

        entryList.getSelectionModel().select(0);
    }

    @FXML
    @SneakyThrows
    private void importEntry(ActionEvent event) {
        File selectedFile = Utils.promptFileOpen("Select the WAD Entry to import...", "All Files", "*");
        if (selectedFile == null)
            return; // Cancelled.

        WADFile.CURRENT_FILE_NAME = selectedEntry.getFileEntry().getDisplayName();
        byte[] newBytes = Files.readAllBytes(selectedFile.toPath());
        this.selectedEntry.setFile(getFile().getMWD().replaceFile(newBytes, selectedEntry.getFileEntry(), selectedEntry.getFile()));

        updateEntry(); // Update the display.
        WADFile.CURRENT_FILE_NAME = null;
        System.out.println("Imported WAD Entry.");
    }

    @FXML
    @SneakyThrows
    private void exportEntry(ActionEvent event) {
        File selectedFile = Utils.promptFileSave("Specify the file to export this entry as...", this.selectedEntry.getFileEntry().getDisplayName(), null, null);
        if (selectedFile == null)
            return; // Cancelled.

        WADFile.CURRENT_FILE_NAME = selectedEntry.getFileEntry().getDisplayName();
        DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
        this.selectedEntry.getFile().save(writer);
        writer.closeReceiver();
        WADFile.CURRENT_FILE_NAME = null;
    }

    @FXML
    @SneakyThrows
    private void exportAll(ActionEvent event) {
        File selectedFolder = Utils.promptChooseDirectory("Select the directory to export WAD contents to.", true);
        if (selectedFolder == null)
            return; // Cancelled.

        for (WADEntry wadEntry : getFile().getFiles()) {
            FileEntry fileEntry = wadEntry.getFileEntry();
            WADFile.CURRENT_FILE_NAME = fileEntry.getDisplayName();

            File save = Utils.getNonExistantFile(new File(selectedFolder, fileEntry.getDisplayName()));
            System.out.println("Saving: " + fileEntry.getDisplayName());

            DataWriter writer = new DataWriter(new FileReceiver(save));
            wadEntry.getFile().save(writer);
            writer.closeReceiver();
        }
        WADFile.CURRENT_FILE_NAME = null;
    }

    @FXML
    private void editSelectedFile(ActionEvent event) {
        selectedEntry.getFile().handleWadEdit(getFile());
    }

    private static class AttachmentListCell extends ListCell<WADEntry> {
        @Override
        public void updateItem(WADEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            setText(empty ? null : entry.getFileEntry().getDisplayName());
        }
    }

    private void updateEntry() {

    }
}
