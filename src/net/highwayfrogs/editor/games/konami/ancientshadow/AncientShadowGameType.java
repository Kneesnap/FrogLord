package net.highwayfrogs.editor.games.konami.ancientshadow;

import javafx.scene.Node;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GamePlatform;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.components.FileOpenBrowseComponent.GameConfigFileOpenBrowseComponent;
import net.highwayfrogs.editor.gui.components.FolderBrowseComponent.GameConfigFolderBrowseComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Represents the game "Frogger Ancient Shadow".
 * Created by Kneesnap on 8/4/2024.
 */
public class AncientShadowGameType implements IGameType {
    public static final AncientShadowGameType INSTANCE = new AncientShadowGameType();
    private static final String CONFIG_MAIN_FILE_PATH = "mainFilePath"; // gamedata.bin (console versions) or the folder containing hfs files on PC.

    @Override
    public String getDisplayName() {
        return "Frogger Ancient Shadow";
    }

    @Override
    public String getIdentifier() {
        return "ancientshadow";
    }

    @Override
    public GameInstance createGameInstance() {
        return new AncientShadowInstance();
    }

    @Override
    public void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, ProgressBarComponent progressBar) {
        if (!(instance instanceof AncientShadowInstance))
            throw new ClassCastException("The provided instance was " + Utils.getSimpleName(instance) + ", when " + AncientShadowInstance.class.getSimpleName() + " was required.");

        String mainFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_MAIN_FILE_PATH).getAsString();
        if (StringUtils.isNullOrWhiteSpace(mainFilePath))
            throw new IllegalArgumentException("Invalid mainFilePath.");

        File mainFile = new File(mainFilePath);
        ((AncientShadowInstance) instance).loadGame(gameVersionConfigName, mainFile, progressBar);
    }

    @Override
    public GameConfig createConfig(String internalName) {
        return new GameConfig(internalName);
    }

    @Override
    public AncientShadowGameConfigUI setupConfigUI(GameConfigController controller, GameConfig gameConfig, Config config) {
        return new AncientShadowGameConfigUI(controller, gameConfig, config);
    }

    /**
     * The UI definition for the game.
     */
    public static class AncientShadowGameConfigUI extends GameConfigUIController {
        private final GameConfigFileOpenBrowseComponent binFileBrowseComponent;
        private final GameConfigFolderBrowseComponent folderBrowseComponent;

        public AncientShadowGameConfigUI(GameConfigController controller, GameConfig gameConfig, Config config) {
            super(controller, gameConfig);
            if (gameConfig != null && gameConfig.getPlatform() == GamePlatform.WINDOWS) {
                this.binFileBrowseComponent = null;
                this.folderBrowseComponent = new GameConfigFolderBrowseComponent(this, config, CONFIG_MAIN_FILE_PATH, "Game Data Folder", "Please locate the folder containing game data (.HFS files)", false);
            } else {
                this.binFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, config, CONFIG_MAIN_FILE_PATH, "Game Archive (gamedata.bin/*.hfs)", "Please locate and open 'gamedata.bin' (Or a .HFS file)", "Frogger Ancient Shadow Data", "gamedata.bin", "hfs");
                this.folderBrowseComponent = null;
            }
            loadController(null);
        }

        @Override
        public boolean isLoadButtonDisabled() {
            return (this.binFileBrowseComponent != null && StringUtils.isNullOrWhiteSpace(this.binFileBrowseComponent.getCurrentFilePath()))
                    || (this.folderBrowseComponent != null && StringUtils.isNullOrWhiteSpace(this.folderBrowseComponent.getCurrentFolderPath()));
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            if (this.binFileBrowseComponent != null)
                addController(this.binFileBrowseComponent);
            if (this.folderBrowseComponent != null)
                addController(this.folderBrowseComponent);
        }
    }
}