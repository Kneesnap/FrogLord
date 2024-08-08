package net.highwayfrogs.editor.games.renderware.chunks;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;

/**
 * Represents an unsupported renderware chunk.
 * Created by Kneesnap on 6/9/2020.
 */
public class RwUnsupportedChunk extends RwStreamChunk {
    public RwUnsupportedChunk(RwStreamFile streamFile, int chunkId, int rwVersion, RwStreamChunk parentChunk) {
        super(streamFile, chunkId, rwVersion, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        getLogger().info("Reading RwUnsupportedChunk.");
        reader.skipBytes(dataLength); // We've already gotten the chunk data stored as the rawReadData.
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        if (getRawReadData() != null)
            writer.writeBytes(getRawReadData());
    }

    @Override
    protected String getLoggerInfo() {
        return "id=" + Integer.toHexString(getTypeId()).toUpperCase() + "," + super.getLoggerInfo();
    }
}