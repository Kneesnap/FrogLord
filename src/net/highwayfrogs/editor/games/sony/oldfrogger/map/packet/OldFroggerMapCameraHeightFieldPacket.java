package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerCameraHeightFieldManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

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

    public OldFroggerMapCameraHeightFieldPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
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
        // TODO: Editing these should update the 3D display.
        editor.addShortField("X Square Count", this.xSquareCount, newValue -> this.xSquareCount = newValue, value -> value > 0);
        editor.addShortField("Z Square Count", this.zSquareCount, newValue -> this.zSquareCount = newValue, value -> value > 0);
        editor.addFixedShort("X Square Size", this.xSquareSize, newValue -> this.xSquareSize = newValue, 256, 1, Short.MAX_VALUE);
        editor.addFixedShort("Z Square Size", this.zSquareSize, newValue -> this.zSquareSize = newValue, 256, 1, Short.MAX_VALUE);
        editor.addFixedShort("Start Grid X", this.startPositionX, newValue -> this.startPositionX = newValue);
        editor.addFixedShort("Start Grid Z", this.startPositionZ, newValue -> this.startPositionZ = newValue);
    }
}