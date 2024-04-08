package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.util.Arrays;

/**
 * Represents a map polygon seen in MediEvil.
 * The polygons use consistent ordering between colors, uvs, and vertices.
 * Assuming viewing from the default perspective, a quad is ordered like this:
 * 1---2
 * |  /|
 * | / |
 * |/  |
 * 3---4
 * This order matches how the PSX GPU processes a quad, first using vertices 1-2-3, then 2-3-4, according to <a href="https://psx-spx.consoledev.net/graphicsprocessingunitgpu/">this link</a>.
 * UVs are based on a different corner though. UVs start from the bottom left corner, instead of the top corner. Meaning 0, 0 (Origin) is the bottom left corner.
 * To counteract this, we can do 1.0 - v to get the texture coordinates relative to the top left corner.
 * Created by Kneesnap on 3/8/2024.
 */
@Getter
public class MediEvilMapPolygon extends SCGameData<MediEvilGameInstance> {
    private final int[] vertices = new int[INTERNAL_VERTEX_COUNT];
    private int textureId = -1;
    private int flags;
    private final SCByteTextureUV[] textureUvs = new SCByteTextureUV[INTERNAL_VERTEX_COUNT];

    private static final SCByteTextureUV[] EMPTY_UV_ARRAY = new SCByteTextureUV[0];
    private static final int INTERNAL_VERTEX_COUNT = 4;

    private static final int FLAG_QUAD = Constants.BIT_FLAG_0;
    private static final int FLAG_TEXTURED = Constants.BIT_FLAG_1;
    public static final int FLAG_SEMI_TRANSPARENT = Constants.BIT_FLAG_9;

    public MediEvilMapPolygon(MediEvilGameInstance instance) {
        super(instance);
        Arrays.fill(this.vertices, -1);
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i] = new SCByteTextureUV();
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.vertices.length; i++)
            this.vertices[i] = reader.readUnsignedShortAsInt();
        this.textureId = reader.readUnsignedShortAsInt();
        this.flags = reader.readUnsignedShortAsInt();
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i].load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.vertices.length; i++)
            writer.writeUnsignedShort(this.vertices[i]);
        writer.writeUnsignedShort(this.textureId);
        writer.writeUnsignedShort(this.flags);
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i].save(writer);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MediEvilMapPolygon{").append(getPolygonType())
                .append(", Texture: ").append(this.textureId).append(", Flags: ").append(this.flags).append(", vertices=[");
        for (int i = 0; i < this.vertices.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(this.vertices[i]);
        }

        return builder.append("]}").toString();
    }

    /**
     * Get the number of vertices supported by this polygon.
     */
    public int getVertexCount() {
        return getPolygonType().getVerticeCount();
    }

    /**
     * If the polygon renders as partially transparent, this will return true.
     */
    public boolean isSemiTransparent(MediEvilLevelTableEntry levelTableEntry) {
        GameImage image = getTexture(levelTableEntry);
        if (image != null && image.testFlag(GameImage.FLAG_TRANSLUCENT))
            return true;

        return (this.flags & FLAG_SEMI_TRANSPARENT) == FLAG_SEMI_TRANSPARENT;
    }

    /**
     * Get the type of PSX polygon.
     */
    public PSXPolygonType getPolygonType() {
        if ((this.flags & FLAG_QUAD) == FLAG_QUAD) {
            if ((this.flags & FLAG_TEXTURED) == FLAG_TEXTURED) {
                return PSXPolygonType.POLY_GT4;
            } else {
                return PSXPolygonType.POLY_G4;
            }
        } else {
            if ((this.flags & FLAG_TEXTURED) == FLAG_TEXTURED) {
                return PSXPolygonType.POLY_GT3;
            } else {
                return PSXPolygonType.POLY_G3;
            }
        }
    }

    /**
     * Gets the texture held by this polygon.
     * @param levelTableEntry The level table entry to lookup data from.
     * @return texture
     */
    public GameImage getTexture(MediEvilLevelTableEntry levelTableEntry) {
        if (!getPolygonType().isTextured() || this.textureId < 0)
            return null; // Untextured or invalid texture ID.

        if (levelTableEntry == null)
            return null; // Don't have the ability to look anything up.

        TextureRemapArray textureRemap = levelTableEntry.getRemap();
        if (textureRemap == null)
            return null; // Failed to get the texture remap.

        Short globalTextureId = textureRemap.getRemappedTextureId(this.textureId);
        if (globalTextureId == null)
            return null; // This texture wasn't found in the remap.

        // Lookup image source.
        GameImage imageSource = null;

        // Try in the main VLO first.
        VLOArchive mainArchive = levelTableEntry.getVloFile();
        if (mainArchive != null)
            imageSource = mainArchive.getImageByTextureId(globalTextureId);

        // Otherwise, search globally.
        if (imageSource == null)
            imageSource = levelTableEntry.getArchive().getImageByTextureId(globalTextureId);

        return imageSource;
    }

    /**
     * Creates a texture shade definition for this polygon.
     * @param levelTableEntry The level table entry necessary for looking up texture remap data.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(MediEvilLevelTableEntry levelTableEntry) {
        PSXPolygonType polygonType = getPolygonType();

        SCByteTextureUV[] uvs = null;
        if (polygonType.isTextured()) {
            uvs = new SCByteTextureUV[getVertexCount()];
            for (int i = 0; i < uvs.length; i++)
                uvs[i] = this.textureUvs[i].clone();
        }

        // Clone colors.
        CVector[] colors = new CVector[polygonType.isGouraud() ? getVertexCount() : 1];
        for (int i = 0; i < colors.length; i++) // TODO: Get shading information from the map file.
            colors[i] = CVector.makeColorFromRGB(polygonType.isGouraud() ? 0x7F7F7F7F : 0xFFFFFFFF);

        ITextureSource textureSource = polygonType.isTextured() ? getTexture(levelTableEntry) : null;
        return new PSXShadeTextureDefinition(polygonType, textureSource, colors, uvs, isSemiTransparent(levelTableEntry));
    }
}