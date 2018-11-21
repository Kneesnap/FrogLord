package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.gui.editor.EditorController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML private SplitPane mainSplitPane;
    @FXML private ListView<GameFile> fileList;
    @FXML private AnchorPane editorPane;
    @FXML private TextArea consoleText;
    private MWDFile mwdFile;

    public static MainController MAIN_WINDOW;
    @Getter @Setter private static EditorController<?> currentController;

    /**
     * Print a message to the console window.
     * @param message    The message to print.
     * @param formatting Any formatting to accompany the message.
     */
    public void printMessage(String message, Object... formatting) {
        if (consoleText != null)
            consoleText.appendText(String.format(message, formatting) + System.lineSeparator());
    }

    /**
     * Load a MWDFile as the active MWD being used.
     * @param file The MWD file to load.
     */
    public void loadMWD(MWDFile file) {
        this.mwdFile = file;

        ObservableList<GameFile> gameFiles = FXCollections.observableArrayList(mwdFile.getFiles());
        fileList.setItems(gameFiles);
        fileList.setCellFactory(param -> new AttachmentListCell(mwdFile));
        fileList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> openEditor(newValue));
        fileList.getSelectionModel().select(0);
    }

    /**
     * Get the current file.
     * @return currentFile
     */
    public GameFile getCurrentFile() {
        return this.fileList.getSelectionModel().getSelectedItem();
    }

    /**
     * Get the FileEntry associated with the selected file.
     * @return fileentry
     */
    public FileEntry getFileEntry() {
        return mwdFile.getEntryMap().get(getCurrentFile());
    }

    /**
     * Import a file to replace the current file.
     */
    public void importFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the file to import...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*.*"));
        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFile = fileChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedFile == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFile.getParentFile());

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(selectedFile.toPath());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read file.", ex);
        }

        GameFile oldFile = getCurrentFile();
        GameFile newFile = mwdFile.replaceFile(fileBytes, getFileEntry(), oldFile);

        this.mwdFile.getEntryMap().put(newFile, getFileEntry());
        int index = this.mwdFile.getFiles().indexOf(oldFile);
        this.mwdFile.getFiles().set(index, newFile);
        this.fileList.getItems().set(index, newFile);

        openEditor(newFile); // Open the editor for the new file.
        System.out.println("Imported " + selectedFile.getName() + " as " + getFileEntry().getDisplayName() + ".");
    }

    /**
     * Export the current file.
     */
    public void exportFile() {
        GameFile currentFile = getCurrentFile();
        FileEntry entry = getFileEntry();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify the file to export this file as...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*.*"));
        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());
        fileChooser.setInitialFileName(entry.getDisplayName());

        File selectedFile = fileChooser.showSaveDialog(GUIMain.MAIN_STAGE);
        if (selectedFile == null)
            return; // Cancel.

        GUIMain.setWorkingDirectory(selectedFile.getParentFile());

        try {
            if (selectedFile.exists()) // Don't merge files, create a new one.
                selectedFile.delete();

            DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
            currentFile.save(writer);
            writer.closeReceiver();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to export file " + selectedFile.getName() + ".", e);
        }

        System.out.println("Exported " + selectedFile.getName() + ".");
    }

    /**
     * Open an editor for a given file.
     * @param file The file to open the editor for.
     */
    @SneakyThrows
    public void openEditor(GameFile file) {
        if (getCurrentController() != null)
            getCurrentController().onClose(editorPane);
        setCurrentController(null);

        editorPane.getChildren().clear(); // Remove any existing editor.

        Node node = file.makeEditor();
        if (node != null) { // null = No editor.
            getCurrentController().onInit(editorPane);
            file.setupEditor(editorPane, node);
        }
    }


    @AllArgsConstructor
    private static class AttachmentListCell extends ListCell<GameFile> {
        private MWDFile mwdFile;

        @Override
        public void updateItem(GameFile file, boolean empty) {
            super.updateItem(file, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            FileEntry entry = mwdFile.getEntryMap().get(file);
            setGraphic(new ImageView(file.getIcon()));
            setText(entry.getDisplayName());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MAIN_WINDOW = this;
        System.out.println("Hello from FrogLord.");
    }
}
