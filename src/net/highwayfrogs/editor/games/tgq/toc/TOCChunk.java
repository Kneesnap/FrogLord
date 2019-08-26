package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.games.tgq.TGQTOCFile;

/**
 * Represents a chunk in a TOC file.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public abstract class TOCChunk extends GameObject {
    private TOCChunkType chunkType;
    private TGQTOCFile parentFile;

    public TOCChunk(TGQTOCFile parentFile, TOCChunkType chunkType) {
        this.chunkType = chunkType;
        this.parentFile = parentFile;
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
