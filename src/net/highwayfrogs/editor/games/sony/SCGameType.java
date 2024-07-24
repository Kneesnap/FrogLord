package net.highwayfrogs.editor.games.sony;

import javafx.scene.Node;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsConfig;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2Config;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2GameInstance;
import net.highwayfrogs.editor.games.sony.moonwarrior.MoonWarriorInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.gui.GameConfigController.GameConfigUIController;
import net.highwayfrogs.editor.gui.components.FileOpenBrowseComponent.GameConfigFileOpenBrowseComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A registry of different game types supported by FrogLord by Sony Cambridge.
 * Created by Kneesnap on 9/6/2023.
 */
public enum SCGameType implements IGameType {
    //HEADRUSH(null), // Aka Brains In Planes
    OLD_FROGGER("Old Frogger (Pre-Recode)", OldFroggerGameInstance::new, OldFroggerConfig::new, true),
    BEAST_WARS("Beast Wars: Transformers", BeastWarsInstance::new, BeastWarsConfig::new, true),
    FROGGER("Frogger: He's Back", FroggerGameInstance::new, FroggerConfig::new, false),
    //TAX_MAN(null),
    MEDIEVIL("MediEvil", MediEvilGameInstance::new, MediEvilConfig::new, true),
    //COMMON_TALES(null),
    MOONWARRIOR("Moon Warrior", MoonWarriorInstance::new, null, true),
    MEDIEVIL2("MediEvil II", MediEvil2GameInstance::new, MediEvil2Config::new, true),
    C12("C-12: Final Resistance", null, null, true);

    @Getter private final String displayName;
    private final Supplier<SCGameInstance> instanceMaker;
    private final Function<String, SCGameConfig> configMaker;
    @Getter private final String identifier;
    @Getter private final boolean showSaveWarning;

    public static final String CONFIG_MWD_PATH = "mwdFilePath";
    public static final String CONFIG_EXE_PATH = "executableFilePath";

    SCGameType(String displayName, Supplier<SCGameInstance> instanceMaker, Function<String, SCGameConfig> configMaker, boolean showSaveWarning) {
        this.displayName = displayName;
        this.instanceMaker = instanceMaker;
        this.configMaker = configMaker;
        this.identifier = name().toLowerCase(Locale.ROOT).replace("_", "");
        this.showSaveWarning = showSaveWarning;
    }

    /**
     * Test if this game was developed at or after the provided game.
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
     * Test if this game was developed at/before the provided game.
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
     * Test if this game was developed before the provided game.
     * This may not work perfectly with prototypes, but the general use of this is testing technical capabilities.
     * @param otherType The other game to test.
     * @return True iff the game was developed before the provided game.
     */
    public boolean isBefore(SCGameType otherType) {
        if (otherType == null)
            throw new RuntimeException("Cannot compare to null game type.");
        return otherType.ordinal() > ordinal();
    }

    @Override
    public SCGameInstance createGameInstance() {
        return this.instanceMaker != null ? this.instanceMaker.get() : new SCGameUnimplemented(this);
    }

    @Override
    public void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, ProgressBarComponent progressBar) {
        if (!(instance instanceof SCGameInstance))
            throw new ClassCastException("The provided instance was " + Utils.getSimpleName(instance) + ", which was not " + SCGameInstance.class.getSimpleName() + ".");

        String exeFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_EXE_PATH).getValue();
        if (Utils.isNullOrWhiteSpace(exeFilePath))
            throw new RuntimeException("Invalid exeFilePath.");

        String mwdFilePath = gameSetupConfig.hasKeyValueNode(CONFIG_MWD_PATH) ? gameSetupConfig.getKeyValueNodeOrError(CONFIG_MWD_PATH).getValue() : null;
        File mwdFile = mwdFilePath != null && mwdFilePath.length() > 0 ? new File(mwdFilePath) : null;
        if (mwdFile != null && (!mwdFile.exists() || !mwdFile.isFile()))
            throw new RuntimeException("Invalid mwdFilePath.");

        File exeFile = new File(exeFilePath);
        ((SCGameInstance) instance).loadGame(gameVersionConfigName, mwdFile, exeFile, progressBar);
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
    public GameConfigUIController setupConfigUI(GameConfigController controller, GameConfig gameConfig, Config config) {
        SCGameConfig scGameConfig = gameConfig instanceof SCGameConfig ? (SCGameConfig) gameConfig : null;
        return new SCGameConfigUI(controller, scGameConfig, config);
    }

    /**
     * The UI definition for the game.
     */
    public static class SCGameConfigUI extends GameConfigUIController {
        private final GameConfigFileOpenBrowseComponent mwdFileBrowseComponent;
        private final GameConfigFileOpenBrowseComponent exeFileBrowseComponent;

        public SCGameConfigUI(GameConfigController controller, SCGameConfig gameConfig, Config config) {
            super(controller, gameConfig);
            this.mwdFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, config, CONFIG_MWD_PATH, "Millennium WAD (.MWD)", "Please select a Millennium WAD", "Millennium WAD", "MWD");
            this.exeFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, config, CONFIG_EXE_PATH, "Game Executable (.EXE, SLUS, etc.)", "Please select the main executable", "Game Executable", "EXE", "dat", "04", "06", "26", "64", "65", "66", "99");
            loadController(null);
        }

        @Override
        public SCGameConfig getGameConfig() {
            return (SCGameConfig) super.getGameConfig();
        }

        @Override
        public boolean isLoadButtonDisabled() {
            return (Utils.isNullOrWhiteSpace(this.mwdFileBrowseComponent.getCurrentFilePath()) && (getGameConfig() == null || !getGameConfig().isMwdLooseFiles()))
                    || Utils.isNullOrWhiteSpace(this.exeFileBrowseComponent.getCurrentFilePath());
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            if (getGameConfig() == null || !getGameConfig().isMwdLooseFiles())
                addController(this.mwdFileBrowseComponent);
            addController(this.exeFileBrowseComponent);
        }
    }
}