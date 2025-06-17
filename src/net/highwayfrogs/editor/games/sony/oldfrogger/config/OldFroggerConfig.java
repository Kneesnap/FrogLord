package net.highwayfrogs.editor.games.sony.oldfrogger.config;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds configuration data for Old Frogger.
 * Created by Kneesnap on 12/10/2023.
 */
@Getter
public class OldFroggerConfig extends SCGameConfig {
    private int remapTableAddress = -1;
    private int remapTableCount;
    private int levelTableAddress = -1;
    private int levelTableCount;
    private final List<String> manualLevelTableEntryStrings = new ArrayList<>();
    private final Map<String, OldFroggerMapConfig> mapConfigs = new HashMap<>();
    private final OldFroggerMapConfig defaultMapConfig = new OldFroggerMapConfig();

    public OldFroggerConfig(String internalName) {
        super(internalName);
    }

    @Override
    protected void readConfigData(Config config) {
        super.readConfigData(config);
        loadRemapTable(config); // Load remap table.
        loadLevelTableInfo(config); // Load level table info.
        loadManuallyConfiguredLevelTableEntries(config); // Load manually configured text.
        readMapConfigs(config);
    }

    private void loadRemapTable(Config config) {
        String remapTableStr = config.getString("remapTable", null);
        if (remapTableStr != null) {
            String[] split = remapTableStr.split("@");
            if (split.length == 2 && NumberUtils.isInteger(split[0]) && NumberUtils.isHexInteger(split[1])) {
                this.remapTableCount = Integer.parseInt(split[0]);
                this.remapTableAddress = NumberUtils.parseHexInteger(split[1]);
            } else {
                getLogger().warning("Invalid remapTable data specified '%s'.", remapTableStr);
            }
        }
    }

    private void loadLevelTableInfo(Config config) {
        String levelTableStr = config.getString("levelTable", null);
        if (levelTableStr != null) {
            String[] split = levelTableStr.split("@");
            if (split.length == 2 && NumberUtils.isInteger(split[0]) && NumberUtils.isHexInteger(split[1])) {
                this.levelTableCount = Integer.parseInt(split[0]);
                this.levelTableAddress = NumberUtils.parseHexInteger(split[1]);
            } else {
                getLogger().warning("Invalid levelTable data specified '%s'", levelTableStr);
            }
        }
    }

    private void loadManuallyConfiguredLevelTableEntries(Config config) {
        this.manualLevelTableEntryStrings.clear();
        if (config.hasChild("ManualLevelTableEntries")) {
            Config childConfig = config.getChild("ManualLevelTableEntries");
            this.manualLevelTableEntryStrings.addAll(childConfig.getText());
        }
    }

    private void readMapConfigs(Config config) {
        this.mapConfigs.clear();
        if (!config.hasChild("MapConfig"))
            return;

        Config defaultMapConfig = config.getChild("MapConfig");
        this.defaultMapConfig.load(defaultMapConfig, this.defaultMapConfig);

        // Read other configs, if there are any.
        for (Config mapConfig : defaultMapConfig.getOrderedChildren()) {
            OldFroggerMapConfig newMapConfig = new OldFroggerMapConfig();
            newMapConfig.load(mapConfig, this.defaultMapConfig);
            for (String mapFileName : newMapConfig.getApplicableMaps())
                this.mapConfigs.put(mapFileName, newMapConfig);
        }
    }
}