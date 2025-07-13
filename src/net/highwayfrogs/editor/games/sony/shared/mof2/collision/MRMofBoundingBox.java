package net.highwayfrogs.editor.games.sony.shared.mof2.collision;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Arrays;

/**
 * Represents a "MR_BBOX" struct. Presumably a bounding box.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MRMofBoundingBox implements IBinarySerializable {
    private final SVector[] vertices;
    private static final int COUNT = 8;

    public MRMofBoundingBox() {
        this.vertices = new SVector[COUNT];
        for (int i = 0; i < this.vertices.length; i++)
            this.vertices[i] = new SVector();
    }

    public MRMofBoundingBox(short minX, short minY, short minZ, short maxX, short maxY, short maxZ) {
        this();
        setBoundaries(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.vertices.length; i++)
            this.vertices[i].loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.vertices.length; i++)
            this.vertices[i].saveWithPadding(writer);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.vertices);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MRMofBoundingBox))
            return false;

        MRMofBoundingBox otherBox = (MRMofBoundingBox) other;
        for (int i = 0; i < this.vertices.length; i++)
            if (!this.vertices[i].equals(otherBox.getVertices()[i]))
                return false;
        return true;
    }

    /**
     * Creates a new bounding box based on the provided box coordinates.
     * @param minX the minimum x value
     * @param minY the minimum y value
     * @param minZ the minimum z value
     * @param maxX the maximum x value
     * @param maxY the maximum y value
     * @param maxZ the maximum z value
     */
    public void setBoundaries(short minX, short minY, short minZ, short maxX, short maxY, short maxZ) {
        if (minX > maxX) {
            short temp = minX;
            minX = maxX;
            maxX = temp;
        }

        if (minY > maxY) {
            short temp = minY;
            minY = maxY;
            maxY = temp;
        }

        if (minZ > maxZ) {
            short temp = minZ;
            minZ = maxZ;
            maxZ = temp;
        }

        this.vertices[0].setValues(minX, minY, minZ);
        this.vertices[1].setValues(minX, minY, maxZ);
        this.vertices[2].setValues(minX, maxY, minZ);
        this.vertices[3].setValues(minX, maxY, maxZ);
        this.vertices[4].setValues(maxX, minY, minZ);
        this.vertices[5].setValues(maxX, minY, maxZ);
        this.vertices[6].setValues(maxX, maxY, minZ);
        this.vertices[7].setValues(maxX, maxY, maxZ);
    }

    /**
     * Gets the minimum X coordinate of the bounding box.
     */
    public float getMinX() {
        return this.vertices[0].getFloatX();
    }

    /**
     * Gets the maximum X coordinate of the bounding box.
     */
    public float getMaxX() {
        return this.vertices[COUNT - 1].getFloatX();
    }

    /**
     * Gets the minimum Z coordinate of the bounding box.
     */
    public float getMinZ() {
        return this.vertices[0].getFloatZ();
    }

    /**
     * Gets the maximum Z coordinate of the bounding box.
     */
    public float getMaxZ() {
        return this.vertices[COUNT - 1].getFloatZ();
    }
}
