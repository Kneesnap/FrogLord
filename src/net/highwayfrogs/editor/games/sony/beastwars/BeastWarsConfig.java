package net.highwayfrogs.editor.games.sony.beastwars;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.SCGameConfig;

/**
 * Represents the configuration file for Beast Wars.
 * Created by Kneesnap on 9/8/2023.
 */
@Getter
public class BeastWarsConfig extends SCGameConfig {
    private int modelRemapTablePointer = -1;
    private int modelRemapTableLength = -1;
    public BeastWarsConfig(String internalName) {
        super(internalName);
    }

    @Override
    protected void readConfigData(Config config) {
        super.readConfigData(config);
        readModelRemapTable(config);
    }

    private void readModelRemapTable(Config config) {
        this.modelRemapTablePointer = config.getInt("modelRemapTablePointer", -1);
        this.modelRemapTableLength = config.getInt("modelRemapTableLength", -1);
    }
}