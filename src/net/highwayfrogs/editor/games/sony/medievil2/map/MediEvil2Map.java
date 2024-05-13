package net.highwayfrogs.editor.games.sony.medievil2.map;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.medievil2.IMediEvil2LevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2GameInstance;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2LevelDefinition;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2LevelDefinition.MediEvil2LevelSectionDefinition;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;

/**
 * Represents a map file from MediEvil 2.
 * Created by Kneesnap on 5/12/2024.
 */
public class MediEvil2Map extends SCMapFile<MediEvil2GameInstance>  {
    @Getter private final SCMapPolygonPacket<MediEvil2GameInstance> polygonPacket;
    private IMediEvil2LevelTableEntry cachedLevelTableEntry;

    public MediEvil2Map(MediEvil2GameInstance instance) {
        super(instance);
        addFilePacket(this.polygonPacket = new SCMapPolygonPacket<>(this));
    }

    @Override
    public ISCLevelTableEntry getLevelTableEntry() {
        // Search & cache the level table entry.
        int resourceId = getIndexEntry().getResourceId();
        if (this.cachedLevelTableEntry == null || this.cachedLevelTableEntry.getMapFile() == null || this.cachedLevelTableEntry.getMapFile().getIndexEntry().getResourceId() != resourceId) {
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