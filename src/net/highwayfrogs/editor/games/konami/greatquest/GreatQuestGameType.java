package net.highwayfrogs.editor.games.konami.greatquest;

import javafx.scene.Node;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.FileOpenBrowseComponent.GameConfigFileOpenBrowseComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Represents all game instances of "Frogger: The Great Quest".
 * Created by Kneesnap on 4/10/2024.
 */
public class GreatQuestGameType implements IGameType {
    public static final GreatQuestGameType INSTANCE = new GreatQuestGameType();
    private static final String CONFIG_BIN_PATH = "binFilePath";
    @Override
    public String getDisplayName() {
        return "Frogger: The Great Quest";
    }

    @Override
    public String getIdentifier() {
        return "greatquest";
    }

    @Override
    public GameInstance createGameInstance() {
        return new GreatQuestInstance();
    }

    @Override
    public ImageResource getFrogLordLogo() {
        return ImageResource.FROGLORD_LOGO_GAME_GREAT_QUEST_LARGE;
    }

    @Override
    public void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, Config instanceConfig, ProgressBarComponent progressBar) {
        if (!(instance instanceof GreatQuestInstance))
            throw new ClassCastException("The provided instance was " + Utils.getSimpleName(instance) + ", when " + GreatQuestInstance.class.getSimpleName() + " was required.");

        String binFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_BIN_PATH).getAsString();
        if (StringUtils.isNullOrWhiteSpace(binFilePath))
            throw new IllegalArgumentException("Invalid binFilePath.");

        File binFile = new File(binFilePath);
        ((GreatQuestInstance) instance).loadGame(gameVersionConfigName, instanceConfig, binFile, progressBar);
    }

    @Override
    public GameConfig createConfig(String internalName) {
        return new GreatQuestConfig(internalName);
    }

    @Override
    public GreatQuestGameConfigUI setupConfigUI(GameConfigController controller, GameConfig gameConfig, Config config) {
        return new GreatQuestGameConfigUI(controller, gameConfig, config);
    }

    /**
     * The UI definition for the game.
     */
    public static class GreatQuestGameConfigUI extends GameConfigUIController {
        private final GameConfigFileOpenBrowseComponent binFileBrowseComponent;

        public GreatQuestGameConfigUI(GameConfigController controller, GameConfig gameConfig, Config config) {
            super(controller, gameConfig);
            this.binFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, config, CONFIG_BIN_PATH, "Game Archive (.bin)", "Please locate and open 'data.bin'", "Frogger The Great Quest Data", "bin");
            loadController(null);
        }

        @Override
        public boolean isLoadButtonDisabled() {
            return StringUtils.isNullOrWhiteSpace(this.binFileBrowseComponent.getCurrentFilePath());
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            addController(this.binFileBrowseComponent);
        }
    }
}