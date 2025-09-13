package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;
import net.highwayfrogs.editor.gui.ImageResource;

import java.util.function.Function;

/**
 * A registry of TGQ resource ids.
 * It appears files are laid out with these in order, by id ordinal.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@AllArgsConstructor
public enum KCResourceID {
    NONE("NON\0", "None", ImageResource.GHIDRA_ICON_RED_X_16, null), // Game Registered ID: 0
    RAW("RAW\0", "Raw Data", ImageResource.WIN98_HEXCHAT_16, null), // Game Registered ID: 1
    TEXTURE("TEX\0", "Texture Ref", ImageResource.PHOTO_ALBUM_16, GreatQuestChunkTextureReference::new), // Game Registered ID: 2
    OCTTREESCENEMGR("OTT\0", "World", ImageResource.TREASURE_MAP_16, kcCResOctTreeSceneMgr::new), // Game Registered ID: 3
    MODEL("6YTV", "Model Ref", ImageResource.GEOMETRIC_SHAPES_16, kcCResourceModel::new), // Game Registered ID: 4
    // IMGd (5)
    TRACK("fEAB", "Animation", ImageResource.GHIDRA_ICON_MULTIMEDIA_16, kcCResourceTrack::new), // Game Registered ID: 6,
    HIERARCHY("fEHB", "Model Skeleton", ImageResource.SKELETON_JOINTS_16, kcCResourceSkeleton::new), // Game Registered ID: 7,
    ANIMSET("RAS\0", "Animation Set", ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_16, kcCResourceAnimSet::new), // Game Registered ID: 8
    ACTORDESC("RAD\0", "Actor Data", ImageResource.GHIDRA_ICON_GEAR_16, null), // Game Registered ID: 9 (Unused?)
    TRIMESH("RTM\0", "Collision Mesh", ImageResource.GOURAUD_TRIANGLE_LIST_16, kcCResourceTriMesh::new),
    GENERIC("GEN\0", "Generic Data", ImageResource.WIN98_HEXCHAT_16, kcCResourceGeneric::new),
    TOC("TOC\0", "Table of Contents", ImageResource.WIN98_HELP_CONTENTS_16, kcCResourceTableOfContents::new),
    ACTIONSEQUENCE("ASQ\0", "Action Sequence", ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_16, kcCActionSequence::new),
    NAMEDHASH("NHS\0", "Action Sequence Names", ImageResource.GHIDRA_ICON_CLIPBOARD_PASTE_16, kcCResourceNamedHash::new),
    ENTITYINST("NST\0", "Entity Instance", ImageResource.GHIDRA_ICON_MONKEY_16, kcCResourceEntityInst::new),
    DUMMY(null,"Dummy", ImageResource.GHIDRA_ICON_RED_X_16, null);

    private final String signature;
    private final String displayName;
    private final ImageResource icon;
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