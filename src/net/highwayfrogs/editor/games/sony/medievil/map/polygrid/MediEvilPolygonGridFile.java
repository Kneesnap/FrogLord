package net.highwayfrogs.editor.games.sony.medievil.map.polygrid;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the MediEvil .PGD files (Polygon grids).
 * Created by Kneesnap on 2/5/2026.
 */
public class MediEvilPolygonGridFile extends SCGameFile<MediEvilGameInstance> {
    private byte gridXSquareCount;
    private byte gridYSquareCount;
    private int gridSquareSize;
    private short gridShift;
    private MWIResourceEntry mapFileEntry;
    private MediEvilPolygonGridSquare[] gridSquaresByPosition = EMPTY_GRID_SQUARE_ARRAY;
    private final List<MediEvilPolygonGridSquare> gridSquares = new ArrayList<>();
    private final List<MediEvilPolygonGridSquare> immutableGridSquares = Collections.unmodifiableList(this.gridSquares);

    private static final MediEvilPolygonGridSquare[] EMPTY_GRID_SQUARE_ARRAY = new MediEvilPolygonGridSquare[0];
    public static final int GRID_WORLD_CENTER_OFFSET = 32768;

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
     * Gets the grid square index containing the given X/Z world position.
     * @param x the world x coordinate
     * @param z the world z coordinate
     * @return gridSquarePositionIndex
     */
    public int getGridSquareIndexFromWorldPosition(int x, int z) {
        return (((z + GRID_WORLD_CENTER_OFFSET) >> this.gridShift) * this.gridXSquareCount)
                + ((x + GRID_WORLD_CENTER_OFFSET) >> this.gridShift);
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

    @Override
    public void load(DataReader reader) {
        this.gridXSquareCount = reader.readByte();
        this.gridYSquareCount = reader.readByte();
        this.gridSquareSize = reader.readUnsignedShortAsInt();
        this.gridShift = reader.readUnsignedByteAsShort();
        reader.alignRequireEmpty(Constants.INTEGER_SIZE);

        int gridIdTablePtr = reader.readInt();
        int gridDataBasePtr = reader.readInt();

        // Read grid ID table.
        reader.requireIndex(getLogger(), gridIdTablePtr, "Expected Grid ID Table");
        IntList gridSquareTableIndices = new IntList();
        this.gridSquaresByPosition = new MediEvilPolygonGridSquare[(this.gridYSquareCount & 0xFF) * (this.gridXSquareCount & 0xFF)];
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
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.gridXSquareCount);
        writer.writeByte(this.gridYSquareCount);
        writer.writeUnsignedShort(this.gridSquareSize);
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
        propertyList.add("Grid Dimensions", getGridXSquareCount() + "x" + getGridYSquareCount());
        propertyList.add("Grid Square Size", this.gridSquareSize);
        propertyList.add("Grid Shift", this.gridShift);
        propertyList.addString(this::addGridSquares, "Grid Squares", String.valueOf(this.gridSquares.size()));
    }

    private void addGridSquares(PropertyListNode propertyList) {
        for (int i = 0; i < this.gridSquares.size(); i++)
            propertyList.addProperties("GridSquare[" + i + "]", this.gridSquares.get(i));
    }
}
