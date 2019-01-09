package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a "MR_BBOX" struct. Presumably a bounding box.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFBBox extends GameObject {
    private SVector vertices[];
    private static final int COUNT = 8;

    @Override
    public void load(DataReader reader) {
        this.vertices = new SVector[COUNT];
        for (int i = 0; i < COUNT; i++)
            this.vertices[i] = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        for (SVector vector : vertices)
            vector.saveWithPadding(writer);
    }

    @Override
    public int hashCode() {
        return vertices[0].hashCode() + vertices[2].hashCode()
                + vertices[4].hashCode() + vertices[6].hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MOFBBox))
            return false;

        MOFBBox otherBox = (MOFBBox) other;
        for (int i = 0; i < vertices.length; i++)
            if (!otherBox.getVertices()[i].equals(getVertices()[i]))
                return false;

        return true;
    }
}
