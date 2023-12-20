package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.util.Arrays;

/**
 * Represents a map polygon seen in old Frogger.
 * Old Frogger only uses quads for map polygons.
 * The polygons use consistent ordering between colors, uvs, and vertices.
 * Assuming viewing from the default perspective, a quad is ordered like this:
 * 1---2
 * |  /|
 * | / |
 * |/  |
 * 3---4
 * UVs are based on a different corner though. UVs start from the bottom left corner, instead of the top corner. Meaning 0, 0 (Origin) is the bottom left corner.
 * To counteract this, we can do 1.0 - v to get the texture coordinates relative to the top left corner.
 * Created by Kneesnap on 12/9/2023.
 */
@Getter
public class OldFroggerMapPolygon extends SCGameData<OldFroggerGameInstance> {
    private final PSXPolygonType polygonType;
    private final int[] vertices;
    private final CVector[] colors;
    private long textureId = -1;
    private final SCByteTextureUV[] textureUvs;

    private static final SCByteTextureUV[] EMPTY_UV_ARRAY = new SCByteTextureUV[0];

    public OldFroggerMapPolygon(OldFroggerGameInstance instance, PSXPolygonType polygonType) {
        super(instance);
        this.polygonType = polygonType;
        this.vertices = new int[polygonType.getVerticeCount()];
        this.colors = new CVector[polygonType.getColorCount()];
        this.textureUvs = polygonType.isTextured() ? new SCByteTextureUV[polygonType.getVerticeCount()] : EMPTY_UV_ARRAY;
        Arrays.fill(this.vertices, -1);
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i] = new CVector();
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i] = new SCByteTextureUV();
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.vertices.length; i++)
            this.vertices[i] = reader.readUnsignedShortAsInt();
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].load(reader);
        if (this.polygonType.isTextured())
            this.textureId = reader.readUnsignedIntAsLong();
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i].load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.vertices.length; i++)
            writer.writeUnsignedShort(this.vertices[i]);
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].save(writer);
        if (this.polygonType.isTextured())
            writer.writeUnsignedInt(this.textureId);
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i].save(writer);
    }

    /**
     * Gets the texture held by this polygon.
     * @param levelTableEntry The level table entry to lookup data from.
     * @return texture
     */
    public GameImage getTexture(OldFroggerLevelTableEntry levelTableEntry) {
        if (!this.polygonType.isTextured() || this.textureId < 0)
            return null; // Untextured or invalid texture ID.

        if (levelTableEntry == null)
            return null; // Don't have the ability to look anything up.

        TextureRemapArray textureRemap = levelTableEntry.getTextureRemap();
        if (textureRemap == null)
            return null; // Failed to get the texture remap.

        Short globalTextureId = textureRemap.getRemappedTextureId((int) this.textureId);
        if (globalTextureId == null)
            return null; // This texture wasn't found in the remap.

        // Lookup image source.
        GameImage imageSource = null;

        // Try in the main VLO first.
        VLOArchive mainArchive = levelTableEntry.getMainVLOArchive();
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
    public PSXShadeTextureDefinition createPolygonShadeDefinition(OldFroggerLevelTableEntry levelTableEntry) {
        SCByteTextureUV[] uvs = this.polygonType.isTextured() ? this.textureUvs : null;
        ITextureSource textureSource = this.polygonType.isTextured() ? getTexture(levelTableEntry) : null;
        return new PSXShadeTextureDefinition(this.polygonType, textureSource, this.colors, uvs);
    }

    /**
     * Get the size in bytes of a polygon of the given psx polygon type.
     * @param polygonType The polygon type to calculate the byte size of
     * @return byteSize
     */
    public static int getByteSize(PSXPolygonType polygonType) {
        return (polygonType.getVerticeCount() * Constants.SHORT_SIZE)
                + (polygonType.getColorCount() * CVector.BYTE_LENGTH)
                + (polygonType.isTextured() ? Constants.INTEGER_SIZE : 0)
                + (polygonType.isTextured() ? polygonType.getVerticeCount() * SCByteTextureUV.BYTE_SIZE : 0);
    }
}