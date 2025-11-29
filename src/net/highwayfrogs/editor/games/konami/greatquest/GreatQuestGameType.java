package net.highwayfrogs.editor.games.konami.greatquest;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.FileOpenBrowseComponent.GameConfigFileOpenBrowseComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * Represents all game instances of "Frogger: The Great Quest".
 * Created by Kneesnap on 4/10/2024.
 */
public class GreatQuestGameType implements IGameType {
    public static final GreatQuestGameType INSTANCE = new GreatQuestGameType();
    private static final String CONFIG_BIN_PATH = "binFilePath";

    public static final BrowserFileType GAME_ARCHIVE_FILE_TYPE = new BrowserFileType("Frogger The Great Quest Data", "bin");

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
    public GreatQuestConfig createConfig(String internalName) {
        return new GreatQuestConfig(internalName);
    }

    @Override
    public GreatQuestGameConfigUI setupConfigUI(GameConfigController controller) {
        return new GreatQuestGameConfigUI(controller);
    }

    /**
     * The UI definition for the game.
     */
    public static class GreatQuestGameConfigUI extends GameConfigUIController<GreatQuestConfig> {
        private final GameConfigFileOpenBrowseComponent binFileBrowseComponent;

        public GreatQuestGameConfigUI(GameConfigController controller) {
            super(controller, GreatQuestConfig.class);
            this.binFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, CONFIG_BIN_PATH, "Game Archive (.bin)", "Please locate and open 'data.bin'", GAME_ARCHIVE_FILE_TYPE, this::verifyDataBinFileLooksOkay);
        }

        @Override
        protected void onChangeGameConfig(GreatQuestConfig oldGameConfig, Config oldEditorConfig, GreatQuestConfig newGameConfig, Config newEditorConfig) {
            this.binFileBrowseComponent.resetFilePath();
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


        @SneakyThrows
        private boolean verifyDataBinFileLooksOkay(String newFilePath, File newFile) {
            long fileSize = newFile.length();

            try (RandomAccessFile fileReader = new RandomAccessFile(newFile, "r")) {
                int fileCountA = fileReader.readShort();
                int fileCountB = fileReader.readShort();

                if (FileUtils.isProbablySonyIso(fileReader)) {
                    FXUtils.showPopup(AlertType.ERROR, "Invalid .bin file", "That file appears to be a CD image, not data.bin.\ndata.bin can be extracted with software such as ISOBuster.");
                    return false;
                }

                // Check if the final bytes are a FrogLord signature.
                fileReader.seek(fileSize - GreatQuestModData.SIGNATURE.length() - Constants.INTEGER_SIZE);
                byte[] signatureBytes = new byte[GreatQuestModData.SIGNATURE.length()];
                fileReader.read(signatureBytes);
                boolean hasFrogLordSignature = DataUtils.testSignature(signatureBytes, GreatQuestModData.SIGNATURE);
                fileReader.read(signatureBytes, 0, Constants.INTEGER_SIZE);
                int headerAddress = DataUtils.readIntFromBytes(signatureBytes, 0);

                // Check if the end of the file has something that looks like a null-terminated string in the form that Great Quest does.
                String lastFilePath = null;
                if (fileSize > GreatQuestAssetBinFile.NAME_SIZE) {
                    fileReader.seek((hasFrogLordSignature ? headerAddress : fileSize) - GreatQuestAssetBinFile.NAME_SIZE);

                    int bytesRead;
                    byte[] bytesAtEndOfFile = new byte[GreatQuestAssetBinFile.NAME_SIZE];
                    if ((bytesRead = fileReader.read(bytesAtEndOfFile)) != bytesAtEndOfFile.length)
                        throw new RuntimeException("Failed to read " + bytesAtEndOfFile.length + " bytes from the end of the file, only " + bytesRead + " were read.");

                    int pathEndIndex = -1;
                    boolean lastPartOfFileLooksLikeFilePath = false;
                    for (int i = 0; i < bytesAtEndOfFile.length; i++) {
                        if (bytesAtEndOfFile[i] != 0) {
                            if (pathEndIndex >= 0) { // Only after the end of the path has been seen.
                                lastPartOfFileLooksLikeFilePath = false;
                                break;
                            } else if (i > 4) { // Only after a few valid looking characters do we consider it valid.
                                lastPartOfFileLooksLikeFilePath = true;
                            }
                        } else if (pathEndIndex < 0) {
                            pathEndIndex = i;
                        }
                    }

                    lastFilePath = lastPartOfFileLooksLikeFilePath && pathEndIndex >= 0 ? new String(bytesAtEndOfFile, 0, pathEndIndex, StandardCharsets.US_ASCII) : null;
                }

                // If this doesn't look like a valid data.bin, alert!
                // While it's technically possible it could have more than 65535 files, I don't think this is likely to ever happen.
                if (fileSize < DataSizeUnit.MEGABYTE.getIncrement() * 10 || fileCountA == 0 || fileCountB != 0 || lastFilePath == null) {
                    FXUtils.showPopup(AlertType.ERROR, "Invalid .bin file", "That file does not appear to be a valid 'data.bin'.\nPlease review the instructions for obtaining 'data.bin'.");
                    return false;
                }

                GreatQuestConfig greatQuestConfig = getActiveGameConfig();
                if (greatQuestConfig != null && !lastFilePath.startsWith(greatQuestConfig.getStrippedHostRootPath())) {
                    StringBuilder errorMessage = new StringBuilder("That data.bin is not applicable to '")
                            .append(greatQuestConfig.getDisplayName()).append("'.\n");

                    int validVersions = 0;
                    for (GameConfig testConfig : INSTANCE.getVersionConfigs()) {
                        if (!(testConfig instanceof GreatQuestConfig))
                            continue;

                        GreatQuestConfig gqTestConfig = (GreatQuestConfig) testConfig;
                        if (lastFilePath.startsWith(gqTestConfig.getStrippedHostRootPath())) {
                            if (validVersions++ == 0)
                                errorMessage.append("To open, choose one of the following versions(s):");
                            errorMessage.append("\n - ").append(gqTestConfig.getDisplayName());
                        }
                    }

                    if (validVersions == 0)
                        errorMessage.append("The provided data file is either not valid,\n or FrogLord may not support this version yet.");

                    getLogger().info("The file path used to invalidate the data.bin was '%s'.", lastFilePath);
                    getLogger().info("It was tested against '%s'.", greatQuestConfig.getHostRootPath());
                    FXUtils.showPopup(AlertType.ERROR, "Wrong version for .bin file", errorMessage.toString());
                    return false;
                }
            }

            return true;
        }
    }
}