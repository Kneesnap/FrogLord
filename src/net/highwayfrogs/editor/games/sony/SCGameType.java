package net.highwayfrogs.editor.games.sony;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.games.shared.basic.GameBuildInfo;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsConfig;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.c12.C12GameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2Config;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2GameInstance;
import net.highwayfrogs.editor.games.sony.moonwarrior.MoonWarriorInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.FileOpenBrowseComponent.GameConfigFileOpenBrowseComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.reader.RandomAccessFileSource;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A registry of different game types supported by FrogLord by Sony Cambridge.
 * Created by Kneesnap on 9/6/2023.
 */
public enum SCGameType implements IGameType {
    //HEADRUSH(null), // Aka Brains In Planes, 1996 -> 1997
    OLD_FROGGER("Old Frogger (Pre-Recode)", OldFroggerGameInstance::new, OldFroggerConfig::new, true), // 1996 <-> March/April 1997
    FROGGER("Frogger: He's Back", FroggerGameInstance::new, FroggerConfig::new, false), // April 1997 -> September 1997 (PC: March 1998)
    BEAST_WARS("Beast Wars: Transformers", BeastWarsInstance::new, BeastWarsConfig::new, true), // 1996 -> October 1997 (PC: March 1998)
    //TAX_MAN(null),
    MEDIEVIL("MediEvil", MediEvilGameInstance::new, MediEvilConfig::new, true), // 1995? -> October 1998
    //COMMON_TALES(null), // 1999
    MOONWARRIOR("Moon Warrior", MoonWarriorInstance::new, null, true), // 1999
    MEDIEVIL2("MediEvil II", MediEvil2GameInstance::new, MediEvil2Config::new, true), // October 1998 -> March 2000
    C12("C-12: Final Resistance", C12GameInstance::new, MediEvil2Config::new, true); // ?

    @Getter private final String displayName;
    private final Supplier<SCGameInstance> instanceMaker;
    private final Function<String, SCGameConfig> configMaker;
    @Getter private final String identifier;
    @Getter private final boolean showSaveWarning;
    private final Map<Long, SCGameConfig> executableCheckSums = new HashMap<>();

    public static final String CONFIG_MWD_PATH = "mwdFilePath";
    public static final String CONFIG_EXE_PATH = "executableFilePath";

    public static final BrowserFileType MWD_FILE_TYPE = new BrowserFileType("Millennium WAD", "MWD");
    public static final BrowserFileType EXECUTABLE_FILE_TYPE = new BrowserFileType("Game Executable",
            "EXE", "SLUS_*.*", "SLES_*.*", "SLPS_*.*", "SLPM_*.*", "SLED_*.*", "SCES_*.*", "SCUS_*.*", "dat");

    SCGameType(String displayName, Supplier<SCGameInstance> instanceMaker, Function<String, SCGameConfig> configMaker, boolean showSaveWarning) {
        this.displayName = displayName;
        this.instanceMaker = instanceMaker;
        this.configMaker = configMaker;
        this.identifier = name().toLowerCase(Locale.ROOT).replace("_", "");
        this.showSaveWarning = showSaveWarning;
    }

    /**
     * Test if this game was finished at or after the provided game.
     * This may not work perfectly with prototypes, but the general use of this is testing technical capabilities.
     * @param otherType The other game to test.
     * @return True iff the game was developed at or after the provided game.
     */
    public boolean isAtLeast(SCGameType otherType) {
        if (otherType == null)
            throw new RuntimeException("Cannot compare to null game type.");
        return ordinal() >= otherType.ordinal();
    }

    /**
     * Test if this game was finished at/before the provided game.
     * This may not work perfectly with prototypes, but the general use of this is testing technical capabilities.
     * @param otherType The other game to test.
     * @return True iff the game was developed before the provided game.
     */
    public boolean isAtOrBefore(SCGameType otherType) {
        if (otherType == null)
            throw new RuntimeException("Cannot compare to null game type.");
        return otherType.ordinal() >= ordinal();
    }

    /**
     * Test if this game was finished before the provided game.
     * This may not work perfectly with prototypes, but the general use of this is testing technical capabilities.
     * @param otherType The other game to test.
     * @return True iff the game was developed before the provided game.
     */
    public boolean isBefore(SCGameType otherType) {
        if (otherType == null)
            throw new RuntimeException("Cannot compare to null game type.");
        return otherType.ordinal() > ordinal();
    }

    /**
     * Test if this game was finished after the provided game.
     * This may not work perfectly with prototypes, but the general use of this is testing technical capabilities.
     * @param otherType The other game to test.
     * @return True iff the game was developed before the provided game.
     */
    public boolean isAfter(SCGameType otherType) {
        if (otherType == null)
            throw new RuntimeException("Cannot compare to null game type.");
        return ordinal() > otherType.ordinal();
    }

    @Override
    public ImageResource getFrogLordLogo() {
        switch (this) {
            case BEAST_WARS:
                return ImageResource.FROGLORD_LOGO_GAME_BEASTWARS_LARGE;
            case MEDIEVIL:
            case MEDIEVIL2:
                return ImageResource.FROGLORD_LOGO_GAME_MEDIEVIL_LARGE;
            default:
                return null;
        }
    }

    @Override
    public SCGameInstance createGameInstance() {
        return this.instanceMaker != null ? this.instanceMaker.get() : new SCGameUnimplemented(this);
    }

    @Override
    public void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, Config instanceConfig, ProgressBarComponent progressBar) {
        if (!(instance instanceof SCGameInstance))
            throw new ClassCastException("The provided instance was " + Utils.getSimpleName(instance) + ", which was not " + SCGameInstance.class.getSimpleName() + ".");

        String exeFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_EXE_PATH).getAsString();
        if (StringUtils.isNullOrWhiteSpace(exeFilePath))
            throw new RuntimeException("Invalid exeFilePath.");

        String mwdFilePath = gameSetupConfig.hasKeyValueNode(CONFIG_MWD_PATH) ? gameSetupConfig.getKeyValueNodeOrError(CONFIG_MWD_PATH).getAsString() : null;
        File mwdFile = mwdFilePath != null && mwdFilePath.length() > 0 ? new File(mwdFilePath) : null;
        if (mwdFile != null && (!mwdFile.exists() || !mwdFile.isFile()))
            throw new RuntimeException("Invalid mwdFilePath.");

        File exeFile = new File(exeFilePath);
        ((SCGameInstance) instance).loadGame(gameVersionConfigName, instanceConfig, mwdFile, exeFile, progressBar);
    }

    @Override
    public SCGameConfig createConfig(String configName) {
        return this.configMaker != null ? this.configMaker.apply(configName) : new SCGameConfig(configName);
    }

    /**
     * Check if the MWI contains a file checksum.
     */
    public boolean doesMwiHaveChecksum() {
        return isAtLeast(MEDIEVIL2);
    }

    @Override
    public SCGameConfigUI setupConfigUI(GameConfigController controller) {
        return new SCGameConfigUI(controller);
    }

    private void ensureExecutableChecksumLookup() {
        if (!this.executableCheckSums.isEmpty())
            return;

        SCGameConfig oldConfig;
        List<GameConfig> gameConfigs = getVersionConfigs();
        for (int i = 0; i < gameConfigs.size(); i++) {
            SCGameConfig gameConfig = (SCGameConfig) gameConfigs.get(i);
            for (int j = 0; j < gameConfig.getExecutableChecksums().length; j++) {
                long checksum = gameConfig.getExecutableChecksums()[j];
                if ((oldConfig = this.executableCheckSums.put(checksum, gameConfig)) != null)
                    Utils.getInstanceLogger().warning("There was an executable checksum collision! %d -> [Old: %s, New: %s]", checksum, oldConfig.getInternalName(), gameConfig.getInternalName());
            }
        }
    }

    /**
     * Resolves a SCGameConfig by its executable checksum.
     * @param checksum the checksum to resolve
     * @return gameConfig, or null
     */
    public SCGameConfig getConfigByExecutableChecksum(long checksum) {
        ensureExecutableChecksumLookup();
        return this.executableCheckSums.get(checksum);
    }

    /**
     * Returns the expected mwd header file name for the given game type, if one is known
     * @return headerFileName
     */
    public String getMwdHeaderFileName() {
        switch (this) {
            case FROGGER:
                return "frogpsx.h";
            case MEDIEVIL:
                return "medres.h";
            case MEDIEVIL2:
            case C12:
                return "projfile.h";
            default:
                return null;
        }
    }

    /**
     * Returns the expected vlo header file name for the given game type, if one is known
     * @return headerFileName
     */
    public String getVloHeaderFileName() {
        switch (this) {
            case FROGGER:
                return "frogvram.h";
            case MEDIEVIL:
                return "medvlo.h";
            case MEDIEVIL2:
                return "med2vlo.h";
            case C12:
                return "roninvlo.h";
            default:
                return null;
        }
    }

    /**
     * The UI definition for the game.
     */
    public static class SCGameConfigUI extends GameConfigUIController<SCGameConfig> {
        private final GameConfigFileOpenBrowseComponent mwdFileBrowseComponent;
        private final GameConfigFileOpenBrowseComponent exeFileBrowseComponent;

        public SCGameConfigUI(GameConfigController controller) {
            super(controller, SCGameConfig.class);
            this.mwdFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, CONFIG_MWD_PATH, "Millennium WAD (.MWD)", "Please select a Millennium WAD", MWD_FILE_TYPE, this::validateSelectedMwdFile);
            this.exeFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, CONFIG_EXE_PATH, "Game Executable (.EXE, SLUS, etc.)", "Please select the main executable", EXECUTABLE_FILE_TYPE, this::validateSelectedExecutable);
        }

        private boolean shouldEnableMwdFileBrowser() {
            // When the game config is null, we choose to disable the MWD file path, because it can't always identify which version of the game to load.
            // So, by requiring the user to start with the exe, we'll ensure it will choose a version for them.
            SCGameConfig gameConfig = getActiveGameConfig();
            return gameConfig != null && !gameConfig.isMwdLooseFiles();
        }

        @Override
        public boolean isLoadButtonDisabled() {
            return (shouldEnableMwdFileBrowser() && StringUtils.isNullOrWhiteSpace(this.mwdFileBrowseComponent.getCurrentFilePath()))
                    || StringUtils.isNullOrWhiteSpace(this.exeFileBrowseComponent.getCurrentFilePath());
        }

        @Override
        protected void onChangeGameConfig(SCGameConfig oldGameConfig, Config oldEditorConfig, SCGameConfig newGameConfig, Config newEditorConfig) {
            this.mwdFileBrowseComponent.setDisable(!shouldEnableMwdFileBrowser());
            this.mwdFileBrowseComponent.resetFilePath();
            this.exeFileBrowseComponent.resetFilePath();
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            addController(this.mwdFileBrowseComponent);
            addController(this.exeFileBrowseComponent);
        }

        @SneakyThrows
        @SuppressWarnings("IfStatementMissingBreakInLoop")
        private boolean validateSelectedExecutable(String newFilePath, File newFile) {
            SCGameType gameType = getGameType() instanceof SCGameType ? (SCGameType) getGameType() : null;
            if (gameType != null) {
                long exeChecksum = DataUtils.getCRC32(newFile);

                boolean foundChecksum = false;
                if (getActiveGameConfig() != null) {
                    long[] checksumsAvailable = getActiveGameConfig().getExecutableChecksums();
                    for (int j = 0; !foundChecksum && j < checksumsAvailable.length; j++)
                        if (checksumsAvailable[j] == exeChecksum)
                            foundChecksum = true;
                }

                // Try to read any configuration stored in the executable.
                Config executableConfig;
                try (RandomAccessFileSource fileSource = new RandomAccessFileSource(newFile)) {
                    DataReader reader = new DataReader(fileSource);
                    executableConfig = FileUtils.loadConfigDataFromExecutable(reader, newFile.getName());
                }

                if (executableConfig != null) {
                    applyBuildInfo(executableConfig);
                } else if (!foundChecksum) {
                    SCGameConfig gameConfig = gameType.getConfigByExecutableChecksum(exeChecksum);
                    if (gameConfig != null) { // Found it.
                        getParentController().selectVersion(gameConfig);
                    } else {
                        // Try to find the hash in other games.
                        SCGameConfig otherGameTypeConfig = null;
                        for (SCGameType otherGameType : SCGameType.values())
                            if ((otherGameTypeConfig = otherGameType.getConfigByExecutableChecksum(exeChecksum)) != null)
                                break;

                        if (otherGameTypeConfig != null) {
                            // Don't select the version, since it overwrite the path on the previous game, instead of the correct place.
                            FXUtils.showPopup(AlertType.ERROR, "Wrong game selected.", "That file is for " + otherGameTypeConfig.getGameType().getDisplayName() + ", not " + getGameType().getDisplayName() + ".");
                            return false;
                        } else {
                            Utils.getInstanceLogger().warning("The provided executable was not registered, and it had a checksum of: %d.", exeChecksum);
                        }
                    }
                }
            }

            return true;
        }

        @SneakyThrows
        private boolean validateSelectedMwdFile(String newFilePath, File newFile) {
            long fileSize = newFile.length();
            if (fileSize < Constants.CD_SECTOR_SIZE) {
                FXUtils.showPopup(AlertType.ERROR, "Invalid file.", "The selected file does not appear to be a valid MWD file.");
                return false;
            }

            try (RandomAccessFile fileReader = new RandomAccessFile(newFile, "r")) {
                byte[] signature = new byte[MWDFile.FILE_SIGNATURE.length()];
                fileReader.read(signature);

                if (!DataUtils.testSignature(signature, MWDFile.FILE_SIGNATURE)) {
                    FXUtils.showPopup(AlertType.ERROR, "Invalid file.", "The selected file does not appear to be a valid MWD file.");
                    return false;
                }

                // Read MWD build note.
                int bytesRead;
                fileReader.seek(MWDFile.BUILD_NOTES_START_OFFSET);
                byte[] buildNoteBytes = new byte[MWDFile.BUILD_NOTES_SIZE];
                if ((bytesRead = fileReader.read(buildNoteBytes)) != buildNoteBytes.length)
                    throw new RuntimeException("Expected to read " + buildNoteBytes.length + " bytes for the build note, but only read " + bytesRead + " bytes instead!");

                if (!parseBuildInfo(buildNoteBytes))
                    return false;
            }

            return true;
        }

        private boolean parseBuildInfo(byte[] configBytes) {
            // Find the end of the string.
            int endIndex;
            for (endIndex = 0; endIndex < configBytes.length; endIndex++)
                if (configBytes[endIndex] == 0)
                    break;

            // Read the build info, and use that to determine the version.
            // This only applies to versions of the game saved by FrogLord.
            String buildComment = new String(configBytes, 0, endIndex, StandardCharsets.US_ASCII);
            Config buildCommentConfig = Config.loadConfigFromString(buildComment, "GameData");
            return applyBuildInfo(buildCommentConfig);
        }

        private boolean applyBuildInfo(Config config) {
            Config buildInfoConfig = config != null ? config.getChildConfigByName(GameBuildInfo.CONFIG_KEY_ROOT_NAME) : null;
            if (buildInfoConfig != null) {
                GameBuildInfo<? extends SCGameInstance> buildInfo = new GameBuildInfo<>(buildInfoConfig);

                // Ensure correct GameType.
                if (!buildInfo.getGameType().equalsIgnoreCase(getGameType().getIdentifier())) {
                    FXUtils.showPopup(AlertType.ERROR, "Wrong game selected.", "That file is for " + StringUtils.capitalize(buildInfo.getGameType()) + ", not " + getGameType().getDisplayName() + ".");
                    return false;
                }

                GameConfig foundConfig = getGameType().getVersionConfigByName(buildInfo.getGameVersion());
                if (foundConfig == null) {
                    FXUtils.showPopup(AlertType.ERROR, "", "That file is an unknown version: '" + buildInfo.getGameVersion() + "'.\nWas it saved with a different version of FrogLord?");
                    return false;
                }

                // Automatically become the correct version.
                getParentController().selectVersion(foundConfig);
            }

            return true;
        }
    }
}