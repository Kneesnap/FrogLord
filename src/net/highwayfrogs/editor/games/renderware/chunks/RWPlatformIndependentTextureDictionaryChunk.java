package net.highwayfrogs.editor.games.renderware.chunks;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RWSChunk;

import java.awt.image.BufferedImage;

/**
 * Platform Independent Texture Dictionary
 * Created by Kneesnap on 6/9/2020.
 */
public class RWPlatformIndependentTextureDictionaryChunk extends RWSChunk {
    private String name;
    private BufferedImage image;

    public static final int TYPE_ID = 0x23;

    public RWPlatformIndependentTextureDictionaryChunk(int typeId, int renderwareVersion, RWSChunk parentChunk) {
        super(typeId, renderwareVersion, parentChunk);
    }

    @Override
    public void loadChunkData(DataReader reader) {
        int textures = reader.readInt(); // Might be number of textures, might be number of frames.


        // Textures:
        // Null-terminated string, texture name.
        // There's 64 bytes of here of unknown data before the image starts.

        // TODO: There are nested chunks.
        // Chunk 0x18 contains chunk 0x01. (0x18 is Image) (0x01 contains the header data)

        // Header: Width (int), Height (int), Bit Depth (int), unknown (int) (BIT DEPTH IS NOT ALWAYS 32)
        // Then color values exist for the rest of the image chunk. (after the struct header.)

        // Then, after that, this chunk continues.
    }

    @Override
    public void saveChunkData(DataWriter writer) {

    }
}
