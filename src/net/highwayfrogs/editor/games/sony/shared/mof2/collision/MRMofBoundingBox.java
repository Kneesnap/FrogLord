package net.highwayfrogs.editor.games.sony.shared.mof2.collision;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;

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

    @Override
    public void load(DataReader reader) {
        for (SVector vector : this.vertices)
            vector.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        for (SVector vector : this.vertices)
            vector.saveWithPadding(writer);
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
        for (int i = 0; i < getVertices().length; i++)
            if (!getVertices()[i].equals(otherBox.getVertices()[i]))
                return false;
        return true;
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
