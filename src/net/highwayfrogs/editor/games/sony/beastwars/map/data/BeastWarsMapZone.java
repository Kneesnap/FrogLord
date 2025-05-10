package net.highwayfrogs.editor.games.sony.beastwars.map.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a ZONE in a Beast Wars map.
 * The first zone region contains the area of ALL other regions.
 * Most regions only contain a single region. However, some like the one seen in MS1_P_01.MAP (on PC release) contain multiple regions.
 * Created by Kneesnap on 9/22/2023.
 */
@Setter
@Getter
public class BeastWarsMapZone extends SCGameData<BeastWarsInstance> {
    private final BeastWarsMapFile mapFile;
    private final BeastWarsMapZoneRegion mainRegion;
    private final List<BeastWarsMapZoneRegion> regions = new ArrayList<>();
    private int unknown1; // TODO: What are these? a pointer to this spot is used interestingly, so these might contain important data?
    private int unknown2;
    private int unknown3;
    private int unknown4;

    private transient BeastWarsMapZoneRegion[][] regionCache;

    public BeastWarsMapZone(BeastWarsMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
        this.mainRegion = new BeastWarsMapZoneRegion(this);
    }

    /**
     * Gets the main region which encompasses all other regions.
     * This region encompasses all the other regions for quick and easy testing.
     */
    public BeastWarsMapZoneRegion getMainRegion() {
        return this.regions.isEmpty() ? null : this.regions.get(0);
    }

    @Override
    public void load(DataReader reader) {
        int regionCount = reader.readUnsignedShortAsInt();
        this.unknown1 = reader.readUnsignedShortAsInt();
        this.unknown2 = reader.readUnsignedShortAsInt();
        this.unknown3 = reader.readUnsignedShortAsInt();
        this.unknown4 = reader.readUnsignedShortAsInt();

        // Read Regions
        if (regionCount < 1)
            throw new RuntimeException("There was no main region for the map ZONE. This is unexpected.");

        this.regions.clear();
        this.mainRegion.load(reader);
        for (int i = 1; i < regionCount; i++) {
            BeastWarsMapZoneRegion region = new BeastWarsMapZoneRegion(this);
            region.load(reader);
            this.regions.add(region);
        }

        // Setup region cache.
        updateRegionCache();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.regions.size() + 1);
        writer.writeUnsignedShort(this.unknown1);
        writer.writeUnsignedShort(this.unknown2);
        writer.writeUnsignedShort(this.unknown3);
        writer.writeUnsignedShort(this.unknown4);

        // Save region data.
        this.mainRegion.save(writer);
        for (int i = 0; i < this.regions.size(); i++)
            this.regions.get(i).save(writer);
    }

    /**
     * Gets the region covered the given area covers.
     * This relies upon having an up to date region cache.
     * @param localX The local x position. (0, 0) is the start of the "mainRegion" area.
     * @param localZ The local z position. (0, 0) is the start of the "mainRegion" area.
     * @return region, if any. Null if this is outside the bounds of the region.
     */
    public BeastWarsMapZoneRegion getRegion(int localX, int localZ) {
        return this.regionCache != null && localX >= 0 && localZ >= 0 && localZ < this.regionCache.length
                && localX < this.regionCache[localZ].length ? this.regionCache[localZ][localX] : null;
    }

    /**
     * Updates the dimensions of the main region.
     */
    public void updateMainRegion() {
        if (this.regions.isEmpty())
            return; // Not much to say.

        byte minX = Byte.MAX_VALUE;
        byte maxX = Byte.MIN_VALUE;
        byte minZ = Byte.MAX_VALUE;
        byte maxZ = Byte.MIN_VALUE;
        for (int i = 0; i < this.regions.size(); i++) {
            BeastWarsMapZoneRegion region = this.regions.get(i);
            byte startX = region.getMinX();
            byte endX = region.getMaxX();
            byte startZ = region.getMinZ();
            byte endZ = region.getMaxZ();

            if (startX < minX)
                minX = startX;
            if (endX > maxX)
                maxX = endX;
            if (startZ < minZ)
                minZ = startZ;
            if (endZ > maxZ)
                maxZ = endZ;
        }

        this.mainRegion.setMinX(minX);
        this.mainRegion.setMaxX(maxX);
        this.mainRegion.setMinZ(minZ);
        this.mainRegion.setMaxZ(maxZ);
    }

    /**
     * Updates the cache of which coordinates correspond to which regions.
     */
    public void updateRegionCache() {
        // Verify any area at all is covered.
        if (this.mainRegion.getXLength() < 0 || this.mainRegion.getZLength() < 0) {
            this.regionCache = null;
            return;
        }

        // Create new cache if the lengths have changed.
        if (this.regionCache == null || (this.mainRegion.getZLength() != this.regionCache.length + 1) || this.regionCache[0].length != this.mainRegion.getXLength() + 1)
            this.regionCache = new BeastWarsMapZoneRegion[this.mainRegion.getZLength() + 1][this.mainRegion.getXLength() + 1];

        // Clear everything to use the main region.
        for (int i = 0; i < this.regionCache.length; i++)
            Arrays.fill(this.regionCache[i], this.mainRegion);

        // Fill.
        for (int i = 0; i < this.regions.size(); i++) {
            BeastWarsMapZoneRegion region = this.regions.get(i);
            int baseX = region.getAbsoluteStartX() - this.mainRegion.getAbsoluteStartX();
            int baseZ = region.getAbsoluteStartZ() - this.mainRegion.getAbsoluteStartZ();

            for (int zOffset = 0; zOffset <= region.getZLength(); zOffset++) {
                for (int xOffset = 0; xOffset <= region.getXLength(); xOffset++) {
                    int x = baseX + xOffset;
                    int z = baseZ + zOffset;
                    if (this.regionCache[z][x] == this.mainRegion) // Only the first region covering an area.
                        this.regionCache[z][x] = region;
                }
            }
        }
    }

    /**
     * Represents a map zone region, or a rectangle covering map grid entries.
     * Both minimum and maximum positions are inclusive. (Including absolute start positions and lengths).
     * So, <= checks should be used instead of <.
     */
    @Getter
    @Setter
    public static class BeastWarsMapZoneRegion extends SCGameData<BeastWarsInstance> {
        private final BeastWarsMapZone zone;
        // These positions are relative to an origin of 0, where [0, 0] (the origin) is the MIDDLE of the map grid.
        // In other words, these positions are relative to the center of the map grid.
        // These values ARE signed, because they need to cover negative space sometimes.
        // Both minimum and maximum positions are inclusive.
        private byte minX;
        private byte minZ;
        private byte maxX;
        private byte maxZ;

        public BeastWarsMapZoneRegion(BeastWarsMapZone zone) {
            super(zone.getGameInstance());
            this.zone = zone;
        }

        /**
         * Gets the absolute X position in the map grid.
         * This is the X position as it is read/written to the map file.
         */
        public byte getAbsoluteStartX() {
            return (byte) (this.minX + (this.zone.getMapFile().getHeightMapXLength() >> 1));
        }

        /**
         * Gets the absolute Z position in the map grid.
         * This is the Z position as it is read/written to the map file.
         */
        public byte getAbsoluteStartZ() {
            return (byte) (this.minZ + (this.zone.getMapFile().getHeightMapZLength() >> 1));
        }

        /**
         * Gets the number of grid units this zone covers in the X direction.
         * This is the X length as it is read/written to the map file.
         */
        public byte getXLength() {
            return (byte) (this.maxX - this.minX);
        }

        /**
         * Gets the number of grid units this zone covers in the Z direction.
         * This is the Z length as it is read/written to the map file.
         */
        public byte getZLength() {
            return (byte) (this.maxZ - this.minZ);
        }

        @Override
        @SuppressWarnings("CommentedOutCode")
        public void load(DataReader reader) {
            byte startX = reader.readByte();
            byte startZ = reader.readByte();
            byte xLength = reader.readByte();
            byte zLength = reader.readByte();

            // Convert from absolute grid pos to the relative of the center of the grid.
            this.minX = (byte) (startX - (this.zone.getMapFile().getHeightMapXLength() >> 1));
            this.minZ = (byte) (startZ - (this.zone.getMapFile().getHeightMapZLength() >> 1));

            // Convert from lengths to relative position.
            this.maxX = (byte) (this.minX + xLength);
            this.maxZ = (byte) (this.minZ + zLength);

            // Swap minX and maxX if necessary.
            if (this.maxX < this.minX) {
                byte temp = this.minX;
                this.minX = this.maxX;
                this.maxX = temp;
            }

            // This is the clever method used by the game executable to swap two values.
            // However, it's much less readable, so I'm disabling it in favor of something more readable.
            /*if (this.maxX < this.minX) {
                this.minX = (byte) (this.maxX ^ this.minX);
                this.maxX = (byte) (this.maxX ^ this.minX);
                this.minX ^= this.maxX;
            }*/

            // Swap minZ and maxZ if necessary.
            if (this.maxZ < this.minZ) {
                byte temp = this.minZ;
                this.minZ = this.maxZ;
                this.maxZ = temp;
            }

            // This is the clever method used by the game executable to swap two values.
            // However, it's much less readable, so I'm disabling it in favor of something more readable.
            /*if (this.maxZ < this.minZ) {
                this.minZ = (byte) (this.maxZ ^ this.minZ);
                this.maxZ = (byte) (this.maxZ ^ this.minZ);
                this.minZ ^= this.maxZ;
            }*/
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeByte(getAbsoluteStartX());
            writer.writeByte(getAbsoluteStartZ());
            writer.writeByte(getXLength());
            writer.writeByte(getZLength());
        }
    }
}