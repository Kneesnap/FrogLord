package net.highwayfrogs.editor.games.sony.shared.landscaper;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.lang.reflect.Array;

/**
 * Represents a grid split up into uniform squares mapped into world coordinate space.
 * Created by Kneesnap on 7/15/2024.
 */
public abstract class SCWorldGrid<TGridEntry extends IBinarySerializable> extends SCSharedGameData {
    private final Class<? extends TGridEntry> gridEntryClass;
    @Getter private final Vector3f originPosition = new Vector3f();
    @Getter private int xSquareCount;
    @Getter private int zSquareCount;
    @Getter private float xSquareSize;
    @Getter private float zSquareSize;
    protected TGridEntry[][] internalGrid;

    protected SCWorldGrid(SCGameInstance instance, Class<? extends TGridEntry> entryClass, int xSquareCount, int zSquareCount) {
        this(instance, entryClass, xSquareCount, zSquareCount, 1F, 1F);
    }

    protected SCWorldGrid(SCGameInstance instance, Class<? extends TGridEntry> entryClass, int xSquareCount, int zSquareCount, float xSquareSize, float zSquareSize) {
        super(instance);
        this.gridEntryClass = entryClass;
        resize(xSquareCount, zSquareCount);
        this.xSquareSize = xSquareSize;
        this.zSquareSize = zSquareSize;
    }

    protected SCWorldGrid(SCGameInstance instance, Class<? extends TGridEntry> entryClass) {
        super(instance);
        this.gridEntryClass = entryClass;
    }

    @Override
    public void load(DataReader reader) {
        this.originPosition.load(reader);
        int xSquareCount = reader.readUnsignedShortAsInt();
        int zSquareCount = reader.readUnsignedShortAsInt();
        resize(xSquareCount, zSquareCount);
        this.xSquareSize = reader.readFloat();
        this.zSquareSize = reader.readFloat();

        // Read array describing which entries have data.
        IndexBitArray bitArray = new IndexBitArray();
        bitArray.load(reader);

        int bitIndex = 0;
        for (int z = 0; z < this.zSquareCount; z++) {
            for (int x = 0; x < this.xSquareCount; x++) {
                if (!bitArray.getBit(bitIndex++)) { // No grid entry exists here.
                    this.internalGrid[z][x] = null;
                    continue;
                }

                this.internalGrid[z][x] = createNewGridEntry(x, z);
            }
        }
    }

    /**
     * Read grid entry data from the reader's current position.
     * @param reader the reader to read the data from
     */
    public void readGridDataEntries(DataReader reader) {
        readDependantGridData(reader);
        for (int z = 0; z < this.zSquareCount; z++)
            for (int x = 0; x < this.xSquareCount; x++)
                if (this.internalGrid[z][x] != null)
                    this.internalGrid[z][x].load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.originPosition.save(writer);
        writer.writeUnsignedShort(this.xSquareCount);
        writer.writeUnsignedShort(this.zSquareCount);
        writer.writeFloat(this.xSquareSize);
        writer.writeFloat(this.zSquareSize);

        // Write array showing which entries have values.
        IndexBitArray bitArray = new IndexBitArray(this.xSquareCount * this.zSquareCount);
        for (int z = 0; z < this.zSquareCount; z++)
            for (int x = 0; x < this.xSquareCount; x++)
                if (this.internalGrid[z][x] != null)
                    bitArray.setBit((z * this.xSquareCount) + x, true);
        bitArray.save(writer);
    }

    /**
     * Write grid entry data to the writer's current position.
     * @param writer the writer to write the grid data to
     */
    public void writeGridDataEntries(DataWriter writer) {
        writeDependantGridData(writer);
        for (int z = 0; z < this.zSquareCount; z++)
            for (int x = 0; x < this.xSquareCount; x++)
                if (this.internalGrid[z][x] != null)
                    this.internalGrid[z][x].save(writer);
    }

    /**
     * Resizes the height-field grid to the new provided dimensions.
     * @param newX the new x square count
     * @param newZ the new z square count
     */
    @SuppressWarnings("unchecked")
    public void resize(int newX, int newZ) {
        if (newX < 0 || newX > getMaxGridDimensions())
            throw new IllegalArgumentException("Invalid x dimension: " + newX + " was expected to be within [0, " + getMaxGridDimensions() + ").");
        if (newZ < 0 || newZ > getMaxGridDimensions())
            throw new IllegalArgumentException("Invalid z dimension: " + newZ + " was expected to be within [0, " + getMaxGridDimensions() + ").");

        if (newX == this.xSquareCount && newZ == this.zSquareCount)
            return; // The new size is the same as the old size.

        // Create new grid, and try to populate it using the old contents.
        TGridEntry[][] newGrid = (TGridEntry[][]) Array.newInstance(this.gridEntryClass, newZ, newX);
        if (this.internalGrid != null) {
            int xCopy = Math.min(newX, this.xSquareCount);
            int zCopy = Math.min(newZ, this.zSquareCount);
            for (int z = 0; z < zCopy; z++)
                System.arraycopy(this.internalGrid[z], 0, newGrid[z], 0, xCopy);
        }

        // Fill empty squares with objects.
        for (int z = 0; z < newGrid.length; z++) {
            TGridEntry[] gridRow = newGrid[z];
            for (int x = 0; x < gridRow.length; x++)
                gridRow[x] = createNewGridEntry(x, z);
        }

        this.xSquareCount = (short) newX;
        this.zSquareCount = (short) newZ;
        this.internalGrid = newGrid;
    }

    /**
     * Gets the grid entry at a given grid position.
     * @param gridX the x grid position
     * @param gridZ the z grid position
     * @return the entry found at the given position
     */
    public TGridEntry getGridEntry(int gridX, int gridZ) {
        if (gridX < 0 || gridX >= this.xSquareCount)
            throw new ArrayIndexOutOfBoundsException("gridX must be within [0, " + this.xSquareCount + "), but was: " + gridX + ".");
        if (gridZ < 0 || gridZ >= this.zSquareCount)
            throw new ArrayIndexOutOfBoundsException("gridZ must be within [0, " + this.zSquareCount + "), but was: " + gridZ + ".");

        return this.internalGrid[gridZ][gridX];
    }

    /**
     * Sets the grid entry at a given grid position.
     * @param gridX the x grid position
     * @param gridZ the z grid position
     * @param newEntry the entry to apply
     * @return the entry which was previously found at the given position
     */
    public TGridEntry setGridEntry(int gridX, int gridZ, TGridEntry newEntry) {
        if (gridX < 0 || gridX >= this.xSquareCount)
            throw new ArrayIndexOutOfBoundsException("gridX must be within [0, " + this.xSquareCount + "), but was: " + gridX + ".");
        if (gridZ < 0 || gridZ >= this.zSquareCount)
            throw new ArrayIndexOutOfBoundsException("gridZ must be within [0, " + this.zSquareCount + "), but was: " + gridZ + ".");

        TGridEntry oldEntry = this.internalGrid[gridZ][gridX];
        this.internalGrid[gridZ][gridX] = newEntry;
        return oldEntry;
    }

    /**
     * Gets the (inclusive) maximum number of grid squares in either direction.
     * @return maxGridDimensions
     */
    public int getMaxGridDimensions() {
        return 0xFF; // Default!
    }

    /**
     * Creates a new grid entry for the given x z position(s).
     * @param x the x position
     * @param z the z position
     * @return newGridEntry
     */
    protected abstract TGridEntry createNewGridEntry(int x, int z);

    /**
     * Reads extra data tracked in this grid.
     * Dependant data is data required by grid entries, and is therefore read before the grid entries.
     * @param reader the reader to read data from
     */
    protected abstract void readDependantGridData(DataReader reader);

    /**
     * Writes dependant grid data.
     * Dependant data is data required by grid entries, and is therefore written before the grid entries.
     * @param writer the writer to write the data to
     */
    protected abstract void writeDependantGridData(DataWriter writer);
}
