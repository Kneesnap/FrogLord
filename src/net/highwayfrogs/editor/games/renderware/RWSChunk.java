package net.highwayfrogs.editor.games.renderware;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a Renderware stream chunk.
 * https://gtamods.com/wiki/RenderWare_binary_stream_file
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
@Setter
public abstract class RWSChunk extends GameObject {
    private int typeId;
    private int renderwareVersion;
    private RWSChunk parentChunk;
    private transient int readSize;

    public RWSChunk(int typeId, int renderwareVersion, RWSChunk parentChunk) {
        this.typeId = typeId;
        this.renderwareVersion = renderwareVersion;
        this.parentChunk = parentChunk;
    }

    @Override
    public final void load(DataReader reader) {
        int readSize = this.readSize = reader.readInt();
        this.renderwareVersion = reader.readInt();
        System.out.println("Reading " + getClass().getSimpleName() + " (Type ID: " + this.typeId + ", Size: " + readSize + ").");

        if (this.parentChunk != null && this.parentChunk.getReadSize() == this.readSize) {
            loadChunkData(reader);
            return;
        }

        byte[] chunkData = reader.readBytes(readSize);
        DataReader chunkReader = new DataReader(new ArraySource(chunkData));
        loadChunkData(chunkReader);
    }

    @Override
    public final void save(DataWriter writer) {
        writer.writeInt(this.typeId);
        int dataSize = writer.writeNullPointer();
        writer.writeInt(this.renderwareVersion);

        // Write chunk-specific stuff.
        int sizeFromIndex = writer.getIndex();
        saveChunkData(writer);
        writer.writeAddressAt(dataSize, writer.getIndex() - sizeFromIndex);
    }

    /**
     * Reads data specific to this chunk type.
     * TODO: When porting to ModToolFramework, just pass the normal data reader with the parameters of data size. It may seem simpler how it's currently done, but it's not.
     * @param reader The reader to read data from.
     */
    public abstract void loadChunkData(DataReader reader);

    /**
     * Saves data specific to this chunk type.
     * @param writer The writer to write data to.
     */
    public abstract void saveChunkData(DataWriter writer);
}
