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
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.StringUtils;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;
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

    public FolderBrowseComponent(GameInstance instance, String folderLabel, String folderPromptTitle, boolean saveDirectory) {
        super(instance);
        this.folderLabel = createLabel(folderLabel);
        this.folderPathField = createFilePathField(folderPromptTitle);
        this.browseButton = createBrowseButton(folderPromptTitle, saveDirectory);
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

    private Button createBrowseButton(String folderPromptTitle, boolean saveDirectory) {
        Button browseButton = new Button("Browse");
        browseButton.setOnMouseClicked(event -> {
            event.consume();
            String oldFilePath = getCurrentFolderPath();
            File selectedFile = FXUtils.promptChooseDirectory(null, folderPromptTitle, saveDirectory);
            if (selectedFile != null) {
                String newFilePath = selectedFile.getAbsolutePath();
                if (!Objects.equals(oldFilePath, newFilePath)) {
                    this.folderPathField.setText(newFilePath);
                    onSetFolderPath(newFilePath);
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
     * Called when a new folder path has been set.
     * @param newFolderPath the new folder path
     */
    protected abstract void onSetFolderPath(String newFolderPath);

    /**
     * Allows implementing the file open browser without requiring a new class.
     */
    public static class LazyFolderBrowseComponent extends FolderBrowseComponent {
        private final Supplier<String> startFolderPathSource;
        private final Consumer<String> newFolderPathHandler;
        public LazyFolderBrowseComponent(GameInstance instance, Consumer<String> newFilePathHandler, String folderLabel, String folderPromptTitle, boolean saveDirectory) {
            this(instance, null, newFilePathHandler, folderLabel, folderPromptTitle, saveDirectory);
        }

        public LazyFolderBrowseComponent(GameInstance instance, Supplier<String> startFolderPathSource, Consumer<String> newFolderPathHandler, String folderLabel, String folderPromptTitle, boolean saveDirectory) {
            super(instance, folderLabel, folderPromptTitle, saveDirectory);
            this.startFolderPathSource = startFolderPathSource;
            this.newFolderPathHandler = newFolderPathHandler;
        }

        @Override
        public String getStartingFolderPath() {
            return this.startFolderPathSource != null ? this.startFolderPathSource.get() : super.getStartingFolderPath();
        }

        @Override
        protected void onSetFolderPath(String newFilePath) {
            if (this.newFolderPathHandler != null)
                this.newFolderPathHandler.accept(newFilePath);
        }
    }

    /**
     * A file open browse component used for selecting game files for configuring a game instance.
     */
    public static class GameConfigFolderBrowseComponent extends FolderBrowseComponent {
        private final GameConfigUIController controller;
        private final Config gameConfig; // TODO: Get config from controller instead?
        private final String configKey;

        public GameConfigFolderBrowseComponent(GameConfigUIController controller, Config config, String configKey, String folderLabel, String folderPromptTitle, boolean saveDirectory) {
            super(null, folderLabel, folderPromptTitle, saveDirectory);
            this.controller = controller;
            this.gameConfig = config;
            this.configKey = configKey;
        }

        @Override
        public String getStartingFolderPath() {
            ConfigValueNode valueNode = this.gameConfig != null ? this.gameConfig.getOptionalKeyValueNode(this.configKey) : null;
            return valueNode != null ? valueNode.getAsString() : null;
        }

        @Override
        protected void onSetFolderPath(String newFilePath) {
            if (this.gameConfig != null) {
                this.gameConfig.getOrCreateKeyValueNode(this.configKey).setAsString(newFilePath);
                if (GUIMain.getWorkingDirectory() != null)
                    this.gameConfig.getOrCreateKeyValueNode(GameConfigController.CONFIG_GAME_LAST_FOLDER).setAsString(GUIMain.getWorkingDirectory().getAbsolutePath());
            }

            this.controller.updateLoadButton();
        }
    }
}