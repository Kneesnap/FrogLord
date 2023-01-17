package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.*;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.sound.VHFile;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.gui.editor.*;
import net.highwayfrogs.editor.gui.extra.DemoTableEditorController;
import net.highwayfrogs.editor.gui.extra.FormEntryController;
import net.highwayfrogs.editor.gui.extra.LevelInfoController;
import net.highwayfrogs.editor.gui.extra.hash.HashPlaygroundController;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Getter
public class MainController implements Initializable {
    @FXML private AnchorPane rootAnchor;
    @FXML private MenuBar menuBar;
    @FXML private SplitPane mainSplitPane;
    @FXML private Accordion accordionMain;
    @FXML private AnchorPane editorPane;
    @FXML private TextArea consoleText;
    @FXML private MenuItem levelInfoEditor;
    @FXML private MenuItem formLibEditor;
    @FXML private MenuItem scriptEditor;
    @FXML private MenuItem textureFinder;
    @FXML private MenuItem demoTableEditor;
    @FXML private MenuItem patchMenu;
    private MWDFile mwdFile;
    private ListView<GameFile> currentFilesList;

    public static MainController MAIN_WINDOW;
    @Getter
    @Setter
    private static EditorController<?> currentController;

    /**
     * Print a message to the console window.
     * @param message The message to print.
     */
    public void printMessage(String message) {
        if (consoleText != null)
            consoleText.appendText(message + System.lineSeparator());
    }

    /**
     * Load a MWDFile as the active MWD being used.
     * @param file The MWD file to load.
     */
    public void loadMWD(MWDFile file) {
        this.mwdFile = file;

        Map<Integer, ObservableList<GameFile>> gameFileRegistry = new HashMap<>();

        for (GameFile gameFile : mwdFile.getFiles()) {
            // Grab corresponding file entry information for the game file
            FileEntry fileEntry = mwdFile.getEntryMap().get(gameFile);
            int type = fileEntry.getSpoofedTypeId();

            if (!gameFileRegistry.containsKey(type))
                gameFileRegistry.put(type, FXCollections.observableArrayList());

            gameFileRegistry.get(type).add(gameFile);
        }

        addFileList(VLOArchive.TYPE_ID, "VLO", gameFileRegistry);
        addFileList(DemoFile.TYPE_ID, "DAT", gameFileRegistry);
        addFileList(MAPFile.TYPE_ID, "MAP", gameFileRegistry);
        addFileList(WADFile.TYPE_ID, "WAD", gameFileRegistry);
        addFileList(PALFile.TYPE_ID, "PAL", gameFileRegistry);
        addFileList(VHFile.TYPE_ID, "VB/VH", gameFileRegistry);

        // Setup!
        FroggerEXEInfo config = mwdFile.getConfig();
        levelInfoEditor.setDisable(config.getArcadeLevelAddress() == 0);
        formLibEditor.setDisable(config.getFullFormBook().isEmpty());
        scriptEditor.setDisable(config.getScripts().isEmpty());
        demoTableEditor.setDisable(config.getDemoTableEntries().isEmpty());
    }

    private void addFileList(int type, String name, Map<Integer, ObservableList<GameFile>> fileMap) {
        if (!fileMap.containsKey(type))
            return; // There are no files of this type.

        TitledPane pane = new TitledPane();
        pane.setPrefSize(200, 180);
        pane.setAnimated(false);

        ListView<GameFile> listView = new ListView<>(fileMap.get(type));
        listView.setCellFactory(param -> new AttachmentListCell(mwdFile));
        listView.setItems(fileMap.get(type));

        pane.setContent(listView);
        pane.setText(name + " Files (" + listView.getItems().size() + " items)");
        accordionMain.getPanes().add(pane);

        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> openEditor(listView, newValue));

        // Expand VLO.
        if (type == VLOArchive.TYPE_ID) {
            listView.getSelectionModel().selectFirst();
            pane.setExpanded(true);
        }
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
     * @return fileEntry
     */
    public FileEntry getFileEntry() {
        return mwdFile.getEntryMap().get(getCurrentFile());
    }

    @FXML
    private void actionLoadMWD(ActionEvent evt) {
        try {
            GUIMain.INSTANCE.openFroggerFiles();
        } catch (IOException ex) {
            Utils.makeErrorPopUp("Failed to load Frogger data.", ex, true);
        }
    }

    @FXML
    private void actionSaveMWD(ActionEvent evt) {
        SaveController.saveFiles(GUIMain.EXE_CONFIG, getMwdFile());
    }

    @FXML
    private void actionSaveMWI(ActionEvent evt) {
        File selectedFile = Utils.promptFileSave("Specify the file to export the MWI as...", "FROGPSX", "Millenium WAD Index", "MWI");
        if (selectedFile == null)
            return; // Cancel.

        Utils.deleteFile(selectedFile); // Don't merge files, create a new one.
        DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
        getMwdFile().getConfig().getMWI().save(writer);
        writer.closeReceiver();

        System.out.println("Exported MWI.");
    }

    @FXML
    private void actionImportFile(ActionEvent evt) {
        importFile();
    }

    @FXML
    private void actionExportFile(ActionEvent evt) {
        exportFile();
    }

    @FXML
    private void actionExportAlternateFile(ActionEvent evt) {
        getCurrentFile().exportAlternateFormat(getFileEntry());
    }

    @FXML
    private void actionMakeHeaders(ActionEvent evt) {
        GUIMain.EXE_CONFIG.exportCode(GUIMain.getWorkingDirectory());
    }

    @FXML
    private void editLevelInfo(ActionEvent evt) {
        LevelInfoController.openEditor(GUIMain.EXE_CONFIG);
    }

    @FXML
    private void editFormBook(ActionEvent evt) {
        FormEntryController.openEditor(GUIMain.EXE_CONFIG);
    }

    @FXML
    private void editScript(ActionEvent evt) {
        ScriptEditorController.openEditor();
    }

    @FXML
    private void editDemoTable(ActionEvent evt) {
        DemoTableEditorController.openEditor();
    }

    @FXML
    private void actionOpenPatchMenu(ActionEvent evt) {
        PatchController.openMenu();
    }

    @FXML
    private void actionHashPlayground(ActionEvent evt) {
        HashPlaygroundController.openEditor();
    }

    @FXML
    private void actionSearchForTexture(ActionEvent evt) {
        InputMenu.promptInput("Please enter the texture id to lookup.", str -> {
            if (!Utils.isInteger(str)) {
                Utils.makePopUp("'" + str + "' is not a valid number.", AlertType.WARNING);
                return;
            }

            int texId = Integer.parseInt(str);
            List<GameImage> images = getMwdFile().getImagesByTextureId(texId);
            if (images.isEmpty()) {
                Utils.makePopUp("Could not find an image with the id " + texId + ".", AlertType.WARNING);
                return;
            }

            for (GameImage image : images)
                System.out.println("Found " + texId + " as texture #" + image.getLocalImageID() + " in " + Utils.stripExtension(image.getParent().getFileEntry().getDisplayName()) + ".");

            GameImage image = images.get(0);
            openEditor(this.currentFilesList, image.getParent());
            ((VLOController) getCurrentController()).selectImage(image, true);
        });
    }

    @FXML
    private void actionOpenAbout(ActionEvent evt) {
        AboutController.openAboutMenu();
    }

    @FXML
    private void actionExportBulkTextures(ActionEvent evt) {
        File targetFolder = Utils.promptChooseDirectory("Choose the directory to save all textures to.", false);

        ImageFilterSettings exportSettings = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(false).setAllowTransparency(true);
        List<VLOArchive> allVlos = getMwdFile().getAllFiles(VLOArchive.class);
        for (VLOArchive saveVLO : allVlos) {
            File vloFolder = new File(targetFolder, Utils.stripExtension(saveVLO.getFileEntry().getDisplayName()));
            Utils.makeDirectory(vloFolder);
            saveVLO.exportAllImages(vloFolder, exportSettings);
        }
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
        this.mwdFile.getFiles().set(this.mwdFile.getFiles().indexOf(oldFile), newFile);
        getCurrentFilesList().getItems().set(getCurrentFilesList().getItems().indexOf(oldFile), newFile);

        newFile.onImport(oldFile, getFileEntry().getDisplayName(), selectedFile.getName());
        openEditor(getCurrentFilesList(), newFile); // Open the editor for the new file.
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

        currentFilesList = activeList;
    }

    /**
     * Open an editor for a given file.
     */
    @SneakyThrows
    public <T extends GameFile> void openEditor(EditorController<T> editor, T file) {
        if (getCurrentController() != null)
            getCurrentController().onClose(editorPane);
        setCurrentController(editor);

        //editorPane.getChildren().clear(); // Remove any existing editor.
        if (editor != null) { // null = No editor.
            editor.loadFile(file);
            editor.onInit(editorPane);
        }
    }


    @AllArgsConstructor
    private static class AttachmentListCell extends ListCell<GameFile> {
        private final MWDFile mwdFile;

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
            setText(entry.getDisplayName() + " [" + entry.getLoadedId() + "]");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MAIN_WINDOW = this;
        System.out.println("Hello! FrogLord is loading config '" + GUIMain.EXE_CONFIG.getInternalName() + "'.");
        menuBar.prefWidthProperty().bind(rootAnchor.widthProperty());
    }
}
