package net.highwayfrogs.editor.games.sony.shared.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMesh;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket.SCMapPolygonUV;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;

/**
 * Represents a map polygon in a later SC game.
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
public class SCMapPolygon extends SCGameData<SCGameInstance> {
    private final SCMapFile<?> mapFile;
    private final int[] vertices = new int[INTERNAL_VERTEX_COUNT];
    private int textureId = -1;
    private int uvInfo;
    private int flags;

    private static final SCByteTextureUV[] EMPTY_UV_ARRAY = new SCByteTextureUV[0];
    private static final int INTERNAL_VERTEX_COUNT = 4;
    public static final int TEXTURE_ID_NO_TEXTURE = 0xFFFF;
    public static final int TEXTURE_ID_DEFAULT_TEXTURE = 0;

    private static final int FLAG_QUAD = Constants.BIT_FLAG_0;
    private static final int FLAG_SORTBYNEARZ = Constants.BIT_FLAG_2;
    private static final int FLAG_SORTBYFARZ = Constants.BIT_FLAG_3;
    public static final int FLAG_SEMI_TRANSPARENT = Constants.BIT_FLAG_4;
    // Further flags unknown.
    private static final CVector UNSHADED_COLOR = CVector.makeColorFromRGB(0x80808080);

    //
    private static final int FLAG_UV_INDEX = Constants.BIT_FLAG_15;

    public SCMapPolygon(SCMapFile<?> mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
        Arrays.fill(this.vertices, -1);
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.vertices.length; i++)
            this.vertices[i] = reader.readUnsignedShortAsInt();
        this.textureId = reader.readUnsignedShortAsInt();
        this.uvInfo = reader.readUnsignedShortAsInt();
        this.flags = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.vertices.length; i++)
            writer.writeUnsignedShort(this.vertices[i]);
        writer.writeUnsignedShort(this.textureId);
        writer.writeUnsignedShort(this.uvInfo);
        writer.writeInt(this.flags);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName()).append("{").append(getPolygonType())
                .append(", Texture: ").append(this.textureId).append(", Flags: ").append(this.flags).append(", vertices=[");
        for (int i = 0; i < this.vertices.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(this.vertices[i]);
        }

        return builder.append("]}").toString();
    }

    /**
     * Gets the texture UV for the given vertex index.
     * @param index the index to get the texture uv for
     * @return textureUvData
     */
    public SCByteTextureUV getTextureUv(int index) {
        return getTextureUv(index, new SCByteTextureUV());
    }

    /**
     * Gets the texture UV for the given vertex index.
     * @param index the index to get the texture uv for
     * @param result the object to store the uv data in.
     * @return textureUvData
     */
    public SCByteTextureUV getTextureUv(int index, SCByteTextureUV result) {
        if (index < 0 || index >= getVertexCount())
            throw new IndexOutOfBoundsException("Invalid UV Index " + index);

        // Get indexed UV.
        if ((this.uvInfo & FLAG_UV_INDEX) == FLAG_UV_INDEX) {
            int uvIndex = (this.uvInfo & ~FLAG_UV_INDEX);
            SCMapPolygonUV polygonUv = this.mapFile.getPolygonPacket().getUvs().get(uvIndex);
            SCByteTextureUV vertexUv = polygonUv.getTextureUvs()[index];
            result.copyFrom(vertexUv);
            return result;
        }

        // What's going on here is annoying.
        // It seems like the game is using 3 bits (multiplied by two) as an offset into the MR_TEXTURE structure.
        // Some values are valid, others point to stuff like the texture ID. Go figure.
        // What this means is that it indexes into the configured uvs for the texture. Which, is always going to be the full size of the image for us since we apply the texture uvs separately from the polygon uvs.
        int value = (this.uvInfo >> (index * 3)) & 0b111;
        switch (value) {
            // 0 - flags
            // 1 - width / height.
            case 2: // u0/v0
                result.setFloatUV(0F, 0F);
                break;
                // 3 - clut ID
            case 4: // u1/v1
                result.setFloatUV(1F, 0F);
                break;
                // 5 - texture page.
            case 6: // u2/v2
                result.setFloatUV(0F, 1F);
                break;
            case 7: // u3/v3
                result.setFloatUV(1F, 1F);
                break;
            default:
                throw new RuntimeException("The UV Info " + Utils.toHexString(this.uvInfo) + " seems to use an invalid offset.");
        }

        return result;
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
    public boolean isSemiTransparent(ISCLevelTableEntry levelTableEntry) {
        GameImage image = getTexture(levelTableEntry);
        if (image != null && (image.testFlag(GameImage.FLAG_TRANSLUCENT) && !getGameInstance().isMediEvil2()))
            return true; // TODO: Investigate changes to images potentially.

        return (this.flags & FLAG_SEMI_TRANSPARENT) == FLAG_SEMI_TRANSPARENT;
    }

    /**
     * Get the type of PSX polygon.
     */
    public PSXPolygonType getPolygonType() {
        if ((this.flags & FLAG_QUAD) == FLAG_QUAD) {
            if (this.textureId != TEXTURE_ID_NO_TEXTURE) {
                return PSXPolygonType.POLY_GT4;
            } else {
                return PSXPolygonType.POLY_G4;
            }
        } else {
            if (this.textureId != TEXTURE_ID_NO_TEXTURE) {
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
                setFlagMask(FLAG_QUAD, false);
                this.textureId = TEXTURE_ID_NO_TEXTURE;
                break;
            case POLY_GT3:
                setFlagMask(FLAG_QUAD, false);
                if (this.textureId == TEXTURE_ID_NO_TEXTURE)
                    this.textureId = TEXTURE_ID_DEFAULT_TEXTURE;
                break;
            case POLY_G4:
                setFlagMask(FLAG_QUAD, true);
                this.textureId = TEXTURE_ID_NO_TEXTURE;
                break;
            case POLY_GT4:
                setFlagMask(FLAG_QUAD, true);
                if (this.textureId == TEXTURE_ID_NO_TEXTURE)
                    this.textureId = TEXTURE_ID_DEFAULT_TEXTURE;
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
    public GameImage getTexture(ISCLevelTableEntry levelTableEntry) {
        if (!getPolygonType().isTextured() || this.textureId == TEXTURE_ID_NO_TEXTURE)
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
            imageSource = getGameInstance().getMainArchive().getImageByTextureId(globalTextureId);

        return imageSource;
    }

    /**
     * Creates a texture shade definition for this polygon.
     * @param mapMesh The map mesh which the polygon is used within.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(SCMapMesh mapMesh, boolean enableGouraudShading) {
        SCMapFile<?> mapFile = mapMesh.getMap();

        SCMapPolygonPacket<?> polygonPacket = mapFile.getPolygonPacket();
        ISCLevelTableEntry levelTableEntry = mapFile.getLevelTableEntry();
        PSXPolygonType polygonType = getPolygonType();
        boolean isSemiTransparent = isSemiTransparent(levelTableEntry);

        SCByteTextureUV[] uvs = null;
        if (polygonType.isTextured() && polygonPacket != null) {
            uvs = new SCByteTextureUV[getVertexCount()];
            for (int i = 0; i < uvs.length; i++)
                uvs[i] = getTextureUv(i);
        }

        // Apply colors.
        if (!polygonType.isGouraud())
            throw new IllegalStateException("MediEvil does not support map polygons with flat shading. (" + polygonType + ")");

        CVector[] colors = new CVector[polygonType.getColorCount()];
        if (enableGouraudShading && polygonPacket != null) {
            for (int i = 0; i < colors.length; i++) {
                SVector vertex = polygonPacket.getVertices().get(this.vertices[i]);
                colors[i] = fromPackedShort(vertex.getPadding(), polygonType, isSemiTransparent);
            }
        } else {
            Arrays.fill(colors, UNSHADED_COLOR);
        }

        // Create definition.
        ITextureSource textureSource = polygonType.isTextured() ? getTexture(levelTableEntry) : null;
        return new PSXShadeTextureDefinition(mapMesh.getShadedTextureManager(), polygonType, textureSource, colors, uvs, isSemiTransparent);
    }

    /**
     * Creates a texture shade definition for this polygon.
     * @param mapFile The map file necessary for looking up texture remap data.
     * @param shadeTexture The shaded texture to load from.
     */
    public void loadDataFromShadeDefinition(SCMapFile<?> mapFile, PSXShadeTextureDefinition shadeTexture, boolean isShadingEnabled) {
        SCMapPolygonPacket<? extends SCGameInstance> polygonPacket = mapFile.getPolygonPacket();
        if (shadeTexture.getPolygonType().getVerticeCount() != getPolygonType().getVerticeCount())
            throw new UnsupportedOperationException("Cannot change between quad/tri polygons."); // This is just not coded yet, we could theoretically add this.

        // Update polygon type.
        PSXPolygonType newPolygonType = shadeTexture.getPolygonType();
        setPolygonType(newPolygonType);

        // Apply texture UVs.
        /*if (newPolygonType.isTextured() && shadeTexture.getTextureUVs() != null) {
            for (int i = 0; i < newPolygonType.getVerticeCount(); i++) {
                SCByteTextureUV modifiedUv = shadeTexture.getTextureUVs()[i];
                if (modifiedUv != null)
                    this.textureUvs[i].copyFrom(modifiedUv);
            }
        }*/ // TODO: We need to come up with a different way of editing UVs which accomodates how the newer games track uvs.

        // Apply colors.
        if (isShadingEnabled && polygonPacket != null && newPolygonType.getColorCount() > 0 && shadeTexture.getColors() != null && newPolygonType.getVerticeCount() == shadeTexture.getColors().length) {
            for (int i = 0; i < shadeTexture.getVerticeCount(); i++) {
                SVector vertex = polygonPacket.getVertices().get(this.vertices[i]);
                vertex.setPadding(toPackedShort(shadeTexture.getColors()[i]));
            }
        }

        // Load texture.
        if (shadeTexture.getTextureSource() instanceof GameImage) {
            GameImage gameImage = (GameImage) shadeTexture.getTextureSource();
            ISCLevelTableEntry levelTableEntry = mapFile.getLevelTableEntry();
            if (levelTableEntry != null) {
                TextureRemapArray remapArray = levelTableEntry.getRemap();
                if (remapArray != null) {
                    int remapIndex = remapArray.getRemapIndex(gameImage.getTextureId());
                    if (remapIndex >= 0)
                        this.textureId = remapIndex;
                }
            }
        }
    }

    /**
     * Process a packed color value into a CVector.
     * The packed color is in the form used by Moon Warrior & MediEvil II.
     * @param packedColor the color to load from.
     * @param polygonType the polygon type to generate the code from.
     * @param isSemiTransparent whether this color is rendered with semi-transparent mode.
     * @return colorVector
     */
    public static CVector fromPackedShort(short packedColor, PSXPolygonType polygonType, boolean isSemiTransparent) {
        // Process padding into color value.
        short red = (short) ((packedColor & 0x1F) << 3);
        short green = (short) (((packedColor >> 5) & 0x1F) << 3);
        short blue = (short) (((packedColor >> 10) & 0x1F) << 3);
        int rgbColor = Utils.toRGB(Utils.unsignedShortToByte(red), Utils.unsignedShortToByte(green), Utils.unsignedShortToByte(blue));

        // Calculate GPU code.
        byte gpuCode = CVector.GP0_COMMAND_POLYGON_PRIMITIVE | CVector.FLAG_GOURAUD_SHADING | CVector.FLAG_MODULATION;
        if (polygonType.isQuad())
            gpuCode |= CVector.FLAG_QUAD;
        if (polygonType.isTextured())
            gpuCode |= CVector.FLAG_TEXTURED;
        if (isSemiTransparent)
            gpuCode |= CVector.FLAG_SEMI_TRANSPARENT;

        // Create color.
        CVector loadedColor = CVector.makeColorFromRGB(rgbColor);
        loadedColor.setCode(gpuCode);
        return loadedColor;
    }

    /**
     * Convert the provided color vector to a packed color value.
     * The packed color is in the form used by Moon Warrior & MediEvil II.
     * @param color the color to pack
     * @return packedColor
     */
    public static short toPackedShort(CVector color) {
        return (short) (((color.getBlueShort() & 0b11111000) << 7) | ((color.getGreenShort() & 0b11111000) << 2)
                | (color.getRedShort() & 0b11111000) >>> 3);
    }
}