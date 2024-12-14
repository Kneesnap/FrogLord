package net.highwayfrogs.editor.games.renderware.chunks.sector;

import net.highwayfrogs.editor.games.renderware.chunks.RwWorldChunk;

/**
 * Represents something which can provide an RwWorldChunk object.
 * Created by Kneesnap on 8/17/2024.
 */
public interface IRwWorldProvider {
    /**
     * Gets the world.
     * @return the world provider
     */
    RwWorldChunk getWorld();
}