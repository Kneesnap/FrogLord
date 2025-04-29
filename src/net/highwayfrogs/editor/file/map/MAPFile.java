package net.highwayfrogs.editor.file.map;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.form.Form;
import net.highwayfrogs.editor.file.map.form.OldForm;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.group.MAPGroup;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;
import net.highwayfrogs.editor.file.map.poly.line.MAPLineType;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygonType;
import net.highwayfrogs.editor.file.map.zone.Zone;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.*;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGeneral.FroggerMapStartRotation;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.*;
import java.util.Map.Entry;

/**
 * Parses Frogger MAP files.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public class MAPFile extends SCGameData<FroggerGameInstance> {
    private final FroggerMapFile newMapFile;
    @Setter private short startXTile;
    @Setter private short startZTile;
    @Setter private FroggerMapStartRotation startRotation;
    private FroggerMapTheme theme; // This controls loads of things. It's dubious we'd be able to change this safely.
    @Setter private short levelTimer;
    @Setter private SVector cameraSourceOffset;
    @Setter private SVector cameraTargetOffset;
    @Setter private short baseXTile; // These point to the bottom left of the map group grid.
    @Setter private short baseZTile;
    private final List<Path> paths = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    private final List<Form> forms = new ArrayList<>();
    private final List<OldForm> oldForms = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();
    private final List<SVector> vertexes = new ArrayList<>();
    private final List<GridStack> gridStacks = new ArrayList<>();
    private final List<MAPAnimation> mapAnimations = new ArrayList<>();
    @Setter private short groupXCount;
    @Setter private short groupZCount;
    @Setter private short groupXSize = (short) 768; // Seems to always be 768. Appears to be related to the X size of one group. This is actually fixed point.
    @Setter private short groupZSize = (short) 768; // Seems to always be 768. Appears to be related to the Z size of one group. This is actually fixed point.

    @Setter private short gridXCount;
    @Setter private short gridZCount;
    private short gridXSize = (short) 256; // Seems to always be 256. This is actually fixed point.
    private short gridZSize = (short) 256; // Seems to always be 256. This is actually fixed point.

    private transient VLOArchive vlo;
    private final transient Map<MAPPrimitiveType, List<MAPPrimitive>> polygons = new HashMap<>();
    private final transient List<MAPPolygon> allPolygons = new ArrayList<>();

    private final transient Map<Integer, MAPPrimitive> loadPointerPolygonMap = new HashMap<>();

    private final transient Map<MAPPrimitive, Integer> savePolygonPointerMap = new HashMap<>();
    private final transient Map<Integer, MAPPrimitive> savePointerPolygonMap = new HashMap<>();

    public static final String SIGNATURE = "FROG";
    private static final String VERSION = "2.00";
    private static final String COMMENT = "Maybe this time it'll all work fine...";
    private static final int COMMENT_BYTES = 64;
    private static final String GENERAL_SIGNATURE = "GENE"; // General Header
    private static final String PATH_SIGNATURE = "PATH"; // Path Table Packet
    private static final String ZONE_SIGNATURE = "ZONE"; // Zone Table Packet
    private static final String FORM_SIGNATURE = "FORM"; // Form Instance Table Packet. Forms are wrappers for MOFS in the world. Their primary function is to define behavior for models used in-game and to have multiple reactions on different parts of a model. Original Tool: Formy.
    private static final String ENTITY_SIGNATURE = "EMTP"; // Entity Markers Table Packet
    private static final String GRAPHICAL_SIGNATURE = "GRAP"; // Graphic Map Header. (Formerly "GRMP")
    private static final String LIGHT_SIGNATURE = "LITE"; // Lights Chunk
    private static final String GROUP_SIGNATURE = "GROU"; // Group Header
    private static final String POLYGON_SIGNATURE = "POLY"; // Formerly "QUAD" (Quad Chunk)
    private static final String VERTEX_SIGNATURE = "VRTX"; // Vertex Chunk
    private static final String GRID_SIGNATURE = "GRID"; // Grid Chunk
    private static final String ANIMATION_SIGNATURE = "ANIM"; // Anim Chunk
    // Removed chunks:
    // "STND" - Standard Chunk, Contained general information like camera data, heights, etc., which seems to have been completely eliminated.

    public static final short MAP_ANIMATION_TEXTURE_LIST_TERMINATOR = (short) 0xFFFF;
    private static final int TOTAL_CHECKPOINT_TIMER_ENTRIES = 5;

    public static final List<MAPPrimitiveType> PRIMITIVE_TYPES = new ArrayList<>();
    public static final List<MAPPrimitiveType> POLYGON_TYPES = Arrays.asList(MAPPolygonType.values());

    // NOTE: Changing MAPFile.VERTEX_COLOR_IMAGE_SIZE to a higher value will improve the shading quality, but may
    //       also break texture related stuff elsewhere (I haven't done much in the way of testing).
    //       Try changing the default value from 8 to 32 for example.
    //       I don't like the fact we are relying on texture generation and resolution to shade the polygons. We should
    //       really just be setting vertex color values and letting the hardware do the shading work. This just seems
    //       very, very wrong - but I don't know how we can get around it right now due to the crappy limitations of
    //       JavaFx. It's a problem for sure.
    public static final byte VERTEX_SHADING_APPROXIMATION_ALPHA = (byte) 0x80;
    public static final int VERTEX_COLOR_IMAGE_SIZE = 12;
    private static final ImageFilterSettings OBJ_EXPORT_FILTER = new ImageFilterSettings(ImageState.EXPORT)
            .setTrimEdges(true).setAllowTransparency(true).setAllowFlip(true);

    static {
        PRIMITIVE_TYPES.addAll(POLYGON_TYPES);
        PRIMITIVE_TYPES.add(MAPLineType.G2);
    }

    public MAPFile(FroggerMapFile newMapFile) {
        super(newMapFile.getGameInstance());
        this.newMapFile = newMapFile;
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.newMapFile.getLogger(), "OldFormat", AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        FroggerMapConfig mapConfig = getMapConfig();
        getLoadPointerPolygonMap().clear();

        reader.verifyString(SIGNATURE);
        reader.skipInt(); // File length.
        reader.verifyString(VERSION);
        reader.skipBytes(COMMENT_BYTES); // Comment bytes.

        int generalAddress = reader.readInt();
        int graphicalAddress = reader.readInt();
        int formAddress = reader.readInt();
        int entityAddress = reader.readInt();
        int zoneAddress = reader.readInt();
        int pathAddress = reader.readInt();

        reader.setIndex(generalAddress);
        reader.verifyString(GENERAL_SIGNATURE);
        this.startXTile = reader.readShort();
        this.startZTile = reader.readShort();
        this.startRotation = FroggerMapStartRotation.values()[reader.readShort()];
        this.theme = FroggerMapTheme.values()[reader.readShort()];

        this.levelTimer = reader.readShort();
        reader.skipBytes((TOTAL_CHECKPOINT_TIMER_ENTRIES - 1) * Constants.SHORT_SIZE);

        reader.skipShort(); // Unused perspective variable.

        this.cameraSourceOffset = new SVector();
        this.cameraSourceOffset.loadWithPadding(reader);

        this.cameraTargetOffset = new SVector();
        this.cameraTargetOffset.loadWithPadding(reader);
        reader.skipBytes(4 * Constants.SHORT_SIZE); // Unused "LEVEL_HEADER" data.

        reader.setIndex(pathAddress);
        reader.verifyString(PATH_SIGNATURE);
        int pathCount = reader.readInt();

        // PATH
        for (int i = 0; i < pathCount; i++) {
            reader.jumpTemp(reader.readInt()); // Starts after the pointers.
            Path path = new Path(this);
            path.load(reader);
            this.paths.add(path);
            reader.jumpReturn();
        }

        // Read Camera Zones.
        reader.setIndex(zoneAddress);
        reader.verifyString(ZONE_SIGNATURE);
        int zoneCount = reader.readInt();

        for (int i = 0; i < zoneCount; i++) {
            reader.jumpTemp(reader.readInt()); // Move to the zone location.
            Zone zone = new Zone();
            zone.load(reader);
            this.zones.add(zone);
            reader.jumpReturn();
        }

        // Read forms.
        reader.setIndex(formAddress);
        reader.verifyString(FORM_SIGNATURE);
        int formCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding.

        for (int i = 0; i < formCount; i++) {
            reader.jumpTemp(reader.readInt());

            if (getMapConfig().isOldFormFormat()) {
                OldForm oldForm = new OldForm(this);
                oldForm.load(reader);
                this.oldForms.add(oldForm);
            } else {
                Form form = new Form();
                form.load(reader);
                forms.add(form);
            }

            reader.jumpReturn();
        }

        // Read entities
        reader.setIndex(entityAddress);
        reader.verifyString(ENTITY_SIGNATURE);
        reader.skipInt(); // Entity packet length.
        int entityCount = reader.readShort();
        reader.skipShort(); // Padding.

        Entity lastEntity = null;
        for (int i = 0; i < entityCount; i++) {
            int newEntityPointer = reader.readInt();
            printInvalidEntityReadDetection(reader, lastEntity, newEntityPointer);

            reader.jumpTemp(newEntityPointer);
            try {
                Entity entity = new Entity(this);
                entity.load(reader);
                entities.add(entity);
                lastEntity = entity;
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Failed to load an entity.");
                lastEntity = null;
            }

            reader.jumpReturn();
        }
        printInvalidEntityReadDetection(reader, lastEntity, graphicalAddress); // Go over the last entity.

        // 'GRAP'
        reader.setIndex(graphicalAddress);
        reader.verifyString(GRAPHICAL_SIGNATURE);
        int lightAddress = reader.readInt();
        int groupAddress = reader.readInt();
        int polygonAddress = reader.readInt();
        int vertexAddress = reader.readInt();
        int gridAddress = reader.readInt();
        int animAddress = mapConfig.isMapAnimationSupported() ? reader.readInt() : 0; // Animations don't exist yet in some of the older maps.

        reader.setIndex(lightAddress);
        reader.verifyString(LIGHT_SIGNATURE);
        int lightCount = reader.readInt();
        for (int i = 0; i < lightCount; i++) {
            Light light = new Light();
            light.load(reader);
            lights.add(light);
        }

        reader.setIndex(groupAddress);
        reader.verifyString(GROUP_SIGNATURE);
        SVector loadedBasePoint = SVector.readWithPadding(reader);
        this.groupXCount = reader.readShort(); // Number of groups in x.
        this.groupZCount = reader.readShort(); // Number of groups in z.
        this.groupXSize = reader.readShort(); // Group X Length
        this.groupZSize = reader.readShort(); // Group Z Length

        MAPGroup[] loadGroups = new MAPGroup[getGroupCount()];
        for (int i = 0; i < loadGroups.length; i++) {
            MAPGroup group = new MAPGroup(mapConfig);
            group.load(reader);
            loadGroups[i] = group;
        }

        // Read POLY
        reader.setIndex(polygonAddress);
        reader.verifyString(POLYGON_SIGNATURE);

        Map<MAPPrimitiveType, Short> polyCountMap = new HashMap<>();
        Map<MAPPrimitiveType, Integer> polyOffsetMap = new HashMap<>();

        List<MAPPrimitiveType> types = getTypes(mapConfig);
        for (MAPPrimitiveType type : types)
            polyCountMap.put(type, reader.readShort());

        if ((types.size() % 2) != 0)
            reader.skipShort(); // Padding.

        for (MAPPrimitiveType type : types)
            polyOffsetMap.put(type, reader.readInt());

        for (MAPPrimitiveType type : PRIMITIVE_TYPES) {
            List<MAPPrimitive> primitives = new ArrayList<>();
            polygons.put(type, primitives);
            if (!types.contains(type))
                continue; // Not being read.

            short polyCount = polyCountMap.get(type);
            int polyOffset = polyOffsetMap.get(type);

            if (polyCount > 0) {
                reader.jumpTemp(polyOffset);

                try {
                    for (int i = 0; i < polyCount; i++) {
                        MAPPrimitive primitive = type.newPrimitive();
                        if (primitive instanceof MAPPolygon)
                            ((MAPPolygon) primitive).setMapFile(this);

                        getLoadPointerPolygonMap().put(reader.getIndex(), primitive);
                        primitive.load(reader);
                        primitives.add(primitive);
                    }
                } catch (Throwable th) {
                    throw new RuntimeException("Failed to load " + primitives.size() + " " + type + " primitives in " + this.newMapFile.getFileDisplayName() + ".", th);
                }

                reader.jumpReturn();
            }
        }

        for (MAPGroup group : loadGroups)
            group.setupPolygonData(this, getPolygons());

        // Read Vertexes.
        reader.setIndex(vertexAddress);
        reader.verifyString(VERTEX_SIGNATURE);
        short vertexCount = reader.readShort();
        reader.skipShort(); // Padding.
        for (int i = 0; i < vertexCount; i++)
            this.vertexes.add(SVector.readWithPadding(reader));

        // Read GRID data.
        reader.setIndex(gridAddress);
        reader.verifyString(GRID_SIGNATURE);
        this.gridXCount = reader.readShort(); // Number of grid squares in x.
        this.gridZCount = reader.readShort();
        this.gridXSize = reader.readShort(); // Grid square x length.
        this.gridZSize = reader.readShort();

        this.baseXTile = (short) ((loadedBasePoint.getX() + 1) / getGridXSize());
        this.baseZTile = (short) ((loadedBasePoint.getZ() + 1) / getGridZSize());
        Utils.verify(loadedBasePoint.getY() == 0, "Base-Point Y is not zero!");

        int stackCount = gridXCount * gridZCount;
        for (int i = 0; i < stackCount; i++) {
            GridStack stack = new GridStack();
            stack.load(reader);
            getGridStacks().add(stack);
        }

        // Find the total amount of squares to read.
        int squareCount = 0;
        for (GridStack stack : gridStacks)
            squareCount = Math.max(squareCount, stack.getTempIndex() + stack.getLoadedSquareCount());

        List<GridSquare> loadedGridSquares = new ArrayList<>();
        for (int i = 0; i < squareCount; i++) {
            GridSquare square = new GridSquare(this);
            square.load(reader);
            loadedGridSquares.add(square);
        }

        getGridStacks().forEach(stack -> stack.loadSquares(loadedGridSquares));

        // Read "ANIM".
        if (animAddress > 0) {
            reader.setIndex(animAddress);
            reader.verifyString(ANIMATION_SIGNATURE);
            int mapAnimCount = reader.readInt(); // 0c
            int mapAnimAddress = reader.readInt(); // 0x2c144
            reader.setIndex(mapAnimAddress); // This points to right after the header.

            for (int i = 0; i < mapAnimCount; i++) {
                MAPAnimation animation = new MAPAnimation(this);
                animation.load(reader);
                mapAnimations.add(animation);
            }
        }

        ThemeBook themeBook = getGameInstance().getThemeBook(getTheme());
        if (themeBook != null)
            this.vlo = themeBook.getVLO(this.newMapFile);
    }

    @Override
    public void save(DataWriter writer) {
        getSavePointerPolygonMap().clear();
        getSavePolygonPointerMap().clear();

        // Write File Header
        writer.writeStringBytes(SIGNATURE);
        int fileLengthPointer = writer.writeNullPointer();
        writer.writeStringBytes(VERSION);
        writer.writeNull(COMMENT_BYTES);

        int generalAddress = writer.getIndex();
        int graphicalAddress = generalAddress + Constants.INTEGER_SIZE;
        int formAddress = graphicalAddress + Constants.INTEGER_SIZE;
        int entityAddress = formAddress + Constants.INTEGER_SIZE;
        int zoneAddress = entityAddress + Constants.INTEGER_SIZE;
        int pathAddress = zoneAddress + Constants.INTEGER_SIZE;
        int writeAddress = pathAddress + Constants.INTEGER_SIZE;

        // Write GENERAL.
        writer.setIndex(writeAddress);
        writer.jumpTemp(generalAddress);
        writer.writeInt(writeAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GENERAL_SIGNATURE);
        writer.writeShort(this.startXTile);
        writer.writeShort(this.startZTile);
        writer.writeShort((short) this.startRotation.ordinal());
        writer.writeShort((short) getTheme().ordinal());
        for (int i = 0; i < TOTAL_CHECKPOINT_TIMER_ENTRIES; i++)
            writer.writeShort(getLevelTimer());

        writer.writeShort((short) 0); // Unused perspective variable.
        this.cameraSourceOffset.saveWithPadding(writer);
        this.cameraTargetOffset.saveWithPadding(writer);
        writer.writeNull(4 * Constants.SHORT_SIZE); // Unused "LEVEL_HEADER" data.

        // Write "PATH".
        int tempAddress = writer.getIndex();
        writer.jumpTemp(pathAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(PATH_SIGNATURE);

        int pathCount = this.paths.size();
        writer.writeInt(pathCount); // Path count.

        int pathPointer = writer.getIndex() + (Constants.POINTER_SIZE * pathCount);
        for (Path path : getPaths()) {
            writer.writeInt(pathPointer);

            writer.jumpTemp(pathPointer);
            path.save(writer);
            pathPointer = writer.getIndex();
            writer.jumpReturn();
        }
        writer.setIndex(pathPointer);

        // Save "ZONE".
        tempAddress = writer.getIndex();
        writer.jumpTemp(zoneAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(ZONE_SIGNATURE);

        int zoneCount = this.zones.size();
        writer.writeInt(zoneCount);

        int zonePointer = writer.getIndex() + (Constants.POINTER_SIZE * zoneCount);
        for (Zone zone : getZones()) {
            writer.writeInt(zonePointer);

            writer.jumpTemp(zonePointer);
            zone.save(writer);
            zonePointer = writer.getIndex();
            writer.jumpReturn();
        }
        writer.setIndex(zonePointer); // Start writing FORM AFTER zone-data. Otherwise, it will write the form data to where the zone data goes.

        // Save "FORM".
        tempAddress = writer.getIndex();
        writer.jumpTemp(formAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(FORM_SIGNATURE);
        int formCount = this.forms.size();
        writer.writeUnsignedShort(formCount);
        writer.writeUnsignedShort(0); // Padding.

        int formPointer = writer.getIndex() + (Constants.POINTER_SIZE * formCount);
        for (Form form : getForms()) {
            writer.writeInt(formPointer);

            writer.jumpTemp(formPointer);
            form.save(writer);
            formPointer = writer.getIndex();
            writer.jumpReturn();
        }
        writer.setIndex(formPointer);

        // Write "EMTP".
        tempAddress = writer.getIndex();
        writer.jumpTemp(entityAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(ENTITY_SIGNATURE);
        writer.writeInt(0); // This is the entity packet length. It is unused.

        short entityCount = (short) this.entities.size();
        writer.writeShort(entityCount);
        writer.writeShort((short) 0); // Padding.

        int entityPointer = writer.getIndex() + (Constants.POINTER_SIZE * entityCount);
        for (Entity entity : getEntities()) {
            writer.writeInt(entityPointer);

            writer.jumpTemp(entityPointer);
            entity.save(writer);
            entityPointer = writer.getIndex();
            writer.jumpReturn();
        }
        writer.setIndex(entityPointer);

        // Write GRAP.
        tempAddress = writer.getIndex();
        writer.jumpTemp(graphicalAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GRAPHICAL_SIGNATURE);
        int lightAddress = writer.getIndex();
        int groupAddress = lightAddress + Constants.POINTER_SIZE;
        int polygonAddress = groupAddress + Constants.POINTER_SIZE;
        int vertexAddress = polygonAddress + Constants.POINTER_SIZE;
        int gridAddress = vertexAddress + Constants.POINTER_SIZE;
        int animAddress = gridAddress + Constants.POINTER_SIZE;
        writer.setIndex(animAddress + Constants.POINTER_SIZE);

        // Write LITE.
        tempAddress = writer.getIndex();
        writer.jumpTemp(lightAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(LIGHT_SIGNATURE);
        writer.writeInt(lights.size());
        getLights().forEach(light -> light.save(writer));

        // Write GROU.
        List<MAPGroup> saveGroups = calculateGroups();

        // This orders polygons a certain way so MAPGroup will have all of its polygons sequentially, which is required for MAPGroup to work properly.
        getPolygons().values().forEach(list -> list.removeIf(MAPPrimitive::isAllowDisplay));
        saveGroups.forEach(group -> {
            for (Entry<MAPPrimitiveType, List<MAPPrimitive>> entry : group.getPolygonMap().entrySet())
                getPolygons().get(entry.getKey()).addAll(entry.getValue());
        });

        tempAddress = writer.getIndex();
        writer.jumpTemp(groupAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GROUP_SIGNATURE);

        makeBasePoint().saveWithPadding(writer);
        writer.writeShort(getGroupXCount());
        writer.writeShort(getGroupZCount());
        writer.writeShort(this.groupXSize);
        writer.writeShort(this.groupZSize);
        saveGroups.forEach(group -> group.save(writer));

        // Save entity indices. The beaver entity uses this.
        getPaths().forEach(path -> path.writeEntityList(this, writer));

        // Write POLY
        tempAddress = writer.getIndex();
        writer.jumpTemp(polygonAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(POLYGON_SIGNATURE);
        for (MAPPrimitiveType type : PRIMITIVE_TYPES)
            writer.writeShort((short) getPolygons().get(type).size());

        writer.writeShort((short) 0); // Padding.

        Map<MAPPrimitiveType, Integer> polyAddresses = new HashMap<>();

        int lastPointer = writer.getIndex();
        for (MAPPrimitiveType type : PRIMITIVE_TYPES) {
            polyAddresses.put(type, lastPointer);
            lastPointer += Constants.POINTER_SIZE;
        }

        writer.setIndex(lastPointer);
        for (MAPPrimitiveType type : PRIMITIVE_TYPES) {
            if (type == MAPLineType.G2)
                continue; // G2 was only enabled as a debug rendering option. It is not enabled in the retail release fairly sure.

            tempAddress = writer.getIndex();

            writer.jumpTemp(polyAddresses.get(type));
            writer.writeInt(tempAddress);
            writer.jumpReturn();

            for (MAPPrimitive prim : getPolygons().get(type)) {
                Integer index = writer.getIndex();
                getSavePointerPolygonMap().put(index, prim);
                getSavePolygonPointerMap().put(prim, index);
                prim.save(writer);
            }
        }

        // Write MAP_GROUP polygon pointers, since we've written polygon data.
        saveGroups.forEach(group -> group.writePolygonPointers(this, writer));

        // Write "VRTX."
        tempAddress = writer.getIndex();
        writer.jumpTemp(vertexAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(VERTEX_SIGNATURE);
        short vertexCount = (short) this.vertexes.size();
        writer.writeShort(vertexCount);
        writer.writeShort((short) 0); // Padding.
        getVertexes().forEach(vertex -> vertex.saveWithPadding(writer));

        // Save GRID data.
        tempAddress = writer.getIndex();
        writer.jumpTemp(gridAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GRID_SIGNATURE);
        writer.writeShort(this.gridXCount);
        writer.writeShort(this.gridZCount);
        writer.writeShort(this.gridXSize);
        writer.writeShort(this.gridZSize);

        int calcStackCount = (getGridXCount() * getGridZCount());
        if (getGridStacks().size() != calcStackCount)
            throw new RuntimeException("Tried to save " + getGridStacks().size() + " grid stacks, required: " + calcStackCount + ".");

        List<GridSquare> saveSquares = new ArrayList<>();
        getGridStacks().forEach(stack -> {
            stack.setTempIndex(saveSquares.size());
            saveSquares.addAll(stack.getGridSquares());
        });

        getGridStacks().forEach(gridStack -> gridStack.save(writer));
        saveSquares.forEach(gridSquare -> gridSquare.save(writer));

        // Save "ANIM" data.
        tempAddress = writer.getIndex();
        writer.jumpTemp(animAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(ANIMATION_SIGNATURE);
        writer.writeInt(this.mapAnimations.size());
        writer.writeInt(writer.getIndex() + Constants.POINTER_SIZE);
        getMapAnimations().forEach(anim -> anim.save(writer));
        getMapAnimations().forEach(anim -> anim.writeTextures(writer));
        writer.writeShort(MAP_ANIMATION_TEXTURE_LIST_TERMINATOR);
        getMapAnimations().forEach(anim -> anim.writeMapUVs(writer));

        writer.writeAddressTo(fileLengthPointer); // Write file length to start of file.
    }

    private void printInvalidEntityReadDetection(DataReader reader, Entity lastEntity, int endPointer) {
        if (lastEntity == null)
            return;

        int realSize = (endPointer - lastEntity.getLoadScriptDataPointer());
        if (realSize != lastEntity.getLoadReadLength()) {
            lastEntity.setInvalid(true);

            FormEntry formEntry = lastEntity.getFormEntry();
            if (!getMapConfig().isIslandPlaceholder()) // No need to print these errors on island placeholders.
                getLogger().warning("[INVALID] Entity " + getEntities().indexOf(lastEntity) + "/" + Integer.toHexString(lastEntity.getLoadScriptDataPointer()) + "/" + lastEntity.getFormGridId() + " REAL: " + realSize + ", READ: " + lastEntity.getLoadReadLength() + (formEntry != null ? ", " + formEntry.getFormTypeName() + ", " + formEntry.getEntityTypeName() : ", " + lastEntity.getTypeName()));

            // Restore reader.
            if (realSize < 1024 && realSize >= 0) {
                reader.jumpTemp(lastEntity.getLoadScriptDataPointer());
                lastEntity.setRawData(reader.readBytes(realSize));
                reader.jumpReturn();
            }
        }
    }

    /**
     * Gets the base point's world x coordinate.
     * @return worldX
     */
    public short getBasePointWorldX() {
        return (short) ((getBaseXTile() * getGridXSize()) - 1);
    }

    /**
     * Gets the base point's world Z coordinate.
     * @return worldZ
     */
    public short getBasePointWorldZ() {
        return (short) ((getBaseZTile() * getGridZSize()) - 1);
    }

    /**
     * Makes a BasePoint SVector.
     * @return basePoint
     */
    public SVector makeBasePoint() {
        return new SVector(getBasePointWorldX(), (short) 0, getBasePointWorldZ());
    }

    /**
     * Get the group X value from a world X value. Expects a 4 bit fixed point short.
     * @param worldX The world X coordinate.
     * @return groupX
     */
    public int getGroupX(int worldX) {
        return (worldX - getBasePointWorldX()) / getGroupXSize();
    }

    /**
     * Get the group Z value from a world Z value. Expects a 4 bit fixed point short.
     * @param worldZ The world Z coordinate.
     * @return groupZ
     */
    public int getGroupZ(int worldZ) {
        return (worldZ - getBasePointWorldZ()) / getGroupZSize();
    }

    /**
     * Gets a map group from group coordinates.
     * @param groupX The group x coordinate.
     * @param groupZ The group z coordinate.
     * @return group
     */
    public int getGroupIndex(int groupX, int groupZ) {
        return (groupZ * getGroupXCount()) + groupX;
    }

    /**
     * Gets the number of groups in this map.
     * @return groupCount
     */
    public int getGroupCount() {
        return getGroupXCount() * getGroupZCount();
    }

    /**
     * Gets all the polygons in this map, in a list.
     * @return allPolygons
     */
    public List<MAPPolygon> getAllPolygons() {
        this.allPolygons.clear();
        for (List<MAPPrimitive> list : getPolygons().values())
            for (MAPPrimitive prim : list)
                if (prim instanceof MAPPolygon)
                    this.allPolygons.add((MAPPolygon) prim);
        return this.allPolygons;
    }
    /**
     * Recalculate map groups.
     */
    public List<MAPGroup> calculateGroups() {
        FroggerMapConfig mapConfig = getMapConfig();
        List<MAPGroup> groups = new ArrayList<>(getGroupCount());
        for (int i = 0; i < getGroupCount(); i++)
            groups.add(new MAPGroup(mapConfig));

        // Recalculate it.
        for (MAPPolygon poly : getAllPolygons()) {
            if (!poly.isAllowDisplay())
                continue;

            SVector vertex = getVertexes().get(poly.getVertices()[poly.getVerticeCount() - 1]);
            int groupX = getGroupX(vertex.getX());
            int groupZ = getGroupZ(vertex.getZ());
            groups.get(getGroupIndex(groupX, groupZ)).getPolygonMap().get(poly.getType()).add(poly);
        }

        return groups;
    }

    /**
     * Get the types to use based on if this is QB or not.
     * @param mapConfig The map config to get types from.
     * @return types
     */
    public static List<MAPPrimitiveType> getTypes(FroggerMapConfig mapConfig) {
        return mapConfig.isG2Supported() ? PRIMITIVE_TYPES : POLYGON_TYPES;
    }

    /**
     * Gets the map config usable by this map.
     * @return mapConfig
     */
    public FroggerMapConfig getMapConfig() {
        return this.newMapFile.getMapConfig();
    }

    /**
     * Applies the map data to the new file format.
     */
    public void applyToNewMapFile() {
        FroggerMapFilePacketGeneral generalPacket = this.newMapFile.getGeneralPacket();
        FroggerMapFilePacketPath pathPacket = this.newMapFile.getPathPacket();
        FroggerMapFilePacketZone zonePacket = this.newMapFile.getZonePacket();
        FroggerMapFilePacketForm formPacket = this.newMapFile.getFormPacket();
        FroggerMapFilePacketEntity entityPacket = this.newMapFile.getEntityPacket();
        FroggerMapFilePacketLight lightPacket = this.newMapFile.getLightPacket();
        FroggerMapFilePacketGroup groupPacket = this.newMapFile.getGroupPacket();
        FroggerMapFilePacketPolygon polygonPacket = this.newMapFile.getPolygonPacket();
        FroggerMapFilePacketVertex vertexPacket = this.newMapFile.getVertexPacket();
        FroggerMapFilePacketGrid gridPacket = this.newMapFile.getGridPacket();
        FroggerMapFilePacketAnimation animationPacket = this.newMapFile.getAnimationPacket();

        // General conversion.
        generalPacket.setStartGridCoordX(this.startXTile);
        generalPacket.setStartGridCoordZ(this.startZTile);
        generalPacket.setStartRotation(this.startRotation);
        generalPacket.setMapTheme(this.theme);
        generalPacket.setStartingTimeLimit(this.levelTimer);
        generalPacket.getDefaultCameraSourceOffset().setValues(this.cameraSourceOffset);
        generalPacket.getDefaultCameraTargetOffset().setValues(this.cameraTargetOffset);
        // TODO: Set ambient frog color to be 0.

        // Paths
        this.newMapFile.getLogger().info("Converting paths...");
        pathPacket.getPaths().clear();
        for (Path path : this.paths)
            pathPacket.getPaths().add(path.convertToNewFormat(this.newMapFile));

        // Zones
        this.newMapFile.getLogger().info("Converting zones...");
        zonePacket.getZones().clear();
        for (Zone zone : this.zones)
            zonePacket.getZones().add(zone.convertToNewFormat(this.newMapFile));

        // Forms
        this.newMapFile.getLogger().info("Converting forms...");
        formPacket.getForms().clear();
        formPacket.getOldForms().clear();
        for (Form form : this.forms)
            formPacket.getForms().add(form.convertToNewFormat(this.newMapFile));
        if (this.oldForms.size() > 0) // Untested, so there's a warning.
            this.newMapFile.getLogger().warning("Cannot convert the %d old forms, as they are not supported for conversion.", this.oldForms.size());

        // Entities
        this.newMapFile.getLogger().info("Converting entities...");
        entityPacket.clear();
        for (Entity entity : this.entities)
            entityPacket.addEntity(entity.convertToNewFormat(this.newMapFile));
        pathPacket.recalculateAllPathEntityLists();

        // Lights
        this.newMapFile.getLogger().info("Converting lights...");
        lightPacket.getLights().clear();
        for (Light light : this.lights)
            lightPacket.getLights().add(light.convertToNewFormat(this.newMapFile));

        // Vertices
        this.newMapFile.getLogger().info("Converting vertices...");
        vertexPacket.getVertices().clear();
        for (int i = 0; i < this.vertexes.size(); i++)
            vertexPacket.getVertices().add(this.vertexes.get(i).clone());

        // Convert polygons.
        this.newMapFile.getLogger().info("Converting polygons...");
        polygonPacket.clearPolygons();
        Map<MAPPrimitive, FroggerMapPolygon> convertedPolygons = new HashMap<>();
        for (MAPPrimitiveType type : PRIMITIVE_TYPES) {
            List<MAPPrimitive> primitives = this.polygons.get(type);
            for (MAPPrimitive primitive : primitives) {
                FroggerMapPolygon newPolygon = primitive.convertToNewFormat(this.newMapFile);
                if ((convertedPolygons.putIfAbsent(primitive, newPolygon)) == null)
                    polygonPacket.addPolygon(newPolygon);
            }
        }

        // Grid Stacks
        this.newMapFile.getLogger().info("Converting collision grid...");
        gridPacket.clear();
        gridPacket.resizeGrid(this.gridXCount, this.gridZCount);
        if (this.gridXSize != gridPacket.getGridXSize())
            throw new RuntimeException("Expected gridXSize of " + this.gridXSize + ", but was actually: " + gridPacket.getGridXSize() + ".");
        if (this.gridZSize != gridPacket.getGridZSize())
            throw new RuntimeException("Expected gridZSize of " + this.gridZSize + ", but was actually: " + gridPacket.getGridZSize() + ".");

        int stackId = 0;
        for (int z = 0; z < this.gridZCount; z++)
            for (int x = 0; x < this.gridXCount; x++)
                this.gridStacks.get(stackId++).convertToNewFormat(gridPacket.getGridStack(x, z), convertedPolygons);

        // Animations
        this.newMapFile.getLogger().info("Converting animations...");
        animationPacket.getAnimations().clear();
        animationPacket.getUnusedTextureIds().clear();
        for (MAPAnimation animation : this.mapAnimations)
            animationPacket.getAnimations().add(animation.convertToNewFormat(this.newMapFile, convertedPolygons));

        // Groups.
        this.newMapFile.getLogger().info("Converting map groups...");
        if (this.groupXSize != groupPacket.getGroupXSize())
            throw new RuntimeException("Expected groupXSize of " + this.groupXSize + ", but was actually: " + groupPacket.getGroupXSize() + ".");
        if (this.groupZSize != groupPacket.getGroupZSize())
            throw new RuntimeException("Expected groupZSize of " + this.groupZSize + ", but was actually: " + groupPacket.getGroupZSize() + ".");
        groupPacket.generateMapGroups(ProblemResponse.THROW_EXCEPTION, true);

        this.newMapFile.getLogger().info("Finished loading map from old format.");
    }

    /**
     * Copy data from an object in the old map format to an object in the new format via serialization.
     * @param oldData the old data to copy from
     * @param newData the new data to copy to
     * @return newData
     * @param <TNewData> the type of the new data
     */
    public static <TNewData extends IBinarySerializable> TNewData copyToNewViaBytes(GameObject oldData, TNewData newData) {
        if (oldData == null)
            throw new NullPointerException("oldData");
        if (newData == null)
            throw new NullPointerException("newData");

        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter writer = new DataWriter(receiver);
        oldData.save(writer);
        writer.closeReceiver();

        DataReader reader = new DataReader(new ArraySource(receiver.toArray()));
        newData.load(reader);
        ILogger logger = (newData instanceof GameData<?>) ? ((GameData<?>) newData).getLogger() : Utils.getInstanceLogger();
        if (reader.getRemaining() != 0)
            logger.warning("New data had %d bytes remaining.", reader.getRemaining());

        return newData;
    }
}