package net.highwayfrogs.editor.games.tgq.toc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.tgq.TGQTOCFile;

import java.util.function.BiFunction;

/**
 * A registry of TOC Chunk Types.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@AllArgsConstructor
public enum TOCChunkType {
    OTT("OTT\0", (file, magic) -> new OTTChunk(file)),
    TEX("TEX\0", (file, magic) -> new TEXChunk(file)),
    NST("NST\0", (file, magic) -> new NSTChunk(file)),
    DUMMY(null, TOCDummyChunk::new);

    private final String signature;
    private BiFunction<TGQTOCFile, String, TOCChunk> maker;

    /**
     * Gets the chunk type based on a magic number.
     * @param magic The magic string.
     * @return chunkType
     */
    public static TOCChunkType getByMagic(String magic) {
        for (TOCChunkType chunkType : values())
            if (chunkType.getSignature() != null && chunkType.getSignature().equals(magic))
                return chunkType;

        return DUMMY;
    }
}
