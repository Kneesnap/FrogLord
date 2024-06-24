package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

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
        writer.writeAddressAt(this.lightPacketAddress, getParentFile().getLightPacket().getLastValidWriteHeaderAddress());
        writer.writeAddressAt(this.groupPacketAddress, getParentFile().getGroupPacket().getLastValidWriteHeaderAddress());
        writer.writeAddressAt(this.polygonPacketAddress, getParentFile().getPolygonPacket().getLastValidWriteHeaderAddress());
        writer.writeAddressAt(this.vertexPacketAddress, getParentFile().getVertexPacket().getLastValidWriteHeaderAddress());
        writer.writeAddressAt(this.gridPacketAddress, getParentFile().getGridPacket().getLastValidWriteHeaderAddress());
        if (getParentFile().isMapAnimationEnabled())
            writer.writeAddressAt(this.animationPacketAddress, getParentFile().getAnimationPacket().getLastValidWriteHeaderAddress());

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
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getGraphicalPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        return propertyList; // Nothing to add.
    }
}