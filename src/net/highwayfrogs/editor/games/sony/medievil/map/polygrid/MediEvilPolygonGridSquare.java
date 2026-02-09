package net.highwayfrogs.editor.games.sony.medievil.map.polygrid;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
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
 * Implements a polygon grid square for MediEvil.
 * Note that this is separate from the grid included in the map data.
 * Created by Kneesnap on 2/5/2026.
 */
public class MediEvilPolygonGridSquare extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator {
    @Getter private final MediEvilPolygonGridFile grid;
    @Getter private final int squareIndex;
    @Getter private final List<MediEvilMapPolygon> polygons = new ArrayList<>();

    private int tempPolygonIdPointer = -1;

    private static final int RUNTIME_RESERVED_FLAG = Constants.BIT_FLAG_15;

    public MediEvilPolygonGridSquare(MediEvilPolygonGridFile gridFile, int squareIndex) {
        super(gridFile.getGameInstance());
        this.grid = gridFile;
        this.squareIndex = squareIndex;
    }

    /**
     * Gets the map file.
     */
    public MediEvilMapFile getMap() {
        return this.grid.getMapFile();
    }

    /**
     * Gets the x grid coordinate of this entry.
     */
    public int getGridX() {
        return this.squareIndex % this.grid.getGridSize();
    }

    /**
     * Gets the y grid coordinate of this entry.
     */
    public int getGridY() {
        return this.squareIndex / this.grid.getGridSize();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{x=" + getGridX() + ",y=" + getGridY() + ",polygons=" + this.polygons.size() + "}";
    }

    @Override
    public void load(DataReader reader) {
        short polygonCount = reader.readShort();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE);
        this.tempPolygonIdPointer = reader.readInt();
        if ((polygonCount & RUNTIME_RESERVED_FLAG) == RUNTIME_RESERVED_FLAG)
            throw new RuntimeException("The polygon count was " + polygonCount + ", which had a reserved bit flag set.");

        // Prepare lists.
        this.polygons.clear();
        for (int i = 0; i < polygonCount; i++)
            this.polygons.add(null);
    }

    @Override
    public void save(DataWriter writer) {
        int polygonCount = this.polygons.size();
        if ((polygonCount & RUNTIME_RESERVED_FLAG) == RUNTIME_RESERVED_FLAG)
            throw new RuntimeException("The polygon count was " + polygonCount + ", which had a reserved bit flag set.");

        writer.writeUnsignedShort(polygonCount);
        writer.writeUnsignedShort(0);
        this.tempPolygonIdPointer = writer.writeNullPointer();
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.grid.getLogger(), getClass().getSimpleName() + "[x=" + getGridX() + ",y=" + getGridY() + "]", AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    /**
     * Loads polygons by their IDs from the reader.
     * @param reader the reader to read polygon IDs from
     */
    void loadPolygons(DataReader reader) {
        if (this.tempPolygonIdPointer <= 0) // This cannot be 0/empty.
            throw new RuntimeException("Cannot read polygon id list, the pointer " + NumberUtils.toHexString(this.tempPolygonIdPointer) + " is invalid.");

        requireReaderIndex(reader, this.tempPolygonIdPointer, "Expected polygon id list");
        this.tempPolygonIdPointer = -1;

        List<MediEvilMapPolygon> polygons = getMap().getGraphicsPacket().getPolygons();
        for (int i = 0; i < this.polygons.size(); i++) {
            int polygonId = reader.readUnsignedShortAsInt();
            if (polygonId >= polygons.size()) {
                getLogger().severe("Skipping reference to invalid polygon (ID: %d)!", polygonId);
                this.polygons.remove(i--);
                continue;
            }

            this.polygons.set(i, polygons.get(polygonId));
        }
    }

    /**
     * Saves polygon IDs to the writer.
     * @param writer the writer to write polygon IDs to
     */
    void savePolygons(DataWriter writer) {
        if (this.tempPolygonIdPointer <= 0)
            throw new RuntimeException("Cannot writer polygon id list, the pointer " + NumberUtils.toHexString(this.tempPolygonIdPointer) + " is invalid.");
        if (this.polygons.isEmpty())
            throw new IllegalStateException("Tried to save a grid square with zero polygons. (Shouldn't this just be null?)");

        writer.writeAddressTo(this.tempPolygonIdPointer);
        this.tempPolygonIdPointer = -1;

        List<MediEvilMapPolygon> polygons = getMap().getGraphicsPacket().getPolygons();
        for (int i = 0; i < this.polygons.size(); i++) {
            MediEvilMapPolygon polygon = this.polygons.get(i);
            int polygonIndex = polygons.indexOf(polygon);
            if (polygonIndex < 0)
                throw new IllegalArgumentException("A MediEvilMapPolygon was referenced by a grid square, but wasn't actually present in the saved level data!");

            writer.writeUnsignedShort(polygonIndex);
        }
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString("Grid Position", "(" + getGridX() + ", " + getGridY() + ")");
        propertyList.addInteger("Polygons", this.polygons.size());
    }
}
