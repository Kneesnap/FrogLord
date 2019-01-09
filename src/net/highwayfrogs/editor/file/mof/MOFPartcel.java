package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_PARTCEL" struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MOFPartcel extends GameObject {
    @Getter private List<SVector> vertices = new ArrayList<>();
    @Getter private List<SVector> normals = new ArrayList<>();
    @Getter private MOFBBox bbox;
    private int vertexCount;
    private int normalCount;

    public MOFPartcel(int vertexCount, int normalCount) {
        this.vertexCount = vertexCount;
        this.normalCount = normalCount;
    }

    @Override
    public void load(DataReader reader) {
        int verticePointer = reader.readInt();
        int normalPointer = reader.readInt();
        int bboxPointer = reader.readInt();
        reader.readInt(); // Unused.

        // Read Vertexes.
        reader.jumpTemp(verticePointer);
        for (int i = 0; i < vertexCount; i++)
            vertices.add(SVector.readWithPadding(reader));
        reader.jumpReturn();

        // Read normals.
        reader.jumpTemp(normalPointer);
        for (int i = 0; i < normalCount; i++)
            normals.add(SVector.readWithPadding(reader));
        reader.jumpReturn();

        // Read BBOX.
        reader.jumpTemp(bboxPointer);
        this.bbox = new MOFBBox();
        this.bbox.load(reader);
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        int vertexPointer = writer.writeNullPointer();
        int normalPointer = writer.writeNullPointer();
        int bboxPointer = writer.writeNullPointer();
        writer.writeNullPointer(); // Unused.

        // Read Vertexes.
        writer.writeAddressTo(vertexPointer);
        for (SVector vertex : getVertices())
            vertex.saveWithPadding(writer);

        // Read normals.
        writer.writeAddressTo(normalPointer);
        for (SVector vector : getNormals())
            vector.saveWithPadding(writer);

        // Read BBOX.
        writer.writeAddressTo(bboxPointer);
        this.bbox.save(writer);
    }
}
