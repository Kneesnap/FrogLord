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
    IMAGE("__rwImage", ImageResource.PHOTO_ALBUM_15),
    WORLD("_rpWorld", ImageResource.GHIDRA_ICON_INTERNET_16),
    UNKNOWN("UNSUPPORTED", ImageResource.QUESTION_MARK_15),
    VECTOR3("RwV3d", ImageResource.GHIDRA_ICON_QUESTION_MARK_16),
    BOUNDING_BOX("RwBBox", ImageResource.GEOMETRIC_SHAPES_15),
    STREAM_TEXTURE("rwStreamTexture", ImageResource.GHIDRA_ICON_QUESTION_MARK_16);

    private final String displayName;
    private final ImageResource icon;
}