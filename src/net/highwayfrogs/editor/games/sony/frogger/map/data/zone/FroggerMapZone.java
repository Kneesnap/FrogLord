package net.highwayfrogs.editor.games.sony.frogger.map.data.zone;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketZone;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Map zones are collections of rectangles which apply to the collision grid.
 * The only usage in Frogger seems to be for controlling camera rotations in different parts of the world.
 * Represents the "ZONE" struct.
 * Created by Kneesnap on 8/22/2018.
 */
public abstract class FroggerMapZone extends SCGameData<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile;
    @Getter private final FroggerMapZoneType zoneType;
    @Getter private final FroggerMapZoneRegion boundingRegion; // This region always contains the child regions, and is automatically generated.
    private final List<FroggerMapZoneRegion> regions = new ArrayList<>(); // This region list only represents the held regions when it contains 2 or more entries. Otherwise, use boundingRegion.
    private final List<FroggerMapZoneRegion> unmodifiableRegions = Collections.unmodifiableList(this.regions);
    private final List<FroggerMapZoneRegion> boundingRegionAsList;

    public FroggerMapZone(FroggerMapFile mapFile, FroggerMapZoneType zoneType) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
        this.zoneType = zoneType;
        this.boundingRegion = new FroggerMapZoneRegion(this);
        this.boundingRegionAsList = Collections.singletonList(this.boundingRegion);
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
        requireReaderIndex(reader, regionDataStartAddress, "Expected FroggerMapZoneRegion list");
        for (int i = 0; i < regionCount; i++) {
            FroggerMapZoneRegion newRegion = new FroggerMapZoneRegion(this);
            this.regions.add(newRegion); // Add before loading, so the Logger can resolve the index if used.
            newRegion.load(reader);
        }

        if (this.regions.size() == 1) { // This is not seen in PSX Build 71.
            getLogger().severe("Zone had a single region, which is odd/unnecessary/may only be seen in prototypes!");
        } else {
            // Validate the bounding regions.
            short oldMinX = this.boundingRegion.getXMin();
            short oldMinZ = this.boundingRegion.getZMin();
            short oldMaxX = this.boundingRegion.getXMax();
            short oldMaxZ = this.boundingRegion.getZMax();
            updateMainBoundingBox();
            short newMinX = this.boundingRegion.getXMin();
            short newMinZ = this.boundingRegion.getZMin();
            short newMaxX = this.boundingRegion.getXMax();
            short newMaxZ = this.boundingRegion.getZMax();
            if (oldMinX != newMinX || oldMinZ != newMinZ || oldMaxX != newMaxX || oldMaxZ != newMaxZ)
                getLogger().severe("Auto-generated boundingRegion[%d,%d,%d,%d] did not match read boundingRegion[%d,%d,%d,%d].",
                        newMinX, newMinZ, newMaxX, newMaxZ, oldMinX, oldMinZ, oldMaxX, oldMaxZ);
        }
    }

    /**
     * Loads the type-specific data.
     * @param reader the reader to load data from.
     */
    protected abstract void loadExtensionData(DataReader reader);

    @Override
    public void save(DataWriter writer) {
        updateMainBoundingBox();

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
     * Gets the list of regions found within the map zone.
     * Note that this list cannot be modified, and contains the bounding region when it is treated as a regular region.
     */
    public List<FroggerMapZoneRegion> getRegions() {
        if (isBoundingRegionTreatedAsRegion()) {
            return this.boundingRegionAsList;
        } else {
            return this.unmodifiableRegions;
        }
    }

    /**
     * When there are no other regions, the boundingRegion is treated as a regular region.
     * @return true, iff the bounding region is treated as a regular region.
     */
    public boolean isBoundingRegionTreatedAsRegion() {
        return this.regions.isEmpty();
    }

    /**
     * Adds a region to the zone
     * @param region the region to add
     * @return true iff the region was added successfully
     */
    public boolean addRegion(FroggerMapZoneRegion region) {
        if (region == null)
            throw new NullPointerException("region");
        if (this.regions.contains(region))
            return false; // Already present.
        if (region.getParentZone() != this)
            throw new RuntimeException("Cannot add region belonging to a different FroggerMapZone object!");
        if (this.boundingRegion == region)
            throw new RuntimeException("Cannot add boundingRegion to the zone region list.");

        // If this is the first region.
        if (isBoundingRegionTreatedAsRegion()) {
            if (this.boundingRegion.getXMin() == 0 && this.boundingRegion.getXMax() == 0 && this.boundingRegion.getZMin() == 0 && this.boundingRegion.getZMax() == 0) {
                // Apply to the main region.
                this.boundingRegion.copyFrom(region);
                return true;
            } else {
                this.regions.add(this.boundingRegion.clone());
            }
        }

        this.regions.add(region);
        if (!this.boundingRegion.contains(region.getXMin(), region.getZMin()) || !this.boundingRegion.contains(region.getXMax(), region.getZMax()))
            updateMainBoundingBox();

        return true;
    }

    /**
     * Removes a region from the zone
     * @param region the region to remove
     * @return true iff the region was removed successfully
     */
    public boolean removeRegion(FroggerMapZoneRegion region) {
        if (region == null)
            throw new NullPointerException("region");
        if (this.boundingRegion == region)
            throw new RuntimeException("Cannot remove boundingRegion from FroggerMapZone.");

        // If this is the first region.
        if (this.regions.isEmpty())
            return false; // Abort!

        if (!this.regions.remove(region))
            return false; // Wasn't removed successfully.

        if (this.regions.size() == 1) { // After removal, if there's only one left, apply it to the main region.
            this.boundingRegion.copyFrom(this.regions.remove(0));
        } else if (this.boundingRegion.getXMin() == region.getXMin() || this.boundingRegion.getXMax() == region.getXMax()
                || this.boundingRegion.getZMin() == region.getZMin() || this.boundingRegion.getZMax() == region.getZMax()) {
            // Update the main bounding box with the change.
            updateMainBoundingBox();
        }

        return true;

    }

    /**
     * Gets the index of the zone in the map file.
     */
    public int getZoneIndex() {
        FroggerMapFilePacketZone zonePacket = this.mapFile.getZonePacket();
        return zonePacket.getLoadingIndex(zonePacket.getZones(), this);
    }

    /**
     * Get the amount of regions.
     * @return regionCount
     */
    public int getRegionCount() {
        return isBoundingRegionTreatedAsRegion() ? 1 : this.regions.size();
    }

    /**
     * Check if this zone contains a grid tile.
     * @param gridX The grid x coordinate.
     * @param gridZ The grid z coordinate.
     * @return contains
     */
    public boolean contains(int gridX, int gridZ) {
        return getRegion(gridX, gridZ) != null;
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
        // However, if the bounding box isn't treated as a regular region, it shouldn't be returned.
        return isBoundingRegionTreatedAsRegion() ? this.boundingRegion : null;
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

    /**
     * Updates/recalculates the main bounding box if necessary.
     */
    public void updateMainBoundingBox() {
        if (this.regions.isEmpty())
            return; // Nothing to update, it's manually controlled by the user.

        if (this.regions.size() == 1) { // This should be treated as the main region.
            FroggerMapZoneRegion singleRegion = this.regions.remove(0);
            this.boundingRegion.copyFrom(singleRegion);
            return;
        }

        // This if statement needs some explanation.
        // The original game (PSX Build 71) has some occasional places where the region zMin can be greater than zMax, or xMin > xMax.
        // These appear to be mistakes, as the game ignores them.
        // In order to avoid potential behavioral mismatches with the original, we'd like to save such zones as-is, which means no-auto updates for them.
        if (this.boundingRegion.getXMin() > this.boundingRegion.getXMax() || this.boundingRegion.getZMin() > this.boundingRegion.getZMax())
            return;

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < this.regions.size(); i++) {
            FroggerMapZoneRegion region = this.regions.get(i);
            if (region.getXMin() > region.getXMax() || region.getZMin() > region.getZMax())
                return; // See above note.

            if (region.getXMin() < minX)
                minX = region.getXMin();
            if (region.getZMin() < minZ)
                minZ = region.getZMin();
            if (region.getXMax() > maxX)
                maxX = region.getXMax();
            if (region.getZMax() > maxZ)
                maxZ = region.getZMax();
        }

        this.boundingRegion.setXMin((short) minX);
        this.boundingRegion.setZMin((short) minZ);
        this.boundingRegion.setXMax((short) maxX);
        this.boundingRegion.setZMax((short) maxZ);
    }
}