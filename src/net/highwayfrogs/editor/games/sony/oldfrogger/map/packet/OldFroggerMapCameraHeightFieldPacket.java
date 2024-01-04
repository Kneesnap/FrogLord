package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerCameraHeightFieldManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;

/**
 * A field which stores a camera height-field.
 * Created by Kneesnap on 12/12/2023.
 */
@Getter
public class OldFroggerMapCameraHeightFieldPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "CMHT";
    private short xSquareCount;
    private short zSquareCount;
    private short xSquareSize;
    private short zSquareSize;
    private short startPositionX;
    private short startPositionZ;
    private short[][] heightMap;

    private static final short DEFAULT_HEIGHT = Utils.floatToFixedPointShort4Bit(-100F);

    public OldFroggerMapCameraHeightFieldPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int heightFieldEntryCount = reader.readInt();
        this.xSquareCount = reader.readShort();
        this.zSquareCount = reader.readShort();
        this.xSquareSize = reader.readShort();
        this.zSquareSize = reader.readShort();
        this.startPositionX = reader.readShort();
        this.startPositionZ = reader.readShort();

        if (heightFieldEntryCount != (this.xSquareCount * this.zSquareCount))
            System.out.println("WARNING: HeightField EntryCount was " + heightFieldEntryCount + " bytes with dimensions: [" + this.xSquareCount + ", " + this.zSquareCount + "]");

        // Read height map.
        if (this.xSquareCount > 0 && this.zSquareCount > 0) {
            this.heightMap = new short[this.zSquareCount][this.xSquareCount];
            for (int z = 0; z < this.zSquareCount; z++)
                for (int x = 0; x < this.xSquareCount; x++)
                    this.heightMap[z][x] = reader.readShort();
        } else {
            // TODO: Why dis happen sometimes.
            this.heightMap = null;
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.xSquareCount * this.zSquareCount);
        writer.writeShort(this.xSquareCount);
        writer.writeShort(this.zSquareCount);
        writer.writeShort(this.xSquareSize);
        writer.writeShort(this.zSquareSize);
        writer.writeShort(this.startPositionX);
        writer.writeShort(this.startPositionZ);

        // Write height map
        for (int z = 0; z < this.zSquareCount; z++)
            for (int x = 0; x < this.xSquareCount; x++)
                writer.writeShort(this.heightMap[z][x]);
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getCameraHeightFieldChunkAddress() : -1;
    }

    /**
     * Setup the UI for the height-field data.
     * @param manager The UI manager
     * @param editor  The editing context to build upon
     */
    public void setupEditor(OldFroggerCameraHeightFieldManager manager, GUIEditorGrid editor) {
        // Square Counts
        editor.addShortField("X Square Count", this.xSquareCount, newValue -> {
            int oldX = this.xSquareCount;
            int oldZ = this.zSquareCount;
            setXSquareCount(newValue);
            manager.onGridSizeChange(oldX, oldZ, newValue, oldZ);
        }, value -> value > 0);
        editor.addShortField("Z Square Count", this.zSquareCount, newValue -> {
            int oldX = this.xSquareCount;
            int oldZ = this.zSquareCount;
            setZSquareCount(newValue);
            manager.onGridSizeChange(oldX, oldZ, oldX, newValue);
        }, value -> value > 0);

        // Square Size:
        editor.addFixedShort("X Square Size", this.xSquareSize, newValue -> {
            this.xSquareSize = newValue;
            manager.updateVertexPositions();
        }, 256, 1, Short.MAX_VALUE);
        editor.addFixedShort("Z Square Size", this.zSquareSize, newValue -> {
            this.zSquareSize = newValue;
            manager.updateVertexPositions();
        }, 256, 1, Short.MAX_VALUE);

        // Start Position:
        editor.addFixedShort("Start Grid X", this.startPositionX, newValue -> {
            this.startPositionX = newValue;
            manager.updateVertexPositions();
        });
        editor.addFixedShort("Start Grid Z", this.startPositionZ, newValue -> {
            this.startPositionZ = newValue;
            manager.updateVertexPositions();
        });
    }

    /**
     * Gets the starting X position which the height grid starts at as a float.
     */
    public float getStartXAsFloat() {
        return Utils.fixedPointIntToFloat4Bit(this.startPositionX);
    }

    /**
     * Gets the starting Z position which the height grid starts at as a float.
     */
    public float getStartZAsFloat() {
        return Utils.fixedPointIntToFloat4Bit(this.startPositionZ);
    }

    /**
     * Gets the length (X axis) of a camera grid square as a float.
     */
    public float getSquareXSizeAsFloat() {
        return Utils.fixedPointShortToFloatNBits(this.xSquareSize, 8);
    }

    /**
     * Gets the length (Z axis) of a camera grid square as a float.
     */
    public float getSquareZSizeAsFloat() {
        return Utils.fixedPointShortToFloatNBits(this.zSquareSize, 8);
    }

    /**
     * Get the world X position from the z grid position.
     * @param gridX the x coordinate in the camera height grid
     * @return worldX
     */
    public float getWorldX(int gridX) {
        if (gridX < 0 || gridX >= this.xSquareCount)
            throw new IndexOutOfBoundsException("The provided grid X coordinate (" + this.xSquareCount + ") was outside of the range [0, " + gridX + ").");

        return Utils.fixedPointIntToFloat4Bit(this.startPositionX + (gridX * this.xSquareSize));
    }

    /**
     * Get the world Y position from a position in the camera height grid.
     * @param gridX the X coordinate in the camera height grid
     * @param gridZ the Z coordinate in the camera height grid
     * @return worldY
     */
    public float getWorldY(int gridX, int gridZ) {
        if (gridX < 0 || gridX >= this.xSquareCount)
            throw new IndexOutOfBoundsException("The provided grid X coordinate (" + this.xSquareCount + ") was outside of the range [0, " + gridX + ").");
        if (gridZ < 0 || gridZ >= this.zSquareCount)
            throw new IndexOutOfBoundsException("The provided grid Z coordinate (" + this.zSquareCount + ") was outside of the range [0, " + gridZ + ").");

        return Utils.fixedPointIntToFloat4Bit(this.heightMap[gridZ][gridX]);
    }

    /**
     * Get the world Z position from the z grid position.
     * @param gridZ the z coordinate in the camera height grid
     * @return worldZ
     */
    public float getWorldZ(int gridZ) {
        if (gridZ < 0 || gridZ >= this.zSquareCount)
            throw new IndexOutOfBoundsException("The provided grid Z coordinate (" + this.zSquareCount + ") was outside of the range [0, " + gridZ + ").");

        return Utils.fixedPointIntToFloat4Bit(this.startPositionZ + (gridZ * this.zSquareSize));
    }

    /**
     * Sets the dimensions of the height-grid, preserving the previous height-grid data when possible.
     * @param newXSquareCount number of squares in the x direction
     */
    public void setXSquareCount(short newXSquareCount) {
        setSquareCount(newXSquareCount, this.zSquareCount);
    }

    /**
     * Sets the dimensions of the height-grid, preserving the previous height-grid data when possible.
     * @param newZSquareCount number of squares in the z direction
     */
    public void setZSquareCount(short newZSquareCount) {
        setSquareCount(this.xSquareCount, newZSquareCount);
    }

    /**
     * Sets the dimensions of the height-grid, preserving the previous height-grid data when possible.
     * @param newXSquareCount number of squares in the x direction
     * @param newZSquareCount number of squares in the z direction
     */
    public void setSquareCount(short newXSquareCount, short newZSquareCount) {
        if (newXSquareCount < 0)
            throw new IllegalArgumentException("The x count cannot be less than zero! (Provided: " + newXSquareCount + ")");
        if (newZSquareCount < 0)
            throw new IllegalArgumentException("The z count cannot be less than zero! (Provided: " + newZSquareCount + ")");

        short oldXSquareCount = this.xSquareCount;
        short oldZSquareCount = this.zSquareCount;
        if (oldXSquareCount == newXSquareCount && oldZSquareCount == newZSquareCount)
            return; // There's no difference between the old & new sizes.

        short[][] newHeightMap = new short[newZSquareCount][newXSquareCount];
        if (newXSquareCount != 0 && newZSquareCount != 0) {
            // Copy old height-data to the new one.
            int copyWidth = Math.min(oldXSquareCount, newXSquareCount);
            int copyHeight = Math.min(oldZSquareCount, newZSquareCount);
            for (int z = 0; z < copyHeight; z++)
                System.arraycopy(this.heightMap[z], 0, newHeightMap[z], 0, copyWidth);

            // expanded to unset values in the positive X direction.
            for (int z = 0; z < copyHeight; z++) {
                short expandValue = (copyWidth > 0) ? this.heightMap[z][copyWidth - 1] : DEFAULT_HEIGHT;
                Arrays.fill(newHeightMap[z], copyWidth, newHeightMap[z].length, expandValue);
            }

            // expanded to unset grid data in the positive Z direction.
            for (int z = copyHeight; z < newZSquareCount; z++) {
                if (z > 0) {
                    System.arraycopy(newHeightMap[z - 1], 0, newHeightMap[z], 0, newXSquareCount);
                } else {
                    Arrays.fill(newHeightMap[z], DEFAULT_HEIGHT);
                }
            }
        }

        this.xSquareCount = newXSquareCount;
        this.zSquareCount = newZSquareCount;
        this.heightMap = newHeightMap;
    }
}