package net.highwayfrogs.editor.games.tgq.toc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

import java.util.function.BiFunction;

/**
 * A registry of TOC Chunk Types.
 * Ids: (According to LoadFileChunks)
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@AllArgsConstructor
public enum TGQChunkType {
    TEX("TEX\0", (file, magic) -> new TEXChunk(file)),
    OTT("OTT\0", (file, magic) -> new OTTChunk(file)),
    VTX("6YTV", (file, magic) -> new VTXChunk(file)),
    IMG("IMGd", null), // This should never be used.
    //ASQ("ASQ\0", (file, magic) -> new ASQChunk(file)),
    TOC("TOC\0", (file, magic) -> new TOCChunk(file)),
    NHS("NHS\0", (file, magic) -> new NHSChunk(file)),
    NST("NST\0", (file, magic) -> new NSTChunk(file)),
    DUMMY(null, TGQDummyFileChunk::new);

    // Ids: (According to LoadFileChunks)
    // NON  - 0
    // RAW  - 1
    // TEX  - 2
    // OTT  - 3
    // 6YTV - 4
    // IMGd - 5
    // fEAB - 6 // Potentially model animation.
    // fEHB - 7
    // RAS  - 8
    // RAD  - 9
    // RTM  - 10
    // GEN  - 11, Seems to be scripting. In dialogue it appears arg2 is length of string.
    // TOC  - 12 //TODO: Is this table of contents, with a pointer to each of the files in order? It seems it has an entry for every chunk. It looks like it's sequentially increasing. Maybe it's a hash.
    // ASQ  - 13 This has some special handling, and it needs further research.
    // NHS  - 14
    // NST  - 15
    // It also seems that the chunks are in this order in the files,

    private final String signature;
    private BiFunction<TGQChunkedFile, String, TGQFileChunk> maker;

    /**
     * Gets the chunk type based on a magic number.
     * @param magic The magic string.
     * @return chunkType
     */
    public static TGQChunkType getByMagic(String magic) {
        for (TGQChunkType chunkType : values())
            if (chunkType.getSignature() != null && chunkType.getSignature().equals(magic))
                return chunkType;

        return DUMMY;
    }
}
