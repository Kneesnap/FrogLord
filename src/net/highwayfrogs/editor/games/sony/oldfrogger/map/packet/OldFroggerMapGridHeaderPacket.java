package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.utils.Utils;

import java.util.*;

/**
 * Definitions of the 3D faces in the world.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapGridHeaderPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "GRID";
    private final List<OldFroggerMapGrid> grids = new ArrayList<>();
    private final List<Integer> entityPointerList1 = new ArrayList<>(); // TODO: Figure this one out better.
    private final List<Integer> entityPointerList2 = new ArrayList<>(); // TODO: Figure this one out better.

    private OldFroggerGridType type; // type of grid (?) 0 = FIXED, 1 = DEFORMED
    private long xSize; // Size of grids (x)
    private long zSize; // Size of grids (z)
    private int xCount; // Number of grids in X
    private int zCount; // Number of grids in Z
    private final SVector basePoint = new SVector(); // Bottom left position of map

    public OldFroggerMapGridHeaderPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.type = OldFroggerGridType.values()[reader.readByte()];
        byte padding = reader.readByte();
        int gridCount = reader.readUnsignedShortAsInt();
        this.xSize = reader.readUnsignedIntAsLong(); // Size of grids (x)
        this.zSize = reader.readUnsignedIntAsLong(); // Size of grids (z)
        this.xCount = reader.readUnsignedShortAsInt(); // Number of grids in X
        this.zCount = reader.readUnsignedShortAsInt(); // Number of grids in Z
        this.basePoint.loadWithPadding(reader);
        int gridDataStartAddress = reader.readInt(); // ptr to first of many grids
        int staticCount = reader.readUnsignedShortAsInt();
        int pathIdCount = reader.readUnsignedShortAsInt();

        // TODO: This pointer here points to the end of the 'GRID' section, where there's an array of pointers.
        // TODO: This array of pointers seems to be a pointer to EMTP entries.
        int entityListPointer = reader.readInt();
        int pathEntityPointer = reader.readInt();

        // Verify header data is okay before continuing.
        if (padding != 0)
            throw new RuntimeException("Grid Padding byte was not zero! (Was: " + padding + ")");
        if (gridDataStartAddress != reader.getIndex())
            throw new RuntimeException("The address where grid data starts was not at the expected location. (Expected: " + Utils.toHexString(reader.getIndex()) + ", Provided: " + Utils.toHexString(gridDataStartAddress) + ")");

        // TODO: System.out.println("GRID HEADER!!! [" + this.type + ", " + gridCount + ", " + this.xSize + ", " + this.zSize + ", " + this.xCount + ", " + this.zCount + ", " + this.basePoint.toFloatString() + ", " + staticCount + ", " + pathIdCount + ", " + entityListPointer + ", " + pathEntityPointer);

        // Read grid data.
        this.grids.clear();
        for (int i = 0; i < gridCount; i++) {
            OldFroggerMapGrid newGrid = new OldFroggerMapGrid(getParentFile().getGameInstance());
            newGrid.load(reader);
            this.grids.add(newGrid);
        }

        // Read entity list.
        if (entityListPointer != reader.getIndex())
            throw new RuntimeException("The address where the first grid entity list starts starts was not at the expected location. (Expected: " + Utils.toHexString(reader.getIndex()) + ", Provided: " + Utils.toHexString(entityListPointer) + ")");

        this.entityPointerList1.clear();
        for (int i = 0; i < staticCount; i++)
            this.entityPointerList1.add(reader.readInt());

        // Read other list.
        if (pathEntityPointer != reader.getIndex())
            throw new RuntimeException("The address where the second grid entity list starts starts was not at the expected location. (Expected: " + Utils.toHexString(reader.getIndex()) + ", Provided: " + Utils.toHexString(pathEntityPointer) + ")");

        this.entityPointerList2.clear();
        for (int i = 0; i < pathIdCount; i++)
            this.entityPointerList2.add(reader.readInt());
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        // TODO: IMPLEMENTO
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getGridChunkAddress() : -1;
    }

    /**
     * Represents a frogger map grid.
     */
    @Getter
    public static class OldFroggerMapGrid extends SCGameData<OldFroggerGameInstance> {
        private int unknown1; // TODO: Seems to differ from documentation.
        private final List<OldFroggerMapPolygon> polygons = new ArrayList<>();
        private final Map<PSXPolygonType, List<OldFroggerMapPolygon>> floorPolygonsByType = new HashMap<>();
        private final Map<PSXPolygonType, List<OldFroggerMapPolygon>> ceilingPolygonsByType = new HashMap<>();

        public static final List<PSXPolygonType> POLYGON_TYPE_ORDER = Arrays.asList(PSXPolygonType.POLY_F4, PSXPolygonType.POLY_FT4, PSXPolygonType.POLY_G4, PSXPolygonType.POLY_GT4);

        public OldFroggerMapGrid(OldFroggerGameInstance instance) {
            super(instance);
        }

        public void load(DataReader reader) {
            this.unknown1 = reader.readInt();

            int numFloorF4 = reader.readUnsignedByteAsShort();
            int numFloorFT4 = reader.readUnsignedByteAsShort();
            int numFloorG4 = reader.readUnsignedByteAsShort();
            int numFloorGT4 = reader.readUnsignedByteAsShort();
            int numCeilingF4 = reader.readUnsignedByteAsShort();
            int numCeilingFT4 = reader.readUnsignedByteAsShort();
            int numCeilingG4 = reader.readUnsignedByteAsShort();
            int numCeilingGT4 = reader.readUnsignedByteAsShort();

            int ptrFloorF4 = reader.readInt();
            int ptrFloorFT4 = reader.readInt();
            int ptrFloorG4 = reader.readInt();
            int ptrFloorGT4 = reader.readInt();
            int ptrCeilingF4 = reader.readInt();
            int ptrCeilingFT4 = reader.readInt();
            int ptrCeilingG4 = reader.readInt();
            int ptrCeilingGT4 = reader.readInt();
            reader.skipBytes(8 * Constants.INTEGER_SIZE); // Runtime pointers.
            int finalValue1 = reader.readInt(); // TODO: POINTER
            int finalValue2 = reader.readInt(); // TODO: Probably also pointer.

            /*if (debug) {
                System.out.println("ITS FKING GRID TIME! [" + Utils.toHexString(this.unknown1) + "]:");
                System.out.println("- Floor Polys:   " + Utils.toHexString(ptrFloorF4) + "/" + numFloorF4 + ", " + Utils.toHexString(ptrFloorFT4) + "/" + numFloorFT4 + ", " + Utils.toHexString(ptrFloorG4) + "/" + numFloorG4 + ", " + Utils.toHexString(ptrFloorGT4) + "/" + numFloorGT4);
                System.out.println("- Ceiling Polys: " + Utils.toHexString(ptrCeilingF4) + "/" + numCeilingF4 + ", " + Utils.toHexString(ptrCeilingFT4) + "/" + numCeilingFT4 + ", " + Utils.toHexString(ptrCeilingG4) + "/" + numCeilingG4 + ", " + Utils.toHexString(ptrCeilingGT4) + "/" + numCeilingGT4);
                System.out.println("- " + Utils.toHexString(finalValue1) + ", " + Utils.toHexString(finalValue2));
            }*/


            this.polygons.clear();
            readPolygons(reader, this.floorPolygonsByType, ptrFloorF4, numFloorF4, PSXPolygonType.POLY_F4);
            readPolygons(reader, this.floorPolygonsByType, ptrFloorFT4, numFloorFT4, PSXPolygonType.POLY_FT4);
            readPolygons(reader, this.floorPolygonsByType, ptrFloorG4, numFloorG4, PSXPolygonType.POLY_G4);
            readPolygons(reader, this.floorPolygonsByType, ptrFloorGT4, numFloorGT4, PSXPolygonType.POLY_GT4);
            readPolygons(reader, this.ceilingPolygonsByType, ptrCeilingF4, numCeilingF4, PSXPolygonType.POLY_F4);
            readPolygons(reader, this.ceilingPolygonsByType, ptrCeilingFT4, numCeilingFT4, PSXPolygonType.POLY_FT4);
            readPolygons(reader, this.ceilingPolygonsByType, ptrCeilingG4, numCeilingG4, PSXPolygonType.POLY_G4);
            readPolygons(reader, this.ceilingPolygonsByType, ptrCeilingGT4, numCeilingGT4, PSXPolygonType.POLY_GT4);
        }

        private void readPolygons(DataReader reader, Map<PSXPolygonType, List<OldFroggerMapPolygon>> polygonsByType, int startAddress, int count, PSXPolygonType polygonType) {
            List<OldFroggerMapPolygon> typedPolygons = polygonsByType.computeIfAbsent(polygonType, key -> new ArrayList<>());
            typedPolygons.clear();

            reader.jumpTemp(startAddress);
            for (int i = 0; i < count; i++) {
                OldFroggerMapPolygon polygon = new OldFroggerMapPolygon(getGameInstance(), polygonType);
                polygon.load(reader);
                this.polygons.add(polygon);
                typedPolygons.add(polygon);
            }

            reader.jumpReturn();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.unknown1);

            writer.writeUnsignedByte((short) this.floorPolygonsByType.get(PSXPolygonType.POLY_F4).size()); // numFloorF4
            writer.writeUnsignedByte((short) this.floorPolygonsByType.get(PSXPolygonType.POLY_FT4).size()); // numFloorFT4
            writer.writeUnsignedByte((short) this.floorPolygonsByType.get(PSXPolygonType.POLY_G4).size()); // numFloorG4
            writer.writeUnsignedByte((short) this.floorPolygonsByType.get(PSXPolygonType.POLY_GT4).size()); // numFloorGT4
            writer.writeUnsignedByte((short) this.ceilingPolygonsByType.get(PSXPolygonType.POLY_F4).size()); // numCeilingF4
            writer.writeUnsignedByte((short) this.ceilingPolygonsByType.get(PSXPolygonType.POLY_FT4).size()); // numCeilingFT4
            writer.writeUnsignedByte((short) this.ceilingPolygonsByType.get(PSXPolygonType.POLY_G4).size()); // numCeilingG4
            writer.writeUnsignedByte((short) this.ceilingPolygonsByType.get(PSXPolygonType.POLY_GT4).size()); // numCeilingGT4

            // TODO: We can calculate these pointers, but we need to track counts and/or positions of previously written pointers.
            /*int ptrFloorF4 = reader.readInt();
            int ptrFloorFT4 = reader.readInt();
            int ptrFloorG4 = reader.readInt();
            int ptrFloorGT4 = reader.readInt();
            int ptrCeilingF4 = reader.readInt();
            int ptrCeilingFT4 = reader.readInt();
            int ptrCeilingG4 = reader.readInt();
            int ptrCeilingGT4 = reader.readInt();*/

            // Runtime pointers
            for (int i = 0; i < 8; i++)
                writer.writeNullPointer();

            // TODO: int finalValue1 = reader.readInt(); // TODO: POINTER
            // TODO: int finalValue2 = reader.readInt(); // TODO: Probably also pointer.

            // The polygons are written by the QUAD chunk.
            // TODO: Implement making it write them.
        }
    }

    /**
     * Represents a grid type in old frogger.
     */
    public enum OldFroggerGridType {
        FIXED,
        DEFORMED
    }
}