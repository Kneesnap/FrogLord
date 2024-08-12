package net.highwayfrogs.editor.games.renderware;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.renderware.chunks.*;
import net.highwayfrogs.editor.system.TriFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Enables the creation of RwStreamChunk objects based on numeric ids.
 * Created by Kneesnap on 6/9/2020.
 */
public class RwStreamChunkTypeRegistry implements Cloneable {
    private final Map<Integer, RwStreamSectionTypeEntry> registeredChunkTypes;
    @Getter private static final RwStreamChunkTypeRegistry defaultRegistry = new RwStreamChunkTypeRegistry();

    public RwStreamChunkTypeRegistry() {
        this(new HashMap<>());
    }

    private RwStreamChunkTypeRegistry(Map<Integer, RwStreamSectionTypeEntry> registryMap) {
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
        IRwStreamSectionType sectionType = chunkCreator.apply(null, 0, null).getSectionType();
        registerChunkType(sectionType, chunkCreator);
    }

    /**
     * Registers a chunk type.
     * @param sectionType The section's type.
     * @param chunkCreator The constructor to create a chunk.
     */
    public void registerChunkType(IRwStreamSectionType sectionType, TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> chunkCreator) {
        this.registeredChunkTypes.put(sectionType.getTypeId(), new RwStreamSectionTypeEntry(sectionType, chunkCreator));
    }

    /**
     * Creates a chunk based on the magic id supplied.
     * @param streamFile the stream file which the chunk belongs to
     * @param sectionId the id of the chunk to create
     * @param rwVersion The renderware version the chunk was built with.
     * @return newChunk
     */
    public RwStreamChunk createSection(RwStreamFile streamFile, int sectionId, int rwVersion, RwStreamChunk parentChunk) {
        RwStreamSectionTypeEntry sectionEntry = this.registeredChunkTypes.get(sectionId);
        if (sectionEntry != null) {
            return sectionEntry.getConstructor().apply(streamFile, rwVersion, parentChunk);
        } else {
            return new RwUnsupportedChunk(streamFile, sectionId, rwVersion, parentChunk);
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

        RwStreamChunk newChunk = createSection(streamFile, chunkType, version, parentChunk); // Version will be loaded during the .load call too.
        newChunk.load(reader);
        return newChunk;
    }

    @Getter
    @RequiredArgsConstructor
    private static class RwStreamSectionTypeEntry {
        private final IRwStreamSectionType sectionType;
        private final TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> constructor;
    }

    static {
        // Register chunks.
        // TODO: Support many more chunks.

        defaultRegistry.registerChunkType(RwStructChunk::new);
        defaultRegistry.registerChunkType(RwImageChunk::new);
        defaultRegistry.registerChunkType(RwTableOfContentsChunk::new);
        defaultRegistry.registerChunkType(RwPlatformIndependentTextureDictionaryChunk::new);

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