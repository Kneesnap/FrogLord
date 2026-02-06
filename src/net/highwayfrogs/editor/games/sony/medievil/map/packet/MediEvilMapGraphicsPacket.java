package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MediEvil map format graphics packet.
 * Created by Kneesnap on 3/8/2024.
 */
@Getter
public class MediEvilMapGraphicsPacket extends MediEvilMapPacket implements IPropertyListCreator {
    public static final String IDENTIFIER = "GXSP"; // 'PSXG'.
    private final List<MediEvilMapPolygon> polygons = new ArrayList<>();
    private final List<SVector> vertices = new ArrayList<>();
    private short vertexGridResolution;
    private short[] vertexGridOffsetTable;
    private short[] vertexGridLengthTable;

    public MediEvilMapGraphicsPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int g3PolyCount = reader.readUnsignedShortAsInt();
        int g4PolyCount = reader.readUnsignedShortAsInt();
        int gt3PolyCount = reader.readUnsignedShortAsInt();
        int gt4PolyCount = reader.readUnsignedShortAsInt();
        int vertexCount = reader.readUnsignedShortAsInt();
        this.vertexGridResolution = reader.readUnsignedByteAsShort(); // Dimensions of vertex grid.
        reader.skipBytesRequireEmpty(Constants.BYTE_SIZE);

        int polygonListPtr = reader.readInt();
        int vertexListPtr = reader.readInt();
        int vertexGridOffsetTablePtr = reader.readInt();
        int vertexGridLengthTablePtr = reader.readInt();

        // Read polygons.
        this.polygons.clear();
        reader.requireIndex(getLogger(), polygonListPtr, "Expected polygon list data");
        int polygonCount = g3PolyCount + g4PolyCount + gt3PolyCount + gt4PolyCount;
        for (int i = 0; i < polygonCount; i++) {
            MediEvilMapPolygon polygon = new MediEvilMapPolygon(getParentFile());
            polygon.load(reader);
            this.polygons.add(polygon);
        }

        // Read vertices.
        this.vertices.clear();
        reader.requireIndex(getLogger(), vertexListPtr, "Expected vertex list data");
        for (int i = 0; i < vertexCount; i++) {
            SVector vertex = new SVector();
            vertex.loadWithPadding(reader);
            this.vertices.add(vertex);
        }

        // Read grid offset table.
        reader.requireIndex(getLogger(), vertexGridOffsetTablePtr, "Expected vertexGridOffsetTable data");
        this.vertexGridOffsetTable = new short[this.vertexGridResolution * this.vertexGridResolution];
        for (int i = 0; i < this.vertexGridOffsetTable.length; i++)
            this.vertexGridOffsetTable[i] = reader.readShort();

        // Read grid length table.
        reader.requireIndex(getLogger(), vertexGridLengthTablePtr, "Expected vertexGridLengthTable data");
        this.vertexGridLengthTable = new short[this.vertexGridResolution * this.vertexGridResolution];
        for (int i = 0; i < this.vertexGridLengthTable.length; i++)
            this.vertexGridLengthTable[i] = reader.readShort();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort((int) this.polygons.stream().filter(polygon -> polygon.getPolygonType() == PSXPolygonType.POLY_G3).count());// g3PolyCount
        writer.writeUnsignedShort((int) this.polygons.stream().filter(polygon -> polygon.getPolygonType() == PSXPolygonType.POLY_G4).count());// g4PolyCount
        writer.writeUnsignedShort((int) this.polygons.stream().filter(polygon -> polygon.getPolygonType() == PSXPolygonType.POLY_GT3).count());// gt3PolyCount
        writer.writeUnsignedShort((int) this.polygons.stream().filter(polygon -> polygon.getPolygonType() == PSXPolygonType.POLY_GT4).count());// gt4PolyCount
        writer.writeUnsignedShort(this.vertices.size()); // Vertex count.
        writer.writeUnsignedByte(this.vertexGridResolution); // Dimensions of vertex grid.
        writer.align(Constants.INTEGER_SIZE);

        int polygonListPtr = writer.writeNullPointer();
        int vertexListPtr = writer.writeNullPointer();
        int vertexGridOffsetTablePtr = writer.writeNullPointer();
        int vertexGridLengthTablePtr = writer.writeNullPointer();

        // Write polygons.
        writer.writeAddressTo(polygonListPtr);
        for (int i = 0; i < this.polygons.size(); i++)
            this.polygons.get(i).save(writer);

        // Write vertices.
        writer.writeAddressTo(vertexListPtr);
        for (int i = 0; i < this.vertices.size(); i++)
            this.vertices.get(i).saveWithPadding(writer);

        // Write grid offset table.
        writer.writeAddressTo(vertexGridOffsetTablePtr);
        for (int i = 0; i < this.vertexGridOffsetTable.length; i++)
            writer.writeShort(this.vertexGridOffsetTable[i]);

        // Write grid length table.
        writer.writeAddressTo(vertexGridLengthTablePtr);
        for (int i = 0; i < this.vertexGridLengthTable.length; i++)
            writer.writeShort(this.vertexGridLengthTable[i]);
    }

    @Override
    public void clear() {
        this.polygons.clear();
        this.vertices.clear();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        int g3Count = 0;
        int g4Count = 0;
        int gt3Count = 0;
        int gt4Count = 0;
        for (int i = 0; i < this.polygons.size(); i++) {
            MediEvilMapPolygon polygon = this.polygons.get(i);
            switch (polygon.getPolygonType()) {
                case POLY_G3:
                    g3Count++;
                    break;
                case POLY_G4:
                    g4Count++;
                    break;
                case POLY_GT3:
                    gt3Count++;
                    break;
                case POLY_GT4:
                    gt4Count++;
                    break;
                default:
                    throw new RuntimeException("Unsupported polygon type: " + polygon.getPolygonType());
            }
        }

        propertyList.add("Polygons", this.polygons.size());
        propertyList.add("Untextured Polygons", "[G3s: " + g3Count + ", G4s: " + g4Count + "]");
        propertyList.add("Textured Polygons", "[GT3s: " + gt3Count + ", GT4s: " + gt4Count + "]");
        propertyList.add("Vertices", this.vertices.size());
    }
}