package net.highwayfrogs.editor.games.sony.medievil.config;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.SCGameConfig;

/**
 * Represents configuration data for MediEvil.
 * Created by Kneesnap on 3/8/2024.
 */
@Getter
public class MediEvilConfig extends SCGameConfig {
    private int levelTableAddress = -1;
    private int levelTableSize = 25;
    private int levelTableEntryByteSize = 100;

    public MediEvilConfig(String internalName) {
        super(internalName);
    }

    @Override
    public void loadData(Config config) {
        super.loadData(config);
        loadLevelTableInfo(config); // Load level table info.
    }

    private void loadLevelTableInfo(Config config) {
        this.levelTableAddress = config.getInt("levelTable", this.levelTableAddress);
        this.levelTableSize = config.getInt("levelTableSize", this.levelTableSize);
        this.levelTableEntryByteSize = config.getInt("levelTableEntryByteSize", this.levelTableEntryByteSize);
    }
}