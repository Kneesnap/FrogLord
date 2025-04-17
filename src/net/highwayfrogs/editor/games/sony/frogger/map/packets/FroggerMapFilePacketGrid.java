package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareReaction;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataUtils;

import java.util.*;

/**
 * Represents a Frogger map's collision grid.
 * Created by Kneesnap on 5/25/2024.
 */
public class FroggerMapFilePacketGrid extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "GRID";
    @Getter private short gridXCount; // Number of grid squares in x. Modify via resizeGrid().
    @Getter private short gridZCount; // Number of grid squares in z. Modify via resizeGrid().
    @Getter private short gridXSize = (short) 256; // x length of single square. Seems to always be 256. This is actually fixed point 4.12, aka 16.0F. Modifying this may lead to weird results by FrogLord, but not the game.
    @Getter private short gridZSize = (short) 256; // z length of single square. Seems to always be 256. This is actually fixed point 4.12, aka 16.0F. Modifying this may lead to weird results by FrogLord, but not the game.
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

        // Warn if an unrecognized grid flag combination is found.
        for (int z = 0; z < this.gridZCount; z++) {
            for (int x = 0; x < this.gridXCount; x++) {
                FroggerGridStack gridStack = this.gridStacks[z][x];
                for (int i = 0; i < gridStack.getGridSquares().size(); i++) {
                    FroggerGridSquare gridSquare = gridStack.getGridSquares().get(i);
                    FroggerGridSquareReaction reaction = gridSquare.getReaction();
                    if (reaction == null) {
                        StringBuilder builder = new StringBuilder();
                        for (FroggerGridSquareFlag flag : FroggerGridSquareFlag.values()) {
                            if (!flag.isLandGridData() || !flag.isPartOfSimpleReaction() || !gridSquare.testFlag(flag))
                                continue;

                            if (builder.length() > 0)
                                builder.append(", ");

                            builder.append(flag.name());
                        }

                        gridSquare.getLogger().warning("Found a GridSquare which had no corresponding FroggerGridSquareReaction! [%s]", builder);
                    }
                }
            }
        }

        // Validate grid stack heights look okay.
        if (doGridStacksHaveCliffHeights()) {
            // Due to floating point precision loss (game uses 16-bit fixed point, and mappy used 32-bit floats), we can't actually use a single threshold.
            // This is because when vertices were rounded to their nearest 16-bit fixed point representation, many vertices also crossed the threshold of "not part of the stack" to "part of the stack" or the other way around.
            // In other words, due to imprecision, the real threshold may be off by +1 or -1 on a per-vertex basis.
            // For performance & conceptual complexity reasons, we don't actually do a full per-vertex calculation.
            // Instead, we just use the same threshold for all vertices, and try the different thresholds, as this works in most cases.
            Map<FroggerGridStack, List<SVector>> lessVertices = calculateVertexSquareMapping(FroggerGridStack.NEARBY_VERTEX_THRESHOLD - 1);
            Map<FroggerGridStack, List<SVector>> usualVertices = calculateVertexSquareMapping(FroggerGridStack.NEARBY_VERTEX_THRESHOLD);
            Map<FroggerGridStack, List<SVector>> moreVertices = calculateVertexSquareMapping(FroggerGridStack.NEARBY_VERTEX_THRESHOLD + 1);

            // The goal is to find grid stacks where our cliff height algorithm recreation isn't accurate, so a warning can be shown.
            int nonMatchingStacks = 0;
            for (int z = 0; z < this.gridStacks.length; z++) {
                for (int x = 0; x < this.gridStacks[z].length; x++) {
                    FroggerGridStack gridStack = this.gridStacks[z][x];
                    if ((FroggerGridStack.calculateRawCliffHeightValue(usualVertices.get(gridStack)) != gridStack.getRawCliffHeightValue())
                            && (FroggerGridStack.calculateRawCliffHeightValue(lessVertices.get(gridStack)) != gridStack.getRawCliffHeightValue())
                            && (FroggerGridStack.calculateRawCliffHeightValue(moreVertices.get(gridStack)) != gridStack.getRawCliffHeightValue()))
                        nonMatchingStacks++;
                }
            }

            // In build 30, the maximum seen is 13 in VOL2.MAP, with the only other being 2 in VOL3.MAP.
            // In build 71, the maximum seen is 12 in VOL2.MAP, followed by 2 in a few other maps.
            if (nonMatchingStacks >= 20)
                getLogger().warning("Found %d grid stacks with unmatched cliff heights.", nonMatchingStacks);
        }
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
     * Clears the contents of all grid squares.
     */
    public void clear() {
        for (int z = 0; z < this.gridZCount; z++)
            for (int x = 0; x < this.gridXCount; x++)
                this.gridStacks[z][x].clear();
    }

    /**
     * Gets the grid square/rectangle X size as a floating point number.
     */
    public float getGridXSizeAsFloat() {
        return DataUtils.fixedPointIntToFloat4Bit(this.gridXSize);
    }

    /**
     * Gets the grid square/rectangle Z size as a floating point number.
     */
    public float getGridZSizeAsFloat() {
        return DataUtils.fixedPointIntToFloat4Bit(this.gridZSize);
    }

    /**
     * Gets the x world origin position for the collision grid.
     * Fixed point 4 bits.
     * TODO: Is this based on the group position? If so, I believe this implies the grid size is automatically calculated? Seems odd. Actually, it could be the other way around that the group size is calculated based on the grid.
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
     * Gets a grid stack from grid coordinates, or throws an error if the desired grid stack is out of bounds.
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
     * Resize the grid to a new size.
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

    private static final List<String> BUILD_31_MAPS_WITHOUT_CLIFF_HEIGHTS = Arrays.asList("DES2.MAP", "DES5.MAP", "FOR1.MAP",
            "FOR2.MAP", "JUN1.MAP", "SWP4.MAP", "SWP5.MAP", "SKY3.MAP", "SUB1.MAP", "SUB4.MAP", "SUB5.MAP");

    /**
     * Returns true iff grid stacks should have cliff heights present.
     */
    public boolean doGridStacksHaveCliffHeights() {
        FroggerConfig config = getParentFile().getConfig();
        // Cliff heights were added in Build 30, but not all maps were exported with them until build 32.
        if (config.getBuild() == 31 && BUILD_31_MAPS_WITHOUT_CLIFF_HEIGHTS.contains(getParentFile().getFileDisplayName())) {
            return false;
        } else if (config.getBuild() == 30 && "JUN2.MAP".equals(getParentFile().getFileDisplayName())) {
            return false;
        } else if (config.isAtOrBeforeBuild29()) {
            return false;
        }

        // ISLAND.MAP and QB.MAP were never updated and thus never received the heights, but all other maps have them.
        return !getParentFile().isQB() && !getParentFile().isIsland();
    }

    /**
     * Recalculates the cliff heights for all grid stacks.
     */
    public void recalculateAllCliffHeights() {
        if (!doGridStacksHaveCliffHeights())
            return;

        Map<FroggerGridStack, List<SVector>> verticesPerStack = calculateVertexSquareMapping(FroggerGridStack.NEARBY_VERTEX_THRESHOLD);
        for (int z = 0; z < this.gridStacks.length; z++) {
            for (int x = 0; x < this.gridStacks[z].length; x++) {
                FroggerGridStack gridStack = this.gridStacks[z][x];
                byte newCliffHeight = FroggerGridStack.calculateRawCliffHeightValue(verticesPerStack.get(gridStack));
                gridStack.setRawCliffHeightValue(newCliffHeight);
            }
        }
    }

    @SuppressWarnings("CommentedOutCode")
    private Map<FroggerGridStack, List<SVector>> calculateVertexSquareMapping(int nearbyVertexThreshold) {
        Map<FroggerGridStack, List<SVector>> results = new HashMap<>();

        List<SVector> vertices = getParentFile().getVertexPacket().getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            SVector vertexPos = vertices.get(i);
            int testX = vertexPos.getX();
            int testZ = vertexPos.getZ();

            int gridX = getGridXFromWorldX(testX);
            int gridZ = getGridZFromWorldZ(testZ);

            int xOffset = 0;
            if (getGridXFromWorldX(testX + nearbyVertexThreshold) > gridX)
                xOffset++;
            else if (getGridXFromWorldX(testX - nearbyVertexThreshold) < gridX)
                xOffset--;

            int zOffset = 0;
            if (getGridZFromWorldZ(testZ + nearbyVertexThreshold) > gridZ)
                zOffset++;
            else if (getGridZFromWorldZ(testZ - nearbyVertexThreshold) < gridZ)
                zOffset--;

            // Debugging code (used with PSX Build 30)
            /*if ((i == 1601 && getParentFile().getFileDisplayName().contains("SUB1"))
                    || (i == 1134 && getParentFile().getFileDisplayName().contains("DES1"))
                    || (i == 2220 && getParentFile().getFileDisplayName().contains("CAV3"))
                    || (i == 2221 && getParentFile().getFileDisplayName().contains("CAV3"))
                    || (i == 2740 && getParentFile().getFileDisplayName().contains("CAV3"))) {
                getLogger().info("DEBUG[%d]:", i);
                getLogger().info("DEBUG[testX=%d,testOffset=%d] -> %d >= %d, %d < %d", testX, testOffset, (testX + testOffset - getBaseGridX()), ((gridX + 1) << 8), (testX - testOffset - getBaseGridX()), (gridX << 8));
                getLogger().info("DEBUG[testZ=%d,testOffset=%d] -> %d >= %d, %d < %d", testZ, testOffset, (testZ + testOffset - getBaseGridZ() - 1), ((gridZ + 1) << 8), (testZ - testOffset - getBaseGridZ() - 1), (gridZ << 8));
                getLogger().info("DEBUG[gridX=%d,gridZ=%d,xOffset=%d,zOffset=%d,worldX=%d,worldZ=%d]", gridX, gridZ, xOffset, zOffset, getWorldXFromGridX(gridX, false), getWorldZFromGridZ(gridZ, false));
            }*/

            addVertexSquareMapping(results, gridX, gridZ, vertexPos);
            if (xOffset != 0)
                addVertexSquareMapping(results, gridX + xOffset, gridZ, vertexPos);
            if (zOffset != 0)
                addVertexSquareMapping(results, gridX, gridZ + zOffset, vertexPos);
            if (xOffset != 0 && zOffset != 0)
                addVertexSquareMapping(results, gridX + xOffset, gridZ + zOffset, vertexPos);
        }

        return results;
    }

    private void addVertexSquareMapping(Map<FroggerGridStack, List<SVector>> results, int x, int z, SVector vertex) {
        if (x < 0 || x >= this.gridXCount || z < 0 || z >= this.gridZCount)
            return; // If the square is outside the grid, don't try to add it.

        FroggerGridStack gridStack = getGridStack(x, z);
        if (gridStack != null)
            results.computeIfAbsent(gridStack, key -> new ArrayList<>()).add(vertex);
    }
}