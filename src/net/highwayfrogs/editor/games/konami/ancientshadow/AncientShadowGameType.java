package net.highwayfrogs.editor.games.konami.ancientshadow;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GamePlatform;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.FileOpenBrowseComponent.GameConfigFileOpenBrowseComponent;
import net.highwayfrogs.editor.gui.components.FolderBrowseComponent.GameConfigFolderBrowseComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Represents the game "Frogger Ancient Shadow".
 * Created by Kneesnap on 8/4/2024.
 */
public class AncientShadowGameType implements IGameType {
    public static final AncientShadowGameType INSTANCE = new AncientShadowGameType();
    private static final String CONFIG_MAIN_FILE_PATH = "mainFilePath"; // gamedata.bin (console versions) or the folder containing hfs files on PC.

    public static final BrowserFileType ANCIENT_SHADOW_FILE_TYPE = new BrowserFileType("Frogger Ancient Shadow Data", "gamedata.bin", "hfs");

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
    public void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, Config instanceConfig, ProgressBarComponent progressBar) {
        if (!(instance instanceof AncientShadowInstance))
            throw new ClassCastException("The provided instance was " + Utils.getSimpleName(instance) + ", when " + AncientShadowInstance.class.getSimpleName() + " was required.");

        String mainFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_MAIN_FILE_PATH).getAsString();
        if (StringUtils.isNullOrWhiteSpace(mainFilePath))
            throw new IllegalArgumentException("Invalid mainFilePath.");

        File mainFile = new File(mainFilePath);
        ((AncientShadowInstance) instance).loadGame(gameVersionConfigName, instanceConfig, mainFile, progressBar);
    }

    @Override
    public GameConfig createConfig(String internalName) {
        return new GameConfig(internalName);
    }

    @Override
    public AncientShadowGameConfigUI setupConfigUI(GameConfigController controller) {
        return new AncientShadowGameConfigUI(controller);
    }

    @Override
    public ImageResource getFrogLordLogo() {
        return ImageResource.FROGLORD_LOGO_GAME_ANCIENT_SHADOW_LARGE;
    }

    /**
     * The UI definition for the game.
     */
    public static class AncientShadowGameConfigUI extends GameConfigUIController<GameConfig> {
        private final GameConfigFileOpenBrowseComponent binFileBrowseComponent;
        private final GameConfigFolderBrowseComponent folderBrowseComponent;

        public AncientShadowGameConfigUI(GameConfigController controller) {
            super(controller, GameConfig.class);
            this.binFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, CONFIG_MAIN_FILE_PATH, "Game Archive (gamedata.bin/*.hfs)", "Please locate and open 'gamedata.bin' (Or a .HFS file)", ANCIENT_SHADOW_FILE_TYPE, this::validateBinFileLooksOkay);
            this.folderBrowseComponent = new GameConfigFolderBrowseComponent(this, CONFIG_MAIN_FILE_PATH, "Game Data Folder", "Please locate the folder containing game data (.HFS files)", null);
        }

        @Override
        protected void onChangeGameConfig(GameConfig oldGameConfig, Config oldEditorConfig, GameConfig newGameConfig, Config newEditorConfig) {
            if (newGameConfig != null && newGameConfig.getPlatform() == GamePlatform.WINDOWS) {
                removeController(this.binFileBrowseComponent);
                this.folderBrowseComponent.resetFolderPath();
                addController(this.folderBrowseComponent);
            } else {
                removeController(this.folderBrowseComponent);
                this.binFileBrowseComponent.resetFilePath();
                addController(this.binFileBrowseComponent);
            }
        }

        @Override
        public boolean isLoadButtonDisabled() {
            return (this.binFileBrowseComponent != null && StringUtils.isNullOrWhiteSpace(this.binFileBrowseComponent.getCurrentFilePath()))
                    || (this.folderBrowseComponent != null && StringUtils.isNullOrWhiteSpace(this.folderBrowseComponent.getCurrentFolderPath()));
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            onChangeGameConfig(getActiveGameConfig(), getActiveEditorConfig(), getActiveGameConfig(), getActiveEditorConfig());
        }

        @SneakyThrows
        private boolean validateBinFileLooksOkay(String newFilePath, File newFile) {
            long fileLength = newFile.length();
            if (fileLength < Constants.INTEGER_SIZE) {
                FXUtils.makePopUp("That file does not appear to be a valid .hfs file/gamedata.bin.", AlertType.ERROR);
                return false;
            }

            try (RandomAccessFile reader = new RandomAccessFile(newFile, "r")) {
                byte[] signature = new byte[HFSFile.SIGNATURE.length()];
                reader.read(signature);

                if (FileUtils.isProbablySonyIso(reader)) {
                    FXUtils.makePopUp("That file appears to be a CD image, not a game data file.\nExtract the game files from the CD image first.", AlertType.ERROR);
                    return false;
                }

                if (!DataUtils.testSignature(signature, HFSFile.SIGNATURE)) {
                    FXUtils.makePopUp("That file does not appear to be a valid .hfs file/gamedata.bin.", AlertType.ERROR);
                    return false;
                }
            }

            return true;
        }
    }
}