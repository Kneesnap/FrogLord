package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.gui.GUIMain;

import java.io.File;
import java.io.IOException;
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
    private void importEntry(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the WAD Entry to import...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*.*"));
        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFile = fileChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedFile == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFile.getParentFile());
        try {
            byte[] newBytes = Files.readAllBytes(selectedFile.toPath());
            this.selectedEntry.setFile(getFile().getParentMWD().replaceFile(newBytes, selectedEntry.getFileEntry(), selectedEntry.getFile()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        updateEntry(); // Update the display.
        System.out.println("Imported WAD Entry.");
    }

    @FXML
    private void exportEntry(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify the file to export this entry as...");
        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());
        fileChooser.setInitialFileName(this.selectedEntry.getFileEntry().getDisplayName());

        File selectedFile = fileChooser.showSaveDialog(GUIMain.MAIN_STAGE);
        if (selectedFile == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFile.getParentFile());
        try {
            DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
            this.selectedEntry.getFile().save(writer);
            writer.closeReceiver();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void exportAll(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select the directory to export WAD contents to.");
        chooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFolder = chooser.showDialog(GUIMain.MAIN_STAGE);
        if (selectedFolder == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFolder);

        try {
            for (WADEntry wadEntry : getFile().getFiles()) {
                FileEntry fileEntry = wadEntry.getFileEntry();
                File save = Utils.getNonExistantFile(new File(selectedFolder, fileEntry.hasFilePath() ? fileEntry.getDisplayName() : String.valueOf(wadEntry.getResourceId())));
                System.out.println("Saving: " + fileEntry.getDisplayName());

                DataWriter writer = new DataWriter(new FileReceiver(save));
                wadEntry.getFile().save(writer);
                writer.closeReceiver();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
