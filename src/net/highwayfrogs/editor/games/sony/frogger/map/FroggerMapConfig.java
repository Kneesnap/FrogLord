package net.highwayfrogs.editor.games.sony.frogger.map;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents configuration for a particular map.
 * Created by Kneesnap on 1/12/2023.
 */
@Getter
public class FroggerMapConfig {
    private String name; // The name of the map config.
    private int groupPaddingAmount = 6; // The amount of padding bytes in a form.
    private boolean g2Supported = true; // Whether G2 primitives are enabled in a map or not.
    private boolean g2Enabled = false; // Whether G2 primitives are enabled in the code via the WIREFRAME compilation flag.
    private boolean mapAnimationSupported = true; // Whether map animation is supported.
    private boolean oldMapTexturedPolyFormat = false; // Whether the old textured polygon format should be used.
    private boolean oldPathFormat = false; // Whether the old path format is used or not.
    private boolean oldFormFormat = false; // Whether the old form format is used or not.
    private boolean islandPlaceholder = false; // Whether the map is ISLAND.MAP, representing a map which is not 'ISLAND.MAP'. Eg: When ARN1.MAP has 'ISLAND.MAP' as a placeholder.
    private final Set<String> applicableMaps = new HashSet<>(); // A list of the names of maps which this config applies to.

    /**
     * Loads data from the config.
     * @param config        The config to load data from.
     * @param defaultConfig The config containing default values.
     */
    public void load(Config config, FroggerMapConfig defaultConfig) {
        boolean isDefaultConfig = (defaultConfig == this);
        this.name = config.getName();
        this.groupPaddingAmount = config.getInt("groupPaddingAmount", defaultConfig.getGroupPaddingAmount());
        this.g2Supported = config.getBoolean("g2Supported", defaultConfig.isG2Supported());
        this.g2Enabled = config.getBoolean("g2Enabled", defaultConfig.isG2Enabled());
        this.mapAnimationSupported = config.getBoolean("enableMapAnimations", defaultConfig.isMapAnimationSupported());
        this.oldMapTexturedPolyFormat = config.getBoolean("oldMapTexturedPolyFormat", defaultConfig.isOldMapTexturedPolyFormat());
        this.oldPathFormat = config.getBoolean("oldPathFormat", defaultConfig.isOldPathFormat());
        this.oldFormFormat = config.getBoolean("oldFormFormat", defaultConfig.isOldFormFormat());
        this.islandPlaceholder = config.getBoolean("islandPlaceholder", defaultConfig.isIslandPlaceholder());

        if (!isDefaultConfig) {
            this.applicableMaps.addAll(config.getText());
            if (this.applicableMaps.isEmpty() && this.name.endsWith(".MAP"))
                this.applicableMaps.add(this.name);
        }
    }
}