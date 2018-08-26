package net.highwayfrogs.editor.file.mof;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a "MR_BBOX" struct. Presumably a bounding box.
 * Created by Kneesnap on 8/25/2018.
 */
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
}
