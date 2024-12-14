package net.highwayfrogs.editor.games.renderware.chunks.sector;

import lombok.NonNull;
import net.highwayfrogs.editor.games.renderware.IRwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.chunks.RwWorldChunk;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the RpSector struct as seen in basector.h.
 * Created by Kneesnap on 8/18/2024.
 */
public abstract class RwSectorBase extends RwStreamChunk implements IRwWorldProvider {
    public RwSectorBase(RwStreamFile streamFile, @NonNull IRwStreamChunkType chunkType, int version, RwStreamChunk parentChunk) {
        super(streamFile, chunkType, version, parentChunk);
        if (!(parentChunk instanceof IRwWorldProvider))
            throw new IllegalArgumentException("Expected parentChunk to be instanceof IRwWorldProvider, but got " + Utils.getSimpleName(parentChunk) + " instead.");
    }

    @Override
    public RwWorldChunk getWorld() {
        if (!(getParentChunk() instanceof IRwWorldProvider))
            throw new IllegalStateException("The parent chunk was " + Utils.getSimpleName(getParentChunk()) + ", but we expected an IRwWorldProvider.");
        return ((IRwWorldProvider) getParentChunk()).getWorld();
    }

    /**
     * Returns true iff the sector is a world sector.
     */
    public abstract boolean isWorldSector();
}