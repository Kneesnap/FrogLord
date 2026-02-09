package net.highwayfrogs.editor.games.sony.medievil.map.polygrid;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.MathUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents the MediEvil .PGD files (Polygon grids).
 * Created by Kneesnap on 2/5/2026.
 */
public class MediEvilPolygonGridFile extends SCGameFile<MediEvilGameInstance> {
    @Getter private short gridShift = 9;
    private MediEvilPolygonGridSquare[] gridSquaresByPosition = EMPTY_GRID_SQUARE_ARRAY;
    private final List<MediEvilPolygonGridSquare> gridSquares = new ArrayList<>();
    private final List<MediEvilPolygonGridSquare> immutableGridSquares = Collections.unmodifiableList(this.gridSquares);
    private MWIResourceEntry mapFileEntry;

    private static final MediEvilPolygonGridSquare[] EMPTY_GRID_SQUARE_ARRAY = new MediEvilPolygonGridSquare[0];
    public static final int GRID_WORLD_CENTER_OFFSET = 32768;
    public static final int MINIMUM_GRID_SHIFT_EXCLUSIVE = 8;
    public static final int MAXIMUM_GRID_SHIFT_EXCLUSIVE = 16;

    public MediEvilPolygonGridFile(MediEvilGameInstance instance) {
        super(instance);
    }

    /**
     * Get the number of grid squares currently active within the grid.
     */
    public List<MediEvilPolygonGridSquare> getGridSquares() {
        return this.immutableGridSquares;
    }

    /**
     * Gets the length (in number of squares) along each of the sides of the grid.
     */
    public int getGridSize() {
        return getGridSize(this.gridShift);
    }

    /**
     * Gets the size (in world units) of each side of a grid square.
     */
    public int getGridSquareSize() {
        return getGridSquareSize(this.gridShift);
    }

    /**
     * Sets the grid shift value for this grid file.
     * @param newGridShift the new grid shift value
     */
    public void setGridShift(int newGridShift) {
        validateGridShift(newGridShift);
        this.gridShift = (short) newGridShift;
    }

    /**
     * Gets the grid square index containing the given X/Z world position.
     * @param x the world x coordinate
     * @param z the world z coordinate
     * @return gridSquarePositionIndex
     */
    public int getGridSquareIndexFromWorldPosition(int x, int z) {
        return (((z + GRID_WORLD_CENTER_OFFSET) >> this.gridShift) * getGridSize())
                + ((x + GRID_WORLD_CENTER_OFFSET) >> this.gridShift);
    }

    /**
     * Gets the grid square indices containing the given polygon.
     * FrogLord's algorithm for determining this is BETTER than the original polygon grid building algorithm.
     * We don't know exactly how it was implemented, but the original game data juuust barely missed some polygons which FrogLord doesn't miss.
     * These overlaps between the polygon and grid square are razor-thin, requiring zooming in extremely close (while using a 3D debug preview) to see that they indeed overlap.
     * So, technically, there are about 150 polygons in MediEvil which FrogLord will include in groups that aren't included in the original.
     * This behavior is (technically) valid, so it is maintained.
     * @param polygon the world x coordinate
     * @param output the list to save to
     */
    public void getGridSquareIndices(MediEvilMapPolygon polygon, IntList output) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (output == null)
            throw new NullPointerException("output");

        // Test vertices themselves.
        int vertexCount = polygon.getVertexCount();
        List<SVector> vertices = getMapFile().getGraphicsPacket().getVertices();
        for (int k = 0; k < vertexCount; k++) {
            SVector vertex = vertices.get(polygon.getVertices()[k]);
            int calculatedGridSquareIndex = getGridSquareIndexFromWorldPosition(vertex.getX(), vertex.getZ());
            if (!output.contains(calculatedGridSquareIndex))
                output.add(calculatedGridSquareIndex);
        }

        // Test polygon edges.
        if (vertexCount == 3) {
            addIntersectingIndices(polygon, output, 0, 1);
            addIntersectingIndices(polygon, output, 1, 2);
            addIntersectingIndices(polygon, output, 2, 0);
        } else if (vertexCount == 4) {
            addIntersectingIndices(polygon, output, 0, 1);
            addIntersectingIndices(polygon, output, 1, 3);
            addIntersectingIndices(polygon, output, 3, 2);
            addIntersectingIndices(polygon, output, 2, 0);
            // The following are not polygon edges, but would break the quad into tris.
            addIntersectingIndices(polygon, output, 0, 3);
            addIntersectingIndices(polygon, output, 1, 2);
        }
    }

    private void addIntersectingIndices(MediEvilMapPolygon polygon, IntList output, int startVertexId, int endVertexId) {
        List<SVector> vertices = getMapFile().getGraphicsPacket().getVertices();
        SVector startVertex = vertices.get(polygon.getVertices()[startVertexId]);
        SVector endVertex = vertices.get(polygon.getVertices()[endVertexId]);

        // 1) Create a box of minGrid / maxGrid coordinates.
        int startVtxGridX = (startVertex.getX() + GRID_WORLD_CENTER_OFFSET) >> this.gridShift;
        int startVtxGridZ = (startVertex.getZ() + GRID_WORLD_CENTER_OFFSET) >> this.gridShift;
        int endVtxGridX = (endVertex.getX() + GRID_WORLD_CENTER_OFFSET) >> this.gridShift;
        int endVtxGridZ = (endVertex.getZ() + GRID_WORLD_CENTER_OFFSET) >> this.gridShift;

        int minGridX = Math.min(startVtxGridX, endVtxGridX), maxGridX = Math.max(startVtxGridX, endVtxGridX);
        int minGridZ = Math.min(startVtxGridZ, endVtxGridZ), maxGridZ = Math.max(startVtxGridZ, endVtxGridZ);

        // 2) Test intersection with all the grid squares in that box.
        for (int gridY = minGridZ; gridY <= maxGridZ; gridY++) {
            for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
                int gridIndex = (gridY * getGridSize()) + gridX;
                if (!output.contains(gridIndex) && doesGridSquareBoxIntersect(gridX, gridY, startVertex, endVertex))
                    output.add(gridIndex);
            }
        }
    }

    private boolean doesGridSquareBoxIntersect(int gridX, int gridY, SVector lineStart, SVector lineEnd) {
        int gridSquareSize = getGridSquareSize();
        int gridBoxMinX = (gridX << this.gridShift) - GRID_WORLD_CENTER_OFFSET;
        int gridBoxMaxX = gridBoxMinX + gridSquareSize;
        int gridBoxMinY = (gridY << this.gridShift) - GRID_WORLD_CENTER_OFFSET;
        int gridBoxMaxY = gridBoxMinY + gridSquareSize;

        // Create a hypothetical box around the grid square area, by testing if the line points intersect with any of the box edges.
        int lineStartX = lineStart.getX();
        int lineStartY = lineStart.getZ();
        int lineEndX = lineEnd.getX();
        int lineEndY = lineEnd.getZ();
        return MathUtils.doLinesIntersect(lineStartX, lineStartY, lineEndX, lineEndY, gridBoxMinX, gridBoxMaxY, gridBoxMaxX, gridBoxMaxY) // Top Edge
                || MathUtils.doLinesIntersect(lineStartX, lineStartY, lineEndX, lineEndY, gridBoxMinX, gridBoxMinY, gridBoxMaxX, gridBoxMinY) // Bottom Edge
                || MathUtils.doLinesIntersect(lineStartX, lineStartY, lineEndX, lineEndY, gridBoxMinX, gridBoxMinY, gridBoxMinX, gridBoxMaxY) // Left Edge
                || MathUtils.doLinesIntersect(lineStartX, lineStartY, lineEndX, lineEndY, gridBoxMaxX, gridBoxMinY, gridBoxMaxX, gridBoxMaxY); // Right Edge
    }


    /**
     * Gets the map file which this quad tree corresponds to.
     */
    public MediEvilMapFile getMapFile() {
        // Try resolving the previous resource as the map file.
        if (this.mapFileEntry == null) {
            MWIResourceEntry lastResourceEntry = getGameInstance().getResourceEntryByID(getFileResourceId() - 2);
            if (lastResourceEntry != null && (lastResourceEntry.getTypeId() == MediEvilGameInstance.FILE_TYPE_MAP || lastResourceEntry.hasExtension("MAP")))
                this.mapFileEntry = lastResourceEntry;
        }

        // Try resolving the resource with the same name.
        if (this.mapFileEntry == null)
            this.mapFileEntry = getGameInstance().getResourceEntryByName(FileUtils.stripExtension(getFileDisplayName()) + ".MAP");

        if (this.mapFileEntry == null)
            throw new IllegalStateException("Failed to find a corresponding map file to '" + getFileDisplayName() + "'.");

        SCGameFile<?> file = this.mapFileEntry.getGameFile();
        if (!(file instanceof MediEvilMapFile))
            throw new IllegalStateException("File '" + file.getFileDisplayName() + "' could not be resolved to an actual map file! (Was: " + Utils.getSimpleName(file) + ")");

        return (MediEvilMapFile) file;
    }

    /**
     * Regenerates the polygon grid based on the current map file.
     */
    public void regenerate() {
        Arrays.fill(this.gridSquaresByPosition, null);
        this.gridSquares.clear();

        IntList gridSquareIds = new IntList();
        List<MediEvilMapPolygon> polygons = getMapFile().getGraphicsPacket().getPolygons();
        for (int i = 0; i < polygons.size(); i++) {
            MediEvilMapPolygon polygon = polygons.get(i);
            gridSquareIds.clear();
            getGridSquareIndices(polygon, gridSquareIds);
            for (int j = 0; j < gridSquareIds.size(); j++) {
                int gridSquareIndex = gridSquareIds.get(j);
                MediEvilPolygonGridSquare gridSquare = this.gridSquaresByPosition[gridSquareIndex];
                if (gridSquare == null)
                    this.gridSquaresByPosition[gridSquareIndex] = gridSquare = new MediEvilPolygonGridSquare(this, gridSquareIndex);

                gridSquare.getPolygons().add(polygon);
            }
        }
    }

    @Override
    public void load(DataReader reader) {
        short gridXSquareCount = reader.readUnsignedByteAsShort();
        short gridYSquareCount = reader.readUnsignedByteAsShort();
        int gridSquareSize = reader.readUnsignedShortAsInt();
        this.gridShift = reader.readUnsignedByteAsShort();
        reader.alignRequireEmpty(Constants.INTEGER_SIZE);

        int gridIdTablePtr = reader.readInt();
        int gridDataBasePtr = reader.readInt();

        // Read grid ID table.
        reader.requireIndex(getLogger(), gridIdTablePtr, "Expected Grid ID Table");
        IntList gridSquareTableIndices = new IntList();
        this.gridSquaresByPosition = new MediEvilPolygonGridSquare[gridYSquareCount * gridXSquareCount];
        for (int i = 0; i < this.gridSquaresByPosition.length; i++) {
            short id = reader.readShort();
            if (id == -1)
                continue;

            int gridSquareIndex = (id & 0xFFFF);
            if (gridSquareIndex != gridSquareTableIndices.size())
                throw new IllegalArgumentException("gridSquareIndex was expected to be " + gridSquareTableIndices.size() + ", but was actually " + gridSquareIndex + "!");

            gridSquareTableIndices.add(i);
        }

        // Read grid squares.
        this.gridSquares.clear();
        reader.requireIndex(getLogger(), gridDataBasePtr, "Expected Grid Square entries");
        for (int i = 0; i < gridSquareTableIndices.size(); i++) {
            int squareIndex = gridSquareTableIndices.get(i);
            MediEvilPolygonGridSquare newGridSquare = new MediEvilPolygonGridSquare(this, squareIndex);
            this.gridSquaresByPosition[squareIndex] = newGridSquare;
            this.gridSquares.add(newGridSquare);
            newGridSquare.load(reader);
        }

        // Read grid square data.
        for (int i = 0; i < this.gridSquares.size(); i++)
            this.gridSquares.get(i).loadPolygons(reader);

        // Ensure the grid loaded seems to match FrogLord's understanding of how polygon grids work.
        validateGrid();

        // Throw errors if data doesn't match expected.
        int expectedGridSize = getGridSize();
        if (gridXSquareCount != expectedGridSize)
            throw new IllegalStateException("Expected a gridXSquareCount of " + expectedGridSize + " for a gridShift of " + this.gridShift + ", but actually got gridXSquareCount=" + gridXSquareCount + ".");
        if (gridYSquareCount != expectedGridSize)
            throw new IllegalStateException("Expected a gridYSquareCount of " + expectedGridSize + " for a gridShift of " + this.gridShift + ", but actually got gridYSquareCount=" + gridYSquareCount + ".");

        int expectedGridSquareSize = getGridSquareSize();
        if (gridSquareSize != expectedGridSquareSize)
            throw new IllegalStateException("Expected a gridSquareSize of " + expectedGridSquareSize + " for a gridShift of " + this.gridShift + ", but actually got gridSquareSize=" + gridSquareSize + ".");
    }

    private void validateGrid() {
        // MediEvil polygons appear to always have their center in the grid square which they occupy.
        // This ensures we'll know if this observation is violated.
        // Being able to rely on this behavior is important for modding capabilities, so that automated grid generation works.
        IntList gridSquareIndices = new IntList();
        for (int i = 0; i < this.gridSquares.size(); i++) {
            MediEvilPolygonGridSquare gridSquare = this.gridSquares.get(i);
            List<MediEvilMapPolygon> polygons = gridSquare.getPolygons();
            for (int j = 0; j < polygons.size(); j++) {
                MediEvilMapPolygon polygon = polygons.get(j);
                gridSquareIndices.clear();
                getGridSquareIndices(polygon, gridSquareIndices);
                if (!gridSquareIndices.contains(gridSquare.getSquareIndex()))
                    getLogger().warning("gridSquarePosition[%d] contained a polygon (%s) which was not observed to be part of that grid square.",
                            gridSquare.getSquareIndex(), polygon);
            }
        }

        // Secondly, go through each of the grid square indices calculated for a polygon, and test if the game uses them.
        // This test has been disabled because FrogLord's algorithm is TOO good.
        // The original game data juuust barely missed some polygons which FrogLord doesn't miss. We're talking you've gotta zoom in really close to a 3D preview to see the mismatch.
        // So, technically, there are about 150 polygons in MediEvil which FrogLord will include in groups that aren't included in the original.
        // This behavior is (technically) valid, so it is maintained.
        /*List<MediEvilMapPolygon> polygons = getMapFile().getGraphicsPacket().getPolygons();
        for (int i = 0; i < polygons.size(); i++) {
            MediEvilMapPolygon polygon = polygons.get(i);
            gridSquareIndices.clear();
            getGridSquareIndices(polygon, gridSquareIndices);

            // Check each grid square.
            for (int j = 0; j < gridSquareIndices.size(); j++) {
                int gridSquareIndex = gridSquareIndices.get(j);
                MediEvilPolygonGridSquare gridSquare = this.gridSquaresByPosition[gridSquareIndex];
                if (gridSquare == null || !gridSquare.getPolygons().contains(polygon)) {
                    getLogger().warning("FrogLord thought that gridSquarePosition[%d] should contain a polygon (%s), but the loaded game data did not agree.",
                            gridSquareIndex, polygon);
                }
            }
        }*/
    }

    @Override
    public void save(DataWriter writer) {
        regenerate();
        writer.writeUnsignedByte((short) getGridSize());
        writer.writeUnsignedByte((short) getGridSize());
        writer.writeUnsignedShort(getGridSquareSize());
        writer.writeUnsignedByte(this.gridShift);
        writer.align(Constants.INTEGER_SIZE);

        int gridIdTablePtr = writer.writeNullPointer();
        int gridDataBasePtr = writer.writeNullPointer();

        // Write grid ID table.
        writer.writeAddressTo(gridIdTablePtr);
        int storageIndex = 0;
        for (int i = 0; i < this.gridSquaresByPosition.length; i++) {
            MediEvilPolygonGridSquare gridSquare = this.gridSquaresByPosition[i];
            if (gridSquare != null) {
                if (this.gridSquares.get(i) != gridSquare)
                    throw new IllegalStateException("The gridSquares list was ordered wrong. Got " + this.gridSquares.get(i) + " at index " + i + ", when " + gridSquare + " was expected.");

                writer.writeUnsignedShort(storageIndex);
                storageIndex++;
            } else {
                writer.writeShort((short) -1);
            }
        }

        // Write grid squares.
        writer.writeAddressTo(gridDataBasePtr);
        for (int i = 0; i < this.gridSquares.size(); i++)
            this.gridSquares.get(i).save(writer);
        for (int i = 0; i < this.gridSquares.size(); i++)
            this.gridSquares.get(i).savePolygons(writer);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.TREASURE_MAP_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return new DefaultFileUIController<>(getGameInstance(), "Polygon Grid", ImageResource.TREASURE_MAP_16.getFxImage());
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("Grid Dimensions", getGridSize() + "x" + getGridSize());
        propertyList.addInteger("Grid Square Size", getGridSquareSize());
        propertyList.addInteger("Grid Shift", this.gridShift,
                newValue -> newValue > MINIMUM_GRID_SHIFT_EXCLUSIVE && newValue < MAXIMUM_GRID_SHIFT_EXCLUSIVE,
                this::setGridShift);
        propertyList.addString(this::addGridSquares, "Grid Squares", String.valueOf(this.gridSquares.size()));
    }

    private void addGridSquares(PropertyListNode propertyList) {
        for (int i = 0; i < this.gridSquares.size(); i++)
            propertyList.addProperties("GridSquare[" + i + "]", this.gridSquares.get(i));
    }

    private static void validateGridShift(int gridShift) {
        // Values outside of this range will cause invalid sizes like zero.
        if (gridShift <= MINIMUM_GRID_SHIFT_EXCLUSIVE || gridShift >= MAXIMUM_GRID_SHIFT_EXCLUSIVE)
            throw new IllegalArgumentException("Invalid gridShift: " + gridShift + ", must be within (" + MINIMUM_GRID_SHIFT_EXCLUSIVE + ", " + MAXIMUM_GRID_SHIFT_EXCLUSIVE + ").");
    }

    private static int getGridSquareSize(int gridShift) {
        validateGridShift(gridShift);
        return (1 << gridShift);
    }

    private static int getGridSize(int gridShift) {
        validateGridShift(gridShift);
        return 1 << (16 - gridShift);
    }
}
