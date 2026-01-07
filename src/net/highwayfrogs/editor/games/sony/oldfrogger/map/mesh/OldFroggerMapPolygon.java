package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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
 * This order matches how the PSX GPU processes a quad, first using vertices 1-2-3, then 2-3-4, according to <a href="https://psx-spx.consoledev.net/graphicsprocessingunitgpu/">this link</a>.
 * UVs are based on a different corner though. UVs start from the bottom left corner, instead of the top corner. Meaning 0, 0 (Origin) is the bottom left corner.
 * To counteract this, we can do 1.0 - v to get the texture coordinates relative to the top left corner.
 * Created by Kneesnap on 12/9/2023.
 */
@Getter
public class OldFroggerMapPolygon extends SCGameData<OldFroggerGameInstance> {
    private PSXPolygonType polygonType;
    private final int[] vertices;
    private CVector[] colors;
    @Setter private long textureId = -1;
    private SCByteTextureUV[] textureUvs;

    private static final SCByteTextureUV[] EMPTY_UV_ARRAY = new SCByteTextureUV[0];
    private static final CVector[] EMPTY_COLOR_ARRAY = new CVector[0];

    public OldFroggerMapPolygon(OldFroggerGameInstance instance, PSXPolygonType polygonType) {
        super(instance);
        this.polygonType = polygonType;
        this.vertices = new int[polygonType.getVerticeCount()];
        this.colors = polygonType.getColorCount() > 0 ? new CVector[polygonType.getColorCount()] : EMPTY_COLOR_ARRAY;
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
    public VloImage getTexture(OldFroggerLevelTableEntry levelTableEntry) {
        if (!this.polygonType.isTextured() || this.textureId < 0)
            return null; // Untextured or invalid texture ID.

        if (levelTableEntry == null)
            return null; // Don't have the ability to look anything up.

        TextureRemapArray textureRemap = levelTableEntry.getTextureRemap();
        return textureRemap != null ? textureRemap.resolveTexture((int) this.textureId, levelTableEntry.getMainVloFile()) : null;
    }

    /**
     * Creates a texture shade definition for this polygon.
     * @param mapMesh The map mesh to create the shading definition for.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(OldFroggerMapMesh mapMesh) {
        OldFroggerMapFile mapFile = mapMesh != null ? mapMesh.getMap() : null;

        SCByteTextureUV[] uvs = null;
        if (this.polygonType.isTextured()) {
            uvs = new SCByteTextureUV[this.textureUvs.length];
            for (int i = 0; i < uvs.length; i++) {
                SCByteTextureUV clonedUv = this.textureUvs[i].clone();
                clonedUv.setFloatV(1F - clonedUv.getFloatV());
                uvs[i] = clonedUv;
            }
        }

        // Clone colors.
        CVector[] colors = null;
        if (this.colors.length > 0)
            colors = copyColors(mapFile, this.colors, null);

        ITextureSource textureSource = this.polygonType.isTextured() && mapFile != null ? getTexture(mapFile.getLevelTableEntry()) : null;
        PSXShadedTextureManager<OldFroggerMapPolygon> shadedTextureManager = mapMesh != null ? mapMesh.getShadedTextureManager() : null;
        return new PSXShadeTextureDefinition(shadedTextureManager, this.polygonType, textureSource, colors, uvs, false, true);
    }

    /**
     * Creates a texture shade definition for this polygon.
     * @param mapFile The level necessary for looking up texture remap data.
     * @param shadeTexture The shaded texture to load from.
     */
    public void loadDataFromShadeDefinition(OldFroggerMapFile mapFile, PSXShadeTextureDefinition shadeTexture) {
        if (shadeTexture.getPolygonType().getVerticeCount() != this.polygonType.getVerticeCount())
            throw new UnsupportedOperationException("Cannot change between quad/tri polygons."); // This is just not coded yet, we could theoretically add this.

        // Update polygon type.
        this.polygonType = shadeTexture.getPolygonType();

        // Clone texture UVs.
        if (this.polygonType.isTextured() && shadeTexture.getTextureUVs() != null) {
            if (this.textureUvs.length != shadeTexture.getTextureUVs().length)
                this.textureUvs = Arrays.copyOf(this.textureUvs, this.polygonType.getVerticeCount());

            for (int i = 0; i < this.textureUvs.length; i++) {
                SCByteTextureUV modifiedUv = shadeTexture.getTextureUVs()[i];

                if (this.textureUvs[i] != null) {
                    this.textureUvs[i].copyFrom(modifiedUv);
                } else {
                    this.textureUvs[i] = modifiedUv.clone();
                }

                this.textureUvs[i].setFloatV(1F - modifiedUv.getFloatV());
            }
        } else {
            this.textureUvs = EMPTY_UV_ARRAY;
        }

        // Clone colors.
        if (this.polygonType.getColorCount() > 0 && shadeTexture.getColors() != null) {
            if (this.colors.length != shadeTexture.getColors().length)
                this.colors = Arrays.copyOf(this.colors, this.polygonType.getColorCount());

            for (int i = 0; i < this.colors.length; i++) {
                if (this.colors[i] != null) {
                    this.colors[i].copyFrom(shadeTexture.getColors()[i]);
                } else {
                    this.colors[i] = shadeTexture.getColors()[i].clone();
                }
            }
        } else {
            this.colors = EMPTY_COLOR_ARRAY;
        }

        // Strip out max brightness for cave levels. (The game lights areas with code)
        if (isCaveLightingEnabledOnSavedColors(mapFile, this.colors))
            for (int i = 0; i < this.colors.length; i++)
                this.colors[i].fromRGB(0);

        // Load texture.
        if (shadeTexture.getTextureSource() instanceof VloImage) {
            VloImage gameImage = (VloImage) shadeTexture.getTextureSource();
            OldFroggerLevelTableEntry levelTableEntry = mapFile.getLevelTableEntry();
            int remapIndex = levelTableEntry.getTextureRemap().getRemapIndex(gameImage.getTextureId());
            if (remapIndex >= 0)
                this.textureId = remapIndex;
        }
    }

    public static CVector[] copyColors(OldFroggerMapFile mapFile, CVector[] oldColors, CVector[] newColors) {
        if (oldColors == null)
            throw new NullPointerException("oldColors");
        if (newColors != null && newColors.length != oldColors.length)
            throw new IllegalArgumentException("newColors.length was expected to be " + oldColors.length + ", but was actually " + newColors.length + "!");
        if (newColors == null)
            newColors = new CVector[oldColors.length];
        for (int i = 0; i < newColors.length; i++)
            newColors[i] = oldColors[i].clone();

        // Enable max brightness for cave levels. (The game lights areas with code)
        if (isCaveLightingEnabledOnLoadedColors(mapFile, newColors))
            for (int i = 0; i < newColors.length; i++)
                newColors[i].fromRGB(0xFFFFFF);

        return newColors;
    }

    private static boolean isCaveLightingEnabledOnLoadedColors(OldFroggerMapFile mapFile, CVector[] colors) {
        if (mapFile == null || !mapFile.getMapConfig().isCaveLightingEnabled())
            return false; // Only the cave map has cave lighting.

        for (int i = 0; i < colors.length; i++) {
            CVector color = colors[i];
            if (color.getRed() != 0 || color.getGreen() != 0 || color.getBlue() != 0)
                return false; // Found a color shading value that wasn't zero.
        }

        return true;
    }

    private static boolean isCaveLightingEnabledOnSavedColors(OldFroggerMapFile mapFile, CVector[] colors) {
        if (!mapFile.getMapConfig().isCaveLightingEnabled())
            return false; // Only the cave map has cave lighting.

        for (int i = 0; i < colors.length; i++) {
            CVector color = colors[i];
            if (color.getRedShort() != 0xFF || color.getGreenShort() != 0xFF || color.getBlueShort() != 0xFF)
                return false; // Found a color shading value that wasn't zero.
        }

        return true;
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