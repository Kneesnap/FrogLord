package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RWSChunk;

/**
 * Represents an unsupported renderware chunk.
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
@Setter
public class RWUnsupportedChunk extends RWSChunk {
    private byte[] rawData;

    public RWUnsupportedChunk(int chunkId, int rwVersion, RWSChunk parentChunk) {
        super(chunkId, rwVersion, parentChunk);
    }

    @Override
    public void loadChunkData(DataReader reader) {
        this.rawData = reader.readBytes(reader.getSize());
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        writer.writeBytes(this.rawData);
    }
}
