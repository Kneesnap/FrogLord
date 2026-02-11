package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.*;

/**
 * Represents the MediEvil map format graphics packet.
 * Created by Kneesnap on 3/8/2024.
 */
public class MediEvilMapGraphicsPacket extends MediEvilMapPacket implements IPropertyListCreator {
    @Getter private final List<MediEvilMapPolygon> polygons = new ArrayList<>();
    private final List<SVector> vertices = new ArrayList<>();
    private final List<SVector> immutableVertices = Collections.unmodifiableList(this.vertices);
    private final Comparator<SVector> vertexOrder = Comparator.comparingInt(this::getVertexGridIndex)
            .thenComparing(Comparator.comparingInt(SVector::getY).reversed());

    // Used for lighting, and nothing else.
    private short vertexGridResolution = EXPECTED_VERTEX_GRID_RESOLUTION;
    private int vertexGridShift = calculateVertexGridShift(EXPECTED_VERTEX_GRID_RESOLUTION);
    private short[] vertexGridOffsetTable;
    private short[] vertexGridLengthTable;

    public static final String IDENTIFIER = "GXSP"; // 'PSXG'.
    private static final int GRID_WORLD_CENTER_OFFSET = 32768;
    private static final short EXPECTED_VERTEX_GRID_RESOLUTION = 32; // The game assumes a value of 32. In previous versions (early 1997?), perhaps this may not have been the case. It MUST have been a power of two, however.

    // Prevents the vertex from being captured for lighting.
    // This is runtime only, so we ensure this flag is not set.
    public static final int VERTEX_FLAG_LIGHT_PROCESSED = Constants.BIT_FLAG_2;

    public MediEvilMapGraphicsPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    /**
     * Gets the vertices used within this packet.
     */
    public List<SVector> getVertices() {
        return this.immutableVertices;
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int g3PolyCount = reader.readUnsignedShortAsInt();
        int g4PolyCount = reader.readUnsignedShortAsInt();
        int gt3PolyCount = reader.readUnsignedShortAsInt();
        int gt4PolyCount = reader.readUnsignedShortAsInt();
        int vertexCount = reader.readUnsignedShortAsInt();
        this.vertexGridResolution = reader.readUnsignedByteAsShort(); // Dimensions of vertex grid.
        this.vertexGridShift = calculateVertexGridShift(this.vertexGridResolution);
        reader.skipBytesRequireEmpty(Constants.BYTE_SIZE);

        // This has never been observed before.
        if (this.vertexGridResolution != EXPECTED_VERTEX_GRID_RESOLUTION)
            getLogger().warning("Expected a grid resolution of %d, but actually found %d!", EXPECTED_VERTEX_GRID_RESOLUTION, this.vertexGridResolution);

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

            if ((vertex.getPadding() & VERTEX_FLAG_LIGHT_PROCESSED) == VERTEX_FLAG_LIGHT_PROCESSED)
                getLogger().warning("Vertex %d was marked with the light processing flag! This was expected to never happen.", i);
        }

        // Read grid offset table.
        int vertexGridSize = this.vertexGridResolution * this.vertexGridResolution;
        reader.requireIndex(getLogger(), vertexGridOffsetTablePtr, "Expected vertexGridOffsetTable data");
        this.vertexGridOffsetTable = new short[vertexGridSize];
        for (int i = 0; i < this.vertexGridOffsetTable.length; i++)
            this.vertexGridOffsetTable[i] = reader.readShort();

        // Read grid length table.
        reader.requireIndex(getLogger(), vertexGridLengthTablePtr, "Expected vertexGridLengthTable data");
        this.vertexGridLengthTable = new short[vertexGridSize];
        for (int i = 0; i < this.vertexGridLengthTable.length; i++)
            this.vertexGridLengthTable[i] = reader.readShort();

        // Validate grid tables to work the way FrogLord expects them to.
        int currentOffset = 0;
        for (int i = 0; i < vertexGridSize; i++) {
            short gridOffset = this.vertexGridOffsetTable[i];
            short gridLength = this.vertexGridLengthTable[i];
            if (gridOffset != currentOffset) {
                getLogger().warning("FrogLord expected vertexGrid[%d] to specify vertex offset %d, but it actually specified vertex offset %d.", i, currentOffset, gridOffset);
                currentOffset = gridOffset;
            }

            currentOffset += gridLength;
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        setVertices(this.vertices); // Since this behavior is accurate to original game data, it is safe to regenerate the vertex tables on save.
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

    /**
     * Sets the vertices which are stored in the packet.
     * @param vertices the vertices to apply
     */
    public void setVertices(List<SVector> vertices) {
        if (vertices == null)
            throw new NullPointerException("vertices");

        // Apply vertices from list.
        if (vertices != this.vertices && vertices != this.immutableVertices) {
            this.vertices.clear();
            this.vertices.addAll(vertices);
        }

        // Sort vertices.
        this.vertices.sort(this.vertexOrder);

        // Clear the vertex tables.
        int newTableSize = this.vertexGridResolution * this.vertexGridResolution;
        if (this.vertexGridOffsetTable == null || this.vertexGridLengthTable == null
                || this.vertexGridOffsetTable.length != newTableSize
                || this.vertexGridLengthTable.length != newTableSize) {
            this.vertexGridOffsetTable = new short[newTableSize];
            this.vertexGridLengthTable = new short[newTableSize];
        } else {
            Arrays.fill(this.vertexGridOffsetTable, (short) 0);
            Arrays.fill(this.vertexGridLengthTable, (short) 0);
        }

        if (this.vertexGridResolution == 0)
            return; // No grid.

        // Populate the tables.
        // This algorithm is accurate to how the original algorithm worked, except that the original algorithm may have had some kind of decimal precision/rounding differences.
        // This is more accurate to how the game should work.
        int lastGridIndex = -1;
        short vertexOffset = 0;
        for (int i = 0; i < this.vertices.size(); i++) {
            SVector vertex = this.vertices.get(i);
            int currentGridIndex = getVertexGridIndex(vertex);

            if (currentGridIndex > lastGridIndex) {
                Arrays.fill(this.vertexGridOffsetTable, lastGridIndex + 1, currentGridIndex, vertexOffset);
                lastGridIndex = currentGridIndex;
                this.vertexGridOffsetTable[currentGridIndex] = vertexOffset = DataUtils.unsignedIntToShort(i);
            }

            this.vertexGridLengthTable[currentGridIndex]++;
        }
    }

    private int getVertexGridIndex(SVector vertex) {
        if (this.vertexGridResolution == 0)
            return 0; // No grid.

        return (((vertex.getZ() + GRID_WORLD_CENTER_OFFSET) >> this.vertexGridShift) * this.vertexGridResolution)
                + ((vertex.getX() + GRID_WORLD_CENTER_OFFSET) >> this.vertexGridShift);
    }

    private static int calculateVertexGridShift(short vertexGridResolution) {
        if (vertexGridResolution == 0)
            return 0;
        if ((65536 % vertexGridResolution) != 0)
            throw new IllegalArgumentException("vertexGridResolution must be a power of two! (Was: " + vertexGridResolution + ")");

        int unit = 65536 / vertexGridResolution;
        int gridShift = 0;
        while ((unit & (1 << gridShift)) == 0)
            gridShift++;

        return gridShift;
    }
}