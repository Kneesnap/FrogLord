package net.highwayfrogs.editor.games.sony.frogger.map.data.zone;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketZone;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Map zones are collections of rectangles which apply to the collision grid.
 * The only usage in Frogger seems to be for controlling camera rotations in different parts of the world.
 * Represents the "ZONE" struct.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public abstract class FroggerMapZone extends SCGameData<FroggerGameInstance> {
    private final FroggerMapFile mapFile;
    private final FroggerMapZoneType zoneType;
    private final FroggerMapZoneRegion boundingRegion = new FroggerMapZoneRegion(); // This region always contains the child regions. TODO: Auto-generate.
    private final List<FroggerMapZoneRegion> regions = new ArrayList<>();

    public FroggerMapZone(FroggerMapFile mapFile, FroggerMapZoneType zoneType) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
        this.zoneType = zoneType;
    }

    @Override
    public void load(DataReader reader) {
        int regionCount = reader.readUnsignedShortAsInt();
        this.boundingRegion.load(reader);
        int regionDataStartAddress = reader.readInt();

        // Read extra data.
        loadExtensionData(reader);

        // Prepare regions for reading.
        this.regions.clear();
        reader.requireIndex(getLogger(), regionDataStartAddress, "Expected FroggerMapZoneRegion list");
        for (int i = 0; i < regionCount; i++) {
            FroggerMapZoneRegion newRegion = new FroggerMapZoneRegion();
            newRegion.load(reader);
            this.regions.add(newRegion);
        }
    }

    /**
     * Loads the type-specific data.
     * @param reader the reader to load data from.
     */
    protected abstract void loadExtensionData(DataReader reader);

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.zoneType.ordinal());
        writer.writeUnsignedShort(this.regions.size());
        this.boundingRegion.save(writer);
        int regionDataStartAddress = writer.writeNullPointer();

        // Write extension data.
        saveExtensionData(writer);

        // Save regions.
        writer.writeAddressTo(regionDataStartAddress);
        for (int i = 0; i < this.regions.size(); i++)
            this.regions.get(i).save(writer);
    }

    /**
     * Gets the index of the zone in the map file.
     */
    public int getZoneIndex() {
        FroggerMapFilePacketZone zonePacket = this.mapFile.getZonePacket();
        return zonePacket.getLoadingIndex(zonePacket.getZones(), this);
    }

    /**
     * Gets the string which provides context surrounding the logger.
     */
    public String getLoggerInfo() {
        return this.mapFile != null ? this.mapFile.getFileDisplayName() + "|FroggerMapZone{" + getZoneIndex() + "," + this.zoneType + "}" : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapZone::getLoggerInfo, this);
    }

    /**
     * Saves the type-specific data.
     * @param writer the writer to write data to.
     */
    protected abstract void saveExtensionData(DataWriter writer);

    /**
     * Get the amount of regions.
     * @return regionCount
     */
    public int getRegionCount() {
        return getRegions().size();
    }

    /**
     * Check if this zone contains a grid tile.
     * @param gridX The grid x coordinate.
     * @param gridZ The grid z coordinate.
     * @return contains
     */
    public boolean contains(int gridX, int gridZ) {
        if (this.regions.size() > 0)
            return getRegion(gridX, gridZ) != this.boundingRegion;

        return this.boundingRegion.contains(gridX, gridZ);
    }

    /**
     * Gets the FroggerMapZoneRegion for a grid tile.
     * @param gridX The grid tile x coordinate.
     * @param gridZ The grid tile z coordinate.
     * @return region
     */
    public FroggerMapZoneRegion getRegion(int gridX, int gridZ) {
        if (!this.boundingRegion.contains(gridX, gridZ))
            return null; // Not present within the bounding zone.

        for (int i = 0; i < this.regions.size(); i++) {
            FroggerMapZoneRegion region = this.regions.get(i);
            if (region.contains(gridX, gridZ))
                return region;
        }

        // Must be found in the bounding region (and none of the sub-regions)
        return this.boundingRegion;
    }

    /**
     * Reads a frogger map zone.
     * @param reader the reader to read the map zone data from
     * @param mapFile the map file to create the zone for
     * @return mapZone
     */
    public static FroggerMapZone readFroggerMapZone(DataReader reader, FroggerMapFile mapFile) {
        short zoneTypeId = reader.readShort();
        if (zoneTypeId < 0 || zoneTypeId >= FroggerMapZoneType.values().length)
            throw new RuntimeException("Invalid FroggerMapZoneType: " + zoneTypeId);

        FroggerMapZoneType zoneType = FroggerMapZoneType.values()[zoneTypeId];
        FroggerMapZone mapZone = zoneType.createNewZoneInstance(mapFile);
        mapZone.load(reader);
        return mapZone;
    }
}