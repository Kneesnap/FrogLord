package net.highwayfrogs.editor.games.generic;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.shared.utils.SCNameBank;
import net.highwayfrogs.editor.utils.TimeUtils;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.time.temporal.TemporalAccessor;
import java.util.function.BiFunction;

/**
 * Represents configuration data for a particular version of a game.
 * Created by Kneesnap on 4/10/2024.
 */
@Getter
public class GameConfig {
    private final String internalName;
    private Config config;
    private IGameType gameType;

    // Loaded configuration data.
    private String displayName;
    private GamePlatform platform;
    private TemporalAccessor buildTime; // The time at which the build was made.

    public static final String CFG_DISPLAY_NAME = "name";
    public static final String CFG_PLATFORM_TYPE = "platform";
    public static final String CFG_BUILD_TIME = "buildTime";

    public GameConfig(String internalName) {
        this.internalName = internalName;
    }

    /**
     * Load data from the specified config.
     * @param config The config to load data from.
     */
    public final void loadData(Config config, IGameType gameType) {
        if (this.config != null)
            throw new RuntimeException("The config " + this.internalName + " already has its data loaded.");

        if (config == null)
            return;

        this.gameType = gameType;
        this.config = config;
        readConfigData(config);
    }

    /**
     * Reads configuration data from the config
     * @param config the config to read data from
     */
    protected void readConfigData(Config config) {
        readBasicConfigData(config);
    }

    private void readBasicConfigData(Config config) {
        this.displayName = config.getString(CFG_DISPLAY_NAME);
        this.platform = config.getEnum(CFG_PLATFORM_TYPE, GamePlatform.class);
        this.buildTime = config.has(CFG_BUILD_TIME) ? TimeUtils.parseAmbiguousTimestamp(config.getString(CFG_BUILD_TIME)) : null;
    }

    protected SCNameBank loadBank(Config config, String configKey, String defaultBank, String bankName, String unknownName, boolean addChildrenToMainBank) {
        return loadBank(config, configKey, defaultBank, bankName, addChildrenToMainBank, (bank, index) -> "Unknown " + unknownName + " [" + index + "]");
    }

    protected SCNameBank loadBank(Config config, String configKey, String defaultBank, String bankName, boolean addChildrenToMainBank, BiFunction<SCNameBank, Integer, String> nameHandler) {
        String animBankName = config.getString(configKey, defaultBank);
        if (animBankName == null)
            return SCNameBank.EMPTY_BANK;

        return SCNameBank.readBank(this.gameType, bankName, animBankName, addChildrenToMainBank, nameHandler);
    }

    /**
     * Gets the logger for this config.
     */
    public ILogger getLogger() {
        return ClassNameLogger.getLogger(null, getClass());
    }
}