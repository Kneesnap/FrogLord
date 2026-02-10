package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.scripting.NoodleConstants;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the UI for the main menu.
 * Created by Kneesnap on 4/11/2024.
 */
public abstract class MainMenuController<TGameInstance extends GameInstance, TFileEntry extends ICollectionViewEntry> extends GameUIController<TGameInstance> {
    private final List<String> queuedLogMessages = new ArrayList<>();
    @Getter private CollectionEditorComponent<TGameInstance, TFileEntry> fileListComponent;
    @FXML protected AnchorPane rootAnchor;

    // Menu Bar:
    @FXML protected MenuBar menuBar;
    @FXML protected Menu menuBarFile;
    @FXML protected MenuItem menuItemLoadMain;
    @FXML protected MenuItem menuItemSaveMain;
    @FXML protected Menu menuBarEdit;
    @FXML protected MenuItem menuItemRunScript;
    @FXML protected Menu menuBarHelp;

    // Main UI Area
    @FXML protected SplitPane mainSplitPane;
    @FXML protected AnchorPane leftSideAnchorPane;
    @FXML protected SplitPane rightSideSplinePane;
    @FXML protected AnchorPane fileEditorPane;
    @FXML protected TextArea consoleTextArea;

    @Getter private GameUIController<?> currentEditor;
    public static final URL MAIN_MENU_FXML_TEMPLATE_URL = FileUtils.getResourceURL("fxml/window-main.fxml");
    public static final FXMLLoader MAIN_MENU_FXML_TEMPLATE_LOADER = new FXMLLoader(MAIN_MENU_FXML_TEMPLATE_URL);
    private static final int MAX_CONSOLE_HISTORY = 25000;

    public MainMenuController(TGameInstance instance) {
        super(instance);
    }

    /**
     * Gets the currently selected file entry, if there is one.
     */
    public TFileEntry getSelectedFileEntry() {
        return this.fileListComponent != null ? this.fileListComponent.getSelectedViewEntry() : null;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.menuBar.prefWidthProperty().bind(this.rootAnchor.widthProperty());

        // Register file list.
        this.fileListComponent = createFileListEditor();
        if (this.fileListComponent != null) {
            setAnchorPaneStretch(this.fileListComponent.getRootNode());
            this.leftSideAnchorPane.getChildren().add(this.fileListComponent.getRootNode());
            addController(this.fileListComponent);
        }

        // Add queued logging.
        String queuedLogMessages = getGameInstance().getAndClearQueuedLogMessages();
        if (queuedLogMessages != null)
            this.consoleTextArea.appendText(queuedLogMessages);

        this.menuItemRunScript = new MenuItem("Run Noodle Script");
        this.menuItemRunScript.setOnMenuValidation(event
                -> ((MenuItem) event.getTarget()).setDisable(getGameInstance().getScriptEngine() == null));
        this.menuItemRunScript.setOnAction(event -> {
            File noodleScript = FileUtils.askUserToOpenFile(getGameInstance(), NoodleConstants.NOODLE_SCRIPT_FILE_PATH);
            if (noodleScript == null)
                return;

            NoodleScript script = getGameInstance().getScriptEngine().loadScriptFile(noodleScript, true, true);
            if (script == null) {
                FXUtils.showPopup(AlertType.ERROR, "The script was not compiled successfully.", "The script was not compiled successfully, so it will not be run.\nCheck the logging console for more information about the error.");
                return;
            }

            // Create a thread to run the script.
            NoodleThread<NoodleScript> thread = new NoodleThread<>(getGameInstance(), script);
            thread.setOnCancelHook(() -> FXUtils.showPopup(AlertType.ERROR, "An error occurred while running the script.", thread.buildScriptInformation()));
            thread.setOnFinishHook(() -> getLogger().info("Reached the end of the script '%s'.", script.getName()));
            thread.addObjectInstanceArgument(getGameInstance());

            try {
                thread.startThread();
            } catch (Throwable th) {
                Utils.handleError(getGameInstance().getScriptEngine().getLogger(), th, true, "An error occurred while running the script '%s'.", script.getName());
            }
        });

        this.menuBarEdit.getItems().add(this.menuItemRunScript);
    }

    /**
     * Adds a message to the console
     * @param message the message to add
     */
    public void addConsoleEntry(String message) {
        if (this.consoleTextArea != null && Platform.isFxApplicationThread()) {
            this.consoleTextArea.appendText(message + System.lineSeparator());
            if (this.consoleTextArea.getLength() > MAX_CONSOLE_HISTORY)
                this.consoleTextArea.deleteText(0, this.consoleTextArea.getLength() - MAX_CONSOLE_HISTORY);
        } else {
            // Queue the message for later.
            synchronized (this.queuedLogMessages) {
                if (this.queuedLogMessages.isEmpty())
                    Platform.runLater(this::showQueuedMessages);
                this.queuedLogMessages.add(message);
            }
        }
    }

    private void showQueuedMessages() {
        // Test if it's possible to add messages now. If it's not, push it down the road again.
        if (this.consoleTextArea == null || !Platform.isFxApplicationThread()) {
            Platform.runLater(this::showQueuedMessages);
            return;
        }

        // Show the messages.
        synchronized (this.queuedLogMessages) {
            for (int i = 0; i < this.queuedLogMessages.size(); i++)
                addConsoleEntry(this.queuedLogMessages.get(i));
            this.queuedLogMessages.clear();
        }
    }

    /**
     * Open an editor for a given file.
     * @param uiController the editor ui to display
     */
    public void showEditor(GameUIController<?> uiController) {
        // Remove any existing editor.
        this.fileEditorPane.getChildren().clear();
        if (this.currentEditor != null)
            removeController(this.currentEditor);

        // Create any new editor.
        this.currentEditor = uiController;
        if (uiController != null) {
            if (uiController.getRootNode() != null) {
                this.fileEditorPane.getChildren().add(uiController.getRootNode());
            } else {
                FXUtils.showPopup(AlertType.WARNING, "No UI Content", "Cannot display UI, since there was no content available.");
            }

            addController(uiController);
        }
    }

    @FXML
    private void actionLoadMain(ActionEvent evt) {
        FrogLordApplication.openLoadGameSettingsMenu();
    }

    @FXML
    private void actionSaveMain(ActionEvent evt) {
        File baseFolder = getGameInstance().getMainGameFolder();
        if (baseFolder == null || !baseFolder.canWrite()) {
            FXUtils.showPopup(AlertType.ERROR, "Can't write to the game folder.", "Does FrogLord need admin permissions to save to this folder?\nFolder: " + baseFolder);
            return;
        }

        if (getGameInstance().isShowSaveWarning()) {
            boolean saveAnyways = FXUtils.makePopUpYesNo("Saving files for " + getGameInstance().getGameType().getDisplayName() + " is not officially supported yet.\n"
                    + "It is likely the game will crash if the modified files are used. Would you like to continue?");
            if (!saveAnyways)
                return;
        }

        try {
            saveMainGameData();
        } catch (Throwable th) {
            handleError(th, true, "Failed to save game data.");
        }
    }

    /**
     * Saves the main game data.
     */
    protected abstract void saveMainGameData();

    /**
     * Creates the file list editor.
     */
    protected abstract CollectionEditorComponent<TGameInstance, TFileEntry> createFileListEditor();

    @FXML
    private void actionOpenAboutWindow(ActionEvent evt) {
        AboutController.openAboutMenu(getGameInstance());
    }
}