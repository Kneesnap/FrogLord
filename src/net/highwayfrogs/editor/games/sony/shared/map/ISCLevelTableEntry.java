package net.highwayfrogs.editor.games.sony.shared.map;

import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;

/**
 * Represents a level table entry for a late Sony Cambridge game.
 * Created by Kneesnap on 5/7/2024.
 */
public interface ISCLevelTableEntry {
    /**
     * Gets the texture remap for the level table entry.
     */
    TextureRemapArray getRemap();

    /**
     * Gets the VLO file containing textures loaded for the level.
     */
    VLOArchive getVloFile();
}