package net.highwayfrogs.editor.games.sony.medievil.map.packet.grid;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapCollprim;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMap2DSplinePacket.MediEvilMap2DSpline;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapCollprimsPacket;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single grid square in the map 'GRID' packet.
 * Created by Kneesnap on 2/8/2026.
 */
public class MediEvilMapGridSquare extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator {
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
     * Gets the x grid coordinate of this entry, as a 12.4 fixed point value.
     */
    public int getGridX() {
        return this.squareIndex % this.gridPacket.getGridSize();
    }

    /**
     * Gets the world Z coordinate where this grid square ends, as a 12.4 fixed point integer.
     */
    public int getStartWorldX() {
        return ((getGridX() * this.gridPacket.getGridSquareSize()) - MediEvilMapGridPacket.GRID_WORLD_CENTER_OFFSET);
    }

    /**
     * Gets the y grid coordinate of this entry, as a 12.4 fixed point value.
     */
    public int getGridY() {
        return this.squareIndex / this.gridPacket.getGridSize();
    }

    /**
     * Gets the world Z coordinate where this grid square begins, as a 12.4 fixed point integer.
     * The world Z coordinate is based on the Y grid coordinate. (2D coordinate system vs 3D coordinate system)
     */
    public int getStartWorldZ() {
        return ((getGridY() * this.gridPacket.getGridSquareSize()) - MediEvilMapGridPacket.GRID_WORLD_CENTER_OFFSET);
    }

    /**
     * Gets the index used to identify this entry while the entry is in a serialized state.
     * This value is only valid during data save, as this value may change as with the grid.
     */
    public int calculateStorageIndex() {
        List<MediEvilMapGridSquare> gridSquares = this.gridPacket.getGridSquares();
        int storageIndex = gridSquares.indexOf(this);
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

    void loadColPrims(DataReader reader) {
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

    void saveColPrims(DataWriter writer) {
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

    void loadSplines(DataReader reader) {
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

    void saveSplines(DataWriter writer) {
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
