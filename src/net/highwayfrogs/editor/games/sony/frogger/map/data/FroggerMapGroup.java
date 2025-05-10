package net.highwayfrogs.editor.games.sony.frogger.map.data;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygonType;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGroup;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketPolygon;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.*;

/**
 * Represents the "MAP_GROUP" struct, which is used to determine which parts of the world need to be rendered, and which don't.
 * Created by Kneesnap on 8/29/2018.
 */
public class FroggerMapGroup extends SCGameData<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile;
    @Getter private final int x;
    @Getter private final int z;
    @SuppressWarnings("unchecked") private final List<FroggerMapPolygon>[] polygonsByType = (List<FroggerMapPolygon>[]) new List[FroggerMapPolygonType.values().length];
    @SuppressWarnings("unchecked") private final List<FroggerMapPolygon>[] unmodifiablePolygonsByType = (List<FroggerMapPolygon>[]) new List[this.polygonsByType.length];
    private final List<FroggerMapPolygon> allPolygons = new ArrayList<>();
    private final List<FroggerMapPolygon> unmodifiablePolygons = Collections.unmodifiableList(this.allPolygons);
    private transient final short[] loadPolygonCounts = new short[FroggerMapPolygonType.values().length];
    private transient final int[] loadPolygonPointers = new int[FroggerMapPolygonType.values().length];
    private transient int staticEntityListPointer = -1;

    public static final int MAX_POLYGON_COUNT = 255;

    public FroggerMapGroup(FroggerMapFile mapFile, int x, int z) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
        this.x = x;
        this.z = z;
        for (int i = 0; i < this.polygonsByType.length; i++) {
            this.polygonsByType[i] = new ArrayList<>();
            this.unmodifiablePolygonsByType[i] = Collections.unmodifiableList(this.polygonsByType[i]);
        }
        Arrays.fill(this.loadPolygonCounts, (short) -1);
        Arrays.fill(this.loadPolygonPointers, -1);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    /**
     * Builds 2 -> 4 do not resolve properly. I have not dug into why this is, but let's avoid the warning message for now since it seems OK.
     * @return earlyJulyPointerFormat
     */
    public boolean isEarlyJulyPointerFormat() {
        return getConfig().isAtOrBeforeBuild4() && !getConfig().isAtOrBeforeBuild1();
    }

    @Override
    public void load(DataReader reader) {
        boolean g2Supported = this.mapFile != null && this.mapFile.getMapConfig().isG2Supported();
        FroggerMapConfig mapConfig = this.mapFile != null ? this.mapFile.getMapConfig() : null;
        if (mapConfig == null)
            throw new NullPointerException("Failed to obtain mapConfig");

        // Read polygon counts.
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            FroggerMapPolygonType type = FroggerMapPolygonType.values()[i];
            this.loadPolygonCounts[i] = (g2Supported || type != FroggerMapPolygonType.G2) ? reader.readUnsignedByteAsShort() : 0;
        }
        reader.alignRequireEmpty(Constants.INTEGER_SIZE); // Padding.

        // Read polygon pointers.
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            FroggerMapPolygonType type = FroggerMapPolygonType.values()[i];
            this.loadPolygonPointers[i] = (g2Supported || type != FroggerMapPolygonType.G2) ? reader.readInt() : 0;

            // Ensure the value is zero unless G2 is enabled.
            if (g2Supported && type == FroggerMapPolygonType.G2 && !mapConfig.isG2Enabled() && this.loadPolygonPointers[i] != 0)
                throw new RuntimeException("The G2 polygon pointer was expected to be NULL, but was " + NumberUtils.toHexString(this.loadPolygonPointers[i]) + ".");
        }

        this.staticEntityListPointer = reader.readInt();
        reader.skipBytes((mapConfig.getGroupPaddingAmount() - 1) * Constants.POINTER_SIZE); // There's actually some data here, but it's not used by the game.
    }

    /**
     * Reads the entity list from the current position.
     * @param reader the reader to read it from
     */
    public void loadStaticEntityList(DataReader reader) {
        if (this.staticEntityListPointer == 0) {
            this.staticEntityListPointer = -1;
            return; // Empty.
        }

        if (this.staticEntityListPointer <= 0)
            throw new RuntimeException("Cannot read static entity list, the pointer " + NumberUtils.toHexString(this.staticEntityListPointer) + " is invalid.");

        // There isn't actually any static entity list saved, so we'll just validate the pointer as a sanity check and continue.
        requireReaderIndex(reader, this.staticEntityListPointer, "Expected static entity list");
        this.staticEntityListPointer = -1;
    }

    /**
     * Loads polygon lists from the polygon packet.
     */
    public void loadPolygonLists(FroggerMapFilePacketPolygon polygonPacket, boolean polygonsVisible) {
        if (this.mapFile.getMapConfig().isOldFormFormat())
            return; // The groups data looks correct, but it doesn't seem to align with the expected data format.

        this.allPolygons.clear();
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            FroggerMapPolygonType polygonType = FroggerMapPolygonType.values()[i];
            List<FroggerMapPolygon> typedPolygons = this.polygonsByType[i];
            int polygonCount = DataUtils.shortToUnsignedInt(this.loadPolygonCounts[i]);
            int polygonPointerAddress = this.loadPolygonPointers[i];
            if (polygonPointerAddress < 0 || polygonCount < 0)
                throw new RuntimeException("Cannot load polygon list with " + polygonCount + " from " + NumberUtils.toHexString(polygonPointerAddress) + ".");

            this.loadPolygonCounts[i] = -1;
            this.loadPolygonPointers[i] = -1;
            typedPolygons.clear();

            // Read polygons.
            if (polygonCount == 0)
                continue;

            // Binary search to find the target polygon.
            List<FroggerMapPolygon> globalPolygonsByType = polygonPacket.getPolygonsByType(polygonType);
            int polygonIndex = Utils.binarySearch(globalPolygonsByType, polygonPointerAddress, FroggerMapPolygon::getLastReadAddress);
            if (polygonIndex < 0) {
                if (!isEarlyJulyPointerFormat())
                    getLogger().severe("Failed to resolve map group polygon block " + polygonType + " at " + NumberUtils.toHexString(polygonPointerAddress) + " with " + polygonCount + " polygons.");
                continue;
            }

            // Copy polygons.
            for (int j = 0; j < polygonCount; j++) {
                FroggerMapPolygon polygon = globalPolygonsByType.get(polygonIndex + j);
                polygon.setVisible(polygonsVisible);
                typedPolygons.add(polygon);
                this.allPolygons.add(polygon);
            }
        }
    }

    /**
     * Checks if the polygons tracked here match the positions FrogLord would place the polygons in itself.
     */
    public void warnIfFrogLordAlgorithmDoesNotMatchData() {
        if (isInvisibleGroup())
            return;

        // Validate the groups of the polygons here.
        FroggerMapFilePacketGroup groupPacket = this.mapFile.getGroupPacket();
        for (int i = 0; i < this.polygonsByType.length; i++) {
            List<FroggerMapPolygon> polygons = this.polygonsByType[i];

            int polygonErrorCount = 0;
            for (int j = 0; j < polygons.size(); j++) {
                FroggerMapPolygon polygon = polygons.get(j);
                if (groupPacket.getMapGroup(polygon) != this)
                    polygonErrorCount++;
            }

            if (polygonErrorCount > 0)
                getLogger().warning("%d %s polygon(s) were in rendering groups that FrogLord did not expect!", polygonErrorCount, FroggerMapPolygonType.values()[i]);
        }

    }

    @Override
    public void save(DataWriter writer) {
        if (isEarlyJulyPointerFormat()) // See other usages of this function to see what's broken.
            throw new RuntimeException("This build '" + getConfig().getInternalName() + "' cannot be saved due to differences in the map group format. If saving this build is desired, request the feature to be added to FrogLord.");

        boolean g2Supported = this.mapFile != null && this.mapFile.getMapConfig().isG2Supported();
        FroggerMapConfig mapConfig = this.mapFile != null ? this.mapFile.getMapConfig() : null;
        if (mapConfig == null)
            throw new NullPointerException("Failed to obtain mapConfig");

        // Write polygon counts.
        boolean disablePolygonLimit = isPolygonLimitDisabled();
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            FroggerMapPolygonType type = FroggerMapPolygonType.values()[i];
            short polygonCount = (short) this.polygonsByType[i].size();

            // The invisible polygon group supports having > 255 polygons because it is never rendered.
            // But, we can't actually write a number > 255, so this is done to maintain consistency with the behavior seen in original maps. (Reference _WIN95 maps)
            if (disablePolygonLimit)
                polygonCount %= (MAX_POLYGON_COUNT + 1);

            if (g2Supported || type != FroggerMapPolygonType.G2)
                writer.writeUnsignedByte(polygonCount);
        }
        writer.align(Constants.INTEGER_SIZE);

        // Write placeholder polygon pointers.
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            FroggerMapPolygonType type = FroggerMapPolygonType.values()[i];
            this.loadPolygonPointers[i] = (g2Supported || type != FroggerMapPolygonType.G2) ? writer.writeNullPointer() : 0;
        }

        this.staticEntityListPointer = writer.writeNullPointer();
        writer.writeNull((mapConfig.getGroupPaddingAmount() - 1) * Constants.POINTER_SIZE);
    }

    /**
     * Writes the static entity list to the current position.
     * @param writer the writer to write it to
     */
    public void saveStaticEntityList(DataWriter writer) {
        if (this.staticEntityListPointer <= 0)
            throw new RuntimeException("Cannot write static entity list, the pointer " + NumberUtils.toHexString(this.staticEntityListPointer) + " is invalid.");

        // If this is the invisible map group, don't write anything, there are no entities.
        // Keep the null pointer written.
        if (isInvisibleGroup()) {
            this.staticEntityListPointer = -1;
            return;
        }

        // This data doesn't actually exist, so just write the pointer and be done with it.
        writer.writeAddressTo(this.staticEntityListPointer);
        this.staticEntityListPointer = -1;
    }

    /**
     * Saves polygon lists. (Should occur after the polygon packet is saved.)
     */
    public void savePolygonLists(DataWriter writer, int[] polygonOffsets) {
        FroggerMapConfig mapConfig = getMapFile().getMapConfig();
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            FroggerMapPolygonType polygonType = FroggerMapPolygonType.values()[i];
            if (polygonType == FroggerMapPolygonType.G2 && (!mapConfig.isG2Supported() || !mapConfig.isG2Enabled())) {
                this.loadPolygonPointers[i] = -1;
                continue;
            }

            List<FroggerMapPolygon> localTypedPolygons = this.polygonsByType[i];
            writer.writeIntAtPos(this.loadPolygonPointers[i], polygonOffsets[i]); // Write polygon data offset.
            polygonOffsets[i] += localTypedPolygons.size() * polygonType.getSizeInBytes();
            this.loadPolygonPointers[i] = -1;
        }
    }

    /**
     * Gets the logger information.
     */
    public String getLoggerInfo() {
        if (this.mapFile == null)
            return Utils.getSimpleName(this);

        return this.mapFile.getFileDisplayName() + "|MapGroup[" + (isInvisibleGroup() ?  "Invisible" : this.z + "][" + this.x) + "]";
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapGroup::getLoggerInfo, this);
    }

    /**
     * Clears the contents of the map group.
     */
    public void clear() {
        this.allPolygons.clear();
        for (int i = 0; i < this.polygonsByType.length; i++)
            this.polygonsByType[i].clear();
    }

    /**
     * Test if this is the map group containing invisible polygons.
     */
    public boolean isInvisibleGroup() {
        return this.x == -1 && this.z == -1;
    }

    /**
     * Returns true iff the polygon limit is disabled for this particular map group.
     */
    private boolean isPolygonLimitDisabled() {
        // Invisible groups have the polygon limit disabled.
        // This behavior is observable in the low poly _WIN95 maps, such as JUN1_WIN95.MAP, which have tons of invisible polygons for collision.
        return isInvisibleGroup();
    }

    /**
     * Gets a Collection containing all registered polygons. The order of polygons is not guaranteed.
     */
    public Collection<FroggerMapPolygon> getAllPolygons() {
        return this.unmodifiablePolygons;
    }

    /**
     * Gets a list containing all polygons of the given type registered to the group.
     * @param polygonType the type of polygons to obtain
     * @return polygonList
     */
    public List<FroggerMapPolygon> getPolygonsByType(FroggerMapPolygonType polygonType) {
        if (polygonType == null)
            throw new NullPointerException("polygonType");

        return this.unmodifiablePolygonsByType[polygonType.ordinal()];
    }

    /**
     * Adds the polygon to the map group.
     * Failure conditions include the polygon already being registered, or the maximum polygon count having been reached.
     * @param polygon the polygon to add
     * @return true iff the polygon was added successfully
     */
    public boolean addPolygon(FroggerMapPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");

        if (polygon.getMapFile() != this.mapFile) {
            String groupMapFileName = this.mapFile != null ? this.mapFile.getFileDisplayName() : null;
            String polygonMapFileName = polygon.getMapFile() != null ? polygon.getMapFile().getFileDisplayName() : null;
            throw new RuntimeException("The polygon belongs to different map file (" + polygonMapFileName + ") than the one the group exists for! (" + groupMapFileName + ")");
        }

        if (polygon.isVisible() == isInvisibleGroup())
            throw new RuntimeException("Cannot add " + (polygon.isVisible() ? "a visible" : "an invisible") + " polygon to this map group.");

        List<FroggerMapPolygon> polygonList = this.polygonsByType[polygon.getPolygonType().ordinal()];

        // The invisible map group is never rendered, so it's fine if it goes past its limits. This even happens in the retail _WIN95 maps.
        // We need to track the polygon, because map group registration is important for actually saving polygons.
        if (polygonList.size() >= MAX_POLYGON_COUNT && !isPolygonLimitDisabled())
            return false;

        if (polygonList.contains(polygon))
            return false;

        polygonList.add(polygon);
        this.allPolygons.add(polygon);
        return true;
    }

    /**
     * Test if a polygon is tracked by the map group
     * @param polygon the polygon to test
     */
    public boolean contains(FroggerMapPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");

        if (polygon.getMapFile() != this.mapFile)
            return false;

        return this.polygonsByType[polygon.getPolygonType().ordinal()].contains(polygon);
    }
}