package net.highwayfrogs.editor.file.map;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.form.Form;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.group.MAPGroup;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.zone.Zone;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXPrimitiveType;
import net.highwayfrogs.editor.file.standard.psx.prims.line.PSXLineType;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygonType;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.*;

/**
 * Parses Frogger MAP files.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public class MAPFile extends GameFile {
    private short startXTile;
    private short startYTile;
    private short startRotation;
    private short themeId;
    private short checkPointTimers[] = new short[5]; // Each frog (checkpoint) has its own timer value. In the vanilla game, they all match.
    private SVector cameraSourceOffset;
    private SVector cameraTargetOffset;
    private List<Path> paths = new ArrayList<>();
    private List<Zone> zones = new ArrayList<>();
    private List<Form> forms = new ArrayList<>();
    private List<Entity> entities = new ArrayList<>();
    private List<Light> lights = new ArrayList<>();
    private SVector basePoint; // This is the bottom left of the map group grid.
    private List<MAPGroup> groups = new ArrayList<>();
    private Map<PSXPrimitiveType, List<PSXGPUPrimitive>> polygons = new HashMap<>();
    private Map<PSXGPUPrimitive, Integer> polygonPointerMap = new HashMap<>();
    private Map<Integer, PSXGPUPrimitive> pointerPolygonMap = new HashMap<>();
    private List<SVector> vertexes = new ArrayList<>();
    private List<GridStack> gridStacks = new ArrayList<>();
    private List<GridSquare> gridSquares = new ArrayList<>();
    private List<MAPAnimation> mapAnimations = new ArrayList<>();

    private short groupXCount;
    private short groupZCount;
    private short groupXLength;
    private short groupZLength;

    private short gridXCount;
    private short gridZCount;
    private short gridXLength;
    private short gridZLength;

    public static final int TYPE_ID = 0;
    private static final String SIGNATURE = "FROG";
    private static final String VERSION = "2.00";
    private static final String COMMENT = "Maybe this time it'll all work fine...";
    private static final int COMMENT_BYTES = 64;
    private static final String GENERAL_SIGNATURE = "GENE";
    private static final String PATH_SIGNATURE = "PATH";
    private static final String ZONE_SIGNATURE = "ZONE";
    private static final String FORM_SIGNATURE = "FORM";
    private static final String ENTITY_SIGNATURE = "EMTP";
    private static final String GRAPHICAL_SIGNATURE = "GRAP";
    private static final String LIGHT_SIGNATURE = "LITE";
    private static final String GROUP_SIGNATURE = "GROU";
    private static final String POLYGON_SIGNATURE = "POLY";
    private static final String VERTEX_SIGNATURE = "VRTX";
    private static final String GRID_SIGNATURE = "GRID";
    private static final String ANIMATION_SIGNATURE = "ANIM";

    public static final Image ICON = loadIcon("map");
    public static final List<PSXPrimitiveType> PRIMITIVE_TYPES = new ArrayList<>();

    static {
        PRIMITIVE_TYPES.addAll(Arrays.asList(PSXPolygonType.values()));
        PRIMITIVE_TYPES.add(PSXLineType.G2);
    }

    @Override
    public void load(DataReader reader) {

        pointerPolygonMap.clear();
        reader.verifyString(SIGNATURE);
        int fileLength = reader.readInt();
        reader.verifyString(VERSION);
        reader.readString(COMMENT_BYTES); // Comment bytes.

        int generalAddress = reader.readInt();
        int graphicalAddress = reader.readInt();
        int formAddress = reader.readInt();
        int entityAddress = reader.readInt();
        int zoneAddress = reader.readInt();
        int pathAddress = reader.readInt();

        reader.setIndex(generalAddress);
        reader.verifyString(GENERAL_SIGNATURE);
        this.startXTile = reader.readShort();
        this.startYTile = reader.readShort();
        this.startRotation = reader.readShort();
        this.themeId = reader.readShort();

        for (int i = 0; i < checkPointTimers.length; i++)
            this.checkPointTimers[i] = reader.readShort();

        reader.readShort(); // Unused perspective variable.

        this.cameraSourceOffset = new SVector();
        this.cameraSourceOffset.loadWithPadding(reader);

        this.cameraTargetOffset = new SVector();
        this.cameraTargetOffset.loadWithPadding(reader);
        reader.readBytes(4 * Constants.SHORT_SIZE); // Unused "LEVEL_HEADER" data.

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
        short formCount = reader.readShort();
        reader.readShort(); // Padding.

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
        int entityPacketLength = reader.readInt();
        int entityCount = reader.readShort();
        reader.readShort(); // Padding.

        for (int i = 0; i < entityCount; i++) {
            reader.jumpTemp(reader.readInt());
            Entity entity = new Entity();
            entity.load(reader);
            entities.add(entity);
            reader.jumpReturn();
        }

        reader.setIndex(graphicalAddress);
        reader.verifyString(GRAPHICAL_SIGNATURE);
        int lightAddress = reader.readInt();
        int groupAddress = reader.readInt();
        int polygonAddress = reader.readInt();
        int vertexAddress = reader.readInt();
        int gridAddress = reader.readInt();
        int animAddress = reader.readInt();

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
        this.basePoint = SVector.readWithPadding(reader);
        this.groupXCount = reader.readShort(); // Number of groups in x.
        this.groupZCount = reader.readShort(); // Number of groups in z.
        this.groupXLength = reader.readShort(); // Group X Length
        this.groupZLength = reader.readShort(); // Group Z Length
        int groupCount = groupXCount * groupZCount;

        for (int i = 0; i < groupCount; i++) {
            MAPGroup group = new MAPGroup();
            group.load(reader);
            groups.add(group);
        }

        // Read POLY
        reader.setIndex(polygonAddress);
        reader.verifyString(POLYGON_SIGNATURE);

        Map<PSXPrimitiveType, Short> polyCountMap = new HashMap<>();
        Map<PSXPrimitiveType, Integer> polyOffsetMap = new HashMap<>();

        for (PSXPrimitiveType type : PRIMITIVE_TYPES)
            polyCountMap.put(type, reader.readShort());
        reader.readShort(); // Padding.
        for (PSXPrimitiveType type : PRIMITIVE_TYPES)
            polyOffsetMap.put(type, reader.readInt());

        for (PSXPrimitiveType type : PRIMITIVE_TYPES) {
            short polyCount = polyCountMap.get(type);
            int polyOffset = polyOffsetMap.get(type);

            List<PSXGPUPrimitive> primitives = new ArrayList<>();
            polygons.put(type, primitives);

            if (polyCount > 0) {
                reader.jumpTemp(polyOffset);

                for (int i = 0; i < polyCount; i++) {
                    PSXGPUPrimitive primitive = type.newPrimitive();
                    pointerPolygonMap.put(reader.getIndex(), primitive);
                    primitive.load(reader);
                    primitives.add(primitive);
                }

                reader.jumpReturn();
            }
        }

        // Read Vertexes.
        reader.setIndex(vertexAddress);
        reader.verifyString(VERTEX_SIGNATURE);
        short vertexCount = reader.readShort();
        reader.readShort(); // Padding.
        for (int i = 0; i < vertexCount; i++)
            this.vertexes.add(SVector.readWithPadding(reader));

        // Read GRID data.
        reader.setIndex(gridAddress);
        reader.verifyString(GRID_SIGNATURE);
        this.gridXCount = reader.readShort(); // Number of grid squares in x.
        this.gridZCount = reader.readShort();
        this.gridXLength = reader.readShort(); // Grid square x length.
        this.gridZLength = reader.readShort();

        int stackCount = gridXCount * gridZCount;
        for (int i = 0; i < stackCount; i++) {
            GridStack stack = new GridStack();
            stack.load(reader);
            getGridStacks().add(stack);
        }

        // Find the total amount of squares to read.
        int squareCount = 0;
        for (GridStack stack : gridStacks)
            squareCount = Math.max(squareCount, stack.getIndex() + stack.getSquareCount());

        for (int i = 0; i < squareCount; i++) {
            GridSquare square = new GridSquare(this);
            square.load(reader);
            gridSquares.add(square);
        }

        //TODO: Can two different grid stacks hold the same square?

        // Read "ANIM".
        reader.setIndex(animAddress);
        reader.verifyString(ANIMATION_SIGNATURE);
        int mapAnimCount = reader.readInt(); // 0c
        int mapAnimAddress = reader.readInt(); // 0x2c144
        reader.setIndex(mapAnimAddress); // This points to right after the header.

        for (int i = 0; i < mapAnimCount; i++) {
            /*MAPAnimation animation = new MAPAnimation();
            animation.load(reader); //TODO: There's an issue where an error is thrown here. It seems to reach the end of the texture list, then it starts getting bad data about what is a texture and what is not.
            mapAnimations.add(animation);*/
        }

        polygonPointerMap.clear(); // This should not be used after the load method.
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: As features are implemented, remove the clearing that's happening here:
        getEntities().clear();
        getZones().clear();
        getPaths().clear();
        getForms().clear();
        getGroups().clear();
        getMapAnimations().clear();

        polygonPointerMap.clear();
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(0); // File length. (Unused)
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
        writer.writeShort(this.startYTile);
        writer.writeShort(this.startRotation);
        writer.writeShort(this.themeId);
        for (short timerValue : this.checkPointTimers)
            writer.writeShort(timerValue);

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

        // Save "FORM".
        tempAddress = writer.getIndex();
        writer.jumpTemp(formAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(FORM_SIGNATURE);
        short formCount = (short) this.forms.size();
        writer.writeShort(formCount);
        writer.writeShort((short) 0); // Padding.

        int formPointer = writer.getIndex() + (Constants.POINTER_SIZE * formCount);
        for (Form form : getForms()) {
            writer.writeInt(formPointer);

            writer.jumpTemp(formPointer);
            form.save(writer);
            formPointer = writer.getIndex();
            writer.jumpReturn();
        }

        // Write "EMTP".
        tempAddress = writer.getIndex();
        writer.jumpTemp(entityAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(ENTITY_SIGNATURE);
        writer.writeInt(0); //TODO: Write entityPacketLength.
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
        tempAddress = writer.getIndex();
        writer.jumpTemp(groupAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GROUP_SIGNATURE);
        this.basePoint.saveWithPadding(writer);
        writer.writeShort(this.groupXCount);
        writer.writeShort(this.groupZCount);
        writer.writeShort(this.groupXLength);
        writer.writeShort(this.groupZLength);
        getGroups().forEach(group -> group.save(writer));

        // Read POLY
        tempAddress = writer.getIndex();
        writer.jumpTemp(polygonAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(POLYGON_SIGNATURE);
        for (PSXPrimitiveType type : PRIMITIVE_TYPES)
            writer.writeShort((short) polygons.get(type).size());

        writer.writeShort((short) 0); // Padding.

        Map<PSXPrimitiveType, Integer> polyAddresses = new HashMap<>();

        int lastPointer = writer.getIndex();
        for (PSXPrimitiveType type : PRIMITIVE_TYPES) {
            polyAddresses.put(type, lastPointer);
            lastPointer += Constants.POINTER_SIZE;
        }

        for (PSXPrimitiveType type : PRIMITIVE_TYPES) {
            tempAddress = writer.getIndex();

            writer.jumpTemp(polyAddresses.get(type));
            writer.writeInt(tempAddress);
            writer.jumpReturn();

            getPolygons().get(type).forEach(polygon -> {
                polygonPointerMap.put(polygon, writer.getIndex());
                polygon.save(writer);
            });
        }

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

        // Read GRID data.
        tempAddress = writer.getIndex();
        writer.jumpTemp(gridAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(GRID_SIGNATURE);
        writer.writeShort(this.gridXCount);
        writer.writeShort(this.gridZCount);
        writer.writeShort(this.gridXLength);
        writer.writeShort(this.gridZLength);

        getGridStacks().forEach(gridStack -> gridStack.save(writer));
        getGridStacks().forEach(square -> square.save(writer));

        // Save "ANIM" data.
        tempAddress = writer.getIndex();
        writer.jumpTemp(animAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        writer.writeStringBytes(ANIMATION_SIGNATURE);
        writer.writeInt(this.mapAnimations.size());
        writer.writeInt(writer.getIndex() + Constants.POINTER_SIZE);

        //TODO: Save ANIM.
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return null;
    }
}
