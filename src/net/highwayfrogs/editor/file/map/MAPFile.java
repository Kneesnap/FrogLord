package net.highwayfrogs.editor.file.map;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.form.Form;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridSquareFlag;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.group.MAPGroup;
import net.highwayfrogs.editor.file.map.light.APILightType;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.poly.MAPPolygonData;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;
import net.highwayfrogs.editor.file.map.poly.line.MAPLineType;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyF4;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygonType;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.map.zone.Zone;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.editor.MAPController;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Map.Entry;

/**
 * Parses Frogger MAP files.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public class MAPFile extends GameFile {
    @Setter private short startXTile;
    @Setter private short startZTile;
    @Setter private StartRotation startRotation;
    private MAPTheme theme; // This controls loads of things. It's dubious we'd be able to change this safely.
    @Setter private short levelTimer;
    @Setter private SVector cameraSourceOffset;
    @Setter private SVector cameraTargetOffset;
    @Setter private short baseXTile; // These point to the bottom left of the map group grid.
    @Setter private short baseZTile;
    private List<Path> paths = new ArrayList<>();
    private List<Zone> zones = new ArrayList<>();
    private List<Form> forms = new ArrayList<>();
    private List<Entity> entities = new ArrayList<>();
    private List<Light> lights = new ArrayList<>();
    private List<SVector> vertexes = new ArrayList<>();
    private List<GridStack> gridStacks = new ArrayList<>();
    private List<MAPAnimation> mapAnimations = new ArrayList<>();
    @Setter private short groupXCount;
    @Setter private short groupZCount;
    private short groupXSize = (short) 256; // Seems to always be 256. Appears to be related to the X size of one group.
    private short groupZSize = (short) 256; // Seems to always be 256. Appears to be related to the Z size of one group.

    @Setter private short gridXCount;
    @Setter private short gridZCount;
    private short gridXSize = (short) 768; // Seems to always be 768.
    private short gridZSize = (short) 768; // Seems to always be 768.

    private transient VLOArchive vlo;
    private transient Map<MAPPrimitiveType, List<MAPPrimitive>> polygons = new HashMap<>();
    private transient List<MAPPolygon> allPolygons = new ArrayList<>();

    private transient Map<Integer, MAPPrimitive> loadPointerPolygonMap = new HashMap<>();

    private transient Map<MAPPrimitive, Integer> savePolygonPointerMap = new HashMap<>();
    private transient Map<Integer, MAPPrimitive> savePointerPolygonMap = new HashMap<>();

    public static final int TYPE_ID = 0;
    private static final String SIGNATURE = "FROG";
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
    // "STND" - Standard Chunk, Contained general information like camera data, heights, etc, which seems to have been completely eliminated.

    public static final short MAP_ANIMATION_TEXTURE_LIST_TERMINATOR = (short) 0xFFFF;
    private static final int TOTAL_CHECKPOINT_TIMER_ENTRIES = 5;

    public static final Image ICON = loadIcon("map");
    public static final List<MAPPrimitiveType> PRIMITIVE_TYPES = new ArrayList<>();
    public static final List<MAPPrimitiveType> POLYGON_TYPES = Arrays.asList(MAPPolygonType.values());

    public static final int VERTEX_COLOR_IMAGE_SIZE = 12;
    private static final ImageFilterSettings OBJ_EXPORT_FILTER = new ImageFilterSettings(ImageState.EXPORT)
            .setTrimEdges(true).setAllowTransparency(true).setAllowFlip(true);

    static {
        PRIMITIVE_TYPES.addAll(POLYGON_TYPES);
        PRIMITIVE_TYPES.add(MAPLineType.G2);
    }

    /**
     * Remove an entity from this map.
     * @param entity The entity to remove.
     */
    public void removeEntity(Entity entity) {
        getEntities().remove(entity);
    }

    /**
     * Remove a path from this map.
     * @param path The path to remove.
     */
    public void removePath(Path path) {
        int pathIndex = getPaths().indexOf(path);
        Utils.verify(pathIndex >= 0, "Path was not registered!");
        getPaths().remove(path);

        // Unlink paths for entities.
        for (Entity entity : getEntities()) {
            PathInfo info = entity.getPathInfo();
            if (info == null)
                continue;

            if (info.getPathId() > pathIndex) {
                info.setPathId(info.getPathId() - 1, false);
            } else if (info.getPathId() == pathIndex) {
                info.setPathId(-1);
            }
        }
    }

    /**
     * Removes a face from this MAPFile.
     * @param selectedFace The face to remove.
     */
    public void removeFace(MAPPolygon selectedFace) {
        getPolygons().get(selectedFace.getType()).remove(selectedFace);

        // Remove MapUV animations.
        for (MAPAnimation animation : getMapAnimations())
            animation.getMapUVs().removeIf(uv -> uv.getPolygon() == selectedFace);

        // Remove Grid data.
        for (GridStack stack : getGridStacks())
            stack.getGridSquares().removeIf(square -> square.getPolygon() == selectedFace);

        // Remove unused vertices.
        for (int i = 0; i < selectedFace.getVerticeCount(); i++) {
            int vertex = selectedFace.getVertices()[i];
            if (!isVerticeUsed(vertex))
                removeVertice(vertex);
        }
    }

    /**
     * Gets the x origin position for the collision grid
     * Fixed point 4 bits.
     * @return baseGridX
     */
    public int getGridBaseX() {
        return (-(getGridXSize() * getGridXCount()) >> 1);
    }

    /**
     * Gets the z origin position for the collision grid
     * Fixed point 4 bits.
     * @return baseGridZ
     */
    public int getGridBaseZ() {
        return (-(getGridZSize() * getGridZCount()) >> 1);
    }

    /**
     * Test if a vertice is used.
     * @param vertice The vertice to test.
     * @return isUsed
     */
    public boolean isVerticeUsed(int vertice) {
        for (List<MAPPrimitive> primList : getPolygons().values())
            for (MAPPrimitive prim : primList)
                for (int i = 0; i < prim.getVerticeCount(); i++)
                    if (prim.getVertices()[i] == vertice)
                        return true;

        return false;
    }

    /**
     * Remove a vertice.
     * @param vertice The vertice to remove.
     */
    public void removeVertice(int vertice) {
        this.vertexes.remove(vertice);
        for (List<MAPPrimitive> primList : getPolygons().values())
            for (MAPPrimitive prim : primList)
                for (int i = 0; i < prim.getVerticeCount(); i++)
                    if (prim.getVertices()[i] > vertice)
                        prim.getVertices()[i]--;
    }

    @Override
    public void load(DataReader reader) {
        boolean isQB = isQB();
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
        this.startRotation = StartRotation.values()[reader.readShort()];
        this.theme = MAPTheme.values()[reader.readShort()];

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
            Path path = new Path();
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
            Form form = new Form();
            form.load(reader);
            forms.add(form);
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
            printInvalidEntityReadDetection(lastEntity, newEntityPointer);

            reader.jumpTemp(newEntityPointer);
            Entity entity = new Entity(this);
            entity.load(reader);
            entities.add(entity);
            reader.jumpReturn();
            lastEntity = entity;
        }
        printInvalidEntityReadDetection(lastEntity, graphicalAddress); // Go over the last entity.

        reader.setIndex(graphicalAddress);
        reader.verifyString(GRAPHICAL_SIGNATURE);
        int lightAddress = reader.readInt();
        int groupAddress = reader.readInt();
        int polygonAddress = reader.readInt();
        int vertexAddress = reader.readInt();
        int gridAddress = reader.readInt();
        int animAddress = isQB ? 0 : reader.readInt(); // Animations don't exist yet in the QB format.

        reader.setIndex(lightAddress);
        reader.verifyString(LIGHT_SIGNATURE);
        int lightCount = reader.readInt();
        for (int i = 0; i < lightCount; i++) {
            Light light = new Light();
            light.load(reader);
            if (light.isWorthKeeping())
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
            MAPGroup group = new MAPGroup(isQB);
            group.load(reader);
            loadGroups[i] = group;
        }

        // Read POLY
        reader.setIndex(polygonAddress);
        reader.verifyString(POLYGON_SIGNATURE);

        Map<MAPPrimitiveType, Short> polyCountMap = new HashMap<>();
        Map<MAPPrimitiveType, Integer> polyOffsetMap = new HashMap<>();

        List<MAPPrimitiveType> types = getTypes(isQB);
        for (MAPPrimitiveType type : types)
            polyCountMap.put(type, reader.readShort());

        if (!isQB)
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

                for (int i = 0; i < polyCount; i++) {
                    MAPPrimitive primitive = type.newPrimitive();
                    getLoadPointerPolygonMap().put(reader.getIndex(), primitive);
                    primitive.load(reader);
                    primitives.add(primitive);
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
        if (!isQB) {
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

        ThemeBook themeBook = getConfig().getThemeBook(getTheme());
        if (themeBook != null)
            this.vlo = themeBook.getVLO(this);
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
        writer.jumpTo(writeAddress);
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

    /**
     * Create a map of textures which were generated
     * @return texMap
     */
    public Map<VertexColor, BufferedImage> makeVertexColorTextures() {
        Map<VertexColor, BufferedImage> texMap = new HashMap<>();

        for (MAPPolygon poly : getAllPolygons()) {
            if (!(poly instanceof VertexColor))
                continue;

            VertexColor vertexColor = (VertexColor) poly;
            BufferedImage image = vertexColor.makeTexture();
            texMap.put(vertexColor, image);
        }

        texMap.put(MapMesh.CURSOR_COLOR, MapMesh.CURSOR_COLOR.makeTexture());
        texMap.put(MapMesh.ANIMATION_COLOR, MapMesh.ANIMATION_COLOR.makeTexture());
        texMap.put(MapMesh.INVISIBLE_COLOR, MapMesh.INVISIBLE_COLOR.makeTexture());
        texMap.put(MapMesh.GRID_COLOR, MapMesh.GRID_COLOR.makeTexture());
        texMap.put(MapMesh.REMOVE_FACE_COLOR, MapMesh.REMOVE_FACE_COLOR.makeTexture());
        texMap.put(MapMesh.GENERAL_SELECTION, MapMesh.GENERAL_SELECTION.makeTexture());
        return texMap;
    }

    @Override
    public Image getIcon() {
        MAPLevel level = MAPLevel.getByName(getFileEntry().getDisplayName());

        if (level != null) {
            getConfig().getLevelImageMap().computeIfAbsent(level, key -> {
                if (getConfig().getLevelInfoMap().isEmpty())
                    return null;

                LevelInfo info = getConfig().getLevelInfoMap().get(key);
                if (info != null)
                    return Utils.toFXImage(Utils.resizeImage(getConfig().getImageFromPointer(info.getLevelTexturePointer()).toBufferedImage(), 35, 35), false);
                return null;
            });
        }

        return getConfig().getLevelImageMap().getOrDefault(level, ICON);
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new MAPController(), "map", this);
    }

    private void printInvalidEntityReadDetection(Entity lastEntity, int endPointer) {
        if (lastEntity == null)
            return;
        int realSize = (endPointer - lastEntity.getLoadScriptDataPointer());
        if (realSize != lastEntity.getLoadReadLength())
            System.out.println("[INVALID/" + getFileEntry().getDisplayName() + "] Entity " + getEntities().indexOf(lastEntity) + "/" + Integer.toHexString(lastEntity.getLoadScriptDataPointer()) + " REAL: " + realSize + ", READ: " + lastEntity.getLoadReadLength() + ", " + lastEntity.getFormEntry().getFormName() + ", " + lastEntity.getFormEntry().getEntityName());
    }

    /**
     * Adds a new polygon to this map.
     * @param data The polygon data to add.
     */
    public void addPolygon(MAPPolygonData data) {
        this.polygons.get(data.getPolygonType()).add(data.makeNewPolygon());
    }

    /**
     * Get the world X the grid starts at.
     * @return baseGridX
     */
    public short getBaseGridX() {
        return (short) (-(getGridXSize() * getGridXCount()) >> 1);
    }

    /**
     * Get the world Z the grid starts at.
     * @return baseGridZ
     */
    public short getBaseGridZ() {
        return (short) (-(getGridZSize() * getGridZCount()) >> 1);
    }

    /**
     * Gets the grid X value from a world X value.
     * @param worldX The world X coordinate.
     * @return gridX
     */
    public int getGridX(int worldX) {
        return (worldX - getBaseGridX()) >> 8;
    }

    /**
     * Gets the grid Z value from a world Z value.
     * @param worldZ The world Z coordinate.
     * @return gridZ
     */
    public int getGridZ(int worldZ) {
        return (worldZ - getBaseGridZ()) >> 8;
    }

    /**
     * Turn a grid x value into a world x value.
     * @param gridX The grid x value to convert.
     * @return worldX
     */
    public int getWorldX(int gridX, boolean useMiddle) {
        return getBaseGridX() + (gridX << 8) + (useMiddle ? 0x80 : 0);
    }

    /**
     * Turn a grid z value into a world z value.
     * @param gridZ The grid z value to convert.
     * @return worldZ
     */
    public int getWorldZ(int gridZ, boolean useMiddle) {
        return getBaseGridZ() + (gridZ << 8) + (useMiddle ? 0x80 : 0);
    }

    /**
     * Gets a grid stack from grid coordinates.
     * @param gridX The grid x coordinate.
     * @param gridZ The grid z coordinate.
     * @return gridStack
     */
    public GridStack getGridStack(int gridX, int gridZ) {
        return getGridStacks().get((gridZ * getGridXCount()) + gridX);
    }

    /**
     * Get the x coordinate of a grid stack.
     * @param stack The grid stack to get the coordinate of.
     * @return gridX
     */
    public int getGridX(GridStack stack) {
        int stackIndex = getGridStacks().indexOf(stack);
        if (stackIndex == -1)
            throw new RuntimeException("This GridStack is not registered!");
        return (stackIndex % getGridXCount());
    }

    /**
     * Get the z coordinate of a grid stack.
     * @param stack The grid stack to get the coordinate of.
     * @return gridZ
     */
    public int getGridZ(GridStack stack) {
        int stackIndex = getGridStacks().indexOf(stack);
        if (stackIndex == -1)
            throw new RuntimeException("This GridStack is not registered!");
        return (stackIndex / getGridXCount());
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
     * Gets all of the polygons in this map, in a list.
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
     * Gets all of the polygons in this map, in a list.
     * @return allPolygons
     */
    public List<MAPPolygon> getAllPolygonsSafe() {
        List<MAPPolygon> polyList = new ArrayList<>();
        for (List<MAPPrimitive> list : getPolygons().values())
            for (MAPPrimitive prim : list)
                if (prim instanceof MAPPolygon)
                    polyList.add((MAPPolygon) prim);
        return polyList;
    }

    /**
     * Recalculate map groups.
     */
    public List<MAPGroup> calculateGroups() {
        List<MAPGroup> groups = new ArrayList<>(getGroupCount());
        for (int i = 0; i < getGroupCount(); i++)
            groups.add(new MAPGroup(false));

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
     * Checks if this map is a multiplayer map.
     * Unfortunately, nothing distinguishes the map files besides where you can access them from and the names.
     * @return isMultiplayer
     */
    public boolean isMultiplayer() {
        return getFileEntry().getDisplayName().startsWith(getTheme().getInternalName() + "M");
    }

    /**
     * Checks if this map is a low-poly map.
     * Unfortunately, nothing distinguishes the map files besides where you can access them from and the names.
     * @return isLowPolyMode
     */
    public boolean isLowPolyMode() {
        return getFileEntry().getDisplayName().contains("_WIN95");
    }

    /**
     * Tests if this is the QB map.
     * @return isQBMap
     */
    public boolean isQB() {
        return getFileEntry().getDisplayName().startsWith(MAPLevel.QB.getInternalName());
    }

    /**
     * This method fixes this MAP (If it is ISLAND.MAP) so it will load properly.
     */
    public void fixAsIslandMap() {
        removeEntity(getEntities().get(11)); // Remove corrupted butterfly entity.
        getConfig().changeRemap(getFileEntry(), getConfig().isPC() ? Constants.PC_ISLAND_REMAP : Constants.PSX_ISLAND_REMAP);
    }

    /**
     * Resizes the grid to a new size.
     * @param xSize The new x size of the grid.
     * @param zSize The new y size of the grid.
     */
    public void resizeGrid(int xSize, int zSize) {
        int newCount = (xSize * zSize);
        List<GridStack> newStackList = new ArrayList<>(newCount);
        for (int i = 0; i < newCount; i++)
            newStackList.add(null);

        for (int z = 0; z < zSize; z++) {
            for (int x = 0; x < xSize; x++) {
                int newIndex = (z * xSize) + x;
                boolean isNewStack = (x >= getGridXCount() || z >= getGridZCount());
                newStackList.set(newIndex, isNewStack ? new GridStack() : getGridStack(x, z));
            }
        }

        // Apply the new grid.
        this.gridXCount = (short) xSize;
        this.gridZCount = (short) zSize;
        this.gridStacks = newStackList;
    }

    /**
     * Procedurally generate a test map.
     * TODO: Fix collision grid.
     */
    public void randomizeMap() {
        final int size = 5;
        int halfSize = size / 2;
        final float SQUARE_SIZE = 20;

        this.startRotation = StartRotation.NORTH;
        this.levelTimer = 99;
        this.cameraSourceOffset.loadFromFloatText("0, -50, -3");
        this.cameraTargetOffset.loadFromFloatText("0.0, 0.0, 0.0");

        this.paths.clear();
        this.zones.clear();
        this.forms.clear();
        this.entities.clear();
        this.lights.clear();
        this.vertexes.clear();
        this.gridStacks.clear();
        this.mapAnimations.clear();
        this.gridXCount = size + 2; // Add two, so we can have a border surrounding the map.
        this.gridZCount = size + 2;
        this.startXTile = (short) (halfSize + 1);
        this.startZTile = (short) 1;

        polygons.values().forEach(List::clear); // Clear the list of polygons.
        List<MAPPrimitive> list = polygons.get(MAPPolygonType.F4);

        // Create stacks.
        Random random = new Random();
        for (int z = 0; z < getGridZCount(); z++) {
            for (int x = 0; x < getGridXCount(); x++) {
                GridStack newStack = new GridStack();
                getGridStacks().add(newStack);
                if (x == 0 || x == getGridXCount() - 1 || z == 0 || z == getGridZCount() - 1)
                    continue; // No squares around edges.

                x -= halfSize;
                z -= halfSize;
                SVector topLeft = new SVector(x * SQUARE_SIZE, 0, (z + 1) * SQUARE_SIZE);
                SVector topRight = new SVector((x + 1) * SQUARE_SIZE, 0, (z + 1) * SQUARE_SIZE);
                SVector botLeft = new SVector(x * SQUARE_SIZE, 0, z * SQUARE_SIZE);
                SVector botRight = new SVector((x + 1) * SQUARE_SIZE, 0, z * SQUARE_SIZE);
                x += halfSize;
                z += halfSize;

                int leftIndex = this.vertexes.size();
                this.vertexes.add(topLeft);
                this.vertexes.add(topRight);
                this.vertexes.add(botLeft);
                this.vertexes.add(botRight);
                MAPPolyF4 polyF4 = new MAPPolyF4();
                polyF4.getColor().setCd((byte) 0xFF);
                polyF4.getColor().setRed((byte) (random.nextInt(511) - 256));
                polyF4.getColor().setGreen((byte) (random.nextInt(511) - 256));
                polyF4.getColor().setBlue((byte) (random.nextInt(511) - 256));
                polyF4.getVertices()[0] = leftIndex;
                polyF4.getVertices()[1] = leftIndex + 1;
                polyF4.getVertices()[2] = leftIndex + 3; // Swapped with the next one to make work.
                polyF4.getVertices()[3] = leftIndex + 2;
                list.add(polyF4);

                GridSquare newSquare = new GridSquare(polyF4, this);
                newSquare.setFlag(GridSquareFlag.CAN_HOP, true);
                newSquare.setFlag(GridSquareFlag.COLLISION, true);
                newStack.getGridSquares().add(newSquare);
            }
        }

        // Add Lights (Makes sure models get colored in):
        Light light1 = new Light(APILightType.PARALLEL);
        light1.setColor(Utils.toBGR(Color.WHITE));
        light1.setDirection(new SVector(-140.375F, 208.375F, 48.125F));
        getLights().add(light1);

        Light light2 = new Light(APILightType.AMBIENT);
        light2.setColor(Utils.toBGR(Utils.fromRGB(0x494949)));
        getLights().add(light2);

        // Setup Group:
        setBaseXTile((short) -halfSize);
        setBaseZTile((short) -halfSize);

        short groupCount = (short) (Math.ceil((double) size / (double) (getGroupXSize() / getGridXSize())));
        setGroupXCount(groupCount);
        setGroupZCount(groupCount);

        System.out.println("Scrambled " + getFileEntry().getDisplayName());
    }

    /**
     * Get the types to use based on if this is QB or not.
     * @param isQB Is this the QB map?
     * @return types
     */
    public static List<MAPPrimitiveType> getTypes(boolean isQB) {
        return isQB ? POLYGON_TYPES : PRIMITIVE_TYPES;
    }
}
