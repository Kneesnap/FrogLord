package net.highwayfrogs.editor.games.tgq.toc;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registry of TOC Chunk Types.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@AllArgsConstructor
public enum TOCChunkType {
    OTT("OTT", "OTT\0"),
    DUMMY("???", "????");

    private final String name;
    private final String signature;

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
