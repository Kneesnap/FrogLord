package net.highwayfrogs.editor.games.renderware;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.chunks.*;
import net.highwayfrogs.editor.games.renderware.chunks.sector.RwAtomicSectorChunk;
import net.highwayfrogs.editor.games.renderware.chunks.sector.RwPlaneSectorChunk;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.system.TriFunction;

/**
 * A registry of the stream chunk types which ship with RenderWare.
 * Created by Kneesnap on 8/11/2024.
 */
@Getter
@AllArgsConstructor
public enum RwStreamChunkType implements IRwStreamChunkType {
    STRUCT(0x01, "Struct", ImageResource.GHIDRA_ICON_TEXT_ALIGN_JUSTIFY_16, null), // Struct is handled separately.
    STRING(0x02, "String", ImageResource.GHIDRA_ICON_INFORMATION_16, RwStringChunk::new),
    EXTENSION(0x03, "Extension", ImageResource.GHIDRA_ICON_LOCATION_OUT_16, RwExtensionChunk::new),
    TEXTURE(0x06, "Texture", ImageResource.PHOTO_ALBUM_15, RwTextureChunk::new, RwStreamChunkTypeDisplayImportance.LOW),
    MATERIAL(0x07, "Material", ImageResource.PAINTERS_PALETTE_16, RwMaterialChunk::new),
    MATERIAL_LIST(0x08, "Material List", ImageResource.PAINTERS_PALETTE_16, RwMaterialListChunk::new),
    ATOMIC_SECTOR(0x09, "World Sector", ImageResource.TREASURE_MAP_15, RwAtomicSectorChunk::new),
    PLANE_SECTOR(0x0A, "Plane Sector", ImageResource.TREASURE_MAP_15, RwPlaneSectorChunk::new),
    WORLD(0x0B, "World", ImageResource.GHIDRA_ICON_INTERNET_16, RwWorldChunk::new, RwStreamChunkTypeDisplayImportance.HIGHEST),
    UNICODE_STRING(0x13, "Unicode String", ImageResource.GHIDRA_ICON_INFORMATION_16, RwUnicodeStringChunk::new),
    IMAGE(0x18, "Image", ImageResource.PHOTO_ALBUM_15, RwImageChunk::new, RwStreamChunkTypeDisplayImportance.LOW),
    PITEX_DICTIONARY(0x23, "Platform Independent Texture Dictionary", ImageResource.PHOTO_ALBUM_15, RwPlatformIndependentTextureDictionaryChunk::new, RwStreamChunkTypeDisplayImportance.LOW),
    TOC(0x24, "Table of Contents", ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_16, RwTableOfContentsChunk::new);

    private final int typeId;
    private final String displayName;
    private final ImageResource icon;
    private final TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> chunkCreator;
    private final RwStreamChunkTypeDisplayImportance displayImportance;

    RwStreamChunkType(int typeId, String displayName, ImageResource icon, TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> chunkCreator) {
        this(typeId, displayName, icon, chunkCreator, null);
    }

    // Clump - 0x10 (First one with child nodes.)
    // Frame List  - 0x0E
    // HAnim PLG - 11E
    // Geometry List - 0x1A
    // Geometry - 0x0F
    // Sky Minimap Val = 0x110
    // Atomic - 0x14
    // Anim Animation - 0x1B

    // Bin Mesh PLG - 0x50E
    // Skin PLG - 0x116
    // Morph PLG - 0x105
    // User Data PLG - 0x11F
    // Right to Render - 0x1F
}