package net.highwayfrogs.editor.games.konami.ancientshadow;

import javafx.scene.Node;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.components.FileOpenBrowseComponent.GameConfigFileOpenBrowseComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Represents the game "Frogger Ancient Shadow".
 * Created by Kneesnap on 8/4/2024.
 */
public class AncientShadowGameType implements IGameType {
    public static final AncientShadowGameType INSTANCE = new AncientShadowGameType();
    private static final String CONFIG_MAIN_FILE_PATH = "mainFilePath"; // gamedata.bin (console versions) or any HFS file. (In the future we'll probably want to support reading all HFS files in a directory as seen in the PC version.)

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
        if (Utils.isNullOrWhiteSpace(mainFilePath))
            throw new IllegalArgumentException("Invalid mainFilePath.");

        File mainFile = new File(mainFilePath);
        ((AncientShadowInstance) instance).loadGame(gameVersionConfigName, mainFile, progressBar);
    }

    @Override
    public GameConfig createConfig(String internalName) {
        return new GameConfig(internalName);
    }

    @Override
    public AncientShadowGameConfigUI setupConfigUI(GameConfigController controller, Config config) {
        return new AncientShadowGameConfigUI(controller, config);
    }

    /**
     * The UI definition for the game.
     */
    public static class AncientShadowGameConfigUI extends GameConfigUIController {
        private final GameConfigFileOpenBrowseComponent binFileBrowseComponent;

        public AncientShadowGameConfigUI(GameConfigController controller, Config config) {
            super(controller);
            this.binFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, config, CONFIG_MAIN_FILE_PATH, "Game Archive (gamedata.bin/*.hfs)", "Please locate and open 'gamedata.bin' (Or a .HFS file)", "Frogger The Great Quest Data", "gamedata.bin", "hfs");
            loadController(null);
        }

        @Override
        public boolean isLoadButtonDisabled() {
            return Utils.isNullOrWhiteSpace(this.binFileBrowseComponent.getCurrentFilePath());
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            addController(this.binFileBrowseComponent);
        }
    }
}