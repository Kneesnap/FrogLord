package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapGroup;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygonType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents map polygons.
 * Created by Kneesnap on 5/25/2024.
 */
public class FroggerMapFilePacketPolygon extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "POLY";
    private final List<FroggerMapPolygon> polygons = new ArrayList<>();
    @SuppressWarnings("unchecked") private final List<FroggerMapPolygon>[] polygonsByType = (List<FroggerMapPolygon>[]) new List[FroggerMapPolygonType.values().length];

    public FroggerMapFilePacketPolygon(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
        for (int i = 0; i < this.polygonsByType.length; i++)
            this.polygonsByType[i] = new ArrayList<>();
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        FroggerMapConfig mapConfig = getMapConfig();
        boolean g2Supported = mapConfig.isG2Supported();
        boolean g2Enabled = g2Supported && mapConfig.isG2Enabled();

        // Read the polygon counts per type.
        short[] polygonCounts = new short[FroggerMapPolygonType.values().length];
        for (int i = 0; i < polygonCounts.length; i++)
            polygonCounts[i] = (g2Supported || i != FroggerMapPolygonType.G2.ordinal()) ? reader.readShort() : 0;
        reader.alignRequireEmpty(Constants.INTEGER_SIZE); // Align.

        // Read the polygon offsets per type.
        int[] polygonOffsets = new int[FroggerMapPolygonType.values().length];
        for (int i = 0; i < polygonOffsets.length; i++)
            polygonOffsets[i] = (g2Supported || i != FroggerMapPolygonType.G2.ordinal()) ? reader.readInt() : 0;

        // Warn if G2 polygon data is seen, but it's not expected.
        int g2PolygonOffset = polygonOffsets[FroggerMapPolygonType.G2.ordinal()];
        if (g2PolygonOffset != 0 && !g2Enabled)
            getLogger().warning("There appears to be G2 polygon data at 0x%X, but G2 polygon support is disabled for this build. (Perhaps enable it?)", g2PolygonOffset);

        // Read polygon blocks.
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            List<FroggerMapPolygon> typedPolygons = this.polygonsByType[i];
            typedPolygons.clear();

            // Get information.
            int polygonCount = DataUtils.shortToUnsignedInt(polygonCounts[i]);
            int polygonBlockStartAddress = polygonOffsets[i];

            // Handle G2.
            FroggerMapPolygonType polygonType = FroggerMapPolygonType.values()[i];
            if (polygonType == FroggerMapPolygonType.G2) {
                if (!g2Supported)
                    continue;
                if (!g2Enabled)
                    polygonBlockStartAddress = getParentFile().getVertexPacket().getKnownStartAddress();
            }

            reader.requireIndex(getLogger(), polygonBlockStartAddress, "Expected " + polygonType.name() + " polygon block");

            try {
                for (int j = 0; j < polygonCount; j++) {
                    FroggerMapPolygon newPolygon = new FroggerMapPolygon(getParentFile(), polygonType);
                    newPolygon.load(reader);
                    this.polygons.add(newPolygon);
                    typedPolygons.add(newPolygon);
                }
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Failed to load %d %s map polygons.", polygonCount, polygonType.name());
            }
        }

        // Now that the polygons are loaded, load the polygon lists in the map groups.
        getParentFile().getGroupPacket().loadGroupPolygonLists(this);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        FroggerMapConfig mapConfig = getMapConfig();
        boolean g2Supported = mapConfig.isG2Supported();

        // Write the polygon counts per type.
        for (int i = 0; i < this.polygonsByType.length; i++)
            if (g2Supported || i != FroggerMapPolygonType.G2.ordinal())
                writer.writeUnsignedShort(this.polygonsByType[i].size());
        writer.align(Constants.INTEGER_SIZE); // Align.

        // Write the slots for polygon offsets per type.
        int polygonPointerOffsetList = writer.getIndex();
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++)
            if (g2Supported || i != FroggerMapPolygonType.G2.ordinal())
                writer.writeNullPointer();

        // Map groups were previously generated, and determine polygon save order.
        // All polygons found in this object are guaranteed to have been placed into map groups by that process.
        // So, instead of getting polygons from this object to save, we must get them from the map group to ensure proper save order.
        // Not doing this will make map groups not load properly.
        FroggerMapFilePacketGroup groupPacket = getParentFile().getGroupPacket();

        // Write the polygon blocks.
        int[] polygonOffsets = new int[FroggerMapPolygonType.values().length];
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            if (i == FroggerMapPolygonType.G2.ordinal() && (!g2Supported || !mapConfig.isG2Enabled()))
                continue;

            // Write the pointer to the start of the polygon data.
            int polygonBlockStartAddress = writer.getIndex();
            polygonOffsets[i] = polygonBlockStartAddress;
            writer.jumpTemp(polygonPointerOffsetList);
            writer.writeInt(polygonBlockStartAddress);
            polygonPointerOffsetList = writer.getIndex();
            writer.jumpReturn();

            // Write polygons based on the group order. (All polygons are guaranteed to be within groups due to the generateMapGroups call)
            FroggerMapPolygonType polygonType = FroggerMapPolygonType.values()[i];
            for (int z = 0; z < groupPacket.getGroupZCount(); z++)
                for (int x = 0; x < groupPacket.getGroupXCount(); x++)
                    savePolygonList(writer, groupPacket.getMapGroup(x, z), polygonType);

            // Lastly, write the polygons in the invisible group.
            if (groupPacket.hasInvisiblePolygonGroup())
                savePolygonList(writer, groupPacket.getInvisibleMapGroup(), polygonType);
        }

        // Now that the polygons are saved, save the polygon lists in the map groups.
        getParentFile().getGroupPacket().saveGroupPolygonLists(writer, polygonOffsets);
    }

    private void savePolygonList(DataWriter writer, FroggerMapGroup mapGroup, FroggerMapPolygonType polygonType) {
        if (mapGroup == null)
            throw new NullPointerException("mapGroup");
        List<FroggerMapPolygon> polygons = mapGroup.getPolygonsByType(polygonType);
        for (int i = 0; i < polygons.size(); i++)
            polygons.get(i).save(writer);
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getGraphicalPacket().getPolygonPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Total Polygon Count", this.polygons.size());
        propertyList.add("F Polygon Counts", "F3: " + getPolygonsByType(FroggerMapPolygonType.F3).size() + ", F4: " + getPolygonsByType(FroggerMapPolygonType.F4).size());
        propertyList.add("FT Polygon Counts", "FT3: " + getPolygonsByType(FroggerMapPolygonType.FT3).size() + ", FT4: " + getPolygonsByType(FroggerMapPolygonType.FT4).size());
        propertyList.add("G Polygon Counts", "G3: " + getPolygonsByType(FroggerMapPolygonType.G3).size() + ", G4: " + getPolygonsByType(FroggerMapPolygonType.G4).size());
        propertyList.add("GT Polygon Counts", "GT3: " + getPolygonsByType(FroggerMapPolygonType.GT3).size() + ", GT4: " + getPolygonsByType(FroggerMapPolygonType.GT4).size());
        int g2Count = getPolygonsByType(FroggerMapPolygonType.G2).size();
        if (g2Count > 0)
            propertyList.add("G2 Polygon Count", g2Count);

        return propertyList;
    }

    /**
     * Gets the polygons.
     */
    public List<FroggerMapPolygon> getPolygons() {
        return Collections.unmodifiableList(this.polygons);
    }

    /**
     * Gets the polygons of a given type.
     */
    public List<FroggerMapPolygon> getPolygonsByType(FroggerMapPolygonType polygonType) {
        if (polygonType == null)
            throw new NullPointerException("polygonType");

        return Collections.unmodifiableList(this.polygonsByType[polygonType.ordinal()]);
    }

    /**
     * Clears the polygons.
     */
    public void clearPolygons() {
        this.polygons.clear();
        for (int i = 0; i < this.polygonsByType.length; i++)
            this.polygonsByType[i].clear();
    }

    /**
     * Registers a new polygon to the packet.
     * @param polygon The polygon to add.
     * @return added successfully
     */
    public boolean addPolygon(FroggerMapPolygon polygon) {
        if (polygon == null || polygon.getPolygonType() == null || this.polygons.contains(polygon))
            return false; // Already contained or null

        this.polygons.add(polygon);
        this.polygonsByType[polygon.getPolygonType().ordinal()].add(polygon);
        return true;
    }
}