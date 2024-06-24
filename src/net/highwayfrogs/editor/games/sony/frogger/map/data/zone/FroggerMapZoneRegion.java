package net.highwayfrogs.editor.games.sony.frogger.map.data.zone;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.TriFunction;

import java.util.function.BiConsumer;

/**
 * Represents a Zone Region.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
public class FroggerMapZoneRegion extends GameObject {
    private short xMin;
    private short zMin;
    private short xMax;
    private short zMax;

    public static final int SIZE_IN_BYTES = 4 * Constants.SHORT_SIZE;

    @Override
    public void load(DataReader reader) {
        this.xMin = reader.readShort();
        this.zMin = reader.readShort();
        this.xMax = reader.readShort();
        this.zMax = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.xMin);
        writer.writeShort(this.zMin);
        writer.writeShort(this.xMax);
        writer.writeShort(this.zMax);
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
     * Test if a grid square is one of the region corners.
     * @param gridX The grid x coordinate to test.
     * @param gridZ The grid z coordinate to test.
     * @return isCorner
     */
    public boolean isCorner(int gridX, int gridZ) {
        return isMinCorner(gridX, gridZ) || isMaxCorner(gridX, gridZ);
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
     * Update the bounds of a region.
     */
    public void updateBounds() {
        if (getXMin() > getXMax()) {
            short temp = this.xMax;
            this.xMax = this.xMin;
            this.xMin = temp;
        }

        if (getZMin() > getZMax()) {
            short temp = this.zMax;
            this.zMax = this.zMin;
            this.zMin = temp;
        }
    }

    @Getter
    @AllArgsConstructor
    public enum RegionEditState {
        CHANGING_MIN(FroggerMapZoneRegion::isMinCorner, FroggerMapZoneRegion::setXMin, FroggerMapZoneRegion::setZMin),
        CHANGING_MAX(FroggerMapZoneRegion::isMaxCorner, FroggerMapZoneRegion::setXMax, FroggerMapZoneRegion::setZMax),
        NONE_SELECTED((region, x, z) -> false, null, null);

        private final TriFunction<FroggerMapZoneRegion, Integer, Integer, Boolean> tester;
        private final BiConsumer<FroggerMapZoneRegion, Short> xSetter;
        private final BiConsumer<FroggerMapZoneRegion, Short> zSetter;

        /**
         * Set the coordinates of the region.
         * @param region The region to set.
         * @param gridX  The grid x coordinate.
         * @param gridZ  The grid z coordinate.
         */
        public void setCoordinates(FroggerMapZoneRegion region, int gridX, int gridZ) {
            this.xSetter.accept(region, (short) gridX);
            this.zSetter.accept(region, (short) gridZ);
            region.updateBounds();
        }
    }
}