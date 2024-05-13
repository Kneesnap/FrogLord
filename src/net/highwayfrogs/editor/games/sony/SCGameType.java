package net.highwayfrogs.editor.games.sony;

import javafx.scene.Node;
import lombok.Getter;
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
@Getter
public enum SCGameType implements IGameType {
    //HEADRUSH(null), // Aka Brains In Planes
    OLD_FROGGER(OldFroggerGameInstance::new, OldFroggerConfig::new),
    BEAST_WARS(BeastWarsInstance::new, BeastWarsConfig::new),
    FROGGER(FroggerGameInstance::new, FroggerConfig::new),
    //TAX_MAN(null),
    MEDIEVIL(MediEvilGameInstance::new, MediEvilConfig::new),
    //COMMON_TALES(null),
    MOONWARRIOR(MoonWarriorInstance::new, null),
    MEDIEVIL2(MediEvil2GameInstance::new, MediEvil2Config::new),
    C12(null, null);

    private final Supplier<SCGameInstance> instanceMaker;
    private final Function<String, SCGameConfig> configMaker;
    private final String identifier;

    public static final String CONFIG_MWD_PATH = "mwdFilePath";
    public static final String CONFIG_EXE_PATH = "executableFilePath";

    SCGameType(Supplier<SCGameInstance> instanceMaker, Function<String, SCGameConfig> configMaker) {
        this.instanceMaker = instanceMaker;
        this.configMaker = configMaker;
        this.identifier = name().toLowerCase(Locale.ROOT).replace("_", "");
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
    public String getDisplayName() {
        return Utils.capitalize(name());
    }

    @Override
    public SCGameInstance createGameInstance() {
        return this.instanceMaker != null ? this.instanceMaker.get() : new SCGameUnimplemented(this);
    }

    @Override
    public void loadGameInstance(GameInstance instance, String gameVersionConfigName, Config gameSetupConfig, ProgressBarComponent progressBar) {
        if (!(instance instanceof SCGameInstance))
            throw new ClassCastException("The provided instance was " + Utils.getSimpleName(instance) + ", which was not " + SCGameInstance.class.getSimpleName() + ".");

        String mwdFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_MWD_PATH).getValue();
        String exeFilePath = gameSetupConfig.getKeyValueNodeOrError(CONFIG_EXE_PATH).getValue();
        if (Utils.isNullOrWhiteSpace(mwdFilePath))
            throw new RuntimeException("Invalid mwdFilePath.");
        if (Utils.isNullOrWhiteSpace(exeFilePath))
            throw new RuntimeException("Invalid exeFilePath.");

        File mwdFile = new File(mwdFilePath);
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
    public GameConfigUIController setupConfigUI(GameConfigController controller, Config config) {
        return new SCGameConfigUI(controller, config);
    }

    /**
     * The UI definition for the game.
     */
    public static class SCGameConfigUI extends GameConfigUIController {
        private final GameConfigFileOpenBrowseComponent mwdFileBrowseComponent;
        private final GameConfigFileOpenBrowseComponent exeFileBrowseComponent;

        public SCGameConfigUI(GameConfigController controller, Config config) {
            super(controller);
            this.mwdFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, config, CONFIG_MWD_PATH, "Millennium WAD (.MWD)", "Please select a Millennium WAD", "Millennium WAD", "MWD");
            this.exeFileBrowseComponent = new GameConfigFileOpenBrowseComponent(this, config, CONFIG_EXE_PATH, "Game Executable (.EXE, SLUS, etc.)", "Please select the main executable", "Game Executable", "EXE", "dat", "04", "06", "26", "64", "65", "66", "99");
            loadController(null);
        }

        @Override
        public boolean isLoadButtonDisabled() {
            return Utils.isNullOrWhiteSpace(this.mwdFileBrowseComponent.getCurrentFilePath())
                    || Utils.isNullOrWhiteSpace(this.exeFileBrowseComponent.getCurrentFilePath());
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            super.onControllerLoad(rootNode);
            addController(this.mwdFileBrowseComponent);
            addController(this.exeFileBrowseComponent);
        }
    }
}