package net.highwayfrogs.editor.games.sony.frogger.map.mesh;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager;
import net.highwayfrogs.editor.games.psx.shading.PSXTextureShader;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.Arrays;

/**
 * Represents a map polygon seen in Frogger.
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
 * Created by Kneesnap on 5/26/2024.
 */
@Getter
public class FroggerMapPolygon extends SCGameData<FroggerGameInstance> {
    private final FroggerMapFile mapFile;
    private FroggerMapPolygonType polygonType;
    private final int[] vertices;
    private CVector[] colors;
    private short flags;
    @Setter private short textureId = -1;
    private SCByteTextureUV[] textureUvs;

    // The last address which the polygon was written to.
    private transient int lastReadAddress = -1;
    private transient int lastWriteAddress = -1;
    @Setter private transient boolean visible; // Whether the polygon should be made visible in-game or not.

    public static final int FLAG_SEMI_TRANSPARENT = Constants.BIT_FLAG_0;
    public static final int FLAG_ENVIRONMENT_MAPPED = Constants.BIT_FLAG_1; // Show the solid environment bitmap. (For instance, how water appears as a solid body, or sludge in the sewer levels.)
    public static final int FLAG_MAX_ORDER_TABLE = Constants.BIT_FLAG_2; // Puts at the back of the order table. Either the very lowest rendering priority, or the very highest.
    public static final int FLAG_VALIDATION_MASK = 0b111;

    private static final SCByteTextureUV[] EMPTY_UV_ARRAY = new SCByteTextureUV[0];
    private static final CVector[] EMPTY_COLOR_ARRAY = new CVector[0];
    private static final CVector[] UNSHADED_SINGLE_COLOR_ARRAY = new CVector[] {PSXTextureShader.UNSHADED_COLOR};

    public FroggerMapPolygon(FroggerMapFile mapFile, FroggerMapPolygonType polygonType) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
        this.polygonType = polygonType;
        this.vertices = new int[polygonType.getVertexCount()];
        this.colors = polygonType.getColorCount() > 0 ? new CVector[polygonType.getColorCount()] : EMPTY_COLOR_ARRAY;
        this.textureUvs = polygonType.isTextured() ? new SCByteTextureUV[polygonType.getVertexCount()] : EMPTY_UV_ARRAY;
        Arrays.fill(this.vertices, -1);
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i] = new CVector();
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i] = new SCByteTextureUV();
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapPolygon::getLoggerInfo, this);
    }

    /**
     * Gets logger information for the polygon.
     */
    public String getLoggerInfo() {
        return (this.mapFile != null ? this.mapFile.getFileDisplayName() + "|" : "") + "FroggerMapPolygon{" + this.polygonType + "," + NumberUtils.toHexString(this.lastReadAddress) + "}";
    }

    @Override
    public void load(DataReader reader) {
        this.lastReadAddress = reader.getIndex();

        // Read vertices:
        for (int i = 0; i < this.vertices.length; i++)
            this.vertices[i] = reader.readUnsignedShortAsInt();
        reader.align(Constants.INTEGER_SIZE); // Padding. The fourth vertex is a duplicate usually, but in some cases such as a polygon from a baked entity model seems to let this value be garbage.

        // Read texture data. (if new format)
        boolean oldPolygonFormat = this.mapFile.getMapConfig().isOldMapTexturedPolyFormat();
        if (!oldPolygonFormat && this.polygonType.isTextured())
            readFinalTextureFormat(reader);

        // Read colors.
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].load(reader);

        // Read texture data. (if old format)
        if (oldPolygonFormat && this.polygonType.isTextured())
            readPrototypeTextureFormat(reader);

        // Validate size.
        int readByteCount = reader.getIndex() - this.lastReadAddress;
        int expectedByteCount = this.mapFile.getMapConfig().isOldMapTexturedPolyFormat() ? this.polygonType.getEarlyFormatSizeInBytes() : this.polygonType.getSizeInBytes();
        if (readByteCount != expectedByteCount)
            throw new RuntimeException("Read a " + this.polygonType.name() + " polygon in " + readByteCount + " bytes, but it was expected to be " + expectedByteCount + " bytes.");
    }

    private void readPrototypeTextureFormat(DataReader reader) {
        this.textureId = reader.readShort();
        this.flags = reader.readShort();
        warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);

        this.textureUvs[0].load(reader);
        this.textureUvs[1].load(reader);
        if (this.textureUvs.length == 3) {
            this.textureUvs[2].load(reader);
            reader.skipBytes(Constants.SHORT_SIZE); // Padding. (THIS IS NOT EMPTY. But I also couldn't figure out what it was. Given it's for such an early version and likely unused, I'm fine not supporting it.)
        } else if (this.textureUvs.length == 4) {
            this.textureUvs[2].load(reader);
            this.textureUvs[3].load(reader);
        } else {
            throw new RuntimeException("Cannot handle " + this.textureUvs.length + " uvs.");
        }
    }

    private void readFinalTextureFormat(DataReader reader) {
        this.flags = reader.readShort();
        warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
        this.textureUvs[0].load(reader);
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Empty Clut ID.
        this.textureUvs[1].load(reader);
        this.textureId = reader.readShort();
        this.textureUvs[2].load(reader);
        if (this.textureUvs.length > 3) {
            this.textureUvs[3].load(reader);
        } else {
            byte u = reader.readByte();
            byte v = reader.readByte();

            SCByteTextureUV copyUv = this.textureUvs[2];
            if ((this.polygonType == FroggerMapPolygonType.GT3) ? (u != 0 || v != 0) : (u != copyUv.getU() || v != copyUv.getV()))
                getLogger().warning(String.format("UV3 was <u=%02X,v=%02x>, but we expected %s.", u, v, copyUv));
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.lastWriteAddress = writer.getIndex();

        // Write Vertices:
        for (int i = 0; i < getVertexCount(); i++)
            writer.writeUnsignedShort(this.vertices[i]);

        // Write padding vertex.
        if (this.vertices.length % 2 > 0)
            writer.writeUnsignedShort(this.vertices[this.vertices.length - 1]);

        // Write texture data. (if new format)
        boolean oldPolygonFormat = this.mapFile.getMapConfig().isOldMapTexturedPolyFormat();
        if (!oldPolygonFormat && this.polygonType.isTextured())
            writeFinalTextureFormat(writer);

        // Write colors.
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].save(writer);

        // Write texture data. (if old format)
        if (oldPolygonFormat && this.polygonType.isTextured())
            writePrototypeTextureFormat(writer);

        // Validate size.
        int writtenByteCount = writer.getIndex() - this.lastWriteAddress;
        int expectedByteCount = oldPolygonFormat ? this.polygonType.getEarlyFormatSizeInBytes() : this.polygonType.getSizeInBytes();
        if (writtenByteCount != expectedByteCount)
            throw new RuntimeException("Wrote a " + this.polygonType.name() + " polygon to " + writtenByteCount + " bytes, but it was expected to be " + expectedByteCount + " bytes.");
    }

    private void writePrototypeTextureFormat(DataWriter writer) {
        writer.writeShort(this.textureId);
        writer.writeShort(this.flags);
        this.textureUvs[0].save(writer);
        this.textureUvs[1].save(writer);

        if (this.textureUvs.length == 3) {
            this.textureUvs[2].save(writer);
            writer.writeNull(Constants.SHORT_SIZE); // Padding. (THIS IS NOT EMPTY. But I also couldn't figure out what it was. Given it's for such an early version and likely unused, I'm fine not supporting it.)
        } else if (this.textureUvs.length == 4) {
            this.textureUvs[2].save(writer);
            this.textureUvs[3].save(writer);
        } else {
            throw new RuntimeException("Cannot handle " + this.textureUvs.length + " uvs.");
        }
    }

    private void writeFinalTextureFormat(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
        this.textureUvs[0].save(writer);
        writer.writeNull(Constants.SHORT_SIZE); // Empty Clut ID.
        this.textureUvs[1].save(writer);
        writer.writeShort(this.textureId);
        this.textureUvs[2].save(writer);
        if (this.textureUvs.length > 3) {
            this.textureUvs[3].save(writer);
        } else if (this.polygonType == FroggerMapPolygonType.GT3) {
            // For GT3, it seems to always get this value as zero.
            writer.writeNull(Constants.SHORT_SIZE); // Empty UV.
        } else {
            // For each polygon except GT3, it seems to duplicate the last vertex.
            this.textureUvs[this.textureUvs.length - 1].save(writer);
        }
    }

    /**
     * Test if a bit flag mask is present.
     * @param flagMask The flag mask in question.
     * @return flagPresent
     */
    public boolean testFlag(int flagMask) {
        return (this.flags & flagMask) == flagMask;
    }

    /**
     * Set a flag state.
     * @param flagMask The flag mask to set.
     * @param newState The new flag state.
     */
    public void setFlagMask(int flagMask, boolean newState) {
        if (newState) {
            this.flags |= (short) flagMask;
        } else {
            this.flags &= (short) ~flagMask;
        }
    }

    /**
     * Gets the active texture on this polygon.
     */
    public GameImage getTexture() {
        if (!this.polygonType.isTextured() && (this.textureId == 0 || this.textureId == -1))
            return null;

        TextureRemapArray textureRemap = getMapFile().getTextureRemap();
        if (textureRemap == null)
            return null;

        Short globalTextureId = textureRemap.getRemappedTextureId(this.textureId);
        if (globalTextureId == null)
            return null;

        VLOArchive vloArchive = this.mapFile != null ? this.mapFile.getVloFile() : null;
        if (vloArchive != null) {
            GameImage gameImage = vloArchive.getImageByTextureId(globalTextureId, false);
            if (gameImage != null)
                return gameImage;
        }

        // If all else fails, resolve the texture ID from any VLO we can find it in.
        return getGameInstance().getMainArchive().getImageByTextureId(globalTextureId);
    }

    /**
     * Get the number of vertices active for this polygon.
     */
    public int getVertexCount() {
        return this.polygonType.getVertexCount();
    }

    /**
     * Test if the image is semi-transparent.
     */
    public boolean isSemiTransparent() {
        // The texture transparency flag is not used here.
        return testFlag(FLAG_SEMI_TRANSPARENT);
    }

    /**
     * Tests if this polygon is fully opaque, all pixels having maximum alpha/opacity.
     */
    public boolean isFullyOpaque() {
        GameImage image = getTexture();
        if (image != null && image.testFlag(GameImage.FLAG_BLACK_IS_TRANSPARENT))
            return false;

        return !isSemiTransparent();
    }

    /**
     * Gets the UV horizontal offset applied from map animation.
     * @param animation the animation to apply the offset from
     * @param frame the animation frame to apply
     * @return uOffset
     */
    public float getOffsetU(FroggerMapAnimation animation, int frame) {
        if (animation == null)
            return 0F;

        int xOffset = animation.getOffsetX(frame);
        GameImage image = animation.getTextureAtFrame(frame);
        if (image == null)
            image = getTexture();
        if (image == null)
            return 0; // Failed to resolve image.

        // The UV offset is counted in pixels, not uv units.
        // So we convert them to uv units.
        return xOffset * (1F / image.getIngameWidth());
    }

    /**
     * Gets the UV vertical offset applied from map animation.
     * @param animation the animation to apply the offset from
     * @param frame the animation frame to apply
     * @return vOffset
     */
    public float getOffsetV(FroggerMapAnimation animation, int frame) {
        if (animation == null)
            return 0F;

        int yOffset = animation.getOffsetY(frame);
        GameImage image = animation.getTextureAtFrame(frame);
        if (image == null)
            image = getTexture();
        if (image == null)
            return 0; // Failed to resolve image.

        // The UV offset is counted in pixels, not uv units.
        // So we convert them to uv units.
        return yOffset * (1F / image.getIngameHeight());
    }

    /**
     * Creates a texture shade definition for this polygon.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(FroggerMapMesh mesh, boolean shadingEnabled, FroggerMapAnimation animation, int frame) {
        SCByteTextureUV[] uvs = null;
        if (this.polygonType.isTextured()) {
            float xOffset = getOffsetU(animation, frame);
            float yOffset = getOffsetV(animation, frame);
            uvs = new SCByteTextureUV[this.textureUvs.length];
            for (int i = 0; i < uvs.length; i++) {
                uvs[i] = this.textureUvs[i].clone();
                uvs[i].add(xOffset, yOffset);
            }
        }

        // Determine the texture.
        ITextureSource textureSource = getTexture();
        if (animation != null) {
            GameImage animatedImage = animation.getTextureAtFrame(frame);
            if (animatedImage != null)
                textureSource = animatedImage;
        }

        // Clone colors.
        CVector[] colors = null;
        if (this.colors.length > 0) {
            colors = new CVector[this.colors.length];
            for (int i = 0; i < colors.length; i++)
                colors[i] = shadingEnabled || !this.polygonType.isTextured() ? this.colors[i].clone() : PSXTextureShader.UNSHADED_COLOR;
        }

        PSXShadedTextureManager<FroggerMapPolygon> shadedTextureManager = mesh != null ? mesh.getShadedTextureManager() : null;
        return new PSXShadeTextureDefinition(shadedTextureManager, this.polygonType.getInternalType(), textureSource, colors, uvs, isSemiTransparent(), true);
    }

    /**
     * Loads data from a texture shade definition for this polygon.
     * @param shadeTexture The shaded texture to load from.
     */
    public void loadDataFromShadeDefinition(PSXShadeTextureDefinition shadeTexture, boolean shadingEnabled) {
        if (shadeTexture.getPolygonType().getVerticeCount() != this.polygonType.getVertexCount())
            throw new UnsupportedOperationException("Cannot change between quad/tri polygons."); // This is just not coded yet, we could theoretically add this.

        // Update polygon type.
        this.polygonType = FroggerMapPolygonType.getByInternalType(shadeTexture.getPolygonType());

        // Clone texture UVs.
        if (this.polygonType.isTextured() && shadeTexture.getTextureUVs() != null) {
            if (this.textureUvs.length != shadeTexture.getTextureUVs().length)
                this.textureUvs = Arrays.copyOf(this.textureUvs, this.polygonType.getVertexCount());

            for (int i = 0; i < this.textureUvs.length; i++) {
                SCByteTextureUV modifiedUv = shadeTexture.getTextureUVs()[i];

                if (this.textureUvs[i] != null) {
                    this.textureUvs[i].copyFrom(modifiedUv);
                } else {
                    this.textureUvs[i] = modifiedUv.clone();
                }
            }
        } else {
            this.textureUvs = EMPTY_UV_ARRAY;
        }

        // Clone colors.
        if (shadingEnabled) {
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
        }

        // Load texture.
        if (shadeTexture.getTextureSource() instanceof GameImage) {
            GameImage gameImage = (GameImage) shadeTexture.getTextureSource();
            TextureRemapArray textureRemap = this.mapFile.getTextureRemap();
            if (textureRemap != null) {
                int remapIndex = textureRemap.getRemapIndex(gameImage.getTextureId());
                if (remapIndex >= 0)
                    this.textureId = DataUtils.unsignedIntToShort(remapIndex);
            }
        }
    }
}