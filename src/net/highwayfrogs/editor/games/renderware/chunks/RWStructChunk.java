package net.highwayfrogs.editor.games.renderware.chunks;

import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RWSChunk;

/**
 * Represents a struct with arbitrary contents.
 * Created by Kneesnap on 6/9/2020.
 */
public class RWStructChunk extends RWSChunk {
    private byte[] structData;
    private transient DataReader reader;

    public RWStructChunk(int renderwareVersion, RWSChunk parentChunk) {
        super(1, renderwareVersion, parentChunk);
    }

    /**
     * Gets a reader for reading the struct data from the start.
     * @return structReader
     */
    public DataReader getReader() {
        if (this.reader == null)
            this.reader = new DataReader(new ArraySource(this.structData));
        this.reader.setIndex(0);
        return this.reader;
    }

    @Override
    public void loadChunkData(DataReader reader) {
        this.structData = reader.readBytes(reader.getSize());
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        writer.writeBytes(this.structData);
    }
}
