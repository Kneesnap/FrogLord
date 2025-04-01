package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapGroup;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapGeneralManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Represents map rendering groups.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketGroup extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "GROU";
    private final SVector basePoint = new SVector(); // TODO: NEEDS DEFAULT VALUE.
    @Setter private int groupXCount; // Number of groups in x. TODO: (AUTOCALCULATE WHENEVER THE GRID IS RESIZED BY ENSURING THE SAME BUFFER AS BEFORE EXISTS SURROUNDING THE GRID)
    @Setter private int groupZCount; // Number of groups in z. TODO: (AUTOCALCULATE WHENEVER THE GRID IS RESIZED BY ENSURING THE SAME BUFFER AS BEFORE EXISTS SURROUNDING THE GRID)
    private int groupXSize = 768; // Group X Length - Seems to always be 768, which is 3.0 in fixed point 8.8. (3.0 * the size of a unit square)
    private int groupZSize = 768; // Group Z Length - Seems to always be 768, which is 3.0 in fixed point 8.8. (3.0 * the size of a unit square)
    private FroggerMapGroup[][] mapGroups;
    private final FroggerMapGroup invisibleMapGroup;

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

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
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
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Map Group Dimensions", this.groupXCount + " x " + this.groupZCount + " (" + getGroupCount() + " groups)");
        propertyList.add("Map Group Square Size", getGroupXSizeAsFloat() + " x " + getGroupZSizeAsFloat());
        return propertyList;
    }

    /**
     * Get the group X value from a world X value. Expects a 4 bit fixed point short.
     * @param worldX The world X coordinate.
     * @return groupX
     */
    public int getGroupXFromWorldX(int worldX) {
        return (worldX - this.basePoint.getX()) / getGroupXSize();
    }

    /**
     * Get the group Z value from a world Z value. Expects a 4 bit fixed point short.
     * @param worldZ The world Z coordinate.
     * @return groupZ
     */
    public int getGroupZFromWorldZ(int worldZ) {
        return (worldZ - this.basePoint.getZ()) / getGroupZSize();
    }

    /**
     * Gets a map group from group coordinates.
     * @param groupX The group x coordinate.
     * @param groupZ The group z coordinate.
     * @return group
     */
    public int getGroupArrayIndex(int groupX, int groupZ) {
        return (groupZ * this.groupXCount) + groupX;
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
        return DataUtils.fixedPointIntToFloatNBits(this.groupXSize, 8);
    }

    /**
     * Gets the group square/rectangle Z size as a floating point number.
     */
    public float getGroupZSizeAsFloat() {
        return DataUtils.fixedPointIntToFloatNBits(this.groupZSize, 8);
    }

    /**
     * Generate a new map groups array.
     * TODO: Look at how we can make this match the file contents perfectly.
     */
    public FroggerMapGroup[][] generateMapGroups() {
        this.invisibleMapGroup.clear();
        FroggerMapGroup[][] newMapGroups = new FroggerMapGroup[this.groupZSize][this.groupXCount];
        for (int z = 0; z < this.groupZCount; z++)
            for (int x = 0; x < this.groupXCount; x++)
                newMapGroups[z][x] = new FroggerMapGroup(getParentFile(), x, z);

        List<SVector> vertices = getParentFile().getVertexPacket().getVertices();
        for (FroggerMapPolygon polygon : getParentFile().getPolygonPacket().getPolygons()) {
            if (!polygon.isVisible()) {
                this.invisibleMapGroup.getPolygonsByType()[polygon.getPolygonType().ordinal()].add(polygon);
                continue;
            }

            SVector vertex = vertices.get(polygon.getVertices()[polygon.getVertexCount() - 1]); // TODO: Find a better way of calculating the group.
            int groupX = getGroupXFromWorldX(vertex.getX());
            int groupZ = getGroupZFromWorldZ(vertex.getZ());
            newMapGroups[groupZ][groupX].getPolygonsByType()[polygon.getPolygonType().ordinal()].add(polygon);
        }

        return this.mapGroups = newMapGroups;
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
        editorGrid.addFixedShort("Base Point X", this.basePoint.getX(), newX -> {
            this.basePoint.setX(newX);
            if (manager != null) {
                manager.updateMapGroupPreview();
                manager.updateGridView();
            }
        });
        editorGrid.addFixedShort("Base Point Z", this.basePoint.getZ(), newZ -> {
            this.basePoint.setZ(newZ);
            if (manager != null) {
                manager.updateMapGroupPreview();
                manager.updateGridView();
            }
        });

        // Group Squares
        editorGrid.addLabel("Group Square Size", getGroupXSizeAsFloat() + " x " + getGroupZSizeAsFloat());
        editorGrid.addUnsignedShortField("Group X Count", this.groupXCount, newX -> {
            this.groupXCount = newX;
            if (manager != null)
                manager.updateMapGroupPreview();
        });
        editorGrid.addUnsignedShortField("Group Z Count", this.groupZCount, newZ -> {
            this.groupZCount = newZ;
            if (manager != null)
                manager.updateMapGroupPreview();
        });
    }
}