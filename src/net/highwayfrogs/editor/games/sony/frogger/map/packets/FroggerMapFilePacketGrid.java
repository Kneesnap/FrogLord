package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a Frogger map's collision grid.
 * Created by Kneesnap on 5/25/2024.
 */
public class FroggerMapFilePacketGrid extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "GRID";
    @Getter private short gridXCount; // Number of grid squares in x. Modify via resizeGrid().
    @Getter private short gridZCount; // Number of grid squares in z. Modify via resizeGrid().
    @Getter private short gridXSize = (short) 256; // x length of single square. Seems to always be 256. This is actually fixed point 8.8 aka 1.0F. Modifying this may lead to weird results.
    @Getter private short gridZSize = (short) 256; // z length of single square. Seems to always be 256. This is actually fixed point 8.8, aka 1.0F. Modifying this may lead to weird results.
    private FroggerGridStack[][] gridStacks;


    public FroggerMapFilePacketGrid(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.gridXCount = reader.readShort();
        this.gridZCount = reader.readShort();
        this.gridXSize = reader.readShort();
        this.gridZSize = reader.readShort();
        this.gridStacks = new FroggerGridStack[this.gridZCount][this.gridXCount];

        // Prevent further loading.
        if (getParentFile().isExtremelyEarlyMapFormat() && !getParentFile().isIsland() && !getParentFile().isQB()) {
            // ISLAND.MAP & QB.MAP have valid grids.
            // I think the difference is it's just GridSquares in this old version, without any GridStack.

            // Skip ahead.
            if (getMapConfig().isMapAnimationSupported()) {
                if (getParentFile().getGraphicalPacket().getAnimationPacketAddress() > reader.getIndex())
                    reader.setIndex(getParentFile().getGraphicalPacket().getAnimationPacketAddress());
            } else {
                reader.skipBytes(reader.getRemaining());
            }

            return;
        }

        // Load the grid stacks.
        int gridSquareIndex = 0;
        for (int z = 0; z < this.gridZCount; z++) {
            for (int x = 0; x < this.gridXCount; x++) {
                FroggerGridStack stack = new FroggerGridStack(getParentFile(), x, z);
                gridSquareIndex = stack.load(reader, gridSquareIndex);
                this.gridStacks[z][x] = stack;
            }
        }

        // Load the grid squares.
        for (int z = 0; z < this.gridZCount; z++)
            for (int x = 0; x < this.gridXCount; x++)
                this.gridStacks[z][x].loadGridSquares(reader);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeShort(this.gridXCount);
        writer.writeShort(this.gridZCount);
        writer.writeShort(this.gridXSize);
        writer.writeShort(this.gridZSize);

        // Save the grid stacks.
        int gridSquareIndex = 0;
        for (int z = 0; z < this.gridZCount; z++)
            for (int x = 0; x < this.gridXCount; x++)
                gridSquareIndex = this.gridStacks[z][x].save(writer, gridSquareIndex);

        // Save the grid squares.
        for (int z = 0; z < this.gridZCount; z++)
            for (int x = 0; x < this.gridXCount; x++)
                this.gridStacks[z][x].saveGridSquares(writer);
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getGraphicalPacket().getGridPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Collision Grid Dimensions", this.gridXCount + " x " + this.gridZCount + " (" + (this.gridXCount * this.gridZCount) + " groups)");
        propertyList.add("Collision Grid Square Size", getGridXSizeAsFloat() + " x " + getGridZSizeAsFloat());

        // Grid Square Count
        if (this.gridStacks != null) {
            int squareCount = 0;
            // Save the grid stacks.
            for (int z = 0; z < this.gridZCount; z++) {
                for (int x = 0; x < this.gridXCount; x++) {
                    FroggerGridStack gridStack = this.gridStacks[z][x];
                    if (gridStack != null)
                        squareCount += gridStack.getGridSquares().size();
                }
            }

            propertyList.add("Grid Square Count", squareCount);
        }

        return propertyList;
    }

    /**
     * Gets the grid square/rectangle X size as a floating point number.
     */
    public float getGridXSizeAsFloat() {
        return Utils.fixedPointIntToFloatNBits(this.gridXSize, 8);
    }

    /**
     * Gets the grid square/rectangle Z size as a floating point number.
     */
    public float getGridZSizeAsFloat() {
        return Utils.fixedPointIntToFloatNBits(this.gridZSize, 8);
    }

    /**
     * Gets the x world origin position for the collision grid.
     * Fixed point 4 bits.
     * TODO: Is this based on the group position? If so, I believe this implies the grid size is automatically calculated? Seems odd.
     * @return baseGridX
     */
    public short getBaseGridX() {
        return (short) (-(this.gridXSize * this.gridXCount) >> 1);
    }

    /**
     * Gets the z world origin position for the collision grid
     * Fixed point 4 bits.
     * @return baseGridZ
     */
    public short getBaseGridZ() {
        return (short) (-(this.gridZSize * this.gridZCount) >> 1);
    }

    /**
     * Gets the grid X value from a world X value.
     * @param worldX The world X coordinate.
     * @return gridX
     */
    public int getGridXFromWorldX(int worldX) {
        return (worldX - getBaseGridX()) >> 8;
    }

    /**
     * Gets the grid Z value from a world Z value.
     * @param worldZ The world Z coordinate.
     * @return gridZ
     */
    public int getGridZFromWorldZ(int worldZ) {
        return (worldZ - getBaseGridZ()) >> 8;
    }

    /**
     * Turn a grid x value into a world x value.
     * @param gridX The grid x value to convert.
     * @return worldX
     */
    public int getWorldXFromGridX(int gridX, boolean useMiddle) {
        return getBaseGridX() + (gridX << 8) + (useMiddle ? 0x80 : 0);
    }

    /**
     * Turn a grid z value into a world z value.
     * @param gridZ The grid z value to convert.
     * @return worldZ
     */
    public int getWorldZFromGridZ(int gridZ, boolean useMiddle) {
        return getBaseGridZ() + (gridZ << 8) + (useMiddle ? 0x80 : 0);
    }

    /**
     * Gets a grid stack from grid coordinates.
     * @param gridX The grid x coordinate.
     * @param gridZ The grid z coordinate.
     * @return gridStack
     */
    public FroggerGridStack getGridStack(int gridX, int gridZ) {
        if (gridX < 0 || gridX >= this.gridXCount)
            throw new ArrayIndexOutOfBoundsException("Invalid grid stack X coordinate: " + gridX);
        if (gridZ < 0 || gridZ >= this.gridZCount)
            throw new ArrayIndexOutOfBoundsException("Invalid grid stack Z coordinate: " + gridZ);

        return this.gridStacks[gridZ][gridX];
    }

    /**
     * Resizes the grid to a new size.
     * @param xSize The new x count of the grid.
     * @param zSize The new z count of the grid.
     */
    public void resizeGrid(int xSize, int zSize) {
        if (xSize == this.gridXCount && zSize == this.gridZCount)
            return; // Same size.

        // Populate new grid.
        FroggerGridStack[][] newGridStacks = new FroggerGridStack[zSize][xSize];
        for (int z = 0; z < zSize; z++) {
            for (int x = 0; x < xSize; x++) {
                if (x >= this.gridXCount || z >= this.gridZCount) {
                    newGridStacks[z][x] = new FroggerGridStack(getParentFile(), x, z);
                } else {
                    newGridStacks[z][x] = this.gridStacks[z][x];
                }
            }
        }

        // Apply the new grid.
        this.gridXCount = (short) xSize;
        this.gridZCount = (short) zSize;
        this.gridStacks = newGridStacks;
    }
}