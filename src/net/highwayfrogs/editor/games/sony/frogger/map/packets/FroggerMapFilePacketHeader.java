package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Represents the header packet for a Frogger map.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketHeader extends FroggerMapFilePacket {
    private String comment;
    private int generalPacketAddress;
    private int graphicalPacketAddress;
    private int formPacketAddress;
    private int entityPacketAddress;
    private int zonePacketAddress;
    private int pathPacketAddress;

    public static final String IDENTIFIER = "FROG";
    private static final String VERSION = "2.00";
    private static final String COMMENT = "Maybe this time it'll all work fine...";
    private static final int COMMENT_BYTES = 64;


    public FroggerMapFilePacketHeader(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int mapFileSize = reader.readInt();
        if (mapFileSize != reader.getSize())
            getLogger().warning("The file reported having a length of " + mapFileSize + " bytes, but it actually was " + reader.getSize() + " bytes.");

        // Read version.
        String versionString = reader.readString(VERSION.length());
        if (!VERSION.equals(versionString))
            throw new RuntimeException("The file '" + getParentFile().getFileDisplayName() + "' reported an unsupported version of v" + versionString + ", whereas only v" + VERSION + " is supported!");

        this.comment = reader.readNullTerminatedFixedSizeString(COMMENT_BYTES); // After the terminator byte we've got uninitialized (uncleared malloc) memory.
        this.generalPacketAddress = reader.readInt();
        this.graphicalPacketAddress = reader.readInt();
        this.formPacketAddress = reader.readInt();
        this.entityPacketAddress = reader.readInt();
        this.zonePacketAddress = reader.readInt();
        this.pathPacketAddress = reader.readInt();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeNullPointer(); // Map file size.
        writer.writeStringBytes(VERSION);
        writer.writeNullTerminatedFixedSizeString(this.comment, COMMENT_BYTES);
        this.generalPacketAddress = writer.writeNullPointer();
        this.graphicalPacketAddress = writer.writeNullPointer();
        this.formPacketAddress = writer.writeNullPointer();
        this.entityPacketAddress = writer.writeNullPointer();
        this.zonePacketAddress = writer.writeNullPointer();
        this.pathPacketAddress = writer.writeNullPointer();
    }

    @Override
    protected void saveBodySecondPass(DataWriter writer, long fileSizeInBytes) {
        // Write file size.
        writer.writeUnsignedInt(fileSizeInBytes);

        // Write pointer addresses.
        writer.writeIntAtPos(this.generalPacketAddress, getParentFile().getGeneralPacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.graphicalPacketAddress, getParentFile().getGraphicalPacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.formPacketAddress, getParentFile().getFormPacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.entityPacketAddress, getParentFile().getEntityPacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.zonePacketAddress, getParentFile().getZonePacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.pathPacketAddress, getParentFile().getPathPacket().getLastValidWriteHeaderAddress());

        // Update internal tracking.
        this.generalPacketAddress = getParentFile().getGeneralPacket().getLastValidWriteHeaderAddress();
        this.graphicalPacketAddress = getParentFile().getGraphicalPacket().getLastValidWriteHeaderAddress();
        this.formPacketAddress = getParentFile().getFormPacket().getLastValidWriteHeaderAddress();
        this.entityPacketAddress = getParentFile().getEntityPacket().getLastValidWriteHeaderAddress();
        this.zonePacketAddress = getParentFile().getZonePacket().getLastValidWriteHeaderAddress();
        this.pathPacketAddress = getParentFile().getPathPacket().getLastValidWriteHeaderAddress();
    }

    @Override
    public int getKnownStartAddress() {
        return 0; // Start of file.
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Comment", this.comment);
        return propertyList;
    }
}