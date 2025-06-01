package net.highwayfrogs.editor.gui.components;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Represents a file path selection component.
 * Created by Kneesnap on 4/26/2024.
 */
@Getter
public abstract class FolderBrowseComponent extends GameUIController<GameInstance> {
    private final Label folderLabel;
    private final TextField folderPathField;
    private final Button browseButton;

    public FolderBrowseComponent(GameInstance instance, String folderLabel, String folderPromptTitle) {
        super(instance);
        this.folderLabel = createLabel(folderLabel);
        this.folderPathField = createFilePathField(folderPromptTitle);
        this.browseButton = createBrowseButton(folderPromptTitle);
        loadController(createContainerBox());
    }

    @Override
    public HBox getRootNode() {
        return (HBox) super.getRootNode();
    }

    private static Label createLabel(String folderLabelText) {
        Label folderLabel = new Label(folderLabelText);
        folderLabel.setMinWidth(100);
        folderLabel.setPadding(new Insets(3, 0, 0, 3));
        HBox.setHgrow(folderLabel, Priority.SOMETIMES);
        return folderLabel;
    }

    private static TextField createFilePathField(String fileOpenPromptTitle) {
        TextField folderPathField = new TextField("");
        folderPathField.setPromptText(fileOpenPromptTitle);
        folderPathField.setEditable(false);
        HBox.setHgrow(folderPathField, Priority.ALWAYS);
        return folderPathField;
    }

    private Button createBrowseButton(String folderPromptTitle) {
        Button browseButton = new Button("Browse");
        browseButton.setOnMouseClicked(event -> {
            event.consume();
            String oldFolderPath = getCurrentFolderPath();
            SavedFilePath pathConfig = new SavedFilePath(this.folderLabel.getText(), folderPromptTitle);
            File selectedFolder = FileUtils.askUserToSelectFolder(null, pathConfig);
            if (selectedFolder != null) {
                String newFolderPath = selectedFolder.getAbsolutePath();
                if (!Objects.equals(oldFolderPath, newFolderPath)) {
                    this.folderPathField.setText(newFolderPath);
                    if (!onSetFolderPath(newFolderPath, selectedFolder))
                        this.folderPathField.setText(oldFolderPath);
                }
            }
        });

        HBox.setHgrow(browseButton, Priority.NEVER);
        return browseButton;
    }

    private HBox createContainerBox() {
        HBox hBox = new HBox();
        hBox.setSpacing(5);

        hBox.getChildren().add(this.folderLabel);
        hBox.getChildren().add(this.browseButton);
        hBox.getChildren().add(this.folderPathField);
        return hBox;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Do nothing, I think.
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        String startFilePath = getStartingFolderPath();
        if (!StringUtils.isNullOrWhiteSpace(startFilePath) && StringUtils.isNullOrWhiteSpace(getCurrentFolderPath()))
            this.folderPathField.setText(startFilePath);
    }

    /**
     * Gets the folder path currently selected.
     */
    public String getCurrentFolderPath() {
        return this.folderPathField.getText();
    }

    /**
     * Gets the folder path which the browse component starts with.
     * Defaults to null.
     */
    public String getStartingFolderPath() {
        return null;
    }

    /**
     * Set whether the component is disabled or not
     * @param disable if true, the component is disabled
     */
    public void setDisable(boolean disable) {
        this.folderPathField.setDisable(disable);
        this.browseButton.setDisable(disable);
    }

    /**
     * Resets the folder path to assume its starting value.
     */
    public void resetFolderPath() {
        this.folderPathField.setText(getStartingFolderPath());
    }

    /**
     * Called when a new folder path has been set.
     * @param newFolderPath the new folder path
     * @param newFolder the new folder
     */
    protected abstract boolean onSetFolderPath(String newFolderPath, File newFolder);

    /**
     * Allows implementing the file open browser without requiring a new class.
     */
    public static class LazyFolderBrowseComponent extends FolderBrowseComponent {
        private final Supplier<String> startFolderPathSource;
        private final BiPredicate<String, File> newFolderPathHandler;
        public LazyFolderBrowseComponent(GameInstance instance, BiPredicate<String, File> newFolderPathHandler, String folderLabel, String folderPromptTitle) {
            this(instance, null, newFolderPathHandler, folderLabel, folderPromptTitle);
        }

        public LazyFolderBrowseComponent(GameInstance instance, Supplier<String> startFolderPathSource, BiPredicate<String, File> newFolderPathHandler, String folderLabel, String folderPromptTitle) {
            super(instance, folderLabel, folderPromptTitle);
            this.startFolderPathSource = startFolderPathSource;
            this.newFolderPathHandler = newFolderPathHandler;
        }

        @Override
        public String getStartingFolderPath() {
            return this.startFolderPathSource != null ? this.startFolderPathSource.get() : super.getStartingFolderPath();
        }

        @Override
        protected boolean onSetFolderPath(String newFolderPath, File newFolder) {
            return (this.newFolderPathHandler == null) || this.newFolderPathHandler.test(newFolderPath, newFolder);
        }
    }

    /**
     * A file open browse component used for selecting game files for configuring a game instance.
     */
    public static class GameConfigFolderBrowseComponent extends FolderBrowseComponent {
        @NonNull private final GameConfigUIController<?> controller;
        private final BiPredicate<String, File> validityTest;
        private final String configKey;

        public GameConfigFolderBrowseComponent(@NonNull GameConfigUIController<?> controller, String configKey, String folderLabel, String folderPromptTitle, BiPredicate<String, File> validityTest) {
            super(null, folderLabel, folderPromptTitle);
            this.controller = controller;
            this.validityTest = validityTest;
            this.configKey = configKey;
        }

        @Override
        public String getStartingFolderPath() {
            Config editorConfig = this.controller.getActiveEditorConfig();
            ConfigValueNode valueNode = editorConfig != null ? editorConfig.getOptionalKeyValueNode(this.configKey) : null;
            return valueNode != null ? valueNode.getAsString() : null;
        }

        @Override
        protected boolean onSetFolderPath(String newFolderPath, File newFolder) {
            Config oldEditorConfig = this.controller.getActiveEditorConfig();

            if (this.validityTest != null) {
                try {
                    if (!this.validityTest.test(newFolderPath, newFolder))
                        return false;
                } catch (Throwable th) {
                    Utils.handleError(null, th, true, "An error occurred while validating the folder path.");
                    return false;
                }
            }

            Config editorConfig = this.controller.getActiveEditorConfig();
            if (editorConfig != null) {
                editorConfig.getOrCreateKeyValueNode(this.configKey).setAsString(newFolderPath);
                if (FrogLordApplication.getWorkingDirectory() != null)
                    editorConfig.getOrCreateKeyValueNode(GameConfigController.CONFIG_GAME_LAST_FOLDER).setAsString(FrogLordApplication.getWorkingDirectory().getAbsolutePath());
            }

            // Apply the new value.
            if (oldEditorConfig != editorConfig)
                resetFolderPath();

            this.controller.updateLoadButton();
            return true;
        }
    }
}