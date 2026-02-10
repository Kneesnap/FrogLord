package net.highwayfrogs.editor.games.sony.medievil.map.quadtree;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.grid.MediEvilMapGridSquare;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.MathUtils;
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
    @Getter private short minX;
    @Getter private short minZ;
    @Getter private short maxX;
    @Getter private short maxZ;
    @Getter private MediEvilMapQuadTreeNode northEastChildNode; // +X +Z
    @Getter private MediEvilMapQuadTreeNode southEastChildNode; // +X -Z
    @Getter private MediEvilMapQuadTreeNode southWestChildNode; // -X -Z
    @Getter private MediEvilMapQuadTreeNode northWestChildNode; // -X +Z

    private int tempPolygonListPtr = Integer.MAX_VALUE;

    private static final int NON_LEAF_NODE_POLYGON_COUNT = -1;
    private static final int SIZE_IN_BYTES = Constants.INTEGER_SIZE + (8 * Constants.SHORT_SIZE) + (5 * Constants.POINTER_SIZE); // 40

    public MediEvilMapQuadTreeNode(MediEvilMapQuadTree quadTree) {
        super(quadTree.getGameInstance());
        this.quadTree = quadTree;
    }

    public MediEvilMapQuadTreeNode(MediEvilMapQuadTree quadTree, MediEvilMapGridSquare gridSquare) {
        super(quadTree.getGameInstance());
        this.quadTree = quadTree;

        this.minX = (short) gridSquare.getStartWorldX();
        this.minZ = (short) gridSquare.getStartWorldZ();
        this.maxX = (short) (this.minX + gridSquare.getGridPacket().getGridSquareSize() - 1);
        this.maxZ = (short) (this.minZ + gridSquare.getGridPacket().getGridSquareSize() - 1);
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

    void tryInsertPolygon(MediEvilMapPolygon polygon, int remainingHeight) {
        // The insertion strategy here mimics the data I see in the original game files (0.31),
        //  but is not accurate enough to be consistent with the original game data.
        if (isLeaf()) {
            if (this.polygons.size() < 50 || remainingHeight <= 0) {
                this.polygons.add(polygon);
                return;
            }

            // Split this into different nodes.
            createChildNodes();
            for (int i = 0; i < this.polygons.size(); i++)
                tryInsertPolygon(this.polygons.get(i), remainingHeight);
            this.polygons.clear();
        }

        // Insert polygon
        if (isPolygonPartOfQuadrant(polygon, Quadrant.NORTH_EAST))
            this.northEastChildNode.tryInsertPolygon(polygon, remainingHeight - 1);
        if (isPolygonPartOfQuadrant(polygon, Quadrant.SOUTH_EAST))
            this.southEastChildNode.tryInsertPolygon(polygon, remainingHeight - 1);
        if (isPolygonPartOfQuadrant(polygon, Quadrant.SOUTH_WEST))
            this.southWestChildNode.tryInsertPolygon(polygon, remainingHeight - 1);
        if (isPolygonPartOfQuadrant(polygon, Quadrant.NORTH_WEST))
            this.northWestChildNode.tryInsertPolygon(polygon, remainingHeight - 1);
    }

    // If one child node exists, all are assumed to exist by the game.
    private void createChildNodes() {
        short centerX = (short) (((roundCoordinate(this.minX) + roundCoordinate(this.maxX)) >> 1) - 1);
        short centerZ = (short) (((roundCoordinate(this.minZ) + roundCoordinate(this.maxZ)) >> 1) - 1);

        this.northEastChildNode = new MediEvilMapQuadTreeNode(this.quadTree);
        this.northEastChildNode.minX = centerX;
        this.northEastChildNode.minZ = centerZ;
        this.northEastChildNode.maxX = this.maxX;
        this.northEastChildNode.maxZ = this.maxZ;

        this.southEastChildNode = new MediEvilMapQuadTreeNode(this.quadTree);
        this.southEastChildNode.minX = centerX;
        this.southEastChildNode.minZ = this.minZ;
        this.southEastChildNode.maxX = this.maxX;
        this.southEastChildNode.maxZ = centerZ;

        this.southWestChildNode = new MediEvilMapQuadTreeNode(this.quadTree);
        this.southWestChildNode.minX = this.minX;
        this.southWestChildNode.minZ = this.minZ;
        this.southWestChildNode.maxX = centerX;
        this.southWestChildNode.maxZ = centerZ;

        this.northWestChildNode = new MediEvilMapQuadTreeNode(this.quadTree);
        this.northWestChildNode.minX = this.minX;
        this.northWestChildNode.minZ = centerZ;
        this.northWestChildNode.maxX = centerX;
        this.northWestChildNode.maxZ = this.maxZ;
    }

    private static short roundCoordinate(short input) {
        if ((input & 0x0F) == 0xF)
            return (short) (input + 1);
        if ((input & 0x0F) == 1)
            return (short) (input - 1);
        return input;
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
        this.minZ = validateSame("z3", "z2", z3, z2);
        this.maxZ = validateSame("z0", "z1", z0, z1);
        this.northEastChildNode = resolveNode(reader.readInt(), "northEastChildNode");
        this.southEastChildNode = resolveNode(reader.readInt(), "southEastChildNode");
        this.southWestChildNode = resolveNode(reader.readInt(), "southWestChildNode");
        this.northWestChildNode = resolveNode(reader.readInt(), "northWestChildNode");
        this.tempPolygonListPtr = reader.readInt(); // If polygonCount == 0, this seems to be garbage. If polygonCount is -1, this is zero.
        if (polygonCount == 0)
            this.tempPolygonListPtr = 0; // For some reason, there is an unexpected (garbage?) value when there are no polygons.

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
        if (!isLeaf()) {
            if (!this.polygons.isEmpty()) { // Should never happen.
                getLogger().severe("MediEvilMapQuadTreeNode was not a leaf node, but also had %d polygon(s)!", this.polygons.size());
                this.polygons.clear();
            }

            writer.writeInt(NON_LEAF_NODE_POLYGON_COUNT);
        } else {
            writer.writeInt(this.polygons.size());
        }

        writer.writeShort(this.minX); // x0
        writer.writeShort(this.maxZ); // z0
        writer.writeShort(this.maxX); // x1
        writer.writeShort(this.maxZ); // z1
        writer.writeShort(this.maxX); // x2
        writer.writeShort(this.minZ); // z2
        writer.writeShort(this.minX); // x3
        writer.writeShort(this.minZ); // z3
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

        for (int i = 0; i < this.polygons.size(); i++)
            writer.writeUnsignedShort(this.polygons.get(i).getPolygonIndex());
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

    private enum Quadrant {
        NORTH_EAST,
        SOUTH_EAST,
        SOUTH_WEST,
        NORTH_WEST;
    }

    private boolean isPolygonPartOfQuadrant(MediEvilMapPolygon polygon, Quadrant quadrant) {
        short centerX = (short) (((roundCoordinate(this.minX) + roundCoordinate(this.maxX)) >> 1) - 1);
        short centerZ = (short) (((roundCoordinate(this.minZ) + roundCoordinate(this.maxZ)) >> 1) - 1);

        short minX, minZ, maxX, maxZ;
        switch (quadrant) {
            case NORTH_EAST:
                minX = centerX;
                minZ = centerZ;
                maxX = this.maxX;
                maxZ = this.maxZ;
                break;
            case SOUTH_EAST:
                minX = centerX;
                minZ = this.minZ;
                maxX = this.maxX;
                maxZ = centerZ;
                break;
            case SOUTH_WEST:
                minX = this.minX;
                minZ = this.minZ;
                maxX = centerX;
                maxZ = centerZ;
                break;
            case NORTH_WEST:
                minX = this.minX;
                minZ = centerZ;
                maxX = centerX;
                maxZ = this.maxZ;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported quadrant: " + quadrant);
        }

        // Test vertices themselves.
        int vertexCount = polygon.getVertexCount();
        List<SVector> vertices = this.quadTree.getMapFile().getGraphicsPacket().getVertices();
        for (int k = 0; k < vertexCount; k++) {
            SVector vertex = vertices.get(polygon.getVertices()[k]);
            if (vertex.getX() >= minX && vertex.getX() <= maxX && vertex.getZ() >= minZ && vertex.getZ() <= maxZ)
                return true;
        }

        // Test polygon edges.
        if (vertexCount == 3) {
            return testIntersectingIndices(vertices, polygon, 0, 1, minX, maxX, minZ, maxZ)
                    || testIntersectingIndices(vertices, polygon, 1, 2, minX, maxX, minZ, maxZ)
                    || testIntersectingIndices(vertices, polygon, 2, 0, minX, maxX, minZ, maxZ);
        } else if (vertexCount == 4) {
            return testIntersectingIndices(vertices, polygon, 0, 1, minX, maxX, minZ, maxZ)
                    || testIntersectingIndices(vertices, polygon, 1, 3, minX, maxX, minZ, maxZ)
                    || testIntersectingIndices(vertices, polygon, 3, 2, minX, maxX, minZ, maxZ)
                    || testIntersectingIndices(vertices, polygon, 2, 0, minX, maxX, minZ, maxZ)
                    // The following are not polygon edges, but would break the quad into tris.
                    || testIntersectingIndices(vertices, polygon, 0, 3, minX, maxX, minZ, maxZ)
                    || testIntersectingIndices(vertices, polygon, 1, 2, minX, maxX, minZ, maxZ);
        }

        throw new UnsupportedOperationException("Unsupported vertexCount: " + vertexCount);
    }

    private static boolean testIntersectingIndices(List<SVector> vertices, MediEvilMapPolygon polygon, int startVertexId, int endVertexId, short minX, short maxX, short minZ, short maxZ) {
        SVector startVertex = vertices.get(polygon.getVertices()[startVertexId]);
        SVector endVertex = vertices.get(polygon.getVertices()[endVertexId]);

        // Create a hypothetical box around the grid square area, by testing if the line points intersect with any of the box edges.
        int lineStartX = startVertex.getX();
        int lineStartY = startVertex.getZ();
        int lineEndX = endVertex.getX();
        int lineEndY = endVertex.getZ();
        return MathUtils.doLinesIntersect(lineStartX, lineStartY, lineEndX, lineEndY, minX, maxZ, maxX, maxZ) // Top Edge
                || MathUtils.doLinesIntersect(lineStartX, lineStartY, lineEndX, lineEndY, minX, minZ, maxX, minZ) // Bottom Edge
                || MathUtils.doLinesIntersect(lineStartX, lineStartY, lineEndX, lineEndY, minX, minZ, minX, maxZ) // Left Edge
                || MathUtils.doLinesIntersect(lineStartX, lineStartY, lineEndX, lineEndY, maxX, minZ, maxX, maxZ); // Right Edge
    }
}
