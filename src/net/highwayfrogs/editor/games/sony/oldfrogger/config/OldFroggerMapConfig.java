package net.highwayfrogs.editor.games.sony.oldfrogger.config;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapVersion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents configuration for a particular map.
 * Created by Kneesnap on 12/19/2023.
 */
@Getter
public class OldFroggerMapConfig {
    private String name; // The name of the map config.
    private boolean caveLightingEnabled;
    private OldFroggerFormConfig formConfig;
    private OldFroggerMapVersion version = OldFroggerMapVersion.MILESTONE3; // The amount of padding bytes in a form.
    private final Set<String> applicableMaps = new HashSet<>(); // A list of the names of maps which this config applies to.

    private static final Map<String, OldFroggerFormConfig> FORM_CONFIG_CACHE = new HashMap<>();

    /**
     * Loads data from the config.
     * @param config        The config to load data from.
     * @param defaultConfig The config containing default values.
     */
    public void load(Config config, OldFroggerMapConfig defaultConfig) {
        boolean isDefaultConfig = (defaultConfig == this);
        this.name = config.getName();
        this.version = config.getEnum("version", defaultConfig.getVersion());
        this.caveLightingEnabled = config.getBoolean("caveLighting", defaultConfig.isCaveLightingEnabled());

        // Load base form config.
        String formConfigName = config.getString("forms", defaultConfig == this ? "1997-03-19-psx-milestone3" : null);
        if (formConfigName != null) {
            this.formConfig = FORM_CONFIG_CACHE.computeIfAbsent(formConfigName, OldFroggerFormConfig::loadFormConfig);
        } else {
            this.formConfig = defaultConfig.getFormConfig();
        }

        // Register any map configs for individual maps.
        if (!isDefaultConfig) {
            this.applicableMaps.addAll(config.getText());
            if (this.applicableMaps.isEmpty() && this.name.endsWith(".MAP"))
                this.applicableMaps.add(this.name);
        }
    }
}