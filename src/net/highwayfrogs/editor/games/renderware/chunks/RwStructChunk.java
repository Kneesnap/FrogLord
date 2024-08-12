package net.highwayfrogs.editor.games.renderware.chunks;

import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwStreamSectionType;

/**
 * Represents a struct with arbitrary contents.
 * TODO: Review this, why does it just give access to raw data..? Eg: Figure out what the purpose of this is.
 * Created by Kneesnap on 6/9/2020.
 */
public class RwStructChunk extends RwStreamChunk {
    private transient DataReader reader;

    public RwStructChunk(RwStreamFile streamFile, int renderwareVersion, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamSectionType.STRUCT, renderwareVersion, parentChunk);
    }

    /**
     * Gets a reader for reading the struct data from the start.
     * @return structReader
     */
    public DataReader getReader() {
        if (this.reader == null)
            this.reader = new DataReader(new ArraySource(getRawReadData()));

        this.reader.setIndex(0);
        return this.reader;
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