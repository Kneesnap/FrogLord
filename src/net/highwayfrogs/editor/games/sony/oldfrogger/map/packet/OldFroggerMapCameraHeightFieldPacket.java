package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerCameraHeightFieldManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * A field which stores a camera heightfield.
 * Created by Kneesnap on 12/12/2023.
 */
@Getter
public class OldFroggerMapCameraHeightFieldPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "CMHT";
    private short xSize;
    private short zSize;
    private short unknown1; // TODO: !
    private short unknown2; // TODO: !
    private short unknown3; // TODO: !
    private short unknown4; // TODO: !
    private short[][] heightMap; // TODO: How do we use this properly?

    public OldFroggerMapCameraHeightFieldPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int heightFieldEntryCount = reader.readInt();
        this.xSize = reader.readShort();
        this.zSize = reader.readShort();
        this.unknown1 = reader.readShort();
        this.unknown2 = reader.readShort();
        this.unknown3 = reader.readShort();
        this.unknown4 = reader.readShort();

        if (heightFieldEntryCount != (this.xSize * this.zSize))
            System.out.println("WARNING: HeightField EntryCount was " + heightFieldEntryCount + " bytes with dimensions: [" + this.xSize + ", " + this.zSize + "]");

        // Read height map.
        if (this.xSize > 0 && this.zSize > 0) {
            this.heightMap = new short[this.zSize][this.xSize];
            for (int z = 0; z < this.zSize; z++)
                for (int x = 0; x < this.xSize; x++)
                    this.heightMap[z][x] = reader.readShort();
        } else {
            // TODO: Why dis happen sometimes.
            this.heightMap = null;
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.xSize * this.zSize);
        writer.writeShort(this.xSize);
        writer.writeShort(this.zSize);
        writer.writeShort(this.unknown1);
        writer.writeShort(this.unknown2);
        writer.writeShort(this.unknown3);
        writer.writeShort(this.unknown4);

        // Write height map
        for (int z = 0; z < this.zSize; z++)
            for (int x = 0; x < this.xSize; x++)
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
        editor.addShortField("X Size", this.xSize, newValue -> this.xSize = newValue, null);
        editor.addShortField("Z Size", this.zSize, newValue -> this.zSize = newValue, null);
        editor.addShortField("Unknown 1", this.unknown1, newValue -> this.unknown1 = newValue, null);
        editor.addShortField("Unknown 2", this.unknown2, newValue -> this.unknown2 = newValue, null);
        editor.addShortField("Unknown 3", this.unknown3, newValue -> this.unknown3 = newValue, null);
        editor.addShortField("Unknown 4", this.unknown4, newValue -> this.unknown4 = newValue, null);
    }
}