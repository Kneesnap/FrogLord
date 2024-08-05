package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
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
    @FXML protected Menu menuBarHelp;

    // Main UI Area
    @FXML protected SplitPane mainSplitPane;
    @FXML protected AnchorPane leftSideAnchorPane;
    @FXML protected SplitPane rightSideSplinePane;
    @FXML protected AnchorPane fileEditorPane;
    @FXML protected TextArea consoleTextArea;

    @Getter private GameUIController<?> currentEditor;
    public static final URL MAIN_MENU_FXML_TEMPLATE_URL = Utils.getResourceURL("fxml/window-main.fxml");
    public static final FXMLLoader MAIN_MENU_FXML_TEMPLATE_LOADER = new FXMLLoader(MAIN_MENU_FXML_TEMPLATE_URL);

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
    }

    @Override
    public void onSceneRemove(Scene oldScene) {
        GUIMain.getActiveGameInstances().remove(getGameInstance()); // Window getting closed.
        super.onSceneRemove(oldScene);
    }

    /**
     * Adds a message to the console
     * @param message the message to add
     */
    public void addConsoleEntry(String message) {
        if (this.consoleTextArea != null && Platform.isFxApplicationThread()) {
            this.consoleTextArea.appendText(message + System.lineSeparator());
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
                this.consoleTextArea.appendText(this.queuedLogMessages.get(i) + System.lineSeparator());
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
            this.currentEditor.onSceneRemove(getScene());

        // Create any new editor.
        this.currentEditor = uiController;
        if (uiController != null) {
            if (uiController.getRootNode() != null) {
                this.fileEditorPane.getChildren().add(uiController.getRootNode());
                uiController.onSceneAdd(getScene());
            } else {
                Utils.makePopUp("Cannot display UI, since there was no content available.", AlertType.WARNING);
            }
        }
    }

    @FXML
    private void actionLoadMain(ActionEvent evt) {
        GUIMain.openLoadGameSettingsMenu();
    }

    @FXML
    private void actionSaveMain(ActionEvent evt) {
        File baseFolder = getGameInstance().getMainGameFolder();
        if (baseFolder == null || !baseFolder.canWrite()) {
            Utils.makePopUp("Can't write to the game folder." + Constants.NEWLINE + "Does FrogLord need admin permissions to save to this folder?", AlertType.ERROR);
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