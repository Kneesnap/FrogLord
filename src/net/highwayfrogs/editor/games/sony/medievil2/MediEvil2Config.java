package net.highwayfrogs.editor.games.sony.medievil2;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.SCGameConfig;

/**
 * Contains configuration data for a MediEvil 2 build.
 * Created by Kneesnap on 5/12/2024.
 */
@Getter
public class MediEvil2Config extends SCGameConfig {
    private int levelTableAddress = -1;
    private int levelTableEntryCount = -1;

    public MediEvil2Config(String internalName) {
        super(internalName);
    }

    @Override
    protected void readConfigData(Config config) {
        super.readConfigData(config);
        this.levelTableAddress = config.getInt("levelTableAddress", -1);
        this.levelTableEntryCount = config.getInt("levelTableEntryCount", -1);
    }
}