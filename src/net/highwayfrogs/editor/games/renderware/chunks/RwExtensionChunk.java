package net.highwayfrogs.editor.games.renderware.chunks;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwStreamSectionType;

/**
 * Represents the rwEXTENSION data.
 * TODO: Parse the data here later.
 * Created by Kneesnap on 8/15/2024.
 */
public class RwExtensionChunk extends RwStreamChunk {
    public RwExtensionChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamSectionType.EXTENSION, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        reader.skipBytes(dataLength); // We've already gotten the chunk data stored as the rawReadData.
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        if (getRawReadData() != null)
            writer.writeBytes(getRawReadData());
    }
}