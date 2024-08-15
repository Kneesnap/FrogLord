package net.highwayfrogs.editor.games.renderware;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * A registry of the stream chunk types which ship with RenderWare.
 * Created by Kneesnap on 8/11/2024.
 */
@Getter
@AllArgsConstructor
public enum RwStreamChunkType implements IRwStreamChunkType {
    STRUCT(0x01, "Struct", ImageResource.GHIDRA_ICON_TEXT_ALIGN_JUSTIFY_16),
    STRING(0x02, "String", ImageResource.GHIDRA_ICON_INFORMATION_16),
    EXTENSION(0x03, "Extension", ImageResource.GHIDRA_ICON_LOCATION_OUT_16),
    TEXTURE(0x06, "Texture", ImageResource.PHOTO_ALBUM_15),
    WORLD(0x0B, "World", ImageResource.GHIDRA_ICON_INTERNET_16),
    UNICODE_STRING(0x13, "Unicode String", ImageResource.GHIDRA_ICON_INFORMATION_16),
    IMAGE(0x18, "Image", ImageResource.PHOTO_ALBUM_15),
    PITEX_DICTIONARY(0x23, "Platform Independent Texture Dictionary", ImageResource.PHOTO_ALBUM_15),
    TOC(0x24, "Table of Contents", ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_16);

    private final int typeId;
    private final String displayName;
    private final ImageResource icon;
}