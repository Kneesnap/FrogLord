package net.highwayfrogs.editor.games.sony.medievil.map.quadtree;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in a MediEvil quad tree.
 *
 * Coordinates of area covered:
 * 0------------1 (-X +Z) 0
 * |            | (+X +Z) 1
 * |            | (+X -Z) 2
 * |            | (-X -Z) 3
 * 3------------2
 * West -X East +X
 * South -Z North +Z
 * Created by Kneesnap on 2/4/2026.
 */
public class MediEvilMapQuadTreeNode extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator {
    @Getter private final MediEvilMapQuadTree quadTree;
    @Getter private final List<MediEvilMapPolygon> polygons = new ArrayList<>();
    @Getter private boolean rootNode;
    @Getter private short minX;
    @Getter private short minZ;
    @Getter private short maxX;
    @Getter private short maxZ;
    @Getter private MediEvilMapQuadTreeNode northEastChildNode; // +X +Z
    @Getter private MediEvilMapQuadTreeNode southEastChildNode; // +X -Z
    @Getter private MediEvilMapQuadTreeNode southWestChildNode; // -X -Z
    @Getter private MediEvilMapQuadTreeNode northWestChildNode; // -X +Z

    private int tempPolygonListPtr = Integer.MAX_VALUE;

    private static final int ROOT_NODE_POLYGON_COUNT = 0;
    private static final int NON_LEAF_NODE_POLYGON_COUNT = -1;
    private static final int SIZE_IN_BYTES = Constants.INTEGER_SIZE + (8 * Constants.SHORT_SIZE) + (5 * Constants.POINTER_SIZE); // 40

    public MediEvilMapQuadTreeNode(MediEvilMapQuadTree quadTree) {
        super(quadTree.getGameInstance());
        this.quadTree = quadTree;
    }

    /**
     * Gets the index of this node within the tree's node list.
     * @return nodeIndex
     */
    public int getIndex() {
        int index = this.quadTree.nodes.indexOf(this);
        if (index < 0)
            throw new IllegalArgumentException("The node was not found to be part of the tree.");

        return index;
    }

    /**
     * Returns true iff this node is a leaf node.
     */
    public boolean isLeaf() {
        return this.northEastChildNode == null && this.southEastChildNode == null
                && this.southWestChildNode == null && this.northWestChildNode == null;
    }

    @Override
    public void load(DataReader reader) {
        int polygonCount = reader.readInt();
        short x0 = reader.readShort();
        short z0 = reader.readShort();
        short x1 = reader.readShort();
        short z1 = reader.readShort();
        short x2 = reader.readShort();
        short z2 = reader.readShort();
        short x3 = reader.readShort();
        short z3 = reader.readShort();
        this.minX = validateSame("x0", "x3", x0, x3);
        this.maxX = validateSame("x1", "x2", x1, x2);
        this.minZ = validateSame("z0", "z1", z0, z1);
        this.maxZ = validateSame("z3", "z2", z3, z2);
        this.northEastChildNode = resolveNode(reader.readInt(), "northEastChildNode");
        this.southEastChildNode = resolveNode(reader.readInt(), "southEastChildNode");
        this.southWestChildNode = resolveNode(reader.readInt(), "southWestChildNode");
        this.northWestChildNode = resolveNode(reader.readInt(), "northWestChildNode");
        this.tempPolygonListPtr = reader.readInt(); // If polygonCount == 0, this seems to be garbage. If polygonCount is -1, this is zero.

        // Determine root node.
        this.rootNode = (polygonCount == ROOT_NODE_POLYGON_COUNT);
        if (this.rootNode)
            this.tempPolygonListPtr = 0; // For some reason, there is an unexpected (garbage?) value in this situation.

        // Validate pointer.
        if (polygonCount == NON_LEAF_NODE_POLYGON_COUNT && this.tempPolygonListPtr != 0)
            getLogger().warning("Unexpected polygon list pointer value 0x%08X for non-leaf node.", this.tempPolygonListPtr);

        // Setup list.
        this.polygons.clear();
        for (int i = 0; i < polygonCount; i++)
            this.polygons.add(null);
    }

    @Override
    public void save(DataWriter writer) {
        if (this.polygons.isEmpty()) {
            if (this.rootNode) {
                writer.writeInt(ROOT_NODE_POLYGON_COUNT);
            } else {
                writer.writeInt(NON_LEAF_NODE_POLYGON_COUNT);
            }
        } else {
            writer.writeInt(this.polygons.size());
        }
        writer.writeShort(this.minX); // x0
        writer.writeShort(this.minZ); // z0
        writer.writeShort(this.maxX); // x1
        writer.writeShort(this.minZ); // z1
        writer.writeShort(this.maxX); // x2
        writer.writeShort(this.maxZ); // z2
        writer.writeShort(this.minX); // x3
        writer.writeShort(this.maxZ); // z3
        writer.writeInt(this.northEastChildNode != null ? (this.northEastChildNode.getIndex() * SIZE_IN_BYTES) : -1);
        writer.writeInt(this.southEastChildNode != null ? (this.southEastChildNode.getIndex() * SIZE_IN_BYTES) : -1);
        writer.writeInt(this.southWestChildNode != null ? (this.southWestChildNode.getIndex() * SIZE_IN_BYTES) : -1);
        writer.writeInt(this.northWestChildNode != null ? (this.northWestChildNode.getIndex() * SIZE_IN_BYTES) : -1);
        this.tempPolygonListPtr = writer.writeNullPointer();
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.quadTree.getLogger(), "QuadTreeNode[" + this.quadTree.nodes.indexOf(this) + "]", AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString("Minimum Position", "(" + DataUtils.fixedPointShortToFloat4Bit(this.minX) + ", " + DataUtils.fixedPointShortToFloat4Bit(this.minZ) + ")");
        propertyList.addString("Maximum Position", "(" + DataUtils.fixedPointShortToFloat4Bit(this.maxX) + ", " + DataUtils.fixedPointShortToFloat4Bit(this.maxZ) + ")");
        addNodeProperty(propertyList, this.northEastChildNode, "North East Node");
        addNodeProperty(propertyList, this.southEastChildNode, "South East Node");
        addNodeProperty(propertyList, this.southWestChildNode, "South West Node");
        addNodeProperty(propertyList, this.northWestChildNode, "North West Node");
        propertyList.addInteger("Polygons", this.polygons.size());
    }

    private static short validateSame(String name1, String name2, short a, short b) {
        if (a != b)
            throw new IllegalArgumentException("The value " + name1 + " (" + a + ") did not match the value of " + name2 + " (" + b + ").");

        return a;
    }

    private static void addNodeProperty(PropertyListNode propertyList, MediEvilMapQuadTreeNode node, String name) {
        if (node != null) {
            propertyList.addString(node, name, "ID: " + node.getIndex());
        } else {
            propertyList.add(name, "None");
        }
    }

    /**
     * Loads the polygon list.
     * @param reader the reader to read polygon ids from
     */
    void loadPolygons(DataReader reader, int polygonIdStartIndex) {
        if (this.tempPolygonListPtr < 0)
            throw new RuntimeException("Cannot load tempPolygonListPtr, the value " + NumberUtils.toHexString(this.tempPolygonListPtr) + " is invalid.");

        // No polygons.
        if (this.tempPolygonListPtr == 0 && this.polygons.isEmpty()) {
            this.tempPolygonListPtr = -1;
            return;
        }

        requireReaderIndex(reader, polygonIdStartIndex + this.tempPolygonListPtr, "Expected polygon ID list");
        this.tempPolygonListPtr = -1;

        List<MediEvilMapPolygon> mapPolygons = this.quadTree.getMapFile().getGraphicsPacket().getPolygons();
        for (int i = 0; i < this.polygons.size(); i++) {
            int polygonIndex = reader.readUnsignedShortAsInt();
            if (polygonIndex < 0 || polygonIndex >= mapPolygons.size())
                throw new IllegalArgumentException("Invalid polygonIndex: " + polygonIndex);

            this.polygons.set(i, mapPolygons.get(polygonIndex));
        }
    }

    /**
     * Saves the polygon list.
     * @param writer the writer to save polygon ids to
     */
    void savePolygons(DataWriter writer, int polygonDataStartIndex) {
        if (this.tempPolygonListPtr < 0)
            throw new RuntimeException("Cannot save tempPolygonListPtr, the value " + NumberUtils.toHexString(this.tempPolygonListPtr) + " is invalid.");

        // No polygons.
        if (this.polygons.isEmpty()) {
            writer.writeIntAtPos(this.tempPolygonListPtr, 0);
            this.tempPolygonListPtr = -1;
            return;
        }

        writer.writeIntAtPos(this.tempPolygonListPtr, writer.getIndex() - polygonDataStartIndex);
        this.tempPolygonListPtr = -1;

        MediEvilMapFile mapFile = this.quadTree.getMapFile();
        for (int i = 0; i < this.polygons.size(); i++)
            writer.writeUnsignedShort(this.polygons.get(i).getPolygonIndex(mapFile));
    }

    private MediEvilMapQuadTreeNode resolveNode(int nodeOffset, String nodeName) {
        if (nodeOffset == -1)
            return null; // -1 represents "no node"/null.
        if ((nodeOffset % SIZE_IN_BYTES) != 0)
            throw new IllegalArgumentException("Cannot resolve '" + nodeName + "', because its offset (" + NumberUtils.toHexString(nodeOffset) + ") was not divisible by the size of a node. (" + SIZE_IN_BYTES + ")");

        int nodeIndex = (nodeOffset / SIZE_IN_BYTES);
        if (nodeIndex < 0 || nodeIndex >= this.quadTree.nodes.size())
            throw new IllegalArgumentException("Cannot resolve '" + nodeName + "', because its offset (" + NumberUtils.toHexString(nodeOffset) + ") was resolved to an invalid nodeIndex! (" + nodeIndex + ")");

        return this.quadTree.nodes.get(nodeIndex);
    }
}
