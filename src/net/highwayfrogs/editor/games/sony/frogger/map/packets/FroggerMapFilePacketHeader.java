package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.nio.charset.StandardCharsets;

/**
 * Represents the header packet for a Frogger map.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketHeader extends FroggerMapFilePacket {
    private String comment = DEFAULT_COMMENT;
    private int generalPacketAddress;
    private int graphicalPacketAddress;
    private int formPacketAddress;
    private int entityPacketAddress;
    private int zonePacketAddress;
    private int pathPacketAddress;

    public static final String IDENTIFIER = "FROG";
    private static final String VERSION = "2.00";
    private static final String DEFAULT_COMMENT = "Maybe this time it'll all work fine...";
    private static final int COMMENT_BYTES = 64;


    public FroggerMapFilePacketHeader(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int mapFileSize = reader.readInt();
        if (mapFileSize != reader.getSize())
            getLogger().warning("The file reported having a length of %d bytes, but it actually was %d bytes.", mapFileSize, reader.getSize());

        // Read version.
        String versionString = reader.readTerminatedString(VERSION.length());
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
    public void clear() {
        this.comment = DEFAULT_COMMENT;
        this.generalPacketAddress = 0;
        this.graphicalPacketAddress = 0;
        this.formPacketAddress = 0;
        this.entityPacketAddress = 0;
        this.zonePacketAddress = 0;
        this.pathPacketAddress = 0;
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketHeader))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketHeader.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketHeader newHeaderChunk = (FroggerMapFilePacketHeader) newChunk;
        newHeaderChunk.comment = this.comment;
        newHeaderChunk.generalPacketAddress = this.generalPacketAddress;
        newHeaderChunk.graphicalPacketAddress = this.graphicalPacketAddress;
        newHeaderChunk.formPacketAddress = this.formPacketAddress;
        newHeaderChunk.entityPacketAddress = this.entityPacketAddress;
        newHeaderChunk.zonePacketAddress = this.zonePacketAddress;
        newHeaderChunk.pathPacketAddress = this.pathPacketAddress;
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

    /**
     * Set the map comment
     * @param newComment the comment to apply
     */
    public void setComment(String newComment) {
        if (newComment == null)
            throw new NullPointerException("newComment");

        int commentByteLength = this.comment.getBytes(StandardCharsets.US_ASCII).length;
        if (commentByteLength > COMMENT_BYTES)
            throw new IllegalArgumentException("The provided comment was too large! (" + commentByteLength + " bytes is greater than the allowed " + COMMENT_BYTES + ".)");

        this.comment = newComment;
    }
}