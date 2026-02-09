package net.highwayfrogs.editor.games.sony.medievil.map.packet.grid;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapPacket;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implements the GRID packet.
 * TODO: On collprim removed from collprims packet, remove it from all grid squares. (Unless the grid is auto-generated on save)
 * TODO: On spline removed from spline packet, remove it from all grid squares. (Unless the grid is auto-generated on save)
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMapGridPacket extends MediEvilMapPacket implements IPropertyListCreator {
    @Getter private short gridShift = 12; // Grid size shift
    private MediEvilMapGridSquare[] gridSquaresByPosition = EMPTY_GRID_SQUARE_ARRAY;
    private final List<MediEvilMapGridSquare> gridSquares = new ArrayList<>();
    private final List<MediEvilMapGridSquare> immutableGridSquares = Collections.unmodifiableList(this.gridSquares);

    public static final String IDENTIFIER = "DIRG"; // 'GRID'.
    private static final MediEvilMapGridSquare[] EMPTY_GRID_SQUARE_ARRAY = new MediEvilMapGridSquare[0];
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
     * Gets the index used to identify this square while the entry is in a serialized state.
     * This value is only valid during data save, as this value may change as with the grid.
     */
    public MediEvilMapGridSquare getGridSquareByStorageIndex(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= this.gridSquares.size())
            throw new IndexOutOfBoundsException("No grid square entry could be found by the storage index " + targetIndex + " (Max: " + (this.gridSquares.size() - 1) + ")");

        return this.gridSquares.get(targetIndex);
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

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
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
