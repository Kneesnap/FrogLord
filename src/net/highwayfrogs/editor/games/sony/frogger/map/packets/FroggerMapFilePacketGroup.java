package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapGroup;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapGeneralManager;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.List;
import java.util.logging.Level;

/**
 * Represents map rendering groups.
 * Created by Kneesnap on 5/25/2024.
 */
public class FroggerMapFilePacketGroup extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "GROU";
    private final SVector basePoint = new SVector();
    @Getter private int groupXCount = 1; // Number of groups in x.
    @Getter private int groupZCount = 1; // Number of groups in z.
    @Getter private int groupXSize = GROUP_WORLD_LENGTH; // Group X Length - Seems to always be 768, which is 3.0 in fixed point 8.8. (3.0 * the size of a unit square)
    @Getter private int groupZSize = GROUP_WORLD_LENGTH; // Group Z Length - Seems to always be 768, which is 3.0 in fixed point 8.8. (3.0 * the size of a unit square)
    private FroggerMapGroup[][] mapGroups;
    @Getter private final FroggerMapGroup invisibleMapGroup;

    public static final int GRID_STACK_LENGTH = 3; // The length of
    public static final int GROUP_WORLD_LENGTH = GRID_STACK_LENGTH * FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH; // 768
    public static final int MAX_ALLOWED_GROUP_COUNT = 85;
    public static final int MIN_GROUP_TILE_POS = -(MAX_ALLOWED_GROUP_COUNT / 2);
    public static final int MAX_GROUP_TILE_POS = (MAX_ALLOWED_GROUP_COUNT / 2);
    public static final int MAX_SAFE_GROUP_Z_COUNT = 64; // The game won't crash if we go over this, but it causes major visibility issues.

    public FroggerMapFilePacketGroup(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
        this.invisibleMapGroup = new FroggerMapGroup(parentFile, -1, -1);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.basePoint.loadWithPadding(reader);
        Utils.verify(this.basePoint.getY() == 0, "Base-Point Y is not zero!");
        this.groupXCount = reader.readUnsignedShortAsInt();
        this.groupZCount = reader.readUnsignedShortAsInt();
        this.groupXSize = reader.readUnsignedShortAsInt();
        this.groupZSize = reader.readUnsignedShortAsInt();

        // Validate size as the expected value.
        if (this.groupXSize != GROUP_WORLD_LENGTH || this.groupZSize != GROUP_WORLD_LENGTH)
            getLogger().warning("Unexpected group sizes! Got: [%d/%f, %d/%f]", this.groupXSize, getGroupXSizeAsFloat(), this.groupZSize, getGroupZSizeAsFloat());

        // Read map groups.
        this.mapGroups = new FroggerMapGroup[getGroupZCount()][getGroupXCount()];
        for (int z = 0; z < this.groupZCount; z++) {
            for (int x = 0; x < this.groupXCount; x++) {
                FroggerMapGroup group = new FroggerMapGroup(getParentFile(), x, z);
                group.load(reader);
                this.mapGroups[z][x] = group;
            }
        }
        if (hasInvisiblePolygonGroup())
            this.invisibleMapGroup.load(reader);

        // Read map group static entity lists.
        if (!shouldSkipStaticEntityLists()) {
            for (int z = 0; z < this.groupZCount; z++)
                for (int x = 0; x < this.groupXCount; x++)
                    this.mapGroups[z][x].loadStaticEntityList(reader);
            if (hasInvisiblePolygonGroup())
                this.invisibleMapGroup.loadStaticEntityList(reader);
        }

        // Read path entity lists. Why are path lists stored here? Hard to say.
        getParentFile().getPathPacket().loadEntityLists(reader);

        // Skip to end of packet if old format to hide warnings.
        if (getParentFile().getGraphicalPacket().getPolygonPacketAddress() > reader.getIndex() && getParentFile().isEarlyMapFormat() || getConfig().isAtOrBeforeBuild4())
            reader.setIndex(getParentFile().getGraphicalPacket().getPolygonPacketAddress());
    }

    /**
     * Load the polygon lists in the map groups.
     * @param polygonPacket The packet containing loaded polygon data.
     */
    public void loadGroupPolygonLists(FroggerMapFilePacketPolygon polygonPacket) {
        for (int z = 0; z < this.groupZCount; z++)
            for (int x = 0; x < this.groupXCount; x++)
                this.mapGroups[z][x].loadPolygonLists(polygonPacket, true);
        if (hasInvisiblePolygonGroup())
            this.invisibleMapGroup.loadPolygonLists(polygonPacket, false);
    }

    /**
     * Warn if the FrogLord map polygon -> group placement algorithm does not match the data seen.
     * Must be called ONLY AFTER READING BOTH the vertex packet (for group placement) and the GRID packet (for entity position resolution calculations).
     */
    public void warnIfFrogLordAlgorithmDoesNotMatchData() {
        for (int z = 0; z < this.groupZCount; z++)
            for (int x = 0; x < this.groupXCount; x++)
                this.mapGroups[z][x].warnIfFrogLordAlgorithmDoesNotMatchData();

        calculateDimensions(false);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        // Firstly, generate map groups.
        // This ensures both group & polygon data is valid/up-to-date, and won't crash the game.
        // The polygon packet relies on this running before its saveBodyFirstPass() is called.
        generateMapGroups(null, ProblemResponse.CREATE_POPUP, false);

        // Warn if about to save a map which will have rendering issues in-game.
        if (this.groupZCount > MAX_SAFE_GROUP_Z_COUNT)
            getGameInstance().showWarning(getLogger(),
                    "%s will have rendering issues in-game due to having a groupZCount of %d.\nTo fix the issues, reduce it to %d.",
                    getParentFile().getFileDisplayName(), this.groupZCount, MAX_SAFE_GROUP_Z_COUNT);

        // Save group header.
        this.basePoint.saveWithPadding(writer);
        writer.writeUnsignedShort(this.groupXCount);
        writer.writeUnsignedShort(this.groupZCount);
        writer.writeUnsignedShort(this.groupXSize);
        writer.writeUnsignedShort(this.groupZSize);

        // Write map groups.
        for (int z = 0; z < this.groupZCount; z++)
            for (int x = 0; x < this.groupXCount; x++)
                this.mapGroups[z][x].save(writer);
        if (hasInvisiblePolygonGroup())
            this.invisibleMapGroup.save(writer);

        // Write map group static entity lists.
        if (!shouldSkipStaticEntityLists()) {
            for (int z = 0; z < this.groupZCount; z++)
                for (int x = 0; x < this.groupXCount; x++)
                    this.mapGroups[z][x].saveStaticEntityList(writer);
            if (hasInvisiblePolygonGroup())
                this.invisibleMapGroup.saveStaticEntityList(writer);
        }

        // Write path entity lists. Why are path lists stored here? Hard to say.
        getParentFile().getPathPacket().saveEntityLists(writer);
    }

    @Override
    public void clear() {
        this.groupXSize = 1;
        this.groupZSize = 1;
        this.groupXSize = GROUP_WORLD_LENGTH;
        this.groupZSize = GROUP_WORLD_LENGTH;
        this.invisibleMapGroup.clear();
        if (this.mapGroups != null)
            for (int z = 0; z < this.mapGroups.length; z++)
                for (int x = 0; x < this.mapGroups[z].length; x++)
                    this.mapGroups[z][x].clear();
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketGroup))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketGroup.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketGroup newGroupChunk = (FroggerMapFilePacketGroup) newChunk;
        newGroupChunk.basePoint.setValues(this.basePoint);
        newGroupChunk.groupXCount = this.groupXCount;
        newGroupChunk.groupZCount = this.groupZCount;
        newGroupChunk.groupXSize = this.groupXSize;
        newGroupChunk.groupZSize = this.groupZSize;
        newGroupChunk.generateMapGroups(null, ProblemResponse.THROW_EXCEPTION, false);
    }

    /**
     * Load the polygon lists in the map groups.
     * @param writer The writer to write data with.
     * @param polygonOffsets The offsets of the polygon blocks into the file.
     */
    public void saveGroupPolygonLists(DataWriter writer, int[] polygonOffsets) {
        for (int z = 0; z < this.groupZCount; z++)
            for (int x = 0; x < this.groupXCount; x++)
                this.mapGroups[z][x].savePolygonLists(writer, polygonOffsets);
        if (hasInvisiblePolygonGroup())
            this.invisibleMapGroup.savePolygonLists(writer, polygonOffsets);
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getGraphicalPacket().getGroupPacketAddress();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("Map Group Dimensions", this.groupXCount + " x " + this.groupZCount + " (" + getGroupCount() + " groups)");
        propertyList.add("Map Group Square Size", getGroupXSizeAsFloat() + " x " + getGroupZSizeAsFloat());
    }

    /**
     * Gets the group base point world X coordinate
     */
    public float getGroupBaseWorldX() {
        return this.basePoint.getFloatX();
    }

    /**
     * Gets the group base point world Z coordinate
     */
    public float getGroupBaseWorldZ() {
        return this.basePoint.getFloatZ();
    }

    /**
     * Gets the map group at the given group coordinates.
     * @param x the group x coordinate to obtain
     * @param z the group z coordinate to obtain
     * @return mapGroup
     */
    public FroggerMapGroup getMapGroup(int x, int z) {
        if (this.mapGroups == null || this.mapGroups.length != this.groupZCount || this.mapGroups[0].length == 0 || this.mapGroups[0].length != this.groupXCount)
            throw new IllegalStateException("The current map group tracking is outdated, so it is not valid to obtain a mapGroup until they are updated.");
        if (z < 0 || z >= this.groupZCount || x < 0 || x >= this.groupXCount)
            return null;

        return this.mapGroups[z][x];
    }

    /**
     * Gets the map group which a given map polygon would be placed within.
     * This is 100% accurate to the original group generation algorithm.
     * If such a map group does not exist, an exception will be thrown.
     * @param polygon the polygon to lookup
     * @return mapGroup
     */
    public FroggerMapGroup getMapGroup(FroggerMapPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");

        if (!polygon.isVisible() && hasInvisiblePolygonGroup())
            return this.invisibleMapGroup;

        // This was not confirmed by checking any code, instead this has just been observed to be perfectly consistent with the behavior of the retail game.
        // Warnings will be printed to the console during game data load if there are any discrepancies between this method and the actual game data loaded.
        SVector vertex = getParentFile().getVertexPacket().getVertices().get(polygon.getVertices()[2]);
        int groupX = getGroupXFromWorldX(vertex.getX());
        int groupZ = getGroupZFromWorldZ(vertex.getZ());
        return getMapGroup(groupX, groupZ);
    }

    /**
     * Get the group X value from a world X value. Expects a 4 bit fixed point short.
     * @param worldX The world X coordinate.
     * @return groupX
     */
    public int getGroupXFromWorldX(int worldX) {
        return (worldX - this.basePoint.getX()) / this.groupXSize;
    }

    /**
     * Get the group X value from a floating point world X value.
     * @param worldX The world X coordinate.
     * @return groupX
     */
    public int getGroupXFromWorldX(float worldX) {
        return getGroupXFromWorldX(DataUtils.floatToFixedPointInt4Bit(worldX));
    }

    /**
     * Get the group Z value from a world Z value. Expects a 4 bit fixed point short.
     * @param worldZ The world Z coordinate.
     * @return groupZ
     */
    public int getGroupZFromWorldZ(int worldZ) {
        return (worldZ - this.basePoint.getZ()) / this.groupZSize;
    }

    /**
     * Get the group Z value from a floating point world Z value.
     * @param worldZ The world Z coordinate.
     * @return groupZ
     */
    public int getGroupZFromWorldZ(float worldZ) {
        return getGroupZFromWorldZ(DataUtils.floatToFixedPointInt4Bit(worldZ));
    }

    /**
     * Gets the number of groups in this map.
     * @return groupCount
     */
    public int getGroupCount() {
        return this.groupXCount * this.groupZCount;
    }

    /**
     * Gets the group square/rectangle X size as a floating point number.
     */
    public float getGroupXSizeAsFloat() {
        return DataUtils.fixedPointIntToFloat4Bit(this.groupXSize);
    }

    /**
     * Gets the group square/rectangle Z size as a floating point number.
     */
    public float getGroupZSizeAsFloat() {
        return DataUtils.fixedPointIntToFloat4Bit(this.groupZSize);
    }

    private boolean useFrogLordGroupAlgorithm() {
        return !getGameInstance().getVersionConfig().isAtOrBeforeBuild2();
    }

    /**
     * Attempts to calculate the dimensions/start position of the group data.
     * This is unable to account for all situations that Mappy likely accounted for, so instead consider this as doing its best to prevent the user from accidentally crashing the game and not knowing why.
     * @param applyChanges Whether to apply changes. If false, warnings will be printed if something seems wrong.
     */
    private void calculateDimensions(boolean applyChanges) {
        if (!useFrogLordGroupAlgorithm())
            return; // Too early to be accurate.

        short newMinWorldX = Short.MAX_VALUE;
        short newMaxWorldX = Short.MIN_VALUE;
        short newMinWorldZ = Short.MAX_VALUE;
        short newMaxWorldZ = Short.MIN_VALUE;
        List<SVector> vertices = getParentFile().getVertexPacket().getVertices();

        for (int i = 0; i < vertices.size(); i++) { // Unused vertices should probably be ignored, the original did that (as seen in SWP2.MAP, Build 30). Rule EXCLUDES QB.MAP,
            SVector vertex = vertices.get(i);
            if (vertex.getX() < newMinWorldX)
                newMinWorldX = vertex.getX();
            if (vertex.getX() > newMaxWorldX)
                newMaxWorldX = vertex.getX();
            if (vertex.getZ() < newMinWorldZ)
                newMinWorldZ = vertex.getZ();
            if (vertex.getZ() > newMaxWorldZ)
                newMaxWorldZ = vertex.getZ();
        }

        // Expand bounds to include entities.
        float[] position = null;
        List<FroggerMapEntity> entities = getParentFile().getEntityPacket().getEntities();
        for (int i = 0; i < entities.size(); i++) {
            FroggerMapEntity entity = entities.get(i);
            if ((position = entity.getPositionAndRotation(position)) == null)
                continue;

            short testX = DataUtils.floatToFixedPointShort4Bit(position[0]);
            short testZ = DataUtils.floatToFixedPointShort4Bit(position[2]);
            if (testX < newMinWorldX)
                newMinWorldX = testX;
            if (testX > newMaxWorldX)
                newMaxWorldX = testX;
            if (testZ < newMinWorldZ)
                newMinWorldZ = testZ;
            if (testZ > newMaxWorldZ)
                newMaxWorldZ = testZ;
        }

        // Expand bounds to include paths.
        final int pathIntervals = 4;
        List<FroggerPath> paths = getParentFile().getPathPacket().getPaths();
        for (int i = 0; i < paths.size(); i++) {
            FroggerPath path = paths.get(i);
            for (int j = 0; j < path.getSegments().size(); j++) {
                FroggerPathSegment pathSegment = path.getSegments().get(j);
                for (int k = 0; k <= pathIntervals; k++) {
                    SVector pathPos = k > 0 ? pathSegment.calculatePosition((int) (pathSegment.getLength() * ((double) k / pathIntervals))).getPosition() : pathSegment.getStartPosition();
                    if (pathPos.getX() < newMinWorldX)
                        newMinWorldX = pathPos.getX();
                    if (pathPos.getX() > newMaxWorldX)
                        newMaxWorldX = pathPos.getX();
                    if (pathPos.getZ() < newMinWorldZ)
                        newMinWorldZ = pathPos.getZ();
                    if (pathPos.getZ() > newMaxWorldZ)
                        newMaxWorldZ = pathPos.getZ();
                }
            }
        }

        int worldGridModuloX = newMinWorldX % this.groupXSize;
        int worldGridModuloZ = newMinWorldZ % this.groupZSize;
        short newBaseWorldX = (short) Math.max(Short.MIN_VALUE, (newMinWorldX - (worldGridModuloX != 0 ? worldGridModuloX + this.groupXSize : 0) - 1));
        short newBaseWorldZ = (short) Math.max(Short.MIN_VALUE, (newMinWorldZ - (worldGridModuloZ != 0 ? worldGridModuloZ + this.groupZSize : 0) - 1));
        int newXCount = 1 + ((newMaxWorldX - Math.min(this.basePoint.getX(), newBaseWorldX)) / this.groupXSize);
        int newZCount = 1 + ((newMaxWorldZ - Math.min(this.basePoint.getZ(), newBaseWorldZ)) / this.groupZSize);

        if (applyChanges) {
            // Replacing the data no matter what is dangerous because not having a large enough grid is likely to lead to crashes.
            // So, we only replace it if we're making crashes less likely.
            if (newBaseWorldX < this.basePoint.getX())
                this.basePoint.setX(newBaseWorldX);
            if (newBaseWorldZ < this.basePoint.getZ())
                this.basePoint.setZ(newBaseWorldZ);
            if (newXCount > this.groupXCount)
                this.groupXCount = Math.min(MAX_ALLOWED_GROUP_COUNT, newXCount);
            if (newZCount > this.groupZCount)
                this.groupZCount = Math.min(MAX_ALLOWED_GROUP_COUNT, newZCount);
        } else if (!getParentFile().isEarlyMapFormat()) {
            // Any situation which would cause an update to occur should warn, as by default this shouldn't change anything about the original data.
            if (newBaseWorldX < this.basePoint.getX())
                getLogger().warning("FrogLord expected the rendering group basePointX to be %d (%f), but it was actually %d (%f).", newBaseWorldX, DataUtils.fixedPointShortToFloat4Bit(newBaseWorldX), this.basePoint.getX(), this.basePoint.getFloatX());
            if (0 != this.basePoint.getY())
                getLogger().warning("FrogLord expected the rendering group basePointY to be zero, but it was actually %d (%f).", this.basePoint.getY(), this.basePoint.getFloatY());
            if (newBaseWorldZ < this.basePoint.getZ())
                getLogger().warning("FrogLord expected the rendering group basePointZ to be %d (%f), but it was actually %d (%f).", newBaseWorldZ, DataUtils.fixedPointShortToFloat4Bit(newBaseWorldZ), this.basePoint.getZ(), this.basePoint.getFloatZ());
            if (newXCount > this.groupXCount)
                getLogger().warning("FrogLord expected a groupXCount of %d, but it was actually %d.", newXCount, this.groupXCount);
            if (newZCount > this.groupZCount)
                getLogger().warning("FrogLord expected a groupZCount of %d, but it was actually %d.", newZCount, this.groupZCount);
        }
    }

    /**
     * Generate the map groups to save for a particular level.
     */
    public FroggerMapGroup[][] generateMapGroups(ILogger logger, ProblemResponse response, boolean resetGroupBoundaries) {
        if (!useFrogLordGroupAlgorithm())
            return this.mapGroups; // Too early.
        if (logger == null)
            logger = getLogger();

        this.invisibleMapGroup.clear();

        // Calculate group boundaries.
        if (resetGroupBoundaries) {
            this.basePoint.setX(Short.MAX_VALUE);
            this.basePoint.setZ(Short.MAX_VALUE);
            this.groupXCount = 1;
            this.groupZCount = 1;
        }

        // Ensure group is large enough to handle the generation algorithm.
        calculateDimensions(true);

        FroggerMapGroup[][] newMapGroups = new FroggerMapGroup[this.groupZCount][this.groupXCount];
        for (int z = 0; z < this.groupZCount; z++)
            for (int x = 0; x < this.groupXCount; x++)
                newMapGroups[z][x] = new FroggerMapGroup(getParentFile(), x, z);

        // Put polygons into the group they belong within.
        int failureCount = 0;
        this.mapGroups = newMapGroups; // Must do this before calling getMapGroup()
        Vector3f temp = new Vector3f();
        FroggerMapFilePacketGrid gridPacket = getParentFile().getGridPacket();
        for (FroggerMapPolygon polygon : getParentFile().getPolygonPacket().getPolygons()) {
            FroggerMapGroup mapGroup = getMapGroup(polygon);
            if (mapGroup == null || !mapGroup.addPolygon(polygon)) {
                failureCount++;
                FroggerGridSquare gridSquare = gridPacket.getGridSquare(polygon, temp);
                if (gridSquare != null)
                    gridSquare.getGridStack().getGridSquares().remove(gridSquare);
            }
        }

        // Seen in PSX Build 02 (SKY5.MAP).
        if (failureCount > 0)
            Utils.handleProblem(response, logger, Level.WARNING, "Failed to add %d polygon(s) to the map render groups. (There are too many polygons in the area!)", failureCount);

        return this.mapGroups;
    }

    /**
     * Test if the packet contains the group entry for invisible polygons.
     */
    public boolean hasInvisiblePolygonGroup() {
        // Build 29 added a map group for invisible polygons. Not sure if invisible polygons existed before this without being in a group or if this build adds invisible polygons.
        // But, even after build 29, maps using the older format are sometimes present (such as QB.MAP), which is where the other check comes in.
        return !getConfig().isAtOrBeforeBuild28() && !getParentFile().isExtremelyEarlyMapFormat();
    }

    /**
     * Tests if static entity lists exist and should be skipped.
     */
    public boolean shouldSkipStaticEntityLists() {
        // Island placeholders skip entity reading due to their currently unknown format.
        // Build 34 seems to have removed the static entity lists, likely due to being unused.
        // In the future we should probably learn how to properly read/write them in the old format.
        return getConfig().isAtOrBeforeBuild33() || getParentFile().isExtremelyEarlyMapFormat();
    }

    /**
     * Creates the editor for basic group data.
     * @param manager the manager which the UI will be created for
     * @param editorGrid the grid to create the editor within
     */
    public void setupEditor(FroggerUIMapGeneralManager manager, GUIEditorGrid editorGrid) {
        editorGrid.addBoldLabel("Map Group Settings:");

        // Base Point:
        int baseTileX = getBaseTileXFromWorldX(this.basePoint.getX());
        int baseTileZ = getBaseTileZFromWorldZ(this.basePoint.getZ());
        if (getWorldXFromBaseTileX(baseTileX) == this.basePoint.getX() && getWorldZFromBaseTileZ(baseTileZ) == this.basePoint.getZ()) {
            editorGrid.addSignedIntegerField("Start X", baseTileX,
                    newTileX -> newTileX >= MIN_GROUP_TILE_POS && newTileX <= MAX_GROUP_TILE_POS,
                    newTileX -> {
                this.basePoint.setX(getWorldXFromBaseTileX(newTileX));
                handleGroupChange(manager);
            });
            editorGrid.addSignedIntegerField("Start Z", baseTileZ,
                    newTileZ -> newTileZ >= MIN_GROUP_TILE_POS && newTileZ <= MAX_GROUP_TILE_POS,
                    newTileZ -> {
                this.basePoint.setZ(getWorldZFromBaseTileZ(newTileZ));
                handleGroupChange(manager);
            });
        } else {
            editorGrid.addFixedShort("Base World X", this.basePoint.getX(), newX -> {
                this.basePoint.setX(newX);
                handleGroupChange(manager);
            });
            editorGrid.addFixedShort("Base World Z", this.basePoint.getZ(), newZ -> {
                this.basePoint.setZ(newZ);
                handleGroupChange(manager);
            });
        }

        // Group Counts
        editorGrid.addLabel("Group Square Size", getGroupXSizeAsFloat() + " x " + getGroupZSizeAsFloat());
        editorGrid.addUnsignedShortField("Group X Count", this.groupXCount,
                newX -> newX + getBaseTileXFromWorldX(this.basePoint.getX()) <= MAX_GROUP_TILE_POS + 1,
                newX -> {
            this.groupXCount = newX;
            handleGroupChange(manager);
        });

        editorGrid.addUnsignedShortField("Group Z Count", this.groupZCount,
                newZ -> newZ + getBaseTileZFromWorldZ(this.basePoint.getZ()) <= MAX_GROUP_TILE_POS + 1,
                newZ -> {
            this.groupZCount = newZ;
            handleGroupChange(manager);
        });
    }

    private void handleGroupChange(FroggerUIMapGeneralManager manager) {
        // Update the map groups, but also ensure the value just supplied by the user isn't blatantly going to crash the game.
        generateMapGroups(manager != null ? manager.getLogger() : null, ProblemResponse.CREATE_POPUP, false);
        if (manager != null) {
            manager.updateMapGroupUI();
            manager.updateMapGroup3DView();
        }
    }

    private short getWorldXFromBaseTileX(int baseTileX) {
        return (short) ((baseTileX * this.groupXSize) - 1);
    }

    private short getWorldZFromBaseTileZ(int baseTileZ) {
        return (short) ((baseTileZ * this.groupZSize) - 1);
    }

    private short getBaseTileXFromWorldX(short worldX) {
        return (short) ((worldX + 1) / this.groupXSize);
    }

    private short getBaseTileZFromWorldZ(short worldZ) {
        return (short) ((worldZ + 1) / this.groupZSize);
    }
}