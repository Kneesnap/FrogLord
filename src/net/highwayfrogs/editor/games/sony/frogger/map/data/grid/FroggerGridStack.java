package net.highwayfrogs.editor.games.sony.frogger.map.data.grid;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameObject;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents the GRID_STACK struct.
 * Created by Kneesnap on 8/27/2018.
 */
public class FroggerGridStack extends SCGameObject<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile;
    @Getter private final int x;
    @Getter private final int z;
    @Getter private final List<FroggerGridSquare> gridSquares = new ArrayList<>();
    private short averageHeight; // This is only used for cliff deaths. I'm not sure yet how this is calculated, it's pretty complicated it seems.
    private transient short loadSquareCount = -1;

    public static final int MAX_GRID_DIMENSION = 256;

    public FroggerGridStack(FroggerMapFile mapFile, int x, int z) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
        this.x = x;
        this.z = z;
    }

    /**
     * Loads the stack data.
     * @param reader the reader to read data from.
     * @param expectedSquareIndex the index we expect to start reading square data from.
     * @return the position we expect the next grid stack to read squares from
     */
    public int load(DataReader reader, int expectedSquareIndex) {
        this.loadSquareCount = reader.readUnsignedByteAsShort();
        this.averageHeight = reader.readUnsignedByteAsShort();
        int realSquareIndex = reader.readUnsignedShortAsInt();
        if (realSquareIndex != expectedSquareIndex)
            throw new RuntimeException("The expected grid square index (" + expectedSquareIndex + ") did not match the grid square index in the game data (" + realSquareIndex + ").");

        return expectedSquareIndex + this.loadSquareCount;
    }

    /**
     * Reads the grid squares from the current position.
     * @param reader the reader to read it from
     */
    public void loadGridSquares(DataReader reader) {
        if (this.loadSquareCount < 0)
            throw new RuntimeException("Cannot read grid squares from " + NumberUtils.toHexString(reader.getIndex()) + ", the amount of squares to load was set to " + this.loadSquareCount + ".");

        this.gridSquares.clear();
        for (int i = 0; i < this.loadSquareCount; i++) {
            FroggerGridSquare newGridSquare = new FroggerGridSquare(this);
            newGridSquare.load(reader);
            this.gridSquares.add(newGridSquare);
        }

        this.loadSquareCount = -1;
    }

    /**
     * Saves the grid stack data to the writer.
     * @param writer the writer to write the grid stack data to
     * @param gridSquareStartIndex the index into the grid squares array which our grid squares will be written to
     * @return the position the next grid stack will write its grid squares to
     */
    public int save(DataWriter writer, int gridSquareStartIndex) {
        writer.writeUnsignedByte((short) this.gridSquares.size());
        writer.writeUnsignedByte(this.averageHeight);
        writer.writeUnsignedShort(gridSquareStartIndex);
        return gridSquareStartIndex + this.gridSquares.size();
    }

    /**
     * Writes the grid squares from the current position.
     * @param writer the writer to write it to
     */
    public void saveGridSquares(DataWriter writer) {
        for (int i = 0; i < this.gridSquares.size(); i++)
            this.gridSquares.get(i).save(writer);
    }

    /**
     * Gets the logger information.
     */
    public String getLoggerInfo() {
        return this.mapFile != null ? this.mapFile.getFileDisplayName() + "|GridStack{x=" + this.x + ",z=" + this.z + "}" : Utils.getSimpleName(this);
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger(getLoggerInfo());
    }

    /**
     * Gets the average world height.
     * This is used to prevent the player from walking into a cliff higher than their current position.
     * An example of this behavior is seen in Lily Islands (SUB1) at the bird pickup spot to the purple frog.
     * @return height
     */
    public int getAverageWorldHeight() {
        return (this.averageHeight << 6);
    }

    /**
     * Gets the average world height as a floating point number.
     * This is used to prevent the player from walking into a cliff higher than their current position.
     * An example of this behavior is seen in Lily Islands (SUB1) at the bird pickup spot to the purple frog.
     * @return height
     */
    public float getAverageWorldHeightAsFloat() {
        return DataUtils.fixedPointIntToFloat4Bit(getAverageWorldHeight());
    }

    /**
     * Set the average world height of this stack.
     * This is used to prevent the player from walking into a cliff higher than their current position.
     * An example of this behavior is seen in Lily Islands (SUB1) at the bird pickup spot to the purple frog.
     * @param newHeight The new height.
     */
    public void setAverageWorldHeight(int newHeight) {
        this.averageHeight = (short) (newHeight >> 6);
    }

    /**
     * Set the average world height of this stack.
     * This is used to prevent the player from walking into a cliff higher than their current position.
     * An example of this behavior is seen in Lily Islands (SUB1) at the bird pickup spot to the purple frog.
     * @param newHeight The new height.
     */
    public void setAverageWorldHeight(float newHeight) {
        setAverageWorldHeight(DataUtils.floatToFixedPointInt4Bit(newHeight));
    }

    /**
     * Calculates the average world height of the squares in the stack.
     * TODO: This calculation could be inaccurate, I'm not sure.
     * @return worldHeight
     */
    public int calculateAverageWorldHeight() {
        int fullWorldHeight = 0;
        int squareCount = 0;
        for (int i = this.gridSquares.size() - 1; i >= 0; i--) {
            FroggerGridSquare square = this.gridSquares.get(i);
            if (square.getPolygon() == null)
                continue;

            fullWorldHeight += square.calculateAverageWorldHeight();
            squareCount++;
        }

        // No usable grid squares.
        return squareCount > 0 ? (-fullWorldHeight / squareCount) : getAverageWorldHeight();
    }

    /**
     * Calculates the world Y of the highest square in the stack.
     * @return highestGridSquareY
     */
    public float getHighestGridSquareYAsFloat() {
        for (int i = this.gridSquares.size() - 1; i >= 0; i--) {
            FroggerGridSquare square = this.gridSquares.get(i);
            if (square.getPolygon() != null)
                return DataUtils.fixedPointIntToFloat4Bit(square.calculateAverageWorldHeight());
        }

        // No usable grid squares.
        return -getAverageWorldHeightAsFloat();
    }

    /**
     * Reimplementation of GRID.C/GetGridInfoFromWorldXZ
     */
    public FroggerGridStackInfo getGridStackInfo() {
        if (this.gridSquares.isEmpty())
            return null;

        FroggerGridSquare gridSquare = this.gridSquares.get(0);
        FroggerMapPolygon polygon = gridSquare.getPolygon();
        if (polygon == null)
            return null;

        // GRID_SQUARE points to a map poly, which we take to define two semi-infinite tris.
        // Work out which one we are over, then project onto it.
        int dx = this.x % MAX_GRID_DIMENSION;
        int dz = this.z % MAX_GRID_DIMENSION;

        int gridY;
        IVector xSlope = new IVector(MAX_GRID_DIMENSION, 0, 0);
        IVector zSlope = new IVector(0, 0, MAX_GRID_DIMENSION);
        List<SVector> vertices = getMapFile().getVertexPacket().getVertices();
        if (dz >= dx) {
            // Top left tri
            int mapPolyY0 = vertices.get(polygon.getVertices()[0]).getY();
            int mapPolyY1 = vertices.get(polygon.getVertices()[1]).getY();
            int mapPolyY2 = vertices.get(polygon.getVertices()[2]).getY();

            dz = MAX_GRID_DIMENSION - dz;
            gridY = mapPolyY0 + ((dx * (mapPolyY1 - mapPolyY0)) >> 8) + ((dz * (mapPolyY2 - mapPolyY0)) >> 8);
            xSlope.setY(mapPolyY1 - mapPolyY0);
            zSlope.setY(mapPolyY0 - mapPolyY2);
        } else {
            // Bottom right tri
            int mapPolyY1 = vertices.get(polygon.getVertices()[1]).getY();
            int mapPolyY2 = vertices.get(polygon.getVertices()[2]).getY();
            int mapPolyY3 = polygon.getPolygonType().isQuad() ? vertices.get(polygon.getVertices()[3]).getY() : mapPolyY2;

            dx = MAX_GRID_DIMENSION - dx;
            gridY = mapPolyY3 + ((dx * (mapPolyY2 - mapPolyY3)) >> 8) + ((dz * (mapPolyY1 - mapPolyY3)) >> 8);
            xSlope.setY(mapPolyY3 - mapPolyY2);
            zSlope.setY(mapPolyY1 - mapPolyY3);
        }

        xSlope.normalise();
        zSlope.normalise();
        return new FroggerGridStackInfo(gridY, xSlope, zSlope);
    }

    @Getter
    @RequiredArgsConstructor
    public static class FroggerGridStackInfo {
        private final int y;
        private final IVector xSlope;
        private final IVector zSlope;
    }
}