package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Allows selecting a game to load.
 * TODO: Add a separate menu for extracting files from .ISO & .bin/.cue.
 * Created by Kneesnap on 4/25/2024.
 */
@Getter
public class GameConfigController extends GameUIController<GameInstance> {
    private final Config gameConfigRoot;
    private final Config unknownGameConfig = new Config("$UnknownGameConfig");
    private GameConfigUIController<?> activeConfigController;
    private String selectedVersionConfigName;
    @FXML private ImageView frogLordGameLogoView;
    @FXML private ComboBox<IGameType> gameTypeComboBox;
    @FXML private ComboBox<GameConfig> gameVersionComboBox;
    @FXML private VBox topBox;
    @FXML private VBox bottomBox;
    @FXML private Button loadButton;
    @FXML private Button cancelButton;

    public static final String CONFIG_GAME_INSTANCE_DATA = "InstanceData";
    public static final String CONFIG_ROOT_LAST_SELECTED_GAME_TYPE = "lastSelectedGame";
    public static final String CONFIG_GAME_LAST_SELECTED_VERSION = "lastSelectedVersion";
    public static final String CONFIG_GAME_TYPE = "gameType";
    public static final String CONFIG_GAME_VERSION = "gameVersion";
    public static final String CONFIG_GAME_LAST_FOLDER = "lastFolder";
    // TODO: Selection logic for game version.
    //  - Allow drag & drop files to select & configure game. (Problem with detected game version -> needing to figure out writes to an arbitrary config..? Perhaps choose a new config if detection occurs before setting occurs. Not sure tho.)

    private static final List<String> SELECT_GAME_TEXT = Arrays.asList("Welcome to FrogLord!",
            "FrogLord supports many games, start by choosing a game.");

    private static final List<String> SELECT_GAME_VERSION_TEXT = Arrays.asList(
            "Next, give FrogLord the information & files it needs to load the game data.",
            "PC game files are often found in the installation folder, eg: 'C:\\Program Files\\...'.",
            "Other games (especially console games) have their files inside a CD image. (iso, bin/cue, etc.)",
            "Those games will need to be extracted first with software such as ISOBuster, PowerISO, etc.",
            "Once all the information is ready, press the 'Load' button to load the game data.");

    private static final URL FXML_TEMPLATE_URL = FileUtils.getResourceURL("fxml/window-load-game.fxml");
    private static final FXMLLoader FXML_TEMPLATE_LOADER = new FXMLLoader(FXML_TEMPLATE_URL);

    public GameConfigController(Config gameConfigRoot) {
        super(null);
        this.gameConfigRoot = gameConfigRoot;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.loadButton.setDisable(true); // Disable load button until game says we're ready.
        this.gameVersionComboBox.setDisable(true); // Disable version selection until the game type is chosen.
        addHelpText(SELECT_GAME_TEXT); // Show the select game help text by default.
        this.gameTypeComboBox.setItems(FXCollections.observableArrayList(Constants.getGameTypes()));
        this.gameTypeComboBox.setConverter(new AbstractStringConverter<>(IGameType::getDisplayName));
        this.gameTypeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldType, newType) -> onGameTypeChange(oldType, newType));
        this.gameVersionComboBox.setConverter(new AbstractStringConverter<>(GameConfig::getDisplayName));
        this.gameVersionComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldVersion, newVersion) -> onGameVersionChange(oldVersion, newVersion));

        // Load previously active gameType.
        ConfigValueNode gameTypeNode = this.gameConfigRoot.getOptionalKeyValueNode(CONFIG_ROOT_LAST_SELECTED_GAME_TYPE);
        String gameTypeIdentifier = gameTypeNode != null ? gameTypeNode.getAsString() : null;
        if (!StringUtils.isNullOrWhiteSpace(gameTypeIdentifier)) {
            for (int i = 0; i < Constants.getGameTypes().size(); i++) {
                IGameType gameType = Constants.getGameTypes().get(i);
                if (Objects.equals(gameTypeIdentifier, gameType.getIdentifier())) {
                    this.gameTypeComboBox.getSelectionModel().select(gameType);
                    break;
                }
            }
        }
    }

    @FXML
    private void openTechSupportLink(ActionEvent event) {
        FrogLordApplication.getApplication().getHostServices().showDocument(Constants.HIGHWAY_FROGS_WEBSITE_URL);
    }

    @FXML
    private void openSourceCodeLink(ActionEvent event) {
        FrogLordApplication.getApplication().getHostServices().showDocument(Constants.SOURCE_CODE_REPOSITORY_URL);
    }

    @FXML
    private void exitMenu(ActionEvent event) {
        closeWindow();
    }

    @FXML
    private void loadGame(ActionEvent event) {
        IGameType gameType = getSelectedGameType();
        String gameVersion = this.selectedVersionConfigName;

        // Failsafe.
        if (gameType == null || StringUtils.isNullOrWhiteSpace(gameVersion) || this.activeConfigController == null || this.activeConfigController.isLoadButtonDisabled()) {
            FXUtils.makePopUp("Cannot load game without being fully configured!", AlertType.ERROR);
            return;
        }

        // Shutdown this menu.
        closeWindow();
        FrogLordApplication.saveMainConfig();

        // Setup the game instance.
        GameInstance newInstance = gameType.createGameInstance();
        ProgressBarComponent.openProgressBarWindow(newInstance, "Loading Game Data...", progressBar -> {
            try {
                gameType.loadGameInstance(newInstance, gameVersion, getOrCreateGameConfig(), getOrCreateInstanceConfig(), progressBar);
            } catch (Throwable th) {
                // Eat the error, we still want to boot into the main menu if an error occurs.
                Utils.handleError(newInstance.getLogger(), th, true, "Failed to load the game data.");
            }
        });


        // Shutdown the config viewer.
        newInstance.setupMainMenuWindow();
    }

    private void onGameTypeChange(IGameType oldGameType, IGameType newGameType) {
        if (oldGameType == newGameType)
            return;

        if (newGameType == null)
            this.loadButton.setDisable(true);

        // Set last selected game type.
        if (this.gameConfigRoot != null && newGameType != null)
            this.gameConfigRoot.getOrCreateKeyValueNode(CONFIG_ROOT_LAST_SELECTED_GAME_TYPE).setAsString(newGameType.getIdentifier());

        // Update the game version UI.
        if (newGameType != null) {
            updateGameTypeUI(); // Guarantee an update to the UI.

            List<GameConfig> versionConfigs = newGameType.getVersionConfigs();
            this.gameVersionComboBox.setDisable(false);
            this.gameVersionComboBox.setItems(FXCollections.observableArrayList(versionConfigs));

            // Select the last seen version.
            Config gameConfig = this.gameConfigRoot != null ? this.gameConfigRoot.getChildConfigByName(newGameType.getIdentifier()) : null;
            ConfigValueNode versionConfigNameNode = gameConfig != null ? gameConfig.getOptionalKeyValueNode(CONFIG_GAME_LAST_SELECTED_VERSION) : null;
            String versionConfigName = versionConfigNameNode != null ? versionConfigNameNode.getAsString() : null;

            for (int i = 0; i < versionConfigs.size(); i++) {
                GameConfig testGameConfig = versionConfigs.get(i);
                if (testGameConfig.getInternalName().equals(versionConfigName)) {
                    this.gameVersionComboBox.getSelectionModel().select(testGameConfig);
                    break;
                }
            }

            // The size of the window depends on how large the "Versions" ComboBox grows to.
            resizeWindow();
        } else {
            this.gameVersionComboBox.getSelectionModel().clearSelection();
            this.gameVersionComboBox.setItems(null);
            this.gameVersionComboBox.setDisable(true);

            // Update the game type specific UI.
            updateGameTypeUI();
        }
    }

    private void onGameVersionChange(GameConfig oldVersionConfig, GameConfig newVersionConfig) {
        if (Objects.equals(oldVersionConfig, newVersionConfig))
            return;

        this.selectedVersionConfigName = newVersionConfig != null ? newVersionConfig.getInternalName() : null;
        if (newVersionConfig == null) {
            this.loadButton.setDisable(true);
            return;
        }

        // Store the last selected option.
        Config gameConfig = this.gameConfigRoot.getOrCreateChildConfigByName(newVersionConfig.getGameType().getIdentifier());
        ConfigValueNode lastVersionNode = gameConfig.getOrCreateKeyValueNode(CONFIG_GAME_LAST_SELECTED_VERSION);
        lastVersionNode.setAsString(newVersionConfig.getInternalName());

        // Apply last working directory, if there's one configured.
        Config lastVersionConfig = gameConfig.getChildConfigByName(newVersionConfig.getInternalName());
        ConfigValueNode lastFolderPathAccessedNode = lastVersionConfig != null ? lastVersionConfig.getOptionalKeyValueNode(CONFIG_GAME_LAST_FOLDER) : null;
        String lastFolderPathAccessed = lastFolderPathAccessedNode != null ? lastFolderPathAccessedNode.getAsString() : null;
        if (!StringUtils.isNullOrWhiteSpace(lastFolderPathAccessed)) {
            File testFolder = new File(lastFolderPathAccessed);
            if (testFolder.isDirectory())
                FrogLordApplication.setWorkingDirectory(testFolder);
        }

        // Update the game type specific UI.
        updateGameTypeUI();
    }

    /**
     * Update the UI for the game type.
     */
    public void updateGameTypeUI() {
        IGameType gameType = getSelectedGameType();
        boolean useExistingController = this.activeConfigController != null && this.activeConfigController.getGameType() == gameType;
        if (this.activeConfigController != null && !useExistingController) {
            removeController(this.activeConfigController);
            this.activeConfigController = null;
            this.loadButton.setDisable(true); // Disable the load button for now.
        }

        // Clear the UI.
        this.topBox.getChildren().clear();
        this.bottomBox.getChildren().clear();

        // Create help UI.
        if (gameType == null) {
            addHelpText(SELECT_GAME_TEXT);
        } else {
            addHelpText(SELECT_GAME_VERSION_TEXT);
        }

        // Create new game-specific UI.
        if (gameType != null) {
            GameConfig gameConfig = this.gameVersionComboBox.getValue();
            if (gameConfig != null && gameConfig.getGameType() != gameType)
                gameConfig = null;

            if (useExistingController) {
                this.activeConfigController.addChildControllers(this.bottomBox.getChildren());
                this.activeConfigController.setActiveGameConfig(gameConfig, getOrCreateGameConfig());
            } else {
                GameConfigUIController<?> uiController = gameType.setupConfigUI(this);
                if (uiController == null)
                    throw new RuntimeException("Game Type '" + gameType.getDisplayName() + "' did not provide a GameConfigUIController, so the user interface cannot be created!!");

                uiController.loadController(this.bottomBox);
                uiController.setActiveGameConfig(gameConfig, getOrCreateGameConfig());
                addController(this.activeConfigController = uiController);
            }
        }

        if (this.activeConfigController != null)
            this.activeConfigController.updateLoadButton();

        // Set the per-game logo.
        ImageResource frogLordPerGameLogo = gameType != null ? gameType.getFrogLordLogo() : null;
        if (frogLordPerGameLogo == null)
            frogLordPerGameLogo = ImageResource.FROGLORD_LOGO_MAIN_LARGE;

        this.frogLordGameLogoView.setImage(frogLordPerGameLogo.getFxImage());
    }

    private void addHelpText(List<String> helpText) {
        for (int i = 0; i < helpText.size(); i++) {
            String newLine = helpText.get(i);
            Label newLabel = new Label(newLine);
            newLabel.setAlignment(Pos.CENTER_LEFT);
            this.topBox.getChildren().add(newLabel);
        }
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        if (this.activeConfigController != null)
            this.activeConfigController.updateLoadButton(); // Update whether the load button should be enabled or not.
    }

    /**
     * Gets or creates the configuration for the active selection.
     * @return gameConfig
     */
    public Config getOrCreateGameConfig() {
        if (this.gameConfigRoot == null)
            return this.unknownGameConfig;

        IGameType gameType = getSelectedGameType();
        if (gameType == null)
            return this.unknownGameConfig;

        Config perGameConfig = this.gameConfigRoot.getOrCreateChildConfigByName(gameType.getIdentifier());

        // Load the last selected version of a game, if there is one.
        ConfigValueNode lastVersionNode = perGameConfig.getOptionalKeyValueNode(CONFIG_GAME_LAST_SELECTED_VERSION);
        String lastVersion = lastVersionNode != null ? lastVersionNode.getAsString() : null;
        if (StringUtils.isNullOrWhiteSpace(lastVersion))
            return this.unknownGameConfig; // Don't know the version yet!

        return perGameConfig.getOrCreateChildConfigByName(lastVersion);
    }

    /**
     * Gets or creates the configuration for the instance which would be loaded for the active selection.
     * @return instanceConfig
     */
    public Config getOrCreateInstanceConfig() {
        return getOrCreateGameConfig().getOrCreateChildConfigByName(CONFIG_GAME_INSTANCE_DATA);
    }

    /**
     * Get the currently selected game type.
     */
    public IGameType getSelectedGameType() {
        return this.gameTypeComboBox.getValue();
    }

    /**
     * Selects the provided game version.
     * @param gameVersion the game version to select
     */
    public void selectVersion(GameConfig gameVersion) {
        if (gameVersion == null)
            throw new NullPointerException("gameVersion");

        if (this.gameTypeComboBox.getValue() != gameVersion.getGameType())
            this.gameTypeComboBox.getSelectionModel().select(gameVersion.getGameType());
        if (this.gameVersionComboBox.getValue() != gameVersion)
            this.gameVersionComboBox.getSelectionModel().select(gameVersion);
    }

    /**
     * Open the game config window.
     * @param configRoot the config root.
     */
    public static GameConfigController openGameConfigMenu(Config configRoot) {
        GameConfigController newController = new GameConfigController(configRoot);
        GameUIController.loadController(null, FXML_TEMPLATE_LOADER, newController);

        // Close the splash screen if there's one active.
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null)
            splash.close();

        GameUIController.openWindow(newController, "FrogLord " + Constants.VERSION, false);
        return newController;
    }

    /**
     * Represents a UI controller for any game's config screen.
     */
    @Getter
    public static abstract class GameConfigUIController<TGameConfig extends GameConfig> extends GameUIController<GameInstance> {
        @NonNull private final GameConfigController parentController;
        @NonNull private final Class<TGameConfig> gameConfigClass;
        private IGameType gameType;
        private TGameConfig activeGameConfig;
        private Config activeEditorConfig;

        public GameConfigUIController(@NonNull GameConfigController parentController, @NonNull Class<TGameConfig> gameConfigClass) {
            super(null);
            this.parentController = parentController;
            this.gameConfigClass = gameConfigClass;
        }

        /**
         * Sets which game config is currently active.
         * @param newGameConfig the game config to apply, if there is one
         * @param newEditorConfig the editor config to apply
         */
        public final void setActiveGameConfig(GameConfig newGameConfig, Config newEditorConfig) {
            if (newEditorConfig == null)
                throw new NullPointerException("newEditorConfig");
            if (newGameConfig != null && !this.gameConfigClass.isInstance(newGameConfig))
                throw new ClassCastException("Cannot treat " + Utils.getSimpleName(newGameConfig) + " as a(n) " + this.gameConfigClass.getSimpleName() + ".");
            if (newGameConfig != null && this.gameType != null && this.gameType != newGameConfig.getGameType())
                throw new IllegalArgumentException("Cannot apply GameType of '" + newGameConfig.getGameType().getDisplayName() + "' to " + Utils.getSimpleName(this) + ", which only accepts GameTypes of '" + this.gameType.getDisplayName() + "'.");

            TGameConfig oldGameConfig = this.activeGameConfig;
            Config oldEditorConfig = this.activeEditorConfig;
            this.activeGameConfig = this.gameConfigClass.cast(newGameConfig);
            this.activeEditorConfig = newEditorConfig;
            if (this.gameType == null && this.activeGameConfig != null)
                this.gameType = this.activeGameConfig.getGameType();

            try {
                onChangeGameConfig(oldGameConfig, oldEditorConfig, this.activeGameConfig, this.activeEditorConfig);
            } catch (Throwable th) {
                Utils.handleError(null, th, true, "Encountered an error processing the change to game configuration data!");
            }
        }

        /**
         * Called when the game configuration changes.
         * @param oldGameConfig the previous game config
         * @param oldEditorConfig the previous editor config
         * @param newGameConfig the current game config
         * @param newEditorConfig the current editor config
         */
        protected abstract void onChangeGameConfig(TGameConfig oldGameConfig, Config oldEditorConfig, TGameConfig newGameConfig, Config newEditorConfig);

        /**
         * Update the load button.
         */
        public void updateLoadButton() {
            this.parentController.getLoadButton().setDisable(isLoadButtonDisabled());
        }

        /**
         * Test if the load button should be disabled.
         */
        public abstract boolean isLoadButtonDisabled();

        @Override
        public void addController(GameUIController<?> childController) {
            super.addController(childController);

            Node childRootNode = childController.getRootNode();
            if (!this.parentController.getBottomBox().getChildren().contains(childRootNode))
                this.parentController.getBottomBox().getChildren().add(childRootNode);
        }

        @Override
        public boolean removeController(GameUIController<?> childController) {
            if (!super.removeController(childController))
                return false;

            this.parentController.getBottomBox().getChildren().remove(childController.getRootNode());
            return true;
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            // Do nothing by default.
        }
    }
}