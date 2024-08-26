package net.highwayfrogs.editor.games.renderware.struct;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * A registry of structs readable by RwStructChunk.
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
@AllArgsConstructor
public enum RwStructType {
    PARENT_DATA_PROXY("FrogLordParentDataProxy", ImageResource.GHIDRA_ICON_RED_EXCLAMATION_CIRCLE_16, false), // Uses Red ! because this should be hidden from the user.
    UNKNOWN("UNSUPPORTED", ImageResource.QUESTION_MARK_16, false),
    IMAGE("__rwImage", ImageResource.PHOTO_ALBUM_16, false),
    WORLD("_rpWorld", ImageResource.GHIDRA_ICON_INTERNET_16, false),
    VECTOR2("RwV2d", ImageResource.GHIDRA_ICON_QUESTION_MARK_16, false),
    VECTOR3("RwV3d", ImageResource.GHIDRA_ICON_QUESTION_MARK_16, false),
    BOUNDING_BOX("RwBBox", ImageResource.NUVOLA_BLACK_BOX_16, false),
    STREAM_TEXTURE("rwStreamTexture", ImageResource.GHIDRA_ICON_QUESTION_MARK_16, false),
    MATERIAL_CHUNK_INFO("RpMaterialChunkInfo", ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_16, false),
    MATERIAL_LIST_STRUCT("Material List Data", ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_16, false),
    WORLD_CHUNK_INFO_SECTOR("_rpWorldSector", ImageResource.TREASURE_MAP_16, true),
    PLANE_SECTOR_CHUNK_INFO("_rpPlaneSector", ImageResource.TREASURE_MAP_16, false),
    VERTEX_NORMAL("RpVertexNormal",  ImageResource.GHIDRA_ICON_ARROW_UP_16, false),
    COLOR_RGBA("RwColorRGBA", ImageResource.COLOR_WHEEL_16, false),
    TRIANGLE("RpTriangle", ImageResource.GOURAUD_TRIANGLE_16, false),
    TEXCOORDS("RwTexCoords", ImageResource.COORDINATE_SYSTEM_XY_16, false),
    COLLISION_SECTOR("RpCollSector", ImageResource.GHIDRA_ICON_QUESTION_MARK_16, false), // TODO: COLLISION ICON
    CLUMP_CHUNK_INFO("_rpClump", ImageResource.GEOMETRIC_SHAPES_16, false),
    STREAM_FRAME("rwStreamFrame", ImageResource.GHIDRA_ICON_SORT_ASCENDING_16, false), // TODO: KEYFRAME ICON
    GEOMETRY_CHUNK_INFO("_rpGeometry", ImageResource.GOURAUD_TRIANGLE_16, false),
    MORPH_TARGET("_rpMorphTarget", ImageResource.SKELETON_JOINTS_16, false), // TODO: MORPH ICON
    SPHERE("RwSphere", ImageResource.GEOMETRIC_SHAPES_16, false), // TODO: SPHERE ICON
    GENERIC_INTEGER("RwInt32", ImageResource.GHIDRA_ICON_INFORMATION_16, false);

    private final String displayName;
    private final ImageResource icon;
    private final boolean readExtensionChunk;
}