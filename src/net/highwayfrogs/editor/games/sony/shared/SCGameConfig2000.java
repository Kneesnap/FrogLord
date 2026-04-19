package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.SCGameConfig;

/**
 * Represents a base game version config for a Sony Cambridge game made in 2000 or later. (MediEvil II or C-12 Final Resistance)
 * Created by Kneesnap on 4/19/2026.
 */
@Getter
public class SCGameConfig2000 extends SCGameConfig {
    private int levelTableAddress = -1;
    private int levelTableEntryCount = -1;

    public SCGameConfig2000(String internalName) {
        super(internalName);
    }

    @Override
    protected void readConfigData(Config config) {
        super.readConfigData(config);
        this.levelTableAddress = config.getInt("levelTableAddress", -1);
        this.levelTableEntryCount = config.getInt("levelTableEntryCount", -1);
    }
}
