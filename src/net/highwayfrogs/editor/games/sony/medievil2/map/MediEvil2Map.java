package net.highwayfrogs.editor.games.sony.medievil2.map;

import net.highwayfrogs.editor.games.sony.medievil2.IMediEvil2LevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2GameInstance;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2LevelDefinition;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2LevelDefinition.MediEvil2LevelSectionDefinition;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;

/**
 * Represents a map file from MediEvil 2.
 * Created by Kneesnap on 5/12/2024.
 */
public class MediEvil2Map extends SCMapFile<MediEvil2GameInstance>  {
    private IMediEvil2LevelTableEntry cachedLevelTableEntry;

    public MediEvil2Map(MediEvil2GameInstance instance) {
        super(instance);
    }

    @Override
    public MediEvil2Map getParentMap() {
        IMediEvil2LevelTableEntry levelTableEntry = getLevelTableEntry();
        if (levelTableEntry instanceof MediEvil2LevelDefinition)
            return null; // This is already the parent map.

        MediEvil2LevelDefinition levelDefinition = levelTableEntry != null ? levelTableEntry.getLevelDefinition() : null;
        return levelDefinition != null ? levelDefinition.getMapFile() : null;
    }

    @Override
    public IMediEvil2LevelTableEntry getLevelTableEntry() {
        // Search & cache the level table entry.
        int resourceId = getFileResourceId();
        if (this.cachedLevelTableEntry == null || this.cachedLevelTableEntry.getMapFile() == null || this.cachedLevelTableEntry.getMapFile().getFileResourceId() != resourceId) {
            for (int i = 0; i < getGameInstance().getLevelTable().size(); i++) {
                MediEvil2LevelDefinition levelDefinition = getGameInstance().getLevelTable().get(i);

                // Check level sections.
                for (int j = 0; j < levelDefinition.getLevelSections().size(); j++) {
                    MediEvil2LevelSectionDefinition levelSection = levelDefinition.getLevelSections().get(j);
                    if (levelSection.getMapFile() == this)
                        return this.cachedLevelTableEntry = levelSection;
                }

                // Check the level definition.
                if (levelDefinition.getMapFile() == this)
                    return this.cachedLevelTableEntry = levelDefinition;
            }
        }

        return this.cachedLevelTableEntry;
    }
}