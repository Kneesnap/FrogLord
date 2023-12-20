package net.highwayfrogs.editor.games.tgq.toc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.tgq.script.kcCActionSequence;

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
    OCTTREESCENEMGR("OTT\0", kcCResOctTreeSceneMgr::new),
    MODEL("6YTV", kcCResourceModel::new),
    TRACK("fEAB", null), // TODO
    HIERARCHY("fEHB", null), // TODO
    ANIMSET("RAS\0", null), // TODO
    ACTORDESC("RAD\0", null),
    TRIMESH("RTM\0", null), // TODO
    GENERIC("GEN\0", kcCResourceGeneric::new),
    TOC("TOC\0", TOCChunk::new),
    ACTIONSEQUENCE("ASQ\0", kcCActionSequence::new),
    NAMEDHASH("NHS\0", kcCResourceNamedHash::new),
    ENTITYINST("NST\0", kcCResourceEntityInst::new),
    DUMMY(null, null);

    private final String signature;
    private final Function<TGQChunkedFile, kcCResource> maker;

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
