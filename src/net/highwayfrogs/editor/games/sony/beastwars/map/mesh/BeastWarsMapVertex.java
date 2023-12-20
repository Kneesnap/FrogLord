package net.highwayfrogs.editor.games.sony.beastwars.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.sony.SCGameObject;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsTexFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.MapTextureInfoEntry;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
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
        if (texFile == null)
            return UnknownTextureSource.CYAN_INSTANCE;

        return texFile.getImages().get(textureInfoEntry.getTextureId());
    }
}