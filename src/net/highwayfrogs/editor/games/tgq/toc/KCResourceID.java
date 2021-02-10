package net.highwayfrogs.editor.games.tgq.toc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

import java.util.function.Function;

/**
 * A registry of TGQ resource ids.
 * It appears files are laid out with these in order, by id ordinal.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@AllArgsConstructor
public enum KCResourceID {
    NONE("NON\0", null),
    RAW("RAW\0", null),
    TEXTURE("TEX\0", TGQChunkTextureReference::new),
    OCTTREESCENEMGR("OTT\0", OTTChunk::new),
    MODEL("6YTV", TGQChunk3DModel::new), // TODO
    IMAGE("IMGd", null),
    TRACK("fEAB", null),
    HIERARCHY("fEHB", null),
    ANIMSET("RAS\0", null),
    ACTORDESC("RAD\0", null),
    TRIMESH("RTM\0", null),
    GENERIC("GEN\0", TGQChunkGeneric::new),
    TOC("TOC\0", TOCChunk::new),
    ACTIONSEQUENCE("ASQ\0", TGQChunkActionSequence::new),
    NAMEDHASH("NHS\0", NHSChunk::new),
    ENTITYINST("NST\0", TGQChunkEntityInstance::new),
    DUMMY(null, null);

    private final String signature;
    private Function<TGQChunkedFile, kcCResource> maker;

    /**
     * Gets the chunk type based on a magic number.
     * @param magic The magic string.
     * @return chunkType
     */
    public static KCResourceID getByMagic(String magic) {
        for (KCResourceID chunkType : values())
            if (chunkType.getSignature() != null && chunkType.getSignature().equals(magic))
                return chunkType;

        return DUMMY;
    }
}
