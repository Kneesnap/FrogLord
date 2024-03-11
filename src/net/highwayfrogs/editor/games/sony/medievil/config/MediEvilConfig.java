package net.highwayfrogs.editor.games.sony.medievil.config;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents configuration data for MediEvil.
 * Created by Kneesnap on 3/8/2024.
 */
@Getter
public class MediEvilConfig extends SCGameConfig {
    private final MediEvilGameInstance instance;
    private final List<MediEvilLevelTableEntry> levelTable = new ArrayList<>();

    public MediEvilConfig(MediEvilGameInstance instance, String internalName) {
        super(internalName);
        this.instance = instance;
    }

    @Override
    public void loadData(Config config) {
        super.loadData(config);
        loadLevelTable(config); // Load level table info.
    }

    private void loadLevelTable(Config config) {
        this.levelTable.clear();
        int levelTableAddress = config.getInt("levelTable", -1);
        int levelTableSize = config.getInt("levelTableSize", 25);
        int levelTableOffset = config.getInt("levelTableOffset", 0);
        int levelTableEntryByteSize = config.getInt("levelTableEntryByteSize", 100);
        if (levelTableAddress < 0)
            return;

        DataReader reader = this.instance.getExecutableReader();
        reader.jumpTemp(levelTableAddress);
        for (int i = 0; i < levelTableSize; i++) {
            MediEvilLevelTableEntry newEntry = new MediEvilLevelTableEntry(this.instance, levelTableEntryByteSize);
            newEntry.load(reader);
            if (newEntry.getTextureRemapPointer() > 0)
                newEntry.setTextureRemapPointer(newEntry.getTextureRemapPointer() + levelTableOffset);
            this.levelTable.add(newEntry);
        }
        reader.jumpReturn();
    }
}