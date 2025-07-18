package net.highwayfrogs.editor.games.sony.beastwars.map;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsTexFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.*;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapMesh;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapVertex;
import net.highwayfrogs.editor.games.sony.beastwars.ui.BeastWarsMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.utils.DynamicMeshObjExporter;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Beast Wars Map File
 * Beast Wars maps are a height-field grid, with uniform spacing.
 * In Open Inventor, mappy most likely modelled them as a "SoQuadMesh", and only gave access to editing the vertex height.
 *
 * TODO: We may have orientation wrong. Eg: This may create a mesh aligned to the wrong axis or something. Use Collprims, arch's information, etc.
 * It appears every map tile's world position is similar to Frogger. In other words, 1.0 in fixed point is (1 << 8), and getting the grid position is (worldPos >> 8).
 * Data which probably is in the level but we don't know where:
 * - Player Start Pos (Object Pos?)
 * - Trigger zones? Information about where on the map your energon meter is drained. These are probably ZONE or tile flags, but not sure.
 * TODO: Ready:
 * - Splines
 * - Zones
 * - Texture Info Entries
 * - Grid Editing
 * - Light
 * Not Ready:
 * - Objects
 * -
 * TODO: Create data editors.
 * TODO: Allow saving. (Adapt to match the SCChunkedFile<> utility?)
 * TODO: Shading is slightly off. For example, the energon shading in the first maximal level looks blueish in-game on the crystals, but redish in FrogLord.
 *  -> The red walls look right though.
 *  -> Also, there are some green mess ups. I wonder if we need to be giving an extra green bit.
 * Created by Kneesnap on 9/12/2023.
 */
@Getter
public class BeastWarsMapFile extends SCGameFile<BeastWarsInstance> {
    private short heightMapXLength = 64; // The scale appears to be << 7 aka multiplied by 128.
    private short heightMapZLength = 64;
    private short worldHeightScale = 4;
    private int unknownInfoValue1 = 8;
    private short unknownInfoValue2 = 2; // Seems to be zero for unused maps, or rather maps with empty data for most things.
    private short[][] heightMap;
    private short[][] tileMap;
    private short[][] faceColors;
    private final MapTextureInfoEntry[] textureInfoEntries = new MapTextureInfoEntry[256];
    private final List<BeastWarsMapCollprim> collprims = new ArrayList<>();
    private final List<BeastWarsMapZone> zones = new ArrayList<>();
    private final List<BeastWarsMapLine> lines = new ArrayList<>();
    private final List<BeastWarsMapSpline> splines = new ArrayList<>();
    private final List<BeastWarsMapObject> objects = new ArrayList<>();
    private final BeastWarsMapLight[] lights;

    private WeakReference<BeastWarsTexFile> cachedTextureFile;

    // Unsaved data.
    private final transient BeastWarsMapVertex[][] verticeWrappers = new BeastWarsMapVertex[MAXIMUM_MAP_DIMENSION][MAXIMUM_MAP_DIMENSION];

    public static final short TEXTURE_ID_NO_TEXTURE = 0xFF;

    // There is a maximum of 0x4000 bytes (16Kb) of data allocated for the grids like the tile map or height map.
    // Additionally, the game will chop off the 7th bit (anything allowing data > 128) when reading the data.
    // So, while in terms of memory it could hold a 64x256 map, the game could itself wouldn't operate correctly.
    // As such, both dimensions are always limited to a maximum size of 128.
    public static final int MAXIMUM_MAP_DIMENSION = 128;

    public static final int LIGHT_COUNT = 4;

    private static final int SIGNATURE_MAIN = 0x304D5742; // 'BMW0' - Empty Section -- Marks the start of a map file.
    private static final int SIGNATURE_INFO = 0x4F464E49; // 'INFO'
    private static final int SIGNATURE_HEIGHT_MAP = 0x54414446; // 'FDAT'
    private static final int SIGNATURE_TILE_MAP = 0x54544146; // 'FATT'
    private static final int SIGNATURE_RDAT = 0x54414452; // 'RDAT' - Always Empty -- It's unclear what this section was but it appears like it would have read an amount of data based on terrain dimensions.
    private static final int SIGNATURE_RATT = 0x54544152; // 'RATT' - Always Empty -- It's unclear what this section was but it appears like it would have read an amount of data based on terrain dimensions.
    private static final int SIGNATURE_TEXTURE_INFORMATION = 0x464E4954; // 'TINF'
    private static final int SIGNATURE_SPLINE = 0x4E4C5053; // 'SPLN' - Map Splines
    private static final int SIGNATURE_ZONE = 0x454E4F5A; // 'ZONE' - Map Zone Definitions
    private static final int SIGNATURE_LINE = 0x454E494C; // 'LINE' - Map Lines
    private static final int SIGNATURE_OBJECTS = 0x534A424F; // 'OBJS' - Map Objects TODO: Need to figure out format.
    private static final int SIGNATURE_FCOL = 0x4C4F4346; // 'FCOL' - Face Colors
    private static final int SIGNATURE_RCOL = 0x4C4F4352; // 'RCOL' - Always Empty, Ghidra doesn't even show any remaining code from this.
    private static final int SIGNATURE_COLP = 0x504C4F43; // 'COLP' - Collision Primitives
    private static final int SIGNATURE_LIGHTS = 0x5447494C; // 'LIGT' TODO: Unimplemented.

    public static final String FILE_SIGNATURE = Utils.toIdentifierString(SIGNATURE_MAIN); // 'BMW0'

    public BeastWarsMapFile(BeastWarsInstance instance) {
        super(instance);
        this.lights = new BeastWarsMapLight[LIGHT_COUNT];
        for (int i = 0; i < this.lights.length; i++)
            this.lights[i] = new BeastWarsMapLight(instance);
    }

    /**
     * Gets a vertex object for a grid position.
     * This vertex object is a wrapper which allows for working with map vertices as objects even if their data is scattered in practice.
     * @param gridX The grid X position.
     * @param gridZ The grid Z position.
     * @return vertexObj or null.
     */
    public BeastWarsMapVertex getVertex(int gridX, int gridZ) {
        if (gridX < 0 || gridZ < 0 || gridX >= this.heightMapXLength || gridZ >= this.heightMapZLength)
            return null;

        BeastWarsMapVertex vertex = this.verticeWrappers[gridZ][gridX];
        if (vertex == null)
            this.verticeWrappers[gridZ][gridX] = vertex = new BeastWarsMapVertex(this, gridX, gridZ);

        return vertex;
    }

    /**
     * Gets the .TEX texture list file which contains the texture file for this map.
     * If it cannot be found, null will be returned.
     */
    public BeastWarsTexFile getTextureFile() {
        BeastWarsTexFile cachedFile = this.cachedTextureFile != null ? this.cachedTextureFile.get() : null;
        if (cachedFile != null)
            return cachedFile;

        BeastWarsInstance instance = getGameInstance();

        // Search for a texture file with the same file name.
        MWIResourceEntry texResourceEntry = instance.getResourceEntryByName(FileUtils.stripExtension(getFileDisplayName()) + ".TEX");
        BeastWarsTexFile texFile = instance.getGameFile(texResourceEntry);
        if (texFile != null) {
            this.cachedTextureFile = new WeakReference<>(texFile);
            return texFile;
        }

        // Check the file immediately after the .MAP, and use it if it is a texture file.
        MWIResourceEntry mwiEntry = getIndexEntry();
        if (mwiEntry != null) {
            SCGameFile<?> nextFile = instance.getGameFile(mwiEntry.getResourceId() + 1);
            if (nextFile instanceof BeastWarsTexFile) {
                texFile = (BeastWarsTexFile) nextFile;
                this.cachedTextureFile = new WeakReference<>(texFile);
                return texFile;
            }
        }

        // Otherwise, we haven't found it.
        return null;
    }

    @Override
    public void load(DataReader reader) {
        while (reader.hasMore()) {
            int signature = reader.readInt();
            int size = reader.readInt();
            int endPos = reader.getIndex() + size;

            if (signature == SIGNATURE_MAIN) { // 'BWM0'.
                readEmptySection(signature, size);
            } else if (signature == SIGNATURE_INFO) { // 'INFO'
                readInfoSection(reader);
            } else if (signature == SIGNATURE_HEIGHT_MAP) { // 'FDAT' - Height Map
                readHeightMap(reader);
            } else if (signature == SIGNATURE_TILE_MAP) { // 'FATT' - Texture Map
                readTextureMap(reader);
            } else if (signature == SIGNATURE_RDAT) { // 'RDAT' - Unknown Contents (Empty Section)
                readEmptySection(signature, size);
            } else if (signature == SIGNATURE_RATT) { // 'RATT' - Unknown Contents (Empty Section)
                readEmptySection(signature, size);
            } else if (signature == SIGNATURE_TEXTURE_INFORMATION) { // 'TINF' - Texture Information
                readTextureInformation(reader);
            } else if (signature == SIGNATURE_SPLINE) { // 'SPLN' - Splines
                readSplines(reader);
            } else if (signature == SIGNATURE_ZONE) { // 'ZONE' - Map Zone Data
                readZones(reader);
            } else if (signature == SIGNATURE_LINE) { // 'LINE' - Map Lines
                readLines(reader);
            } else if (signature == SIGNATURE_OBJECTS) { // 'OBJS' - Entites / Objects
                readObjects(reader, size);
            } else if (signature == SIGNATURE_FCOL) { // 'FCOL' - Face Colors? Face Collision?
                readFaceColors(reader);
            } else if (signature == SIGNATURE_RCOL) { // 'RCOL' - Unknown Contents (Empty Section)
                readEmptySection(signature, size);
            } else if (signature == SIGNATURE_COLP) { // 'COLP' - Collision Primitives
                readCollisionPrimitives(reader, size);
            } else if (signature == SIGNATURE_LIGHTS) {
                readLights(reader, size);
            } else {
                getLogger().warning("Skipping unsupported section '%s' (%d bytes).", Utils.toIdentifierString(signature), size);
                reader.skipBytes(size);
            }

            reader.align(4); // Automatically align to the next section.
            if (endPos != reader.getIndex()) {
                getLogger().warning("Didn't end at the right position for '%s'. (Expected: 0x%X, Actual: 0x%X)", Utils.toIdentifierString(signature), endPos, reader.getIndex());
                reader.setIndex(endPos);
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(getRawFileData()); // TODO: TOSS

        // TODO: Implement
        // TODO: Don't forget to align to 4 byte intervals after writing a section!
        /*
        writeEmptySection(writer, SIGNATURE_MAIN); // 'BWM0'
        writeInfoSection(writer); // 'INFO'
        writeHeightMap(writer); // 'FDAT' (Height Map)
        writeTextureMap(writer); // 'FATT' (Texture Map)
        writeEmptySection(writer, SIGNATURE_RDAT); // 'RDAT' (Unknown Contents / Empty Section)
        writeEmptySection(writer, SIGNATURE_RATT); // 'RATT' (Unknown Contents / Empty Section)
        writeTextureInformation(writer); // 'TINF' (Map Texture Information)
        writeSplines(writer); // 'SPLN' (Spline Data)
        writeZones(writer); // 'ZONE' (Zone Data)
        writeLines(writer); // 'LINE' (Line data)
        writeObjects(writer); // 'OBJS' (Entity / Object Data)
        writeFaceColors(writer); // 'FCOL'
        writeEmptySection(writer, SIGNATURE_RCOL); // 'RCOL' (Unknown Contents / Empty Section)
        writeCollisionPrimitives(writer); // 'COLP' (Collision Primitives)
        writeLights(writer); // 'LIGT' (Map Lighting Data)
         */
    }

    private void readEmptySection(int signature, int size) {
        if (size != 0)
            getLogger().warning("Section '%s' was expected to be empty, but reporting having %d bytes.", Utils.toIdentifierString(signature), size);
    }

    private void readInfoSection(DataReader reader) {
        this.heightMapXLength = reader.readShort();
        this.heightMapZLength = reader.readShort();
        this.worldHeightScale = reader.readShort();
        this.unknownInfoValue1 = reader.readInt();
        this.unknownInfoValue2 = reader.readShort();
    }

    private void readHeightMap(DataReader reader) {
        this.heightMap = new short[this.heightMapZLength][this.heightMapXLength];
        for (int z = 0; z < this.heightMapZLength; z++)
            for (int x = 0; x < this.heightMapXLength; x++)
                this.heightMap[z][x] = reader.readUnsignedByteAsShort();
    }

    private void readTextureMap(DataReader reader) {
        this.tileMap = new short[this.heightMapZLength][this.heightMapXLength];
        for (int z = 0; z < this.heightMapZLength; z++)
            for (int x = 0; x < this.heightMapXLength; x++)
                this.tileMap[z][x] = reader.readUnsignedByteAsShort();
    }

    private void readTextureInformation(DataReader reader) {
        for (int i = 0; i < this.textureInfoEntries.length; i++) {
            MapTextureInfoEntry newEntry = new MapTextureInfoEntry(getGameInstance());
            newEntry.load(reader);
            this.textureInfoEntries[i] = newEntry;
        }
    }

    private void readSplines(DataReader reader) {
        this.splines.clear();
        int splineDataStart = reader.getIndex();
        int splineCount = reader.readUnsignedShortAsInt();

        int highestEndPos = reader.getIndex();
        for (int i = 0; i < splineCount; i++) {
            int splineDataOffset = reader.readUnsignedShortAsInt();

            // Read spline.
            reader.jumpTemp(splineDataStart + splineDataOffset);
            BeastWarsMapSpline spline = new BeastWarsMapSpline(this);
            spline.load(reader);
            highestEndPos = Math.max(highestEndPos, reader.getIndex());
            reader.jumpReturn();

            this.splines.add(spline);
        }

        reader.setIndex(highestEndPos);
    }

    private void readZones(DataReader reader) {
        this.zones.clear();
        int zoneDataStart = reader.getIndex();
        int zoneCount = reader.readUnsignedShortAsInt();

        int highestEndPos = reader.getIndex();
        for (int i = 0; i < zoneCount; i++) {
            int zoneDataOffset = reader.readUnsignedShortAsInt();

            // Read ZONE.
            reader.jumpTemp(zoneDataStart + zoneDataOffset);
            BeastWarsMapZone zone = new BeastWarsMapZone(this);
            zone.load(reader);
            highestEndPos = Math.max(highestEndPos, reader.getIndex());
            reader.jumpReturn();

            this.zones.add(zone);
        }

        reader.setIndex(highestEndPos);
    }

    private void readLines(DataReader reader) {
        this.lines.clear();
        int lineDataStart = reader.getIndex();
        int lineCount = reader.readUnsignedShortAsInt();

        // Read data.
        int highestEndPos = reader.getIndex();
        for (int i = 0; i < lineCount; i++) {
            int lineDataOffset = reader.readUnsignedShortAsInt();

            // Read line.
            reader.jumpTemp(lineDataStart + lineDataOffset);
            BeastWarsMapLine line = new BeastWarsMapLine(this);
            line.load(reader);
            highestEndPos = Math.max(highestEndPos, reader.getIndex());
            reader.jumpReturn();

            this.lines.add(line);
        }

        reader.setIndex(highestEndPos);
    }

    private void readObjects(DataReader reader, int size) {
        int dataEndPointer = reader.getIndex() + size;
        this.objects.clear();
        // TODO: PC Problem: "Didn't end at the right position for 'OBJS' in MS3_M_IB.MAP. (Expected: 0x936C, Actual: 0x9354)"
        while (dataEndPointer >= reader.getIndex() + BeastWarsMapObject.SIZE_IN_BYTES) {
            BeastWarsMapObject mapObject = new BeastWarsMapObject(this);
            mapObject.load(reader);
            this.objects.add(mapObject);
        }
    }

    private void readFaceColors(DataReader reader) {
        // TODO: There's a decent chance this is a PSXClutColor.
        this.faceColors = new short[this.heightMapZLength][this.heightMapXLength];
        for (int z = 0; z < this.heightMapZLength; z++)
            for (int x = 0; x < this.heightMapXLength; x++)
                this.faceColors[z][x] = reader.readShort();
    }

    private void readCollisionPrimitives(DataReader mapReader, int size) {
        // We need a new reader because the MRCollprim wants to get an absolute offset to the matrix, if it plans to load one.
        byte[] fullBytes = mapReader.readBytes(size);
        DataReader reader = new DataReader(new ArraySource(fullBytes));

        //getLogger().warning(getFileDisplayName() + ":"); // TODO: TOSS
        this.collprims.clear();
        int collPrimCount = reader.readInt();
        for (int i = 0; i < collPrimCount; i++) {
            BeastWarsMapCollprim collprim = new BeastWarsMapCollprim(this);
            this.collprims.add(collprim); // Add collprim first so in-case something in the read process wants to get the position in the list, it's there. Used for debugging sometimes.
            collprim.load(reader);
            //getLogger().warning(collprim.toString()); // TODO: TOSS
        }
    }

    private void readLights(DataReader reader, int size) {
        if (size != BeastWarsMapLight.SIZE_IN_BYTES * this.lights.length)
            throw new RuntimeException("Unexpected size value for light section: " + size);

        for (int i = 0; i < this.lights.length; i++)
            this.lights[i].load(reader);

        // The game does NOT copy the direction from the first light, which I think forces it to be an ambient light source.
        SVector firstLightDirection = this.lights[0].getDirection();
        if (firstLightDirection.getX() != 0 || firstLightDirection.getY() != 0 || firstLightDirection.getZ() != 0 || firstLightDirection.getPadding() != 0)
            getLogger().warning("Expected the direction of the first light to be zeroed, but it wasn't! (%d, %d, %d, %d)",
                    firstLightDirection.getX(), firstLightDirection.getY(), firstLightDirection.getZ(), firstLightDirection.getPadding());
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.TREASURE_MAP_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return null;
    }

    @Override
    public void performDefaultUIAction() {
        MeshViewController.setupMeshViewer(getGameInstance(), new BeastWarsMapMeshController(getGameInstance()), new BeastWarsMapMesh(this));
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Map Dimensions", this.heightMapXLength + "x" + this.heightMapZLength);
        propertyList.add("World Height Scale", this.worldHeightScale);
        propertyList.add("Unknown Values: ", this.unknownInfoValue1 + ", " + this.unknownInfoValue2);

        // Texture Counts
        int activeTextureCount = 0;
        for (int i = 0; i < this.textureInfoEntries.length; i++) {
            MapTextureInfoEntry infoEntry = this.textureInfoEntries[i];
            if (infoEntry != null && infoEntry.isActive())
                activeTextureCount++;
        }

        // Region count
        int regionCount = 0;
        for (int i = 0; i < this.zones.size(); i++)
            regionCount += this.zones.get(i).getRegions().size();

        // Active light count.
        int lightCount = 0;
        for (int i = 0; i < this.lights.length; i++)
            if (this.lights[i].isActive())
                lightCount++;

        propertyList.add("Textures", activeTextureCount);
        propertyList.add("Collision Primitives", this.collprims.size());
        propertyList.add("Zones", this.zones.size() + " (" + regionCount + " Regions)");
        propertyList.add("Lines", this.lines.size());
        propertyList.add("Splines", this.splines.size());
        propertyList.add("Objects", this.objects.size());
        propertyList.add("Lights", lightCount);
        return propertyList;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportAsObjFile = new MenuItem("Export as .obj file.");
        contextMenu.getItems().add(exportAsObjFile);
        exportAsObjFile.setOnAction(event ->
                DynamicMeshObjExporter.askUserToMeshToObj(getGameInstance(), getLogger(), new BeastWarsMapMesh(this), FileUtils.stripExtension(getFileDisplayName()), true));
    }

    /**
     * Return true iff the map viewer should enable shading by default.
     */
    public boolean shouldMapViewerEnableShadingByDefault() {
        // If there are no textures, don't enable shading!
        BeastWarsTexFile textureFile = getTextureFile();
        if (textureFile == null || textureFile.getImages().isEmpty())
            return false;

        // If the shading is minimum brightness (fully black), don't enable shading.
        for (int z = 0; z < this.faceColors.length; z++)
            for (int x = 0; x < this.faceColors.length; x++)
                if (this.faceColors[z][x] > 0)
                    return true;

        return false;
    }
}