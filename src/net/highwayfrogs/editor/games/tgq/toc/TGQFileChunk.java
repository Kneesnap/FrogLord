package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Represents a chunk in a TOC file.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public abstract class TGQFileChunk extends GameObject {
    private TGQChunkType chunkType;
    private TGQChunkedFile parentFile;

    public TGQFileChunk(TGQChunkedFile parentFile, TGQChunkType chunkType) {
        this.chunkType = chunkType;
        this.parentFile = parentFile;
    }

    /**
     * Test if this is the root chunk in the file.
     * @return isRootChunk
     */
    public boolean isRootChunk() {
        return getParentFile().getChunks().size() == 0 || getParentFile().getChunks().get(0) == this;
    }

    /**
     * Gets the signature this chunk uses
     * @return signature
     */
    public String getSignature() {
        if (getChunkType().getSignature() == null)
            throw new UnsupportedOperationException("getSignature() was called on " + getChunkType() + ", which needs to be overwritten instead.");

        return getChunkType().getSignature();
    }
}
