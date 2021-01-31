package net.highwayfrogs.editor.games.renderware;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.renderware.chunks.RWUnsupportedChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Manages chunks.
 * Created by Kneesnap on 6/9/2020.
 */
public class RWSChunkManager {
    private static Map<Integer, BiFunction<Integer, RWSChunk, RWSChunk>> supportedChunks = new HashMap<>();

    /**
     * Registers a chunk type.
     * @param chunkId      The chunk's id.
     * @param chunkCreator The constructor to create a chunk.
     */
    public static void registerChunkType(int chunkId, BiFunction<Integer, RWSChunk, RWSChunk> chunkCreator) {
        supportedChunks.put(chunkId, chunkCreator);
    }

    /**
     * Creates a chunk based on the magic id supplied.
     * @param chunkId   The chunk id to read.
     * @param rwVersion The renderware version the chunk was built with.
     * @return newChunk
     */
    public static RWSChunk createChunk(int chunkId, int rwVersion, RWSChunk parentChunk) {
        BiFunction<Integer, RWSChunk, RWSChunk> chunkCreator = supportedChunks.get(chunkId);
        if (chunkCreator != null) {
            return chunkCreator.apply(rwVersion, parentChunk);
        } else {
            return new RWUnsupportedChunk(chunkId, rwVersion, parentChunk);
        }
    }

    /**
     * Reads a new chunk from a DataReader.
     * @param reader The reader to read the chunk from.
     * @return newChunk.
     */
    public static RWSChunk readChunk(DataReader reader, RWSChunk parentChunk) {
        int chunkType = reader.readInt();
        RWSChunk newChunk = RWSChunkManager.createChunk(chunkType, 0, parentChunk); // Version is unknown, it will be during the .load call.
        newChunk.load(reader);
        return newChunk;
    }

    static {
        // Register chunks.
        //TODO

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
