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
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
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
public abstract class FileOpenBrowseComponent extends GameUIController<GameInstance> {
    private final Label fileIdLabel;
    private final TextField filePathField;
    private final Button browseButton;

    public FileOpenBrowseComponent(GameInstance instance, String fileTypeLabel, String fileOpenPromptTitle, BrowserFileType browserFileType) {
        super(instance);
        this.fileIdLabel = createLabel(fileTypeLabel);
        this.filePathField = createFilePathField(fileOpenPromptTitle);
        this.browseButton = createBrowseButton(fileOpenPromptTitle, browserFileType);
        loadController(createContainerBox());
    }

    @Override
    public HBox getRootNode() {
        return (HBox) super.getRootNode();
    }

    private static Label createLabel(String fileTypeLabel) {
        Label fileIdLabel = new Label(fileTypeLabel);
        fileIdLabel.setMinWidth(100);
        fileIdLabel.setPadding(new Insets(3, 0, 0, 3));
        HBox.setHgrow(fileIdLabel, Priority.SOMETIMES);
        return fileIdLabel;
    }

    private static TextField createFilePathField(String fileOpenPromptTitle) {
        TextField filePathField = new TextField("");
        // TODO: TOSS? filePathField.setMinWidth(200);
        //filePathField.setMinWidth(Region.USE_COMPUTED_SIZE);
        //filePathField.setMaxWidth(Double.POSITIVE_INFINITY);

        //filePathField.setPrefWidth(Double.MAX_VALUE);
        filePathField.setPromptText(fileOpenPromptTitle);
        filePathField.setEditable(false);
        HBox.setHgrow(filePathField, Priority.ALWAYS);
        return filePathField;
    }

    private Button createBrowseButton(String fileOpenPromptTitle, BrowserFileType browserFileType) {
        Button browseButton = new Button("Browse");
        browseButton.setOnMouseClicked(event -> {
            event.consume();
            String oldFilePath = getCurrentFilePath();
            SavedFilePath pathConfig = new SavedFilePath(this.fileIdLabel.getText(), fileOpenPromptTitle, browserFileType);
            File selectedFile = FileUtils.askUserToOpenFile(null, pathConfig);
            if (selectedFile != null) {
                String newFilePath = selectedFile.getAbsolutePath();
                if (!Objects.equals(oldFilePath, newFilePath)) {
                    this.filePathField.setText(newFilePath);
                    onSetFilePath(newFilePath);
                }
            }
        });

        HBox.setHgrow(browseButton, Priority.NEVER);
        return browseButton;
    }

    private HBox createContainerBox() {
        HBox hBox = new HBox();
        hBox.setSpacing(5);

        hBox.getChildren().add(this.fileIdLabel);
        hBox.getChildren().add(this.browseButton);
        hBox.getChildren().add(this.filePathField);
        return hBox;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Do nothing, I think.
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        String startFilePath = getStartingFilePath();
        if (!StringUtils.isNullOrWhiteSpace(startFilePath) && StringUtils.isNullOrWhiteSpace(getCurrentFilePath()))
            this.filePathField.setText(startFilePath);
    }

    /**
     * Gets the file path currently selected.
     */
    public String getCurrentFilePath() {
        return this.filePathField.getText();
    }

    /**
     * Gets the file path which the browse component starts with.
     * Defaults to null.
     */
    public String getStartingFilePath() {
        return null;
    }

    /**
     * Set whether the component is disabled or not
     * @param disable if true, the component is disabled
     */
    public void setDisable(boolean disable) {
        this.filePathField.setDisable(disable);
        this.browseButton.setDisable(disable);
    }

    /**
     * Called when a new file path has been set.
     * @param newFilePath the new file path
     */
    protected abstract void onSetFilePath(String newFilePath);

    /**
     * Allows implementing the file open browser without requiring a new class.
     */
    public static class LazyFileOpenBrowseComponent extends FileOpenBrowseComponent {
        private final Supplier<String> startFilePathSource;
        private final Consumer<String> newFilePathHandler;
        public LazyFileOpenBrowseComponent(GameInstance instance, Consumer<String> newFilePathHandler, String fileTypeLabel, String fileOpenPromptTitle, BrowserFileType browserFileType) {
            this(instance, null, newFilePathHandler, fileTypeLabel, fileOpenPromptTitle, browserFileType);
        }

        public LazyFileOpenBrowseComponent(GameInstance instance, Supplier<String> startFilePathSource, Consumer<String> newFilePathHandler, String fileTypeLabel, String fileOpenPromptTitle, BrowserFileType browserFileType) {
            super(instance, fileTypeLabel, fileOpenPromptTitle, browserFileType);
            this.startFilePathSource = startFilePathSource;
            this.newFilePathHandler = newFilePathHandler;
        }

        @Override
        public String getStartingFilePath() {
            return this.startFilePathSource != null ? this.startFilePathSource.get() : super.getStartingFilePath();
        }

        @Override
        protected void onSetFilePath(String newFilePath) {
            if (this.newFilePathHandler != null)
                this.newFilePathHandler.accept(newFilePath);
        }
    }

    /**
     * A file open browse component used for selecting game files for configuring a game instance.
     */
    public static class GameConfigFileOpenBrowseComponent extends FileOpenBrowseComponent {
        private final GameConfigUIController controller;
        private final Config gameConfig; // TODO: Get config from controller instead?
        private final String configKey;

        public GameConfigFileOpenBrowseComponent(GameConfigUIController controller, Config config, String configKey, String fileTypeLabel, String fileOpenPromptTitle, BrowserFileType browserFileType) {
            super(null, fileTypeLabel, fileOpenPromptTitle, browserFileType);
            this.controller = controller;
            this.gameConfig = config;
            this.configKey = configKey;
        }

        @Override
        public String getStartingFilePath() {
            ConfigValueNode valueNode = this.gameConfig != null ? this.gameConfig.getOptionalKeyValueNode(this.configKey) : null;
            return valueNode != null ? valueNode.getAsString() : null;
        }

        @Override
        protected void onSetFilePath(String newFilePath) {
            if (this.gameConfig != null) {
                this.gameConfig.getOrCreateKeyValueNode(this.configKey).setAsString(newFilePath);
                if (FrogLordApplication.getWorkingDirectory() != null)
                    this.gameConfig.getOrCreateKeyValueNode(GameConfigController.CONFIG_GAME_LAST_FOLDER).setAsString(FrogLordApplication.getWorkingDirectory().getAbsolutePath());
            }

            this.controller.updateLoadButton();
        }
    }
}