package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the graphical header packet.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketGraphical extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "GRAP";
    private int lightPacketAddress = -1;
    private int groupPacketAddress = -1;
    private int polygonPacketAddress = -1;
    private int vertexPacketAddress = -1;
    private int gridPacketAddress = -1;
    private int animationPacketAddress = -1;

    public FroggerMapFilePacketGraphical(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.lightPacketAddress = reader.readInt();
        this.groupPacketAddress = reader.readInt();
        this.polygonPacketAddress = reader.readInt();
        this.vertexPacketAddress = reader.readInt();
        this.gridPacketAddress = reader.readInt();
        this.animationPacketAddress = getParentFile().isMapAnimationEnabled() ? reader.readInt() : -1;
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        this.lightPacketAddress = writer.writeNullPointer();
        this.groupPacketAddress = writer.writeNullPointer();
        this.polygonPacketAddress = writer.writeNullPointer();
        this.vertexPacketAddress = writer.writeNullPointer();
        this.gridPacketAddress = writer.writeNullPointer();
        this.animationPacketAddress = getParentFile().isMapAnimationEnabled() ? writer.writeNullPointer() : -1;
    }

    @Override
    protected void saveBodySecondPass(DataWriter writer, long fileSizeInBytes) {
        // Write file size.
        writer.writeUnsignedInt(fileSizeInBytes);

        // Write pointer addresses.
        writer.writeIntAtPos(this.lightPacketAddress, getParentFile().getLightPacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.groupPacketAddress, getParentFile().getGroupPacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.polygonPacketAddress, getParentFile().getPolygonPacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.vertexPacketAddress, getParentFile().getVertexPacket().getLastValidWriteHeaderAddress());
        writer.writeIntAtPos(this.gridPacketAddress, getParentFile().getGridPacket().getLastValidWriteHeaderAddress());
        if (getParentFile().isMapAnimationEnabled())
            writer.writeIntAtPos(this.animationPacketAddress, getParentFile().getAnimationPacket().getLastValidWriteHeaderAddress());

        // Update internal tracking.
        this.lightPacketAddress = getParentFile().getLightPacket().getLastValidWriteHeaderAddress();
        this.groupPacketAddress = getParentFile().getGroupPacket().getLastValidWriteHeaderAddress();
        this.polygonPacketAddress = getParentFile().getPolygonPacket().getLastValidWriteHeaderAddress();
        this.vertexPacketAddress = getParentFile().getVertexPacket().getLastValidWriteHeaderAddress();
        this.gridPacketAddress = getParentFile().getGridPacket().getLastValidWriteHeaderAddress();
        if (getParentFile().isMapAnimationEnabled())
            this.animationPacketAddress = getParentFile().getAnimationPacket().getLastValidWriteHeaderAddress();
    }

    @Override
    public void clear() {
        this.lightPacketAddress = -1;
        this.groupPacketAddress = -1;
        this.polygonPacketAddress = -1;
        this.vertexPacketAddress = -1;
        this.gridPacketAddress = -1;
        this.animationPacketAddress = -1;
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketGraphical))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketGraphical.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketGraphical newGraphicalChunk = (FroggerMapFilePacketGraphical) newChunk;
        newGraphicalChunk.lightPacketAddress = this.lightPacketAddress;
        newGraphicalChunk.groupPacketAddress = this.groupPacketAddress;
        newGraphicalChunk.polygonPacketAddress = this.polygonPacketAddress;
        newGraphicalChunk.vertexPacketAddress = this.vertexPacketAddress;
        newGraphicalChunk.gridPacketAddress = this.gridPacketAddress;
        newGraphicalChunk.animationPacketAddress = this.animationPacketAddress;
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getGraphicalPacketAddress();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) { // Nothing to add.
    }
}