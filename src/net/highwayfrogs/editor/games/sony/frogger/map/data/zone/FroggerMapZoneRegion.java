package net.highwayfrogs.editor.games.sony.frogger.map.data.zone;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

/**
 * Represents a Zone Region.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
public class FroggerMapZoneRegion extends SCGameData<FroggerGameInstance> {
    private final FroggerMapZone parentZone;
    private short xMin;
    private short zMin;
    private short xMax;
    private short zMax;

    public static final int SIZE_IN_BYTES = 4 * Constants.SHORT_SIZE;

    public FroggerMapZoneRegion(@NonNull FroggerMapZone parentZone) {
        super(parentZone.getGameInstance());
        this.parentZone = parentZone;
    }

    @Override
    public void load(DataReader reader) {
        this.xMin = reader.readShort();
        this.zMin = reader.readShort();
        this.xMax = reader.readShort();
        this.zMax = reader.readShort();
        // We could test that xMin <= xMax, and zMin <= zMax, but PSX Build 71 (and presumably others) have a few places where they violate this rule.
        // The game will pretty much just ignore these, so instead of fixing them (which would save data with potentially different behavior from the real game), we ignore it keep it as-is.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.xMin);
        writer.writeShort(this.zMin);
        writer.writeShort(this.xMax);
        writer.writeShort(this.zMax);
    }

    /**
     * Gets the string which provides context surrounding the logger.
     */
    public String getLoggerInfo() {
        if (this.parentZone.getBoundingRegion() == this) {
            return this.parentZone.getLoggerInfo() + "[boundingRegion]";
        } else {
            return this.parentZone.getLoggerInfo() + "[Region=" + getRegionIndex() + "]";
        }
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapZoneRegion::getLoggerInfo, this);
    }

    /**
     * Gets the index of this region within the parent zone.
     * Note that if this is the bounding region, and it is not currently used as a region, -1 will be returned.
     */
    public int getRegionIndex() {
        return this.parentZone.getRegions().lastIndexOf(this);
    }

    /**
     * Copies the data from another region.
     * @param otherRegion the region to copy data from
     */
    public void copyFrom(FroggerMapZoneRegion otherRegion) {
        if (otherRegion == null)
            throw new NullPointerException("otherRegion");

        this.xMin = otherRegion.getXMin();
        this.zMin = otherRegion.getZMin();
        this.xMax = otherRegion.getXMax();
        this.zMax = otherRegion.getZMax();
    }

    /**
     * Test if this region contains a grid coordinate.
     * @param gridX The grid x coordinate to test.
     * @param gridZ The grid z coordinate to test.
     * @return contains
     */
    public boolean contains(int gridX, int gridZ) {
        return gridX >= getXMin() && gridX <= getXMax()
                && gridZ >= getZMin() && gridZ <= getZMax();
    }

    /**
     * Check if two coordinates form the min corner.
     * @param gridX The grid x coordinate to test.
     * @param gridZ The grid z coordinate to test.
     * @return isMinCorner
     */
    public boolean isMinCorner(int gridX, int gridZ) {
        return (gridX == getXMin() && gridZ == getZMin());
    }

    /**
     * Check if two coordinates form the max corner.
     * @param gridX The grid x coordinate to test.
     * @param gridZ The grid z coordinate to test.
     * @return isMaxCorner
     */
    public boolean isMaxCorner(int gridX, int gridZ) {
        return (gridX == getXMax() && gridZ == getZMax());
    }

    /**
     * Swaps xMin and xMax, if xMin > xMax
     * @return if a swap occurred
     */
    public boolean swapXIfNecessary() {
        if (this.xMin > this.xMax) {
            short temp = this.xMax;
            this.xMax = this.xMin;
            this.xMin = temp;
            return true;
        }

        return false;
    }

    /**
     * Swaps zMin and zMax, if zMin > zMax
     * @return if a swap occurred
     */
    public boolean swapZIfNecessary() {
        if (this.zMin > this.zMax) {
            short temp = this.zMax;
            this.zMax = this.zMin;
            this.zMin = temp;
            return true;
        }

        return false;
    }

    /**
     * Clones the region.
     */
    public FroggerMapZoneRegion clone() {
        FroggerMapZoneRegion newRegion = new FroggerMapZoneRegion(this.parentZone);
        newRegion.copyFrom(this);
        return newRegion;
    }
}