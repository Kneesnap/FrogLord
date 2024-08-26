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
 * Reference: <a href="https://gtamods.com/wiki/List_of_RW_section_IDs"/>
 * Created by Kneesnap on 8/11/2024.
 */
@Getter
@AllArgsConstructor
public enum RwStreamChunkType implements IRwStreamChunkType {
    STRUCT(0x01, "Struct", ImageResource.GHIDRA_ICON_TEXT_ALIGN_JUSTIFY_16, null), // Struct is handled separately.
    STRING(0x02, "String", ImageResource.GHIDRA_ICON_INFORMATION_16, RwStringChunk::new),
    EXTENSION(0x03, "Extension", ImageResource.GHIDRA_ICON_LOCATION_OUT_16, RwExtensionChunk::new),
    // 0x04 does not appear to exist, was it removed?
    CAMERA(0x05, "Camera", ImageResource.WIN98_SCREENSHOOTER_16, RwCameraChunk::new),
    TEXTURE(0x06, "Texture", ImageResource.PHOTO_ALBUM_16, RwTextureChunk::new, RwStreamChunkTypeDisplayImportance.LOW),
    MATERIAL(0x07, "Material", ImageResource.PAINTERS_PALETTE_16, RwMaterialChunk::new),
    MATERIAL_LIST(0x08, "Material List", ImageResource.PAINTERS_PALETTE_16, RwMaterialListChunk::new),
    ATOMIC_SECTOR(0x09, "World Sector", ImageResource.TREASURE_MAP_16, RwAtomicSectorChunk::new),
    PLANE_SECTOR(0x0A, "Plane Sector", ImageResource.TREASURE_MAP_16, RwPlaneSectorChunk::new),
    WORLD(0x0B, "World", ImageResource.GHIDRA_ICON_INTERNET_16, RwWorldChunk::new, RwStreamChunkTypeDisplayImportance.HIGHEST),
    // TODO: C Spline
    MATRIX(0x0D, "Matrix", ImageResource.MATRIX_16, RwStreamMatrixChunk::new),
    FRAME_LIST(0x0E, "Frame List", ImageResource.GHIDRA_ICON_SORT_ASCENDING_16, RwFrameListChunk::new),
    GEOMETRY(0x0F, "Geometry", ImageResource.GOURAUD_TRIANGLE_16, RwGeometryChunk::new),
    CLUMP(0x10, "Clump (3D Model)", ImageResource.GEOMETRIC_SHAPES_16, RwClumpChunk::new, RwStreamChunkTypeDisplayImportance.HIGH),
    // 0x11 does not exist, was it removed?
    LIGHT(0x12, "Light", ImageResource.VEXELS_OFFICE_BULB_ICON_16, RwLightChunk::new),
    UNICODE_STRING(0x13, "Unicode String", ImageResource.GHIDRA_ICON_INFORMATION_16, RwUnicodeStringChunk::new),
    ATOMIC(0x14, "Atomic", ImageResource.GOURAUD_TRIANGLE_16, RwAtomicChunk::new), // TODO: ATOM ICON
    IMAGE(0x18, "Image", ImageResource.PHOTO_ALBUM_16, RwImageChunk::new, RwStreamChunkTypeDisplayImportance.LOW),
    GEOMETRY_LIST(0x1A, "Geometry List", ImageResource.GOURAUD_TRIANGLE_LIST_16, RwGeometryListChunk::new),
    // *TODO: 1B Anim Animation
    PITEX_DICTIONARY(0x23, "Platform Independent Texture Dictionary", ImageResource.PHOTO_ALBUM_16, RwPlatformIndependentTextureDictionaryChunk::new, RwStreamChunkTypeDisplayImportance.LOW),
    TOC(0x24, "Table of Contents", ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_16, RwTableOfContentsChunk::new);
    // *TODO: 29, 2A, 2B

    private final int typeId;
    private final String displayName;
    private final ImageResource icon;
    private final TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> chunkCreator;
    private final RwStreamChunkTypeDisplayImportance displayImportance;

    RwStreamChunkType(int typeId, String displayName, ImageResource icon, TriFunction<RwStreamFile, Integer, RwStreamChunk, RwStreamChunk> chunkCreator) {
        this(typeId, displayName, icon, chunkCreator, null);
    }
}