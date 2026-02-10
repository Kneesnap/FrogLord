package net.highwayfrogs.editor.games.sony.medievil.map.packet.grid;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapCollprim;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMap2DSplinePacket.MediEvilMap2DSpline;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapPacket;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.MathUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.*;

/**
 * Implements the GRID packet.
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMapGridPacket extends MediEvilMapPacket implements IPropertyListCreator {
    @Getter private short gridShift = 12; // Grid size shift
    private MediEvilMapGridSquare[] gridSquaresByPosition = EMPTY_GRID_SQUARE_ARRAY;
    private final List<MediEvilMapGridSquare> gridSquares = new ArrayList<>();
    private final List<MediEvilMapGridSquare> immutableGridSquares = Collections.unmodifiableList(this.gridSquares);

    public static final String IDENTIFIER = "DIRG"; // 'GRID'.
    private static final MediEvilMapGridSquare[] EMPTY_GRID_SQUARE_ARRAY = new MediEvilMapGridSquare[0];
    public static final int GRID_WORLD_CENTER_OFFSET = 32768;
    public static final int MINIMUM_GRID_SHIFT_EXCLUSIVE = 8;
    public static final int MAXIMUM_GRID_SHIFT_EXCLUSIVE = 16;

    public MediEvilMapGridPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    /**
     * Get the number of grid squares currently active within the grid packet.
     */
    public List<MediEvilMapGridSquare> getGridSquares() {
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
     * Gets the grid square by its position index.
     * @param positionIndex the position index to resolve
     * @return gridSquare
     */
    public MediEvilMapGridSquare getGridSquareByPosition(int positionIndex) {
        return this.gridSquaresByPosition[positionIndex];
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
        List<SVector> vertices = getParentFile().getGraphicsPacket().getVertices();
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
        List<SVector> vertices = getParentFile().getGraphicsPacket().getVertices();
        SVector startVertex = vertices.get(polygon.getVertices()[startVertexId]);
        SVector endVertex = vertices.get(polygon.getVertices()[endVertexId]);
        addSquaresIntersectingWithLine(output, startVertex, endVertex);
    }

    /**
     * Gets the grid square indices containing the given spline.
     * @param spline the spline
     * @param output the list to save to
     */
    public void getGridSquareIndices(MediEvilMap2DSpline spline, IntList output) {
        if (spline == null)
            throw new NullPointerException("spline");
        if (output == null)
            throw new NullPointerException("output");

        // Test vertices themselves.
        List<SVector> positions = spline.getSubDivisions();
        for (int i = 0; i < positions.size(); i++) {
            SVector position = positions.get(i);
            int index = getGridSquareIndexFromWorldPosition(position.getX(), position.getZ());
            if (!output.contains(index))
                output.add(index);
        }
    }

    /**
     * Gets the grid square indices containing the given spline.
     * @param collprim the collprim to
     * @param output the list to save to
     */
    public void getGridSquareIndices(MediEvilMapCollprim collprim, IntList output, SVector startVertex, SVector endVertex, IVector temp) {
        if (collprim == null)
            throw new NullPointerException("collprim");
        if (output == null)
            throw new NullPointerException("output");
        if (startVertex == null)
            startVertex = new SVector();
        if (endVertex == null)
            endVertex = new SVector();
        if (temp == null)
            temp = new IVector();

        // First, try to add the grid square where the center of the box falls within.
        // This is important because the checks below for square intersection will not add squares which fully contain the collprim.
        int baseGridIndex = getGridSquareIndexFromWorldPosition(collprim.getMatrix().getTransform()[0], collprim.getMatrix().getTransform()[2]);
        if (!output.contains(baseGridIndex))
            output.add(baseGridIndex);

        // Then, let's try intersections.
        // NOTE: Because this is a box, and the grid is exclusively a 2D data structure which ignores the Y axis,
        //  we can ignore the top layer of vertices, and all connections to them, since they are a mirror of the bottom ones.
        // Therefore, we only test the connections between the Bottom (-Y) vertices.
        // 4--------5
        // |        |
        // 0 ------ 1

        tryAddCollprimBoxIntersection(collprim, 0, 1, output, startVertex, endVertex, temp);
        tryAddCollprimBoxIntersection(collprim, 0, 4, output, startVertex, endVertex, temp);
        tryAddCollprimBoxIntersection(collprim, 1, 5, output, startVertex, endVertex, temp);
        tryAddCollprimBoxIntersection(collprim, 4, 5, output, startVertex, endVertex, temp);
    }

    private void tryAddCollprimBoxIntersection(MediEvilMapCollprim collprim, int startVertexId, int endVertexId, IntList output, SVector startVertex, SVector endVertex, IVector temp) {
        temp = collprim.getVertexWorldPosition(startVertexId, startVertex, temp);
        startVertex.setValues((short) temp.getX(), (short) temp.getY(), (short) temp.getZ());
        temp = collprim.getVertexWorldPosition(endVertexId, endVertex, temp);
        endVertex.setValues((short) temp.getX(), (short) temp.getY(), (short) temp.getZ());
        addSquaresIntersectingWithLine(output, startVertex, endVertex);
    }

    private void addSquaresIntersectingWithLine(IntList output, SVector startVertex, SVector endVertex) {
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
     * Gets the index used to identify this square while the entry is in a serialized state.
     * This value is only valid during data save, as this value may change as with the grid.
     */
    public MediEvilMapGridSquare getGridSquareByStorageIndex(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= this.gridSquares.size())
            throw new IndexOutOfBoundsException("No grid square entry could be found by the storage index " + targetIndex + " (Max: " + (this.gridSquares.size() - 1) + ")");

        return this.gridSquares.get(targetIndex);
    }

    /**
     * Regenerates the polygon grid based on the current map file.
     */
    public void regenerate() {
        Arrays.fill(this.gridSquaresByPosition, null);
        this.gridSquares.clear();

        IntList gridSquareIds = new IntList();

        // Generate by polygons to ensure the quad tree always has entries if polygons are present.
        // Example of when this occurs: PP_DATA.MAP
        List<MediEvilMapPolygon> polygons = getParentFile().getGraphicsPacket().getPolygons();
        for (int i = 0; i < polygons.size(); i++) {
            MediEvilMapPolygon polygon = polygons.get(i);
            gridSquareIds.clear();
            getGridSquareIndices(polygon, gridSquareIds);
            for (int j = 0; j < gridSquareIds.size(); j++) {
                int gridSquareIndex = gridSquareIds.get(j);
                if (this.gridSquaresByPosition[gridSquareIndex] == null)
                    addGridSquareToList(this.gridSquaresByPosition[gridSquareIndex]  = new MediEvilMapGridSquare(this, gridSquareIndex));
            }
        }

        // Generate based on splines.
        List<MediEvilMap2DSpline> splines = getParentFile().getSpline2DPacket().getSplines();
        for (int i = 0; i < splines.size(); i++) {
            MediEvilMap2DSpline spline = splines.get(i);
            gridSquareIds.clear();
            getGridSquareIndices(spline, gridSquareIds);
            for (int j = 0; j < gridSquareIds.size(); j++) {
                int gridSquareIndex = gridSquareIds.get(j);
                MediEvilMapGridSquare gridSquare = this.gridSquaresByPosition[gridSquareIndex];
                if (gridSquare == null) {
                    this.gridSquaresByPosition[gridSquareIndex] = gridSquare = new MediEvilMapGridSquare(this, gridSquareIndex);
                    addGridSquareToList(gridSquare);
                }

                gridSquare.getSplines().add(spline);
            }
        }

        // Generate based on collprims.
        SVector startVertex = new SVector(), endVertex = new SVector();
        IVector temp = new IVector();
        List<MediEvilMapCollprim> collprims = getParentFile().getCollprimsPacket().getCollprims();
        for (int i = 0; i < collprims.size(); i++) {
            MediEvilMapCollprim collprim = collprims.get(i);
            gridSquareIds.clear();
            getGridSquareIndices(collprim, gridSquareIds, startVertex, endVertex, temp);
            for (int j = 0; j < gridSquareIds.size(); j++) {
                int gridSquareIndex = gridSquareIds.get(j);
                MediEvilMapGridSquare gridSquare = this.gridSquaresByPosition[gridSquareIndex];
                if (gridSquare == null) {
                    this.gridSquaresByPosition[gridSquareIndex] = gridSquare = new MediEvilMapGridSquare(this, gridSquareIndex);
                    addGridSquareToList(gridSquare);
                }

                gridSquare.getCollprims().add(collprim);
            }
        }
    }

    private void addGridSquareToList(MediEvilMapGridSquare gridSquare) {
        int searchIndex = Collections.binarySearch(this.gridSquares, gridSquare, Comparator.comparingInt(MediEvilMapGridSquare::getSquareIndex));
        if (searchIndex >= 0)
            throw new IllegalStateException(gridSquare + " is already registered in the list.");

        this.gridSquares.add(-(searchIndex + 1), gridSquare);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        short gridXSquareCount = reader.readUnsignedByteAsShort();
        short gridYSquareCount = reader.readUnsignedByteAsShort();
        int gridSquareSize = reader.readUnsignedShortAsInt();
        this.gridShift = reader.readUnsignedByteAsShort();
        reader.alignRequireByte((byte) 0xFF, Constants.INTEGER_SIZE);
        int gridIdTablePtr = reader.readInt();
        int gridDataBasePtr = reader.readInt();

        // Read grid ID table.
        reader.requireIndex(getLogger(), gridIdTablePtr, "Expected Grid ID Table");
        IntList gridSquareTableIndices = new IntList();
        this.gridSquaresByPosition = new MediEvilMapGridSquare[gridYSquareCount * gridXSquareCount];
        for (int i = 0; i < this.gridSquaresByPosition.length; i++) {
            short id = reader.readShort();
            if (id == -1)
                continue;

            int gridSquareIndex = (id & 0xFFFF);
            if (gridSquareIndex != gridSquareTableIndices.size())
                throw new IllegalArgumentException("gridSquareEntryIndex was expected to be " + gridSquareTableIndices.size() + ", but was actually " + gridSquareIndex + "!");

            gridSquareTableIndices.add(i);
        }

        // Read grid squares.
        this.gridSquares.clear();
        reader.requireIndex(getLogger(), gridDataBasePtr, "Expected Grid Square entries");
        for (int i = 0; i < gridSquareTableIndices.size(); i++) {
            int squareIndex = gridSquareTableIndices.get(i);
            MediEvilMapGridSquare newGridSquare = new MediEvilMapGridSquare(this, squareIndex);
            this.gridSquaresByPosition[squareIndex] = newGridSquare;
            this.gridSquares.add(newGridSquare);
            newGridSquare.load(reader);
        }

        // Read grid square data.
        for (int i = 0; i < this.gridSquares.size(); i++)
            this.gridSquares.get(i).loadColPrims(reader);
        for (int i = 0; i < this.gridSquares.size(); i++)
            this.gridSquares.get(i).loadSplines(reader);

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

    @SuppressWarnings("CommentedOutCode")
    private void validateGrid() {
        // We want to automatically generate these grids.
        // This ensures we'll know if this observation is violated.
        // Being able to rely on this behavior is important for modding capabilities, so that automated grid generation works.
        IntList gridSquareIndices = new IntList();
        //SVector startVertex = new SVector(), endVertex = new SVector();
        //IVector temp = new IVector();
        for (int i = 0; i < this.gridSquares.size(); i++) {
            MediEvilMapGridSquare gridSquare = this.gridSquares.get(i);

            // Test splines.
            // Disabled to prevent unnecessary warnings.
            // NOTE: Spline grid generation has not actually been tested to work.
            /*List<MediEvilMap2DSpline> splines = gridSquare.getSplines();
            for (int j = 0; j < splines.size(); j++) {
                MediEvilMap2DSpline spline = splines.get(j);
                gridSquareIndices.clear();
                getGridSquareIndices(spline, gridSquareIndices);
                if (!gridSquareIndices.contains(gridSquare.getSquareIndex()))
                    getLogger().warning("gridSquarePosition[%d] contained a spline (%s) which was not observed to be part of that grid square.",
                            gridSquare.getSquareIndex(), spline);
            }*/

            // Test collprims.
            // This test has been disabled because it appears the original would include collprims far away from an unknown reason.
            /*List<MediEvilMapCollprim> collprims = gridSquare.getCollprims();
            for (int j = 0; j < collprims.size(); j++) {
                MediEvilMapCollprim collprim = collprims.get(j);
                gridSquareIndices.clear();
                getGridSquareIndices(collprim, gridSquareIndices, startVertex, endVertex, temp);
                if (!gridSquareIndices.contains(gridSquare.getSquareIndex()))
                    getLogger().warning("gridSquarePosition[%d] contained a collprim (%s) which was not observed to be part of that grid square.",
                            gridSquare.getSquareIndex(), collprim);
            }*/
        }

        // Secondly, go through each of the grid square indices calculated for each thing tracked, and test if the game uses them.
        // Eg: See if we're placing things in places they don't belong.
        // NOTE: Spline grid generation has not actually been tested to work.
        /*List<MediEvilMap2DSpline> splines = getParentFile().getSpline2DPacket().getSplines();
        for (int i = 0; i < splines.size(); i++) {
            MediEvilMap2DSpline spline = splines.get(i);
            gridSquareIndices.clear();
            getGridSquareIndices(spline, gridSquareIndices);

            // Check each grid square.
            for (int j = 0; j < gridSquareIndices.size(); j++) {
                int gridSquareIndex = gridSquareIndices.get(j);
                MediEvilMapGridSquare gridSquare = this.gridSquaresByPosition[gridSquareIndex];
                if (gridSquare == null || !gridSquare.getSplines().contains(spline)) {
                    getLogger().warning("FrogLord thought that gridSquarePosition[%d] should contain a spline (%s), but the loaded game data did not agree.",
                            gridSquareIndex, spline);
                }
            }
        }*/

        // Disabled because it appears to work, and FrogLord's algorithm is accurate enough to enter into squares the original algorithm probably should have but didn't.
        /*List<MediEvilMapCollprim> collprims = getParentFile().getCollprimsPacket().getCollprims();
        for (int i = 0; i < collprims.size(); i++) {
            MediEvilMapCollprim collprim = collprims.get(i);
            gridSquareIndices.clear();
            getGridSquareIndices(collprim, gridSquareIndices, startVertex, endVertex, temp);

            // Check each grid square.
            for (int j = 0; j < gridSquareIndices.size(); j++) {
                int gridSquareIndex = gridSquareIndices.get(j);
                MediEvilMapGridSquare gridSquare = this.gridSquaresByPosition[gridSquareIndex];
                if (gridSquare == null || !gridSquare.getCollprims().contains(collprim)) {
                    getLogger().warning("FrogLord thought that gridSquarePosition[%d] should contain a collprim (%s), but the loaded game data did not agree.",
                            gridSquareIndex, collprim);
                }
            }
        }*/
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        regenerate();
        writer.writeUnsignedByte((short) getGridSize());
        writer.writeUnsignedByte((short) getGridSize());
        writer.writeUnsignedShort(getGridSquareSize());
        writer.writeUnsignedByte(this.gridShift);
        writer.align(Constants.INTEGER_SIZE, (byte) 0xFF);
        int gridIdTablePtr = writer.writeNullPointer();
        int gridDataBasePtr = writer.writeNullPointer();

        // Write grid ID table.
        writer.writeAddressTo(gridIdTablePtr);
        int storageIndex = 0;
        for (int i = 0; i < this.gridSquaresByPosition.length; i++) {
            MediEvilMapGridSquare gridSquare = this.gridSquaresByPosition[i];
            if (gridSquare != null) {
                if (this.gridSquares.get(storageIndex) != gridSquare)
                    throw new IllegalStateException("The gridSquares list was ordered wrong. Got " + this.gridSquares.get(storageIndex) + " at index " + storageIndex + ", when " + gridSquare + " was expected.");

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
            this.gridSquares.get(i).saveColPrims(writer);
        for (int i = 0; i < this.gridSquares.size(); i++)
            this.gridSquares.get(i).saveSplines(writer);
    }

    @Override
    public void clear() {
        Arrays.fill(this.gridSquaresByPosition, null);
        this.gridSquares.clear();
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
