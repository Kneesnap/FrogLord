package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapCollprim;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMap2DSplinePacket.MediEvilMap2DSpline;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements the GRID packet.
 * TODO: On collprim removed from collprims packet, remove it from all grid squares. (Unless the grid is auto-generated on save)
 * TODO: On spline removed from spline packet, remove it from all grid squares. (Unless the grid is auto-generated on save)
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMapGridPacket extends MediEvilMapPacket {
    private byte gridXSquareCount;
    private byte gridYSquareCount;
    private int gridSquareSize;
    private short gridShift; // Grid size shift
    private MediEvilMapGridSquareDataEntry[] gridSquares = EMPTY_GRID_SQUARE_ARRAY;

    public static final String IDENTIFIER = "DIRG"; // 'GRID'.
    private static final MediEvilMapGridSquareDataEntry[] EMPTY_GRID_SQUARE_ARRAY = new MediEvilMapGridSquareDataEntry[0];

    public MediEvilMapGridPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
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
        this.gridSquares = new MediEvilMapGridSquareDataEntry[(this.gridYSquareCount & 0xFF) * (this.gridXSquareCount & 0xFF)];
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
        reader.requireIndex(getLogger(), gridDataBasePtr, "Expected Grid Square entries");
        List<MediEvilMapGridSquareDataEntry> gridSquares = new ArrayList<>();
        for (int i = 0; i < gridSquareTableIndices.size(); i++) {
            MediEvilMapGridSquareDataEntry newGridSquare = new MediEvilMapGridSquareDataEntry(this);
            this.gridSquares[gridSquareTableIndices.get(i)] = newGridSquare;
            gridSquares.add(newGridSquare);
            newGridSquare.load(reader);
        }

        // Read grid square data.
        for (int i = 0; i < gridSquares.size(); i++)
            gridSquares.get(i).loadColPrims(reader);
        for (int i = 0; i < gridSquares.size(); i++)
            gridSquares.get(i).loadSplines(reader);
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
        List<MediEvilMapGridSquareDataEntry> gridSquares = new ArrayList<>();
        for (int i = 0; i < this.gridSquares.length; i++) {
            MediEvilMapGridSquareDataEntry gridSquare = this.gridSquares[i];
            if (gridSquare != null) {
                writer.writeUnsignedShort(gridSquares.size());
                gridSquares.add(gridSquare);
            } else {
                writer.writeShort((short) -1);
            }
        }

        // Write grid squares.
        writer.writeAddressTo(gridDataBasePtr);
        for (int i = 0; i < gridSquares.size(); i++)
            gridSquares.get(i).save(writer);
        for (int i = 0; i < gridSquares.size(); i++)
            gridSquares.get(i).saveColPrims(writer);
        for (int i = 0; i < gridSquares.size(); i++)
            gridSquares.get(i).saveSplines(writer);
    }

    @Override
    public void clear() {
        Arrays.fill(this.gridSquares, null);
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

    public static class MediEvilMapGridSquareDataEntry extends SCGameData<MediEvilGameInstance> {
        @Getter private final MediEvilMapGridPacket gridPacket;
        @Getter private final List<MediEvilMapCollprim> collprims = new ArrayList<>();
        @Getter private final List<MediEvilMap2DSpline> splines = new ArrayList<>();
        private int polygonCount; // This value seems to be the number of polygons in the grid square, but the game does not seem to use this value for anything.

        private int tempColPrimIdPointer = -1;
        private int tempSplinePointer = -1;

        public MediEvilMapGridSquareDataEntry(MediEvilMapGridPacket gridPacket) {
            super(gridPacket.getGameInstance());
            this.gridPacket = gridPacket;
        }

        @Override
        public void load(DataReader reader) {
            this.tempColPrimIdPointer = reader.readInt();
            reader.skipBytesRequireEmpty(Constants.POINTER_SIZE); // Seems to be set to null during map load.
            this.tempSplinePointer = reader.readInt();
            short colPrimCount = reader.readUnsignedByteAsShort();
            short splineCount = reader.readUnsignedByteAsShort();
            this.polygonCount = reader.readUnsignedShortAsInt();

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
            writer.writeUnsignedShort(this.polygonCount);

            // 2 Runtime pointer fields.
            writer.writeNullPointer();
            writer.writeNullPointer();
        }

        @Override
        public ILogger getLogger() {
            return new AppendInfoLoggerWrapper(this.gridPacket.getLogger(), getClass().getSimpleName(), AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
        }

        /**
         * Gets the map file.
         */
        public MediEvilMapFile getMap() {
            return this.gridPacket.getParentFile();
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
    }
}
