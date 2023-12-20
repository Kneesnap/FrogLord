package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains vertex data for old Frogger maps.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapVertexPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "VRTX";
    private final List<SVector> vertices = new ArrayList<>();

    public OldFroggerMapVertexPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        // Read header.
        int vertexCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding

        // Read vertices.
        this.vertices.clear();
        for (int i = 0; i < vertexCount; i++) {
            this.vertices.add(SVector.readWithPadding(reader));
            if (getParentFile().getMapConfig().getVersion() == OldFroggerMapVersion.EARLY)
                reader.skipBytes(8); // TODO: ?
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.vertices.size());
        writer.writeUnsignedShort(0); // Padding
        for (int i = 0; i < this.vertices.size(); i++)
            this.vertices.get(i).saveWithPadding(writer);
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getVertexChunkAddress() : -1;
    }
}