package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import javafx.scene.control.Alert.AlertType;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerGridManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputMenu;
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
    private OldFroggerGridType type = OldFroggerGridType.DEFORMED; // type of grid (?) 0 = FIXED, 1 = DEFORMED
    private int xSize; // Size of grids (x)
    private int zSize; // Size of grids (z)
    private int xCount; // Number of grids in X
    private int zCount; // Number of grids in Z
    private final SVector basePoint = new SVector(); // Bottom left position of map

    public OldFroggerMapGridHeaderPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.type = OldFroggerGridType.values()[reader.readByte()];
        reader.skipBytesRequireEmpty(1);
        int gridCount = reader.readUnsignedShortAsInt();
        this.xSize = reader.readInt(); // Size of grids (x)
        this.zSize = reader.readInt(); // Size of grids (z)
        this.xCount = reader.readUnsignedShortAsInt(); // Number of grids in X
        this.zCount = reader.readUnsignedShortAsInt(); // Number of grids in Z
        this.basePoint.loadWithPadding(reader);
        int gridDataStartAddress = reader.readInt(); // ptr to first of many grids
        int quadCount = reader.readUnsignedShortAsInt();
        int staticEntityCount = reader.readUnsignedShortAsInt();
        int quadPointer = reader.readInt();
        int staticEntityPointer = reader.readInt();

        // Verify header data is okay before continuing.
        if (gridDataStartAddress != reader.getIndex())
            throw new RuntimeException("The address where grid data starts was not at the expected location. (Expected: " + Utils.toHexString(reader.getIndex()) + ", Provided: " + Utils.toHexString(gridDataStartAddress) + ")");

        // Read grid data. (Needs entity info)
        reader.setIndex(gridDataStartAddress);
        this.grids.clear();
        for (int i = 0; i < gridCount; i++) {
            OldFroggerMapGrid newGrid = new OldFroggerMapGrid(getParentFile());
            newGrid.load(reader);
            this.grids.add(newGrid);
        }

        // Ensure we ended at the right spot.
        if (quadPointer != reader.getIndex())
            throw new RuntimeException("The address where the quad list starts was not at the expected location. (Expected: " + Utils.toHexString(reader.getIndex()) + ", Provided: " + Utils.toHexString(quadPointer) + ")");

        // Read quad data.
        // (Do nothing)

        // Ensure quad data ended at the expected position.
        if (staticEntityPointer != reader.getIndex())
            throw new RuntimeException("The address where the static entity list starts was not at the expected location. (Expected: " + Utils.toHexString(reader.getIndex()) + ", Provided: " + Utils.toHexString(staticEntityPointer) + ", Quad Count: " + quadCount + ")");

        // Skip entity list.
        reader.skipBytes(staticEntityCount * Constants.INTEGER_SIZE);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedByte((short) (this.type != null ? this.type.ordinal() : 0));
        writer.writeNull(1);
        writer.writeUnsignedShort(this.grids.size());
        writer.writeInt(this.xSize);
        writer.writeInt(this.zSize);
        writer.writeUnsignedShort(this.xCount);
        writer.writeUnsignedShort(this.zCount);
        this.basePoint.saveWithPadding(writer);
        int gridDataStartAddress = writer.writeNullPointer();
        writer.writeUnsignedShort(getPolygonCount());
        writer.writeUnsignedShort(getStaticEntityCount());
        int quadPointer = writer.writeNullPointer();
        int staticEntityPointer = writer.writeNullPointer();

        // Write grid data.
        writer.writeAddressTo(gridDataStartAddress);
        for (int i = 0; i < this.grids.size(); i++)
            this.grids.get(i).save(writer);

        // Write quad data. (Currently unimplemented)
        writer.writeAddressTo(quadPointer);

        // Write entity list.
        writer.writeAddressTo(staticEntityPointer);
        for (int i = 0; i < this.grids.size(); i++) {
            OldFroggerMapGrid grid = this.grids.get(i);
            grid.saveEntityListAddress(writer, writer.getIndex());
            for (int j = 0; j < grid.getStaticEntities().size(); j++)
                writer.writeInt(getParentFile().getEntityMarkerPacket().getEntityFileOffset(grid.getStaticEntities().get(j)));
        }
    }

    @Override
    protected void saveBodySecondPass(DataWriter writer, long fileSizeInBytes) {
        super.saveBodySecondPass(writer, fileSizeInBytes);
        for (int i = 0; i < this.grids.size(); i++)
            this.grids.get(i).savePolygonAddresses(writer);
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getGridChunkAddress() : -1;
    }

    /**
     * Setup the editor for grid packet data.
     * @param manager The UI manager
     * @param editor  The editor context to build upon.
     */
    public void setupEditor(OldFroggerGridManager manager, GUIEditorGrid editor) {
        // Disable this since I don't think the other type is supported.
        // And if it is, I don't know what data changes might be required.
        editor.addEnumSelector("Type", this.type, OldFroggerGridType.values(), false, newValue -> this.type = newValue).setDisable(true);
        editor.addFixedInt("Square X Size", this.xSize, newValue -> this.xSize = newValue, 256, 0, Integer.MAX_VALUE);
        editor.addFixedInt("Square Z Size", this.zSize, newValue -> this.zSize = newValue, 256, 0, Integer.MAX_VALUE);
        editor.addUnsignedFixedShort("Square X Count", this.xCount, newValue -> this.xCount = newValue, 1);
        editor.addUnsignedFixedShort("Square Z Count", this.zCount, newValue -> this.zCount = newValue, 1);
        editor.addFloatSVector("Base Position", this.basePoint, manager.getController());
    }

    /**
     * Returns the total number of polygons tracked by this packet.
     */
    public int getPolygonCount() {
        int count = 0;
        for (int i = 0; i < this.grids.size(); i++)
            count += this.grids.get(i).getPolygons().size();

        return count;
    }

    /**
     * Returns the total number of static entities tracked by this packet.
     */
    public int getStaticEntityCount() {
        int count = 0;
        for (int i = 0; i < this.grids.size(); i++)
            count += this.grids.get(i).getStaticEntities().size();

        return count;
    }

    /**
     * Represents a frogger map grid.
     */
    public static class OldFroggerMapGrid extends SCGameData<OldFroggerGameInstance> {
        @Getter private final OldFroggerMapFile map;
        @Getter private final List<OldFroggerMapPolygon> polygons = new ArrayList<>();
        @Getter private final List<OldFroggerMapEntity> staticEntities = new ArrayList<>();
        private final Map<PSXPolygonType, List<OldFroggerMapPolygon>> floorPolygonsByType = new HashMap<>();
        private final Map<PSXPolygonType, List<OldFroggerMapPolygon>> ceilingPolygonsByType = new HashMap<>();
        private transient int writtenDataOffset = -1;

        public static final List<PSXPolygonType> POLYGON_TYPE_ORDER = Arrays.asList(PSXPolygonType.POLY_F4, PSXPolygonType.POLY_FT4, PSXPolygonType.POLY_G4, PSXPolygonType.POLY_GT4);

        public OldFroggerMapGrid(OldFroggerMapFile map) {
            super(map.getGameInstance());
            this.map = map;
        }

        /**
         * Gets the ceiling polygons of a given type.
         * @param polygonType The type of polygon to get.
         * @return ceilingPolygonsOfType, or null if they don't exist.
         */
        public List<OldFroggerMapPolygon> getCeilingPolygons(PSXPolygonType polygonType) {
            return this.ceilingPolygonsByType.get(polygonType);
        }

        /**
         * Gets the floor polygons of a given type.
         * @param polygonType The type of polygon to get.
         * @return floorPolygonsOfType, or null if they don't exist.
         */
        public List<OldFroggerMapPolygon> getFloorPolygons(PSXPolygonType polygonType) {
            return this.floorPolygonsByType.get(polygonType);
        }

        @Override
        public void load(DataReader reader) {
            short entityCount = reader.readUnsignedByteAsShort();
            reader.skipBytesRequireEmpty(3);

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
            reader.skipBytesRequireEmpty(8 * Constants.INTEGER_SIZE); // Runtime pointers.
            int staticEntityListPointer = reader.readInt();
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Seems to be some pointer.

            // Read entities.
            reader.jumpTemp(staticEntityListPointer);
            this.staticEntities.clear();
            for (int i = 0; i < entityCount; i++) {
                OldFroggerMapEntity entity = this.map.getEntityMarkerPacket().getEntityByFileOffset(reader.readInt());
                if (entity != null)
                    this.staticEntities.add(entity);
            }
            reader.jumpReturn();

            // Read polygons.
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
            writer.writeUnsignedByte((short) this.staticEntities.size());
            writer.writeNull(3);
            writer.writeUnsignedByte((short) this.floorPolygonsByType.get(PSXPolygonType.POLY_F4).size()); // numFloorF4
            writer.writeUnsignedByte((short) this.floorPolygonsByType.get(PSXPolygonType.POLY_FT4).size()); // numFloorFT4
            writer.writeUnsignedByte((short) this.floorPolygonsByType.get(PSXPolygonType.POLY_G4).size()); // numFloorG4
            writer.writeUnsignedByte((short) this.floorPolygonsByType.get(PSXPolygonType.POLY_GT4).size()); // numFloorGT4
            writer.writeUnsignedByte((short) this.ceilingPolygonsByType.get(PSXPolygonType.POLY_F4).size()); // numCeilingF4
            writer.writeUnsignedByte((short) this.ceilingPolygonsByType.get(PSXPolygonType.POLY_FT4).size()); // numCeilingFT4
            writer.writeUnsignedByte((short) this.ceilingPolygonsByType.get(PSXPolygonType.POLY_G4).size()); // numCeilingG4
            writer.writeUnsignedByte((short) this.ceilingPolygonsByType.get(PSXPolygonType.POLY_GT4).size()); // numCeilingGT4

            this.writtenDataOffset = writer.getIndex();
            writer.writeNull(8 * Constants.INTEGER_SIZE); // Pointers to polygon data. (Overwritten to not be null later)
            writer.writeNull(8 * Constants.INTEGER_SIZE); // Runtime pointers.
            writer.writeNullPointer(); // Entity list pointer (overwritten later)
            writer.writeNullPointer(); // Seems to be some pointer.

            // The polygons are written by the QUAD chunk.
        }


        private void saveEntityListAddress(DataWriter writer, int entityListAddress) {
            writer.jumpTemp(this.writtenDataOffset);
            writer.skipBytes(8 * Constants.INTEGER_SIZE); // Skip polygon pointers.
            writer.skipBytes(8 * Constants.INTEGER_SIZE); // Runtime pointers.
            writer.writeInt(entityListAddress); // Entity list address.
            writer.jumpReturn();
        }

        private void savePolygonAddresses(DataWriter writer) {
            writer.jumpTemp(this.writtenDataOffset);
            this.writtenDataOffset = -1;

            // Write floor polygon pointers.
            for (int i = 0; i < POLYGON_TYPE_ORDER.size(); i++) {
                PSXPolygonType polygonType = POLYGON_TYPE_ORDER.get(i);
                writer.writeInt(getMap().getQuadPacket().getPolygonStartPointer(this, polygonType));
            }

            // Write ceiling polygon pointers.
            for (int i = 0; i < POLYGON_TYPE_ORDER.size(); i++) {
                PSXPolygonType polygonType = POLYGON_TYPE_ORDER.get(i);
                writer.writeInt(getMap().getQuadPacket().getPolygonStartPointer(this, polygonType));
            }

            writer.jumpReturn();
        }

        /**
         * Setup the editor for this grid entry.
         * @param manager The manager to setup the UI for.
         * @param editor  The editor to use to create the UI.
         */
        public void setupEditor(OldFroggerGridManager manager, GUIEditorGrid editor) {
            editor.addLabel("All Polygons", String.valueOf(this.polygons.size()));
            for (PSXPolygonType polygonType : POLYGON_TYPE_ORDER) {
                List<OldFroggerMapPolygon> polygons = this.floorPolygonsByType.get(polygonType);
                editor.addLabel("Floor " + polygonType.getName() + "s", String.valueOf(polygons != null ? polygons.size() : 0));
            }

            for (PSXPolygonType polygonType : POLYGON_TYPE_ORDER) {
                List<OldFroggerMapPolygon> polygons = this.ceilingPolygonsByType.get(polygonType);
                editor.addLabel("Ceiling " + polygonType.getName() + "s", String.valueOf(polygons != null ? polygons.size() : 0));
            }

            // Entity List
            editor.addBoldLabel("Entities (" + this.staticEntities.size() + "):");
            for (int i = 0; i < this.staticEntities.size(); i++) {
                OldFroggerMapEntity entity = this.staticEntities.get(i);
                editor.addLabelButton(entity.getEntityId() + "/" + entity.getDebugName(), "Remove", () -> {
                    this.staticEntities.remove(entity);
                    manager.updateEditor(); // Update the editor to remove this from the display.
                });
            }

            // Add Entity Button
            editor.addButton("Add Entity", () -> {
                InputMenu.promptInput("Please enter the ID of the entity to add.", str -> {
                    if (!Utils.isInteger(str)) {
                        Utils.makePopUp("'" + str + "' is not a valid number.", AlertType.WARNING);
                        return;
                    }

                    int entityId = Integer.parseInt(str);
                    OldFroggerMapEntity foundEntity = null;
                    for (OldFroggerMapEntity testEntity : getMap().getEntityMarkerPacket().getEntities()) {
                        if (testEntity.getEntityId() == entityId) {
                            foundEntity = testEntity;
                            break;
                        }
                    }

                    if (foundEntity == null) {
                        Utils.makePopUp("No entity was found with ID " + entityId, AlertType.WARNING);
                        return;
                    }

                    this.staticEntities.add(foundEntity); // Add the entity.
                    manager.updateEditor(); // Update the UI.
                });
            });
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