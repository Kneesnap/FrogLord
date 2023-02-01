package net.highwayfrogs.editor.file.config;

import lombok.Getter;

/**
 * Represents configuration for a particular map.
 * Created by Kneesnap on 1/12/2023.
 */
@Getter
public class FroggerMapConfig {
    private int groupPaddingAmount = 6; // The amount of padding bytes in a form.
    private boolean g2Supported = true; // Whether G2 primitives are enabled in a map or not.
    private boolean mapAnimationSupported = true; // Whether map animation is supported.
    private boolean oldMapTexturedPolyFormat = false; // Whether the old textured polygon format should be used.
    private boolean oldPathFormat = false; // Whether the old path format is used or not.
    private boolean oldFormFormat = false; // Whether the old form format is used or not.

    /**
     * Loads data from the config.
     * @param config        The config to load data from.
     * @param defaultConfig The config containing default values.
     */
    public void load(Config config, FroggerMapConfig defaultConfig) {
        this.groupPaddingAmount = config.getInt("groupPaddingAmount", defaultConfig.getGroupPaddingAmount());
        this.g2Supported = config.getBoolean("g2Supported", defaultConfig.isG2Supported());
        this.mapAnimationSupported = config.getBoolean("enableMapAnimations", defaultConfig.isMapAnimationSupported());
        this.oldMapTexturedPolyFormat = config.getBoolean("oldMapTexturedPolyFormat", defaultConfig.isOldMapTexturedPolyFormat());
        this.oldPathFormat = config.getBoolean("oldPathFormat", defaultConfig.isOldPathFormat());
        this.oldFormFormat = config.getBoolean("oldFormFormat", defaultConfig.isOldFormFormat());
    }
}
