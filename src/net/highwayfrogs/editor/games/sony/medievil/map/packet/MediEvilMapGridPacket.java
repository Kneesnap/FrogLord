package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapCollprim;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMap2DSplinePacket.MediEvilMap2DSpline;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

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

    public static class MediEvilMapGridSquare extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator {
        @Getter private final MediEvilMapGridPacket gridPacket;
        @Getter private final int squareIndex;
        @Getter private final List<MediEvilMapCollprim> collprims = new ArrayList<>();
        @Getter private final List<MediEvilMap2DSpline> splines = new ArrayList<>();

        private int tempColPrimIdPointer = -1;
        private int tempSplinePointer = -1;

        public MediEvilMapGridSquare(MediEvilMapGridPacket gridPacket, int squareIndex) {
            super(gridPacket.getGameInstance());
            this.gridPacket = gridPacket;
            this.squareIndex = squareIndex;
        }

        /**
         * Gets the map file.
         */
        public MediEvilMapFile getMap() {
            return this.gridPacket.getParentFile();
        }

        /**
         * Gets the x grid coordinate of this entry.
         */
        public int getGridX() {
            return this.squareIndex % this.gridPacket.getGridSize();
        }

        /**
         * Gets the y grid coordinate of this entry.
         */
        public int getGridY() {
            return this.squareIndex / this.gridPacket.getGridSize();
        }

        /**
         * Gets the index used to identify this entry while the entry is in a serialized state.
         * This value is only valid during data save, as this value may change as with the grid.
         */
        public int calculateStorageIndex() {
            int storageIndex = this.gridPacket.gridSquares.indexOf(this);
            if (storageIndex < 0)
                throw new IllegalStateException("The square is not part of the grid, so it therefore has no storageIndex.");

            return storageIndex;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{x=" + getGridX() + ",y=" + getGridY() + ",collprims=" + this.collprims.size()
                    + ",splines=" + this.splines.size() + "}";
        }

        @Override
        public void load(DataReader reader) {
            this.tempColPrimIdPointer = reader.readInt();
            reader.skipBytesRequireEmpty(Constants.POINTER_SIZE); // Seems to be set to null during map load.
            this.tempSplinePointer = reader.readInt();
            short colPrimCount = reader.readUnsignedByteAsShort();
            short splineCount = reader.readUnsignedByteAsShort();
            reader.skipShort(); // Unknown garbage. Thought it was polygon count, but maybe not.

            // 2 Runtime pointer fields.
            reader.skipBytesRequireEmpty(2 * Constants.POINTER_SIZE);

            // Prepare lists.
            this.collprims.clear();
            for (int i = 0; i < colPrimCount; i++)
                this.collprims.add(null);

            this.splines.clear();
            for (int i = 0; i < splineCount; i++)
                this.splines.add(null);
        }

        @Override
        public void save(DataWriter writer) {
            this.tempColPrimIdPointer = writer.writeNullPointer();
            writer.writeNullPointer(); // Seems to be set to null during map load.
            this.tempSplinePointer = writer.writeNullPointer();
            writer.writeUnsignedByte((short) this.collprims.size());
            writer.writeUnsignedByte((short) this.splines.size());
            writer.writeUnsignedShort(0); // Skip unknown value.

            // 2 Runtime pointer fields.
            writer.writeNullPointer();
            writer.writeNullPointer();
        }

        @Override
        public ILogger getLogger() {
            return new AppendInfoLoggerWrapper(this.gridPacket.getLogger(), getClass().getSimpleName() + "[x=" + getGridX() + ",y=" + getGridY() + "]", AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
        }

        private void loadColPrims(DataReader reader) {
            if (this.tempColPrimIdPointer < 0) // This can be 0/empty.
                throw new RuntimeException("Cannot read colPrim id list, the pointer " + NumberUtils.toHexString(this.tempColPrimIdPointer) + " is invalid.");

            if (this.tempColPrimIdPointer == 0 && this.collprims.isEmpty()) { // This can be zero/empty.
                this.tempColPrimIdPointer = -1;
                return; // Empty.
            }

            requireReaderIndex(reader, this.tempColPrimIdPointer, "Expected colPrim id list");
            this.tempColPrimIdPointer = -1;

            MediEvilMapCollprimsPacket collprimsPacket = getMap().getCollprimsPacket();
            for (int i = 0; i < this.collprims.size(); i++) {
                int colPrimId = reader.readUnsignedShortAsInt();
                if (colPrimId >= collprimsPacket.getCollprims().size()) {
                    getLogger().severe("Skipping reference to invalid colPrim (ID: %d)!", colPrimId);
                    this.collprims.remove(i--);
                    continue;
                }

                this.collprims.set(i, collprimsPacket.getCollprims().get(colPrimId));
            }
        }

        private void saveColPrims(DataWriter writer) {
            if (this.tempColPrimIdPointer <= 0)
                throw new RuntimeException("Cannot writer colPrim id list, the pointer " + NumberUtils.toHexString(this.tempColPrimIdPointer) + " is invalid.");

            if (this.collprims.isEmpty()) { // This can be zero/empty.
                writer.writeIntAtPos(this.tempColPrimIdPointer, 0);
                this.tempColPrimIdPointer = -1;
                return;
            }

            writer.writeAddressTo(this.tempColPrimIdPointer);
            this.tempColPrimIdPointer = -1;

            MediEvilMapCollprimsPacket collprimsPacket = getMap().getCollprimsPacket();
            for (int i = 0; i < this.collprims.size(); i++) {
                MediEvilMapCollprim colPrim = this.collprims.get(i);
                int colPrimIndex = collprimsPacket.getCollprims().indexOf(colPrim);
                if (colPrimIndex < 0)
                    throw new IllegalArgumentException("A MediEvilMapCollprim was referenced by a grid square, but wasn't actually present in the saved level data!");

                writer.writeUnsignedShort(colPrimIndex);
            }
        }

        private void loadSplines(DataReader reader) {
            if (this.tempSplinePointer == 0 && this.splines.isEmpty()) {
                this.tempSplinePointer = -1;
                return; // Empty.
            }

            if (this.tempSplinePointer <= 0)
                throw new RuntimeException("Cannot read spline id list, the pointer " + NumberUtils.toHexString(this.tempSplinePointer) + " is invalid.");

            requireReaderIndex(reader, this.tempSplinePointer, "Expected spline id list");
            this.tempSplinePointer = -1;

            List<MediEvilMap2DSpline> splines = this.gridPacket.getParentFile().getSpline2DPacket().getSplines();
            for (int i = 0; i < this.splines.size(); i++) {
                int splineIndex = reader.readUnsignedByteAsShort();
                if (splineIndex >= splines.size())
                    throw new IllegalArgumentException("Invalid splineIndex: " + splineIndex);

                this.splines.set(i, splines.get(splineIndex));
            }
        }

        private void saveSplines(DataWriter writer) {
            if (this.tempSplinePointer <= 0)
                throw new RuntimeException("Cannot writer spline id list, the pointer " + NumberUtils.toHexString(this.tempSplinePointer) + " is invalid.");

            if (this.splines.isEmpty()) {
                writer.writeIntAtPos(this.tempSplinePointer, 0);
                this.tempSplinePointer = -1;
                return;
            }

            writer.writeAddressTo(this.tempSplinePointer);
            this.tempSplinePointer = -1;

            for (int i = 0; i < this.splines.size(); i++)
                writer.writeUnsignedByte((short) this.splines.get(i).getId());
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            propertyList.addString("Grid Position", "(" + getGridX() + ", " + getGridY() + ")");
            propertyList.addInteger("Collprims", this.collprims.size());
            propertyList.addInteger("2D Splines", this.splines.size());
        }
    }
}
