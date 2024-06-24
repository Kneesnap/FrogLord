package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

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
        short vertexGridSize = reader.readUnsignedByteAsShort(); // Dimensions of vertex grid.
        reader.skipBytesRequireEmpty(Constants.BYTE_SIZE);

        int polygonListPtr = reader.readInt();
        int vertexListPtr = reader.readInt();
        int vertexGridOffsetTablePtr = reader.readInt();
        int vertexGridLengthTablePtr = reader.readInt();

        // Read polygons.
        this.polygons.clear();
        reader.jumpTemp(polygonListPtr);
        int polygonCount = g3PolyCount + g4PolyCount + gt3PolyCount + gt4PolyCount;
        for (int i = 0; i < polygonCount; i++) {
            MediEvilMapPolygon polygon = new MediEvilMapPolygon(getParentFile().getGameInstance());
            polygon.load(reader);
            this.polygons.add(polygon);
        }

        if (reader.getIndex() != vertexListPtr)
            getLogger().warning("The polygon data ended at " + Utils.toHexString(reader.getIndex()) + ", which was not when the vertex data started! (" + Utils.toHexString(vertexListPtr) + ")");

        reader.jumpReturn();

        // Read vertices.
        this.vertices.clear();
        reader.jumpTemp(vertexListPtr);
        for (int i = 0; i < vertexCount; i++) {
            SVector vertex = new SVector();
            vertex.loadWithPadding(reader);
            this.vertices.add(vertex);
        }
        if (reader.getIndex() != vertexGridOffsetTablePtr)
            getLogger().warning("The vertex data ended at " + Utils.toHexString(reader.getIndex()) + ", which was not when the grid offset table started! (" + Utils.toHexString(vertexGridOffsetTablePtr) + ")");

        // Read grid offset table.
        // TODO: !

        // Read grid length table.
        // TODO: !

        reader.setIndex(endIndex);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        // TODO: Implement.
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
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
        return propertyList;
    }
}