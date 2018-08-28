package net.highwayfrogs.editor.file.map;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.form.Form;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.light.Light;
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
public class MAPFile extends GameFile {
    private short startXTile;
    private short startYTile;
    private short startRotation;
    private short themeId;
    private short checkPointTimers[] = new short[5]; // Each frog (checkpoint) has its own timer value. In the vanilla game, they all match.
    private SVector cameraSourceOffset;
    private SVector cameraTargetOffset;
    private List<Zone> zones = new ArrayList<>();
    private List<Form> forms = new ArrayList<>();
    private List<Entity> entities = new ArrayList<>();
    private List<Light> lights = new ArrayList<>();
    private Map<PSXPrimitiveType, List<PSXGPUPrimitive>> polygons = new HashMap<>();
    private List<SVector> vertexes = new ArrayList<>();
    private SVector basePoint; // This is the bottom left of the map group grid.
    private List<GridStack> gridStacks = new ArrayList<>();
    private List<GridSquare> gridSquares = new ArrayList<>();
    private List<MAPAnimation> mapAnimations = new ArrayList<>();

    public static final int TYPE_ID = 0;
    private static final String SIGNATURE = "FROG";
    private static final String VERSION = "2.00";
    private static final String COMMENT = "Maybe this time it'll all work fine...";
    private static final int COMMENT_BYTES = 64;
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

    private static final List<PSXPrimitiveType> PRIMITIVE_TYPES = new ArrayList<>();

    static {
        PRIMITIVE_TYPES.addAll(Arrays.asList(PSXPolygonType.values()));
        PRIMITIVE_TYPES.add(PSXLineType.G2);
    }

    @Override
    public void load(DataReader reader) {
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

        for (int i = 0; i < pathCount; i++) {
            reader.jumpTemp(reader.readInt()); // Starts after the pointers.

            int entityIndicePointer = reader.readInt(); //TODO: What is this? They seem to be placed right before EMTP, but after the zone data.
            int segmentCount = reader.readInt();
            int segmentPointer = reader.readInt();

            reader.jumpReturn();

            // Read segments.
            reader.jumpTemp(segmentPointer);
            int[] segmentOffsets = new int[segmentCount];
            for (int j = 0; j < segmentCount; j++)
                segmentOffsets[j] = reader.readInt();
            reader.jumpReturn();

            //TODO: Finish.
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
        short xNum = reader.readShort(); // Number of groups in x.
        short zNum = reader.readShort(); // Number of groups in z.
        short xLen = reader.readShort(); // Group X Length
        short zLen = reader.readShort(); // Group Z Length
        int groupCount = xNum * zNum;
        //TODO: Read MAP_GROUPs

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
        short gridXCount = reader.readShort(); // Number of grid squares in x.
        short gridZCount = reader.readShort();
        short gridXLength = reader.readShort(); // Grid square x length.
        short gridZLength = reader.readShort();

        int stackCount = gridXCount * gridZCount;
        for (int i = 0; i < stackCount; i++) {
            GridStack stack = new GridStack();
            stack.load(reader);
        }

        // Find the total amount of squares to read.
        int squareCount = 0;
        for (GridStack stack : gridStacks)
            squareCount = Math.max(squareCount, stack.getIndex() + stack.getSquareCount());

        for (int i = 0; i < squareCount; i++) {
            GridSquare square = new GridSquare();
            square.load(reader);
            gridSquares.add(square);
        }

        //TODO: Can two different grid stacks hold the same square?

        // Read "ANIM".
        reader.setIndex(animAddress);
        reader.verifyString(ANIMATION_SIGNATURE);
        int mapAnimCount = reader.readInt();
        int mapAnimAddress = reader.readInt();
        reader.setIndex(mapAnimAddress); // This points to right after the header.

        for (int i = 0; i < mapAnimCount; i++) {
            MAPAnimation animation = new MAPAnimation();
            animation.load(reader);
            mapAnimations.add(animation);
        }
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: Save map.
    }
}
