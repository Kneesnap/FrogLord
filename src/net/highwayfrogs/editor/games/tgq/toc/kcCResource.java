package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Represents a resource in a TGQ file.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public abstract class kcCResource extends GameObject {
    private KCResourceID chunkType;
    @Setter private String name;
    private TGQChunkedFile parentFile;

    private static final int NAME_SIZE = 32;

    public kcCResource(TGQChunkedFile parentFile, KCResourceID chunkType) {
        this.chunkType = chunkType;
        this.parentFile = parentFile;
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeTerminatedStringOfLength(this.name, NAME_SIZE);
    }

    /**
     * Test if this is the root chunk in the file.
     * @return isRootChunk
     */
    public boolean isRootChunk() {
        return getParentFile() == null || getParentFile().getChunks().size() == 0 || getParentFile().getChunks().get(0) == this;
    }

    /**
     * Gets the signature this chunk uses
     * @return signature
     */
    public String getChunkMagic() {
        if (getChunkType().getSignature() == null)
            throw new UnsupportedOperationException("getSignature() was called on " + getChunkType() + ", which needs to be overwritten instead.");
        return getChunkType().getSignature();
    }
}
