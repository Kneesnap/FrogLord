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
 * TODO: Perhaps rename all 'y' occurrences to 'z'.
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMapGridPacket extends MediEvilMapPacket implements IPropertyListCreator {
    private byte gridXSquareCount;
    private byte gridYSquareCount;
    private int gridSquareSize;
    private short gridShift; // Grid size shift
    private MediEvilMapGridSquare[] gridSquares = EMPTY_GRID_SQUARE_ARRAY;
    private final List<MediEvilMapGridSquare> gridSquareEntries = new ArrayList<>();
    private final List<MediEvilMapGridSquare> immutableGridSquareEntries = Collections.unmodifiableList(this.gridSquareEntries);

    public static final String IDENTIFIER = "DIRG"; // 'GRID'.
    private static final MediEvilMapGridSquare[] EMPTY_GRID_SQUARE_ARRAY = new MediEvilMapGridSquare[0];

    public MediEvilMapGridPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    /**
     * Get the number of grid squares currently active within the grid packet.
     */
    public List<MediEvilMapGridSquare> getGridSquares() {
        return this.immutableGridSquareEntries;
    }

    /**
     * Gets the grid length on the x-axis.
     */
    public int getGridXSquareCount() {
        return (this.gridXSquareCount & 0xFF);
    }

    /**
     * Gets the grid length on the y-axis.
     */
    public int getGridYSquareCount() {
        return (this.gridYSquareCount & 0xFF);
    }

    /**
     * Gets the index used to identify this square while the entry is in a serialized state.
     * This value is only valid during data save, as this value may change as with the grid.
     */
    public MediEvilMapGridSquare getGridSquareByStorageIndex(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= this.gridSquareEntries.size())
            throw new IndexOutOfBoundsException("No grid square entry could be found by the storage index " + targetIndex + " (Max: " + (this.gridSquareEntries.size() - 1) + ")");

        return this.gridSquareEntries.get(targetIndex);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.gridXSquareCount = reader.readByte();
        this.gridYSquareCount = reader.readByte();
        this.gridSquareSize = reader.readUnsignedShortAsInt();
        this.gridShift = reader.readUnsignedByteAsShort();
        reader.alignRequireByte((byte) 0xFF, Constants.INTEGER_SIZE);
        int gridIdTablePtr = reader.readInt();
        int gridDataBasePtr = reader.readInt();

        // Read grid ID table.
        reader.requireIndex(getLogger(), gridIdTablePtr, "Expected Grid ID Table");
        IntList gridSquareTableIndices = new IntList();
        this.gridSquares = new MediEvilMapGridSquare[(this.gridYSquareCount & 0xFF) * (this.gridXSquareCount & 0xFF)];
        for (int i = 0; i < this.gridSquares.length; i++) {
            short id = reader.readShort();
            if (id == -1)
                continue;

            int gridSquareEntryIndex = (id & 0xFFFF);
            if (gridSquareEntryIndex != gridSquareTableIndices.size())
                throw new IllegalArgumentException("gridSquareEntryIndex was expected to be " + gridSquareTableIndices.size() + ", but was actually " + gridSquareEntryIndex + "!");

            gridSquareTableIndices.add(i);
        }

        // Read grid squares.
        this.gridSquareEntries.clear();
        reader.requireIndex(getLogger(), gridDataBasePtr, "Expected Grid Square entries");
        for (int i = 0; i < gridSquareTableIndices.size(); i++) {
            int squareIndex = gridSquareTableIndices.get(i);
            MediEvilMapGridSquare newGridSquare = new MediEvilMapGridSquare(this, squareIndex);
            this.gridSquares[squareIndex] = newGridSquare;
            this.gridSquareEntries.add(newGridSquare);
            newGridSquare.load(reader);
        }

        // Read grid square data.
        for (int i = 0; i < this.gridSquareEntries.size(); i++)
            this.gridSquareEntries.get(i).loadColPrims(reader);
        for (int i = 0; i < this.gridSquareEntries.size(); i++)
            this.gridSquareEntries.get(i).loadSplines(reader);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeByte(this.gridXSquareCount);
        writer.writeByte(this.gridYSquareCount);
        writer.writeUnsignedShort(this.gridSquareSize);
        writer.writeUnsignedByte(this.gridShift);
        writer.align(Constants.INTEGER_SIZE, (byte) 0xFF);
        int gridIdTablePtr = writer.writeNullPointer();
        int gridDataBasePtr = writer.writeNullPointer();

        // Write grid ID table.
        writer.writeAddressTo(gridIdTablePtr);
        int storageIndex = 0;
        for (int i = 0; i < this.gridSquares.length; i++) {
            MediEvilMapGridSquare gridSquare = this.gridSquares[i];
            if (gridSquare != null) {
                if (this.gridSquareEntries.get(i) != gridSquare)
                    throw new IllegalStateException("The gridSquareEntries list was ordered wrong. Got " + this.gridSquareEntries.get(i) + " at index " + i + ", when " + gridSquare + " was expected.");

                writer.writeUnsignedShort(storageIndex);
                storageIndex++;
            } else {
                writer.writeShort((short) -1);
            }
        }

        // Write grid squares.
        writer.writeAddressTo(gridDataBasePtr);
        for (int i = 0; i < this.gridSquareEntries.size(); i++)
            gridSquareEntries.get(i).save(writer);
        for (int i = 0; i < gridSquareEntries.size(); i++)
            gridSquareEntries.get(i).saveColPrims(writer);
        for (int i = 0; i < gridSquareEntries.size(); i++)
            gridSquareEntries.get(i).saveSplines(writer);
    }

    @Override
    public void clear() {
        Arrays.fill(this.gridSquares, null);
        this.gridSquareEntries.clear();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("Grid Dimensions", getGridXSquareCount() + "x" + getGridYSquareCount());
        propertyList.add("Grid Square Size", this.gridSquareSize);
        propertyList.add("Grid Shift", this.gridShift);
        propertyList.addString(this::addGridSquares, "Grid Squares", String.valueOf(this.gridSquareEntries.size()));
    }

    private void addGridSquares(PropertyListNode propertyList) {
        for (int i = 0; i < this.gridSquareEntries.size(); i++)
            propertyList.addProperties("GridSquare[" + i + "]", this.gridSquareEntries.get(i));
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
            return this.squareIndex % this.gridPacket.gridXSquareCount;
        }

        /**
         * Gets the z grid coordinate of this entry.
         */
        public int getGridZ() {
            return this.squareIndex / this.gridPacket.gridXSquareCount;
        }

        /**
         * Gets the index used to identify this entry while the entry is in a serialized state.
         * This value is only valid during data save, as this value may change as with the grid.
         */
        public int calculateStorageIndex() {
            int storageIndex = 0;
            for (int i = 0; i < this.gridPacket.gridSquares.length; i++) {
                MediEvilMapGridSquare gridSquare = this.gridPacket.gridSquares[i];
                if (gridSquare == null)
                    continue;

                if (gridSquare == this) {
                    return storageIndex;
                } else {
                    storageIndex++;
                }
            }

            throw new IllegalStateException("The square is not part of the grid, so it therefore has no storageIndex.");
        }

        @Override
        public String toString() {
            return "MediEvilMapGridSquare{x=" + getGridX() + ",z=" + getGridZ() + ",collprims=" + this.collprims.size()
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
            return new AppendInfoLoggerWrapper(this.gridPacket.getLogger(), getClass().getSimpleName() + "[x=" + getGridX() + ",z=" + getGridZ() + "]", AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
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
            propertyList.addString("Grid Position", "(" + getGridX() + ", " + getGridZ() + ")");
            propertyList.addInteger("Collprims", this.collprims.size());
            propertyList.addInteger("2D Splines", this.splines.size());
        }
    }
}
