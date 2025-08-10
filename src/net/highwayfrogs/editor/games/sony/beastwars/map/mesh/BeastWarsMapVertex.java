package net.highwayfrogs.editor.games.sony.beastwars.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager;
import net.highwayfrogs.editor.games.psx.shading.PSXTextureShader;
import net.highwayfrogs.editor.games.sony.SCGameObject;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsTexFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.MapTextureInfoEntry;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * This is a representation of a Beast Wars map vertex, and the corresponding tile data.
 * While the real data is stored in separate arrays, it can often be useful to bring that all together into a single coherent concept.
 * Created by Kneesnap on 9/24/2023.
 */
@Getter
public class BeastWarsMapVertex extends SCGameObject<BeastWarsInstance> {
    private final BeastWarsMapFile map;
    private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    private final int gridX;
    private final int gridZ;
    // TODO: Needs some way of accessing the mesh to update stuff when it's time.

    public BeastWarsMapVertex(BeastWarsMapFile map, int gridX, int gridZ) {
        super(map.getGameInstance());
        this.map = map;
        this.gridX = gridX;
        this.gridZ = gridZ;
    }

    /**
     * Test if this vertex is also a valid tile (Meaning it is the owner of a primitive that connects to other vertices)
     * The last row & last column of vertices are not tiles since they don't have any further vertices to form a tile with.
     */
    public boolean hasTile() {
        return !isMaxX() && !isMaxZ();
    }

    /**
     * Tests if this vertex is at the maximum X position the map can hold.
     */
    public boolean isMaxX() {
        return getGridX() >= this.map.getHeightMapXLength() - 1;
    }

    /**
     * Tests if this vertex is at the maximum Z position the map can hold.
     */
    public boolean isMaxZ() {
        return getGridZ() >= this.map.getHeightMapZLength() - 1;
    }

    /**
     * Gets the x position of the vertex in 3D space.
     */
    public float getWorldX() {
        // TODO: Scaling is likely incorrect.
        // TODO: Rotation could be wrong.
        return getGridX() * 16F;
    }

    /**
     * Gets the y position of the vertex in 3D space.
     */
    public float getWorldY() {
        // TODO: The scaling is likely incorrect.
        // TODO: Why do we divide by 16? Not entirely sure, but it seems to scale it right
        return -((this.map.getHeightMap()[getGridZ()][getGridX()] << (this.map.getWorldHeightScale() & 0x1F)) >> 4);
    }

    /**
     * Gets the z position of the vertex in 3D space.
     */
    public float getWorldZ() {
        // TODO: Scaling is likely incorrect.
        // TODO: Rotation could be wrong.
        return getGridZ() * 16F;
    }

    /**
     * Gets the index to the texture entry info currently assigned to this index.
     */
    public short getTextureInfoEntryIndex() {
        return getMap().getTileMap()[getGridZ()][getGridX()];
    }

    /**
     * Gets the texture entry currently assigned to this index.
     */
    public MapTextureInfoEntry getTextureInfoEntry() {
        short textureInfoEntryIndex = getTextureInfoEntryIndex();
        if (textureInfoEntryIndex == 0xFF)
            return null; // No texture!

        return getMap().getTextureInfoEntries()[textureInfoEntryIndex];
    }

    /**
     * Sets the texture info entry index assigned to this vertex.
     * @param newTextureEntryInfoIndex The new texture info entry index.
     */
    public void setTextureInfoEntryIndex(short newTextureEntryInfoIndex) {
        if (newTextureEntryInfoIndex < 0 || newTextureEntryInfoIndex > 255)
            throw new IllegalArgumentException("The new texture info entry index was not in the range [0, 255]. (Value: " + newTextureEntryInfoIndex + ")");

        this.map.getTileMap()[getGridX()][getGridX()] = newTextureEntryInfoIndex;

        // Update mesh.
        // TODO: Update mesh

        // Update shading.
        // TODO: Update texture map shading if enabled (and this feature gets implemented).
    }

    /**
     * Set the texture applied to this tile.
     * @param entry The entry to update.
     */
    public void setTextureInfoEntry(MapTextureInfoEntry entry) {
        int textureIndex = Utils.indexOf(this.map.getTextureInfoEntries(), entry);
        if (textureIndex == -1 && entry != null)
            throw new IllegalArgumentException("The provided texture entry (" + entry + ") is not registered in the map file, and therefore cannot be applied to the ");

        // Set texture.
        short rawTextureIndex = (entry != null) ? (short) textureIndex : 0xFF;
        setTextureInfoEntryIndex(rawTextureIndex);
    }

    /**
     * Gets the raw height value between [0, 255] representing how tall the vertex is.
     */
    public short getHeight() {
        return this.map.getHeightMap()[getGridZ()][getGridX()];
    }

    /**
     * Sets the raw height value between [0, 255] representing how tall the vertex is.
     */
    public void setHeight(short newHeight) {
        if (newHeight < 0 || newHeight > 255)
            throw new IllegalArgumentException("The new height value was not in the range [0, 255]. (Value: " + newHeight + ")");

        this.map.getHeightMap()[getGridZ()][getGridX()] = newHeight;

        // Update height.
        // TODO: UPDATE VERTEX HEIGHT IN MESH.
    }

    /**
     * Gets the raw shading color value between [0, 65535] representing vertex color shading.
     */
    public short getColor() {
        return this.map.getFaceColors()[getGridZ()][getGridX()];
    }

    /**
     * Gets shading color as a {@code CVector}.
     */
    public CVector getColorVector() {
        return fromPackedShort(getColor());
    }

    /**
     * Sets the raw shading color value between [0, 65535] representing vertex shading color.
     */
    public void setColor(short newColor) {
        this.map.getFaceColors()[getGridZ()][getGridX()] = newColor;

        // Update shading.
        // TODO: Update texture map shading if enabled (and this feature gets implemented).
    }

    /**
     * Swap the data at this vertex with the data at the other provided vertex.
     * @param otherVertex The other vertex.
     */
    public void swapDataWith(BeastWarsMapVertex otherVertex) {
        if (otherVertex == null)
            throw new IllegalArgumentException("The 'otherVertex' value cannot be null!");

        // Copy data from other tile.
        short copyHeight = otherVertex.getHeight();
        short copyTextureEntryIndex = otherVertex.getTextureInfoEntryIndex();
        short copyColor = otherVertex.getColor();

        // Assign data from this tile to the other tile.
        otherVertex.setHeight(getHeight());
        otherVertex.setTextureInfoEntryIndex(getTextureInfoEntryIndex());
        otherVertex.setColor(getColor());

        // Assign copied data from the other tile to this tile.
        this.setHeight(copyHeight);
        this.setTextureInfoEntryIndex(copyTextureEntryIndex);
        this.setColor(copyColor);
    }

    /**
     * Gets the texture source for this specific tile.
     */
    public ITextureSource getTextureSource() {
        MapTextureInfoEntry textureInfoEntry = getTextureInfoEntry();
        if (textureInfoEntry == null)
            return UnknownTextureSource.MAGENTA_INSTANCE;

        if (!textureInfoEntry.isActive())
            return UnknownTextureSource.CYAN_INSTANCE;

        BeastWarsTexFile texFile = this.map.getTextureFile();
        if (texFile == null || textureInfoEntry.getTextureId() >= texFile.getImages().size())
            return BeastWarsMapMesh.PINK_COLOR;

        return texFile.getImages().get(textureInfoEntry.getTextureId());
    }

    private static final SCByteTextureUV[] CONSTANT_UVS = {
            new SCByteTextureUV((byte) 1, (byte) 254), // 0F, 1F, uvBottomLeft
            new SCByteTextureUV((byte) 254, (byte) 254), // 1F, 1F, uvBottomRight
            new SCByteTextureUV((byte) 1, (byte) 1), // 0F, 0F, uvTopLeft
            new SCByteTextureUV((byte) 254, (byte) 1), // 1F, 0F, uvTopRight
    };

    /**
     * Creates a PSX shade definition for the polygon.
     * @param mesh the mesh to create the shade definition for
     * @param shadingEnabled if shading is enabled
     * @return shadeDefinition
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(BeastWarsMapMesh mesh, boolean shadingEnabled) {
        // Determine the texture.
        ITextureSource textureSource = getTextureSource();

        // Clone colors.
        CVector[] colors = new CVector[4];
        if (shadingEnabled) {
            // This may appear to be flipped from the UVs on the Y axis, and it is-- that's because the the top corner is wrong.
            colors[0] = getColorVector();
            colors[1] = getMap().getVertex(this.gridX + 1, this.gridZ).getColorVector();
            colors[2] = getMap().getVertex(this.gridX, this.gridZ + 1).getColorVector();
            colors[3] = getMap().getVertex(this.gridX + 1, this.gridZ + 1).getColorVector();
        } else {
            Arrays.fill(colors, PSXTextureShader.UNSHADED_COLOR);
        }

        PSXShadedTextureManager<BeastWarsMapVertex> shadedTextureManager = mesh != null ? mesh.getShadedTextureManager() : null;
        return new PSXShadeTextureDefinition(shadedTextureManager, PSXPolygonType.POLY_GT4, textureSource, colors, CONSTANT_UVS, false, true);
    }

    @Override
    public int hashCode() {
        return this.map.hashCode() ^ this.gridX ^ (this.gridZ << 16);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BeastWarsMapVertex))
            return false;

        BeastWarsMapVertex vertex = (BeastWarsMapVertex) obj;
        return vertex.map == this.map && vertex.gridX == this.gridX && vertex.gridZ == this.gridZ;
    }

    /**
     * Loads a CVector color from the given 16-bit short encoded in Beast Wars map vertex color format.
     * @param packedColor the packed color in Beast Wars formatt
     * @return color
     */
    public static CVector fromPackedShort(short packedColor) {
        // Process padding into color value.
        short blue = (short) ((packedColor & 0x1F) << 2);
        short green = (short) (((packedColor >> 5) & 0x1F) << 2);
        short red = (short) (((packedColor >> 10) & 0x1F) << 2);
        int rgbColor = ColorUtils.toRGB(DataUtils.unsignedShortToByte(red), DataUtils.unsignedShortToByte(green), DataUtils.unsignedShortToByte(blue));

        // Calculate GPU code.
        byte gpuCode = CVector.GP0_COMMAND_POLYGON_PRIMITIVE | CVector.FLAG_GOURAUD_SHADING | CVector.FLAG_QUAD | CVector.FLAG_TEXTURED | CVector.FLAG_MODULATION;

        // Create color.
        CVector loadedColor = CVector.makeColorFromRGB(rgbColor);
        loadedColor.setCode(gpuCode);
        return loadedColor;
    }

    /**
     * Saves the color to a packed 16-bit short in Beast Wars map vertex color format.
     * @param color the color to pack
     * @return packedShort
     */
    public static short toPackedShort(CVector color) {
        return (short) (((color.getRedShort() & 0b01111100) << 8) | ((color.getGreenShort() & 0b01111100) << 3)
                | (color.getBlueShort() & 0b01111100) >>> 2);
    }
}