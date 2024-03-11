package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
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
import lombok.*;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.editor.*;
import net.highwayfrogs.editor.gui.extra.DemoTableEditorController;
import net.highwayfrogs.editor.gui.extra.FormEntryController;
import net.highwayfrogs.editor.gui.extra.LevelInfoController;
import net.highwayfrogs.editor.gui.extra.hash.HashPlaygroundController;
import net.highwayfrogs.editor.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiPredicate;

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
    @FXML private MenuItem differenceReport;
    @FXML private MenuItem findUnusedVertices;
    private ListView<SCGameFile<?>> currentFilesList;
    @Getter private SCGameInstance gameInstance;

    public static MainController MAIN_WINDOW;
    @Getter
    @Setter
    private static EditorController<?, ?, ?> currentController;
    private static final List<String> queuedMessages = new ArrayList<>();

    /**
     * Gets the game configuration.
     */
    public SCGameConfig getConfig() {
        return this.gameInstance != null ? this.gameInstance.getConfig() : null;
    }

    /**
     * Gets the main file archive.
     */
    public MWDFile getArchive() {
        return this.gameInstance != null ? this.gameInstance.getMainArchive() : null;
    }

    @Getter
    @RequiredArgsConstructor
    public static abstract class SCMainMenuFileGroup {
        private final List<SCGameFile<?>> files = new ArrayList<>();
        private final String name;

        /**
         * Test if the given file is part of this group.
         * @param gameFile The file to test.
         * @return true iff the file is part of the group.
         */
        public abstract boolean isPartOfGroup(SCGameFile<?> gameFile);

        /**
         * Creates the UI accordion pane.
         * @param controller The controller to create the UI for.
         */
        public void createUI(MainController controller) {
            TitledPane pane = new TitledPane();
            pane.setPrefSize(200, 180);
            pane.setAnimated(false);

            ObservableList<SCGameFile<?>> fxFilesList = FXCollections.observableArrayList(this.files);
            ListView<SCGameFile<?>> listView = new ListView<>(fxFilesList);
            listView.setCellFactory(param -> new AttachmentListCell());
            listView.setItems(fxFilesList);

            pane.setContent(listView);
            pane.setText(this.name + " Files (" + listView.getItems().size() + " items)");
            controller.getAccordionMain().getPanes().add(pane);

            listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> controller.openEditor(listView, newValue));

            // Expand first pane.
            if (controller.getAccordionMain().getPanes().size() <= 1) {
                listView.getSelectionModel().selectFirst();
                pane.setExpanded(true);
            }
        }
    }

    @Getter
    public static class SCMainMenuFileGroupFileID extends SCMainMenuFileGroup {
        private final int typeId;

        public SCMainMenuFileGroupFileID(String name, int typeId) {
            super(name);
            this.typeId = typeId;
        }

        @Override
        public boolean isPartOfGroup(SCGameFile<?> gameFile) {
            return gameFile.getIndexEntry().getTypeId() == this.typeId;
        }
    }

    public static class LazySCMainMenuFileGroup extends SCMainMenuFileGroup {
        private final BiPredicate<SCGameFile<?>, FileEntry> predicate;

        public LazySCMainMenuFileGroup(String name, BiPredicate<SCGameFile<?>, FileEntry> predicate) {
            super(name);
            this.predicate = predicate;
        }

        @Override
        public boolean isPartOfGroup(SCGameFile<?> gameFile) {
            return this.predicate != null && this.predicate.test(gameFile, gameFile.getIndexEntry());
        }
    }

    /**
     * Load a MWDFile as the active MWD being used.
     */
    public void loadMWD(SCGameInstance instance) {
        System.out.println("Hello! FrogLord is loading config '" + instance.getConfig().getInternalName() + "'.");
        this.gameInstance = instance;

        // Create the list of groups to add files amongst.
        List<SCMainMenuFileGroup> fileGroups = new ArrayList<>();
        instance.setupFileGroups(fileGroups);
        fileGroups.add(new SCMainMenuFileGroupFileID("WAD", WADFile.TYPE_ID));
        fileGroups.add(new SCMainMenuFileGroupFileID("Uncategorized", 0));

        // Add files to the different groups.
        for (SCGameFile<?> gameFile : getArchive().getFiles()) {
            // Check each file group to see if the file belongs there.
            boolean addedSuccessfully = false;
            for (int i = 0; i < fileGroups.size(); i++) {
                SCMainMenuFileGroup fileGroup = fileGroups.get(i);
                if (fileGroup != null && fileGroup.isPartOfGroup(gameFile)) {
                    addedSuccessfully = true;
                    fileGroup.getFiles().add(gameFile);
                    break;
                }
            }

            // If no group was found, add a new one which covers this.
            if (!addedSuccessfully) {
                int id = gameFile.getIndexEntry().getResourceId();
                SCMainMenuFileGroup newGroup = new SCMainMenuFileGroupFileID("Unknown [Resource ID: " + id + "]", id);
                fileGroups.add(newGroup);
                newGroup.getFiles().add(gameFile);
            }
        }

        // Create the UI.
        for (SCMainMenuFileGroup fileGroup : fileGroups)
            if (fileGroup != null && !fileGroup.getFiles().isEmpty())
                fileGroup.createUI(this);

        // Setup!
        FroggerGameInstance frogger = getGameInstance().isFrogger() ? (FroggerGameInstance) getGameInstance() : null;
        levelInfoEditor.setDisable(frogger == null || frogger.getArcadeLevelInfo().isEmpty());
        formLibEditor.setDisable(frogger == null || frogger.getFullFormBook().isEmpty());
        scriptEditor.setDisable(frogger == null || frogger.getScripts().isEmpty());
        demoTableEditor.setDisable(frogger == null || frogger.getDemoTableEntries().isEmpty());
        differenceReport.setDisable(!FroggerVersionComparison.isEnabled());
    }

    /**
     * Get the current file.
     * @return currentFile
     */
    public SCGameFile<?> getCurrentFile() {
        return getCurrentFilesList().getSelectionModel().getSelectedItem();
    }

    /**
     * Get the FileEntry associated with the selected file.
     * @return fileEntry
     */
    public FileEntry getFileEntry() {
        return getCurrentFile().getIndexEntry();
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
        SaveController.saveFiles(getGameInstance());
    }

    @FXML
    private void actionSaveMWI(ActionEvent evt) {
        File selectedFile = Utils.promptFileSave("Specify the file to export the MWI as...", "FROGPSX", "Millennium WAD Index", "MWI");
        if (selectedFile == null)
            return; // Cancel.

        Utils.deleteFile(selectedFile); // Don't merge files, create a new one.
        DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
        getGameInstance().getArchiveIndex().save(writer);
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
        if (getGameInstance().isFrogger())
            ((FroggerGameInstance) getGameInstance()).exportCode(GUIMain.getWorkingDirectory());
    }

    @FXML
    private void actionGenerateDifferenceReport(ActionEvent evt) {
        FroggerVersionComparison.generateReport();
    }

    @FXML
    private void actionFindUnusedVertices(ActionEvent evt) {
        getGameInstance().getMainArchive().getAllFiles(MAPFile.class).forEach(mapFile -> {
            List<SVector> unusedVertices = mapFile.findUnusedVertices();
            if (unusedVertices.size() > 1)
                System.out.println(" - " + mapFile.getFileDisplayName() + " has " + unusedVertices.size() + " unused vertices.");
        });
    }

    @FXML
    private void editLevelInfo(ActionEvent evt) {
        if (getGameInstance().isFrogger())
            LevelInfoController.openEditor((FroggerGameInstance) getGameInstance());
    }

    @FXML
    private void editFormBook(ActionEvent evt) {
        if (getGameInstance().isFrogger())
            FormEntryController.openEditor((FroggerGameInstance) getGameInstance());
    }

    @FXML
    private void editScript(ActionEvent evt) {
        if (getGameInstance().isFrogger())
            ScriptEditorController.openEditor((FroggerGameInstance) getGameInstance());
    }

    @FXML
    private void editDemoTable(ActionEvent evt) {
        if (getGameInstance().isFrogger())
            DemoTableEditorController.openEditor((FroggerGameInstance) getGameInstance());
    }

    @FXML
    private void actionOpenPatchMenu(ActionEvent evt) {
        if (getGameInstance().isFrogger())
            PatchController.openMenu((FroggerGameInstance) getGameInstance());
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
            List<GameImage> images = getArchive().getImagesByTextureId(texId);
            if (images.isEmpty()) {
                Utils.makePopUp("Could not find an image with the id " + texId + ".", AlertType.WARNING);
                return;
            }

            for (GameImage image : images)
                System.out.println("Found " + texId + " as texture #" + image.getLocalImageID() + " in " + Utils.stripExtension(image.getParent().getFileDisplayName()) + ".");

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
        List<VLOArchive> allVlos = getArchive().getAllFiles(VLOArchive.class);
        for (VLOArchive saveVLO : allVlos) {
            File vloFolder = new File(targetFolder, Utils.stripExtension(saveVLO.getFileDisplayName()));
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
        SCGameFile<?> oldFile = getCurrentFile();
        SCGameFile<?> newFile = getArchive().replaceFile(fileBytes, getFileEntry(), oldFile);
        getArchive().getFiles().set(getArchive().getFiles().indexOf(oldFile), newFile);
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
        SCGameFile<?> currentFile = getCurrentFile();
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
    public void openEditor(ListView<SCGameFile<?>> activeList, SCGameFile<?> file) {
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
    public <T extends SCGameFile<?>> void openEditor(EditorController<T, ?, ?> editor, T file) {
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
    private static class AttachmentListCell extends ListCell<SCGameFile<?>> {

        @Override
        public void updateItem(SCGameFile<?> file, boolean empty) {
            super.updateItem(file, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            FileEntry entry = file.getIndexEntry();
            setGraphic(new ImageView(file.getIcon()));

            // Update text.
            boolean isIslandPlaceholder = file instanceof MAPFile && ((MAPFile) file).getMapConfig().isIslandPlaceholder();
            setStyle(isIslandPlaceholder ? "-fx-text-fill: red;" : null);
            setText(entry.getDisplayName() + " [" + entry.getResourceId() + ", " + entry.getTypeId() + "]");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MAIN_WINDOW = this;
        menuBar.prefWidthProperty().bind(rootAnchor.widthProperty());
    }

    /**
     * Print a message to the console window.
     * @param message The message to print.
     */
    public static void addMessage(String message) {
        if (MAIN_WINDOW != null && MAIN_WINDOW.consoleText != null && Platform.isFxApplicationThread()) {
            MAIN_WINDOW.consoleText.appendText(message + System.lineSeparator());
        } else {
            // Queue the message for later.
            synchronized (queuedMessages) {
                if (queuedMessages.isEmpty())
                    Platform.runLater(MainController::showQueuedMessages);
                queuedMessages.add(message);
            }
        }
    }

    private static void showQueuedMessages() {
        // Test if it's possible to add messages now. If it's not, push it down the road again.
        if (MAIN_WINDOW == null || MAIN_WINDOW.consoleText == null || !Platform.isFxApplicationThread()) {
            Platform.runLater(MainController::showQueuedMessages);
            return;
        }

        // Show the messages.
        synchronized (queuedMessages) {
            for (int i = 0; i < queuedMessages.size(); i++)
                MAIN_WINDOW.consoleText.appendText(queuedMessages.get(i) + System.lineSeparator());
            queuedMessages.clear();
        }
    }
}