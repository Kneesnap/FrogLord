package net.highwayfrogs.editor.file.map.zone;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a Zone Region.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
public class ZoneRegion extends GameObject {
    private short xMin;
    private short zMin;
    private short xMax;
    private short zMax;

    public static final int REGION_SIZE = 4 * Constants.SHORT_SIZE;

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
}
