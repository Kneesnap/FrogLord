package net.highwayfrogs.editor.games.renderware.game;

import javafx.scene.Node;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.components.FolderBrowseComponent.GameConfigFolderBrowseComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Represents a generic RenderWare game.
 * Created by Kneesnap on 8/18/2024.
 */
public class RwGenericGameType implements IGameType {
    public static final RwGenericGameType INSTANCE = new RwGenericGameType();
    private static final String CONFIG_MAIN_FOLDER_PATH = "mainFolderPath";

    @Override
    public String getDisplayName() {
        return "RenderWare Engine";
    }

    @Override
    public String getIdentifier() {
        return "renderware";
    }

    @Override
    public RwGenericGameInstance createGameInstance() {
        return new RwGenericGameInstance();
    }

    @Override
    public void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, ProgressBarComponent progressBar) {
        if (!(instance instanceof RwGenericGameInstance))
            throw new ClassCastException("The provided instance was " + Utils.getSimpleName(instance) + ", when " + RwGenericGameInstance.class.getSimpleName() + " was required.");

        String mainFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_MAIN_FOLDER_PATH).getAsString();
        if (StringUtils.isNullOrWhiteSpace(mainFilePath))
            throw new IllegalArgumentException("Invalid mainFolderPath.");

        File mainFile = new File(mainFilePath);
        ((RwGenericGameInstance) instance).loadGame(gameVersionConfigName, mainFile, progressBar);
    }

    @Override
    public GameConfig createConfig(String internalName) {
        return new GameConfig(internalName);
    }

    @Override
    public RwGenericGameConfigUI setupConfigUI(GameConfigController controller, GameConfig gameConfig, Config config) {
        return new RwGenericGameConfigUI(controller, gameConfig, config);
    }

    /**
     * The UI definition for the game.
     */
    public static class RwGenericGameConfigUI extends GameConfigUIController {
        private final GameConfigFolderBrowseComponent binFileBrowseComponent;

        public RwGenericGameConfigUI(GameConfigController controller, GameConfig gameConfig, Config config) {
            super(controller, gameConfig);
            this.binFileBrowseComponent = new GameConfigFolderBrowseComponent(this, config, CONFIG_MAIN_FOLDER_PATH, "Game Data Folder", "Please locate the folder containing RenderWare game files", false);
            loadController(null);
        }

        @Override
        public boolean isLoadButtonDisabled() {
            return StringUtils.isNullOrWhiteSpace(this.binFileBrowseComponent.getCurrentFolderPath());
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            addController(this.binFileBrowseComponent);
        }
    }
}