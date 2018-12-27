package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.*;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.sound.VHFile;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.gui.editor.EditorController;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML private SplitPane mainSplitPane;

    @FXML private Accordion accordionMain;
    @FXML private TitledPane accPaneFilesVLO;
    @FXML private TitledPane accPaneFilesDAT;
    @FXML private TitledPane accPaneFilesMAP;
    @FXML private TitledPane accPaneFilesWAD;
    @FXML private TitledPane accPaneFilesPAL;
    @FXML private TitledPane accPaneFilesVBH;
    @FXML private ListView<GameFile> listFilesVLO;
    @FXML private ListView<GameFile> listFilesDAT;
    @FXML private ListView<GameFile> listFilesMAP;
    @FXML private ListView<GameFile> listFilesWAD;
    @FXML private ListView<GameFile> listFilesPAL;
    @FXML private ListView<GameFile> listFilesVBH;

    @FXML private AnchorPane editorPane;
    @FXML private TextArea consoleText;
    @Getter private MWDFile mwdFile;

    @Getter @Setter private static ListView<GameFile> currentFilesList;

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

        ObservableList<GameFile> gameFilesVLO = FXCollections.observableArrayList();
        ObservableList<GameFile> gameFilesDAT = FXCollections.observableArrayList();
        ObservableList<GameFile> gameFilesMAP = FXCollections.observableArrayList();
        ObservableList<GameFile> gameFilesWAD = FXCollections.observableArrayList();
        ObservableList<GameFile> gameFilesPAL = FXCollections.observableArrayList();
        ObservableList<GameFile> gameFilesVBH = FXCollections.observableArrayList();

        for (GameFile gameFile : mwdFile.getFiles())
        {
            // Grab corresponding file entry information for the game file
            FileEntry fileEntry = mwdFile.getEntryMap().get(gameFile);

            // Add the file to the relevant list (determined by game file type)
            switch (fileEntry.getTypeId())
            {
                case WADFile.TYPE_ID:
                    gameFilesWAD.add(gameFile);
                    break;

                case MAPFile.TYPE_ID:
                    // Special case test for level select VLOs...
                    if (fileEntry.getDisplayName().startsWith("LS_ALL"))
                    {
                        // We actually need to add these to the VLO list!!!
                        gameFilesVLO.add(gameFile);
                    }
                    else
                    {
                        gameFilesMAP.add(gameFile);
                    }
                    break;

                case VLOArchive.TYPE_ID:
                    gameFilesVLO.add(gameFile);
                    break;

                case VHFile.TYPE_ID:
                    gameFilesVBH.add(gameFile);
                    break;

                case DemoFile.TYPE_ID:
                    gameFilesDAT.add(gameFile);
                    break;

                case PALFile.TYPE_ID:
                    gameFilesPAL.add(gameFile);
                    break;

                default:
                    // Unknown type
                    break;
            }
        }

        listFilesVLO.setItems(gameFilesVLO);
        listFilesVLO.setCellFactory(param -> new AttachmentListCell(mwdFile));
        listFilesVLO.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> openEditor(listFilesVLO, newValue));
        accPaneFilesVLO.setText("VLO Files (" + gameFilesVLO.size() + " items)");

        listFilesDAT.setItems(gameFilesDAT);
        listFilesDAT.setCellFactory(param -> new AttachmentListCell(mwdFile));
        listFilesDAT.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> openEditor(listFilesDAT, newValue));
        accPaneFilesDAT.setText("DAT Files (" + gameFilesDAT.size() + " items)");

        listFilesMAP.setItems(gameFilesMAP);
        listFilesMAP.setCellFactory(param -> new AttachmentListCell(mwdFile));
        listFilesMAP.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> openEditor(listFilesMAP, newValue));
        accPaneFilesMAP.setText("MAP Files (" + gameFilesMAP.size() + " items)");

        listFilesWAD.setItems(gameFilesWAD);
        listFilesWAD.setCellFactory(param -> new AttachmentListCell(mwdFile));
        listFilesWAD.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> openEditor(listFilesWAD, newValue));
        accPaneFilesWAD.setText("WAD Files (" + gameFilesWAD.size() + " items)");

        listFilesPAL.setItems(gameFilesPAL);
        listFilesPAL.setCellFactory(param -> new AttachmentListCell(mwdFile));
        listFilesPAL.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> openEditor(listFilesPAL, newValue));
        accPaneFilesPAL.setText("PAL Files (" + gameFilesPAL.size() + " items)");

        listFilesVBH.setItems(gameFilesVBH);
        listFilesVBH.setCellFactory(param -> new AttachmentListCell(mwdFile));
        listFilesVBH.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> openEditor(listFilesVBH, newValue));
        accPaneFilesVBH.setText("VB/VH Files (" + gameFilesVBH.size() + " items)");

        // Default selection to first item in the VLO archives
        listFilesVLO.getSelectionModel().select(0);
        accPaneFilesVLO.setExpanded(true);
    }

    /**
     * Get the current file.
     * @return currentFile
     */
    public GameFile getCurrentFile() {
        return getCurrentFilesList().getSelectionModel().getSelectedItem();
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
    @SneakyThrows
    public void importFile() {
        File selectedFile = Utils.promptFileOpen("Select the file to import...", "All Files", "*");
        if (selectedFile == null)
            return; // Cancelled.

        byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
        GameFile oldFile = getCurrentFile();
        GameFile newFile = mwdFile.replaceFile(fileBytes, getFileEntry(), oldFile);

        this.mwdFile.getEntryMap().put(newFile, getFileEntry());
        int index = this.mwdFile.getFiles().indexOf(oldFile);
        this.mwdFile.getFiles().set(index, newFile);
        getCurrentFilesList().getItems().set(index, newFile);

        newFile.onImport(oldFile, getFileEntry().getDisplayName(), selectedFile.getName());
        openEditor(listFilesVLO, newFile); // Open the editor for the new file.
        System.out.println("Imported " + selectedFile.getName() + " as " + getFileEntry().getDisplayName() + ".");
    }

    /**
     * Export the current file.
     */
    @SneakyThrows
    public void exportFile() {
        GameFile currentFile = getCurrentFile();
        FileEntry entry = getFileEntry();

        File selectedFile = Utils.promptFileSave("Specify the file to export this data as...", entry.getDisplayName(), "All Files", "*");
        if (selectedFile == null)
            return; // Cancel.

        Utils.deleteFile(selectedFile); // Don't merge files, create a new one.
        DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
        currentFile.save(writer);
        writer.closeReceiver();

        System.out.println("Exported " + selectedFile.getName() + ".");
    }

    /**
     * Open an editor for a given file.
     * @param file The file to open the editor for.
     */
    @SneakyThrows
    public void openEditor(ListView<GameFile> activeList, GameFile file) {
        if (getCurrentController() != null)
            getCurrentController().onClose(editorPane);
        setCurrentController(null);

        editorPane.getChildren().clear(); // Remove any existing editor.

        Node node = file.makeEditor();
        if (node != null) { // null = No editor.
            getCurrentController().onInit(editorPane);
            file.setupEditor(editorPane, node);
        }

        setCurrentFilesList(activeList);
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
