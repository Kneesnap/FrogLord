package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;

import java.util.function.Function;

/**
 * A registry of TGQ resource ids.
 * It appears files are laid out with these in order, by id ordinal.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@AllArgsConstructor
public enum KCResourceID {
    NONE("NON\0", null), // Game Registered ID: 0
    RAW("RAW\0", null), // Game Registered ID: 1
    TEXTURE("TEX\0", GreatQuestChunkTextureReference::new), // Game Registered ID: 2
    OCTTREESCENEMGR("OTT\0", kcCResOctTreeSceneMgr::new), // Game Registered ID: 3
    MODEL("6YTV", kcCResourceModel::new), // Game Registered ID: 4
    // IMGd (5)
    TRACK("fEAB", kcCResourceTrack::new), // Game Registered ID: 6,
    HIERARCHY("fEHB", kcCResourceSkeleton::new), // Game Registered ID: 7,
    ANIMSET("RAS\0", kcCResourceAnimSet::new), // Game Registered ID: 8
    ACTORDESC("RAD\0", null), // Game Registered ID: 9 (Unused?)
    TRIMESH("RTM\0", null), // TODO
    GENERIC("GEN\0", kcCResourceGeneric::new),
    TOC("TOC\0", TOCChunk::new),
    ACTIONSEQUENCE("ASQ\0", kcCActionSequence::new),
    NAMEDHASH("NHS\0", kcCResourceNamedHash::new),
    ENTITYINST("NST\0", kcCResourceEntityInst::new),
    DUMMY(null, null);

    private final String signature;
    private final Function<GreatQuestChunkedFile, kcCResource> maker;

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