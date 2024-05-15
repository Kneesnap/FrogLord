package net.highwayfrogs.editor.games.sony.medievil2;

import net.highwayfrogs.editor.games.sony.medievil2.map.MediEvil2Map;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;

/**
 * Represents a MediEvil2 level table entry.
 * Created by Kneesnap on 5/12/2024.
 */
public interface IMediEvil2LevelTableEntry extends ISCLevelTableEntry {
    /**
     * Gets the map file available.
     */
    MediEvil2Map getMapFile();

    /**
     * Gets the level definition.
     */
    MediEvil2LevelDefinition getLevelDefinition();
}