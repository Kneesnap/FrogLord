package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about quad polygons.
 * Created by Kneesnap on 12/9/2023.
 */
@Getter
public class OldFroggerMapQuadPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "QUAD";

    private int xSize;
    private int zSize;
    private int xCount;
    private int zCount;
    private int textureCount;
    private final List<OldFroggerMapPolygon> polygons = new ArrayList<>();

    public OldFroggerMapQuadPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.xSize = reader.readUnsignedShortAsInt();
        this.zSize = reader.readUnsignedShortAsInt();
        this.xCount = reader.readUnsignedShortAsInt();
        this.zCount = reader.readUnsignedShortAsInt();
        int quadCount = reader.readUnsignedShortAsInt();
        this.textureCount = reader.readUnsignedShortAsInt();
        int quadDataStartAddress = reader.readInt();

        if (quadDataStartAddress != reader.getIndex())
            throw new RuntimeException("The address where quad data starts was not at the expected location. (Expected: " + Utils.toHexString(reader.getIndex()) + ", Provided: " + Utils.toHexString(quadDataStartAddress) + ")");

        // TODO: This packet is where Polygons are written, not where they are used from.,
        this.polygons.clear(); // TODO: Not all the polygons are GT4... How does the game know? Could it be the 'code' part? Nope.
        /*for (int i = 0; i < quadCount; i++) {
            OldFroggerMapPolygon polygon = new OldFroggerMapPolygon(getParentFile().getGameInstance(), PSXPolygonType.POLY_GT4);
            polygon.load(reader);
            this.polygons.add(polygon);
        }*/

        // This ensures that we end up at the end of this section.
        reader.setIndex(getParentFile().getGraphicalHeaderPacket().getVertexChunkAddress());
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.xSize);
        writer.writeUnsignedShort(this.zSize);
        writer.writeUnsignedShort(this.xCount);
        writer.writeUnsignedShort(this.zCount);
        writer.writeUnsignedShort(this.polygons.size());
        writer.writeUnsignedShort(this.textureCount);
        int quadDataStartAddress = writer.writeNullPointer();

        // Write quads.
        writer.writeAddressTo(quadDataStartAddress);
        for (int i = 0; i < this.polygons.size(); i++)
            this.polygons.get(i).save(writer);
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getQuadChunkAddress() : -1;
    }
}