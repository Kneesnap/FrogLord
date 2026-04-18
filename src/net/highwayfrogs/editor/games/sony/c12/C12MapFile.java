package net.highwayfrogs.editor.games.sony.c12;

import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.section.SCLevelDefinition;

/**
 * Represents a map file from C-12 Final Resistance.
 * Created by Kneesnap on 4/17/2026.
 */
public class C12MapFile extends SCMapFile<C12GameInstance> {
    private ISCLevelTableEntry cachedLevelTableEntry;

    public C12MapFile(C12GameInstance instance) {
        super(instance);
    }

    @Override
    public C12MapFile getParentMap() {
        ISCLevelTableEntry levelTableEntry = getLevelTableEntry();
        if (levelTableEntry instanceof SCLevelDefinition)
            return null; // This is already the parent map.

        ISCLevelTableEntry levelDefinition = levelTableEntry != null ? levelTableEntry.getLevelDefinition() : null;
        return levelDefinition != null ? (C12MapFile) levelDefinition.getMapFile() : null;
    }

    @Override
    public ISCLevelTableEntry getLevelTableEntry() {
        // Search & cache the level table entry.
        int resourceId = getFileResourceId();
        if (this.cachedLevelTableEntry == null || this.cachedLevelTableEntry.getMapFile() == null || this.cachedLevelTableEntry.getMapFile().getFileResourceId() != resourceId) {
            for (int i = 0; i < getGameInstance().getLevelTable().size(); i++) {
                SCLevelDefinition levelDefinition = getGameInstance().getLevelTable().get(i);

                // Check level sections.
                for (int j = 0; j < levelDefinition.getLevelSections().size(); j++) {
                    ISCLevelTableEntry levelSection = levelDefinition.getLevelSections().get(j);
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
