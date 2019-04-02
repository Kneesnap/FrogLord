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
    private SVector[] vertices;
    private static final int COUNT = 8;

    public MOFBBox() {
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
    public boolean equals(Object other) {
        if (!(other instanceof MOFBBox))
            return false;

        MOFBBox otherBox = (MOFBBox) other;
        for (int i = 0; i < getVertices().length; i++)
            if (!getVertices()[i].equals(otherBox.getVertices()[i]))
                return false;
        return true;
    }
}
