package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds information about quad polygons.
 * Created by Kneesnap on 12/9/2023.
 */
public class OldFroggerMapQuadPacket extends OldFroggerMapPacket implements IPropertyListCreator {
    public static final String IDENTIFIER = "QUAD";

    @Getter private int xSize;
    @Getter private int zSize;
    @Getter private int xCount;
    @Getter private int zCount;
    @Getter private int textureCount;

    private final Map<OldFroggerMapPolygon, Integer> polygonFileOffsets = new HashMap<>();
    private final Map<OldFroggerMapGrid, Map<PSXPolygonType, Integer>> gridPolygonFileOffsets = new HashMap<>();

    public OldFroggerMapQuadPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    /**
     * Gets the pointer which polygon data begins for the specific grid & polygon type.
     * @param grid        the grid to get the start pointer for
     * @param polygonType the polygon type to get the start pointer for
     * @return polygonStartPointer
     */
    public int getPolygonStartPointer(OldFroggerMapGrid grid, PSXPolygonType polygonType) {
        Map<PSXPolygonType, Integer> typeMap = this.gridPolygonFileOffsets.get(grid);
        if (typeMap == null)
            throw new IllegalStateException("Failed to lookup grid polygon pointers for a grid entry.");

        Integer value = typeMap.get(polygonType);
        if (value == null)
            throw new IllegalStateException("Failed to lookup grid polygon pointer for the grid entry.");

        return value;
    }

    @Override
    public void clearReadWriteData() {
        super.clearReadWriteData();
        this.polygonFileOffsets.clear();
        this.gridPolygonFileOffsets.clear();
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.xSize = reader.readUnsignedShortAsInt();
        this.zSize = reader.readUnsignedShortAsInt();
        this.xCount = reader.readUnsignedShortAsInt();
        this.zCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // ushort quadCount?
        this.textureCount = reader.readUnsignedShortAsInt();
        int quadDataStartAddress = reader.readInt();

        if (quadDataStartAddress != reader.getIndex())
            throw new RuntimeException("The address where quad data starts was not at the expected location. (Expected: " + NumberUtils.toHexString(reader.getIndex()) + ", Provided: " + NumberUtils.toHexString(quadDataStartAddress) + ")");

        // This ensures that we end up at the end of this section.
        // This packet doesn't have the information necessary to read the data, but it is responsible for writing it.
        reader.setIndex(getParentFile().getGraphicalHeaderPacket().getVertexChunkAddress());
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.xSize);
        writer.writeUnsignedShort(this.zSize);
        writer.writeUnsignedShort(this.xCount);
        writer.writeUnsignedShort(this.zCount);
        writer.writeUnsignedShort(getParentFile().getGridPacket().getPolygonCount());
        writer.writeUnsignedShort(this.textureCount);
        int quadDataStartAddress = writer.writeNullPointer();

        // Write quads.
        writer.writeAddressTo(quadDataStartAddress);
        OldFroggerMapGridHeaderPacket gridPacket = getParentFile().getGridPacket();
        List<PSXPolygonType> polygonOrder = OldFroggerMapGrid.POLYGON_TYPE_ORDER;

        // Write floor polygons.
        for (int i = 0; i < polygonOrder.size(); i++) {
            PSXPolygonType polygonType = polygonOrder.get(i);

            for (int j = 0; j < gridPacket.getGrids().size(); j++) {
                OldFroggerMapGrid grid = gridPacket.getGrids().get(j);
                List<OldFroggerMapPolygon> polygons = grid.getFloorPolygons(polygonType);
                if (polygons == null || polygons.isEmpty())
                    continue;

                this.gridPolygonFileOffsets.computeIfAbsent(grid, key -> new HashMap<>())
                        .put(polygonType, writer.getIndex());

                // Write polygon data.
                for (int k = 0; k < polygons.size(); k++) {
                    OldFroggerMapPolygon polygon = polygons.get(k);
                    this.polygonFileOffsets.put(polygon, writer.getIndex());
                    polygon.save(writer);
                }
            }
        }

        // Write ceiling polygons.
        for (int i = 0; i < polygonOrder.size(); i++) {
            PSXPolygonType polygonType = polygonOrder.get(i);

            for (int j = 0; j < gridPacket.getGrids().size(); j++) {
                OldFroggerMapGrid grid = gridPacket.getGrids().get(j);
                List<OldFroggerMapPolygon> polygons = grid.getCeilingPolygons(polygonType);
                if (polygons == null || polygons.isEmpty())
                    continue;

                this.gridPolygonFileOffsets.computeIfAbsent(grid, key -> new HashMap<>())
                        .put(polygonType, writer.getIndex());

                // Write polygon data.
                for (int k = 0; k < polygons.size(); k++) {
                    OldFroggerMapPolygon polygon = polygons.get(k);
                    this.polygonFileOffsets.put(polygon, writer.getIndex());
                    polygon.save(writer);
                }
            }
        }
    }

    @Override
    public void clear() {
        this.xSize = 0;
        this.zSize = 0;
        this.xCount = 0;
        this.zCount = 0;
        this.textureCount = 0;
        this.polygonFileOffsets.clear();
        this.gridPolygonFileOffsets.clear();
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getQuadChunkAddress() : -1;
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Quad Size", this.xSize + "x" + this.zSize);
        propertyList.add("Quad Count", this.xCount + "x" + this.zCount);
        propertyList.add("Quad Texture Count", this.textureCount);
        return propertyList;
    }
}