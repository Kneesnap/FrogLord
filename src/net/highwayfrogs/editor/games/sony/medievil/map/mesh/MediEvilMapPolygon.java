package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.Utils;

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
    private static final CVector UNSHADED_COLOR = CVector.makeColorFromRGB(0x7F7F7F7F);

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
     * Test if a bit mask containing flags is set
     * @param flagMask the bits containing flags to test
     * @return flagMaskSet
     */
    public boolean isFlagMaskSet(int flagMask) {
        return (this.flags & flagMask) == flagMask;
    }

    /**
     * Sets whether the given flags are set.
     * @param flagMask the bit flags to apply
     * @param newState whether the bits should be set true or false
     */
    public void setFlagMask(int flagMask, boolean newState) {
        if (newState) {
            this.flags |= flagMask;
        } else {
            this.flags &= ~flagMask;
        }
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
     * Set the type of PSX polygon.
     */
    public void setPolygonType(PSXPolygonType newType) {
        switch (newType) {
            case POLY_G3:
                setFlagMask(FLAG_QUAD | FLAG_TEXTURED, false);
                break;
            case POLY_GT3:
                setFlagMask(FLAG_QUAD, false);
                setFlagMask(FLAG_TEXTURED, true);
                break;
            case POLY_G4:
                setFlagMask(FLAG_QUAD, true);
                setFlagMask(FLAG_TEXTURED, false);
                break;
            case POLY_GT4:
                setFlagMask(FLAG_QUAD | FLAG_TEXTURED, true);
                break;
            default:
                throw new IllegalArgumentException("MediEvil map polygons cannot be set to type " + newType + ".");
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
     * @param mapFile The map file which the polygon is used within.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(MediEvilMapFile mapFile, boolean enableGouraudShading) {
        MediEvilLevelTableEntry levelTableEntry = mapFile.getLevelTableEntry();
        PSXPolygonType polygonType = getPolygonType();
        boolean isSemiTransparent = isSemiTransparent(levelTableEntry);

        SCByteTextureUV[] uvs = null;
        if (polygonType.isTextured()) {
            uvs = new SCByteTextureUV[getVertexCount()];
            for (int i = 0; i < uvs.length; i++)
                uvs[i] = this.textureUvs[i].clone();
        }

        // Apply colors.
        if (!polygonType.isGouraud())
            throw new IllegalStateException("MediEvil does not support map polygons with flat shading. (" + polygonType + ")");

        CVector[] colors = new CVector[polygonType.getColorCount()];
        if (enableGouraudShading) {
            for (int i = 0; i < colors.length; i++) {
                SVector vertex = mapFile.getGraphicsPacket().getVertices().get(this.vertices[i]);
                colors[i] = fromPackedShort(vertex.getPadding(), polygonType, isSemiTransparent);
            }
        } else {
            Arrays.fill(colors, UNSHADED_COLOR);
        }

        // Create definition.
        ITextureSource textureSource = polygonType.isTextured() ? getTexture(levelTableEntry) : null;
        return new PSXShadeTextureDefinition(polygonType, textureSource, colors, uvs, isSemiTransparent);
    }

    private static CVector fromPackedShort(short packedColor, PSXPolygonType polygonType, boolean isSemiTransparent) {
        // Process padding into color value.
        int blueHiBits = (packedColor & 0x700);
        int blueLoBits = (packedColor & 3);
        int bgrColor = (packedColor & 0xF8F8) | (blueHiBits << 13) | (blueLoBits << 19);

        // Calculate GPU code.
        byte gpuCode = CVector.GP0_COMMAND_POLYGON_PRIMITIVE | CVector.FLAG_GOURAUD_SHADING;
        if (polygonType.isQuad())
            gpuCode |= CVector.FLAG_QUAD;
        if (polygonType.isTextured())
            gpuCode |= CVector.FLAG_TEXTURED;
        if (isSemiTransparent)
            gpuCode |= CVector.FLAG_SEMI_TRANSPARENT;

        // Create color.
        CVector loadedColor = CVector.makeColorFromRGB(Utils.swapRedBlue(bgrColor));
        loadedColor.setCode(gpuCode);
        return loadedColor;
    }

    /**
     * Creates a texture shade definition for this polygon.
     * @param mapFile The level necessary for looking up texture remap data.
     * @param shadeTexture The shaded texture to load from.
     */
    public void loadDataFromShadeDefinition(MediEvilMapFile mapFile, PSXShadeTextureDefinition shadeTexture, boolean isShadingEnabled) {
        if (shadeTexture.getPolygonType().getVerticeCount() != getPolygonType().getVerticeCount())
            throw new UnsupportedOperationException("Cannot change between quad/tri polygons."); // This is just not coded yet, we could theoretically add this.

        // Update polygon type.
        PSXPolygonType newPolygonType = shadeTexture.getPolygonType();
        setPolygonType(newPolygonType);

        // Apply texture UVs.
        if (newPolygonType.isTextured() && shadeTexture.getTextureUVs() != null) {
            for (int i = 0; i < newPolygonType.getVerticeCount(); i++) {
                SCByteTextureUV modifiedUv = shadeTexture.getTextureUVs()[i];
                if (modifiedUv != null)
                    this.textureUvs[i].copyFrom(modifiedUv);
            }
        }

        // Apply colors.
        if (isShadingEnabled && newPolygonType.getColorCount() > 0 && shadeTexture.getColors() != null && newPolygonType.getVerticeCount() == shadeTexture.getColors().length) {
            for (int i = 0; i < shadeTexture.getVerticeCount(); i++) {
                SVector vertex = mapFile.getGraphicsPacket().getVertices().get(this.vertices[i]);
                vertex.setPadding(toPackedShort(shadeTexture.getColors()[i]));
            }
        }

        // Load texture.
        if (shadeTexture.getTextureSource() instanceof GameImage) {
            GameImage gameImage = (GameImage) shadeTexture.getTextureSource();
            MediEvilLevelTableEntry levelTableEntry = mapFile.getLevelTableEntry();
            int remapIndex = levelTableEntry.getRemap().getRemapIndex(gameImage.getTextureId());
            if (remapIndex >= 0)
                this.textureId = remapIndex;
        }
    }

    private static short toPackedShort(CVector color) {
        return (short) (((color.getGreenShort() & 0b11111000) << 8) | (color.getRedShort() & 0b11111000)
                | ((color.getBlueShort() & 0b111000000) << 3) | ((color.getBlueShort() & 0b00011000) >> 3));
    }
}