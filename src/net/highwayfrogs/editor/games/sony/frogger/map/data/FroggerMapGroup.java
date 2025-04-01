package net.highwayfrogs.editor.games.sony.frogger.map.data;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygonType;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketPolygon;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the "MAP_GROUP" struct, which is used to determine which parts of the world need to be rendered, and which don't.
 * Created by Kneesnap on 8/29/2018.
 */
public class FroggerMapGroup extends SCGameData<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile;
    @Getter private final int x;
    @Getter private final int z;
    @Getter @SuppressWarnings("unchecked") private final List<FroggerMapPolygon>[] polygonsByType = (List<FroggerMapPolygon>[]) new List[FroggerMapPolygonType.values().length];
    private transient final short[] loadPolygonCounts = new short[FroggerMapPolygonType.values().length];
    private transient final int[] loadPolygonPointers = new int[FroggerMapPolygonType.values().length];
    private transient int staticEntityListPointer = -1;

    public FroggerMapGroup(FroggerMapFile mapFile, int x, int z) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
        this.x = x;
        this.z = z;
        for (int i = 0; i < this.polygonsByType.length; i++)
            this.polygonsByType[i] = new ArrayList<>();
        Arrays.fill(this.loadPolygonCounts, (short) -1);
        Arrays.fill(this.loadPolygonPointers, -1);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
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
     * Clears the contents of the map group.
     */
    public void clear() {
        for (int i = 0; i < this.polygonsByType.length; i++)
            this.polygonsByType[i].clear();
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
            }
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
        for (int i = 0; i < FroggerMapPolygonType.values().length; i++) {
            FroggerMapPolygonType type = FroggerMapPolygonType.values()[i];
            if (g2Supported || type != FroggerMapPolygonType.G2)
                writer.writeUnsignedByte((short) this.polygonsByType[i].size());
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
     * Builds 2 -> 4 do not resolve properly. I have not dug into why this is, but let's avoid the warning message for now since it seems OK.
     * @return earlyJulyPointerFormat
     */
    public boolean isEarlyJulyPointerFormat() {
        return getConfig().isAtOrBeforeBuild4() && !getConfig().isAtOrBeforeBuild1();
    }

    /**
     * Test if this is the map group containing invisible polygons.
     */
    public boolean isInvisibleGroup() {
        return this.x == -1 && this.z == -1;
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
        return this.mapFile != null ? this.mapFile.getFileDisplayName() + "|MapGroup[" + this.z + "][" + this.x + "]" : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapGroup::getLoggerInfo, this);
    }
}