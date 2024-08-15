package net.highwayfrogs.editor.games.renderware;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.renderware.chunks.*;
import net.highwayfrogs.editor.games.renderware.struct.RwUnsupportedStruct;
import net.highwayfrogs.editor.system.TriFunction;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Enables the creation of RwStreamChunk objects based on numeric ids.
 * Created by Kneesnap on 6/9/2020.
 */
public class RwStreamChunkTypeRegistry implements Cloneable {
    private final Map<Integer, RwStreamChunkTypeEntry> registeredChunkTypes;
    @Getter private static final RwStreamChunkTypeRegistry defaultRegistry = new RwStreamChunkTypeRegistry();

    public RwStreamChunkTypeRegistry() {
        this(new HashMap<>());
    }

    private RwStreamChunkTypeRegistry(Map<Integer, RwStreamChunkTypeEntry> registryMap) {
        this.registeredChunkTypes = registryMap;
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public RwStreamChunkTypeRegistry clone() {
        return new RwStreamChunkTypeRegistry(new HashMap<>(this.registeredChunkTypes));
    }

    /**
     * Registers a chunk type.
     * @param chunkCreator The constructor to create a chunk.
     */
    public void registerChunkType(TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> chunkCreator) {
        IRwStreamChunkType chunkType = chunkCreator.apply(null, 0, null).getChunkType();
        registerChunkType(chunkType, chunkCreator);
    }

    /**
     * Registers a chunk type.
     * @param chunkType The chunk's type.
     * @param chunkCreator The constructor to create a chunk.
     */
    public void registerChunkType(IRwStreamChunkType chunkType, TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> chunkCreator) {
        this.registeredChunkTypes.put(chunkType.getTypeId(), new RwStreamChunkTypeEntry(chunkType, chunkCreator));
    }

    /**
     * Creates a chunk based on the magic id supplied.
     * @param streamFile the stream file which the chunk belongs to
     * @param chunkId the id of the chunk to create
     * @param rwVersion The renderware version the chunk was built with.
     * @return newChunk
     */
    public RwStreamChunk createChunk(RwStreamFile streamFile, int chunkId, int rwVersion, RwStreamChunk parentChunk) {
        RwStreamChunkTypeEntry chunkEntry = this.registeredChunkTypes.get(chunkId);
        if (chunkEntry != null) {
            return chunkEntry.getConstructor().apply(streamFile, rwVersion, parentChunk);
        } else {
            return new RwUnsupportedChunk(streamFile, chunkId, rwVersion, parentChunk);
        }
    }

    /**
     * Reads a new chunk from a DataReader.
     * @param reader The reader to read the chunk from.
     * @param parentChunk the chunk which contains this chunk, if there is one
     * @return newChunk.
     */
    public RwStreamChunk readChunk(DataReader reader, RwStreamChunk parentChunk) {
        if (parentChunk == null)
            throw new NullPointerException("parentChunk");

        return readChunk(reader, parentChunk.getStreamFile(), parentChunk);
    }

    /**
     * Reads a new chunk from a DataReader.
     * @param reader The reader to read the chunk from.
     * @param streamFile the stream file which the chunk belongs to
     * @return newChunk.
     */
    public RwStreamChunk readChunk(DataReader reader, RwStreamFile streamFile) {
        return readChunk(reader, streamFile, null);
    }

    /**
     * Reads a new chunk from a DataReader.
     * @param reader The reader to read the chunk from.
     * @param streamFile the stream file which the chunk belongs to
     * @param parentChunk the chunk which contains this chunk, if there is one
     * @return newChunk
     */
    private RwStreamChunk readChunk(DataReader reader, RwStreamFile streamFile, RwStreamChunk parentChunk) {
        if (reader == null)
            throw new NullPointerException("reader");

        int chunkType = reader.readInt();
        reader.jumpTemp(reader.getIndex() + Constants.INTEGER_SIZE);
        int version = reader.readInt();
        reader.jumpReturn();

        RwStreamChunk newChunk = createChunk(streamFile, chunkType, version, parentChunk); // Version will be loaded during the .load call too.
        newChunk.load(reader);
        return newChunk;
    }

    /**
     * Reads an already existing chunk object from a DataReader.
     * If an unexpected chunk is loaded instead, an IllegalArgumentException will be thrown.
     * @param reader The reader to read the chunk from.
     * @param chunk the chunk to attempt to read
     * @return chunk
     */
    public RwStreamChunk readChunkObject(DataReader reader, RwStreamChunk chunk) {
        if (reader == null)
            throw new NullPointerException("reader");
        if (chunk == null)
            throw new NullPointerException("chunk");

        int typeId = reader.readInt();
        if (typeId != chunk.getChunkType().getTypeId())
            throw new IllegalArgumentException("Expected the stream type ID to be " + Utils.toHexString(chunk.getChunkType().getTypeId()) + " to read the provided " + Utils.getSimpleName(chunk) + ", but got " + Utils.toHexString(typeId) + " instead!");

        chunk.load(reader);
        return chunk;
    }

    @Getter
    @RequiredArgsConstructor
    private static class RwStreamChunkTypeEntry {
        private final IRwStreamChunkType chunkType;
        private final TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> constructor;
    }

    static {
        // Register chunks.
        // TODO: Support many more chunks.

        defaultRegistry.registerChunkType((gameInstance, version, parentChunk) -> new RwStructChunk<>(gameInstance, version, parentChunk, RwUnsupportedStruct.class)); // By default, treat structs as unsupported, since we can't identify them without extra info.
        defaultRegistry.registerChunkType(RwStringChunk::new);
        defaultRegistry.registerChunkType(RwExtensionChunk::new);
        defaultRegistry.registerChunkType(RwTextureChunk::new);
        defaultRegistry.registerChunkType(RwWorldChunk::new);
        defaultRegistry.registerChunkType(RwUnicodeStringChunk::new);
        defaultRegistry.registerChunkType(RwImageChunk::new);
        defaultRegistry.registerChunkType(RwPlatformIndependentTextureDictionaryChunk::new);
        defaultRegistry.registerChunkType(RwTableOfContentsChunk::new);

        // First, Table of Contents - 0x24.
        // Platform Independent Texture Dictionary, 0x23.
        // Clump - 0x10 (First one with child nodes.)
        // Struct - 0x01
        // Frame List  - 0x0E
        // Extension - 0x03
        // HAnim PLG - 11E
        // Geometry List - 0x1A
        // Geometry - 0x0F
        // Material List - 0x08
        // Material - 0x07
        // Sky Minimap Val = 0x110
        // Texture - 0x06
        // String - 0x02
        // Atomic - 0x14
        // Anim Animation - 0x1B

        // Bin Mesh PLG - 0x50E
        // Skin PLG - 0x116
        // Morph PLG - 0x105
        // User Data PLG - 0x11F
        // Right to Render - 0x1F
    }
}