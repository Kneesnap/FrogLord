package net.highwayfrogs.editor.games.konami.rescue;

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
 * Represents the game "Frogger's Adventures: The Rescue".
 * Created by Kneesnap on 8/8/2024.
 */
public class FroggerRescueGameType implements IGameType {
    // TODO: I'd like to support loading a list of game files, instead of a single main hfs file.
    public static final FroggerRescueGameType INSTANCE = new FroggerRescueGameType();
    private static final String CONFIG_MAIN_FILE_PATH = "mainFilePath";

    @Override
    public String getDisplayName() {
        return "Frogger's Adventures: The Rescue";
    }

    @Override
    public String getIdentifier() {
        return "rescue";
    }

    @Override
    public GameInstance createGameInstance() {
        return new FroggerRescueInstance();
    }

    @Override
    public void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, ProgressBarComponent progressBar) {
        if (!(instance instanceof FroggerRescueInstance))
            throw new ClassCastException("The provided instance was " + Utils.getSimpleName(instance) + ", when " + FroggerRescueInstance.class.getSimpleName() + " was required.");

        String mainFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_MAIN_FILE_PATH).getAsString();
        if (Utils.isNullOrWhiteSpace(mainFilePath))
            throw new IllegalArgumentException("Invalid mainFilePath.");

        File mainFile = new File(mainFilePath);
        ((FroggerRescueInstance) instance).loadGame(gameVersionConfigName, mainFile, progressBar);
    }

    @Override
    public GameConfig createConfig(String internalName) {
        return new GameConfig(internalName);
    }

    @Override
    public FroggerRescueGameConfigUI setupConfigUI(GameConfigController controller, Config config) {
        return new FroggerRescueGameConfigUI(controller, config);
    }

    /**
     * The UI definition for the game.
     */
    public static class FroggerRescueGameConfigUI extends GameConfigUIController {
        private final GameConfigFileOpenBrowseComponent binFileBrowseComponent;

        public FroggerRescueGameConfigUI(GameConfigController controller, Config config) {
            super(controller);
            this.binFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, config, CONFIG_MAIN_FILE_PATH, "Game Archive (*.hfs)", "Please locate and open a .HFS file", "Frogger's Adventures: The Rescue Data", "hfs");
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