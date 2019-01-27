package net.highwayfrogs.editor.file.map.poly.line;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a PSX line.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MAPLine extends MAPPrimitive {
    private int[] vertices;

    public MAPLine(MAPPrimitiveType type, int verticeCount) {
        super(type);
        this.vertices = new int[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (int vertice : vertices)
            writer.writeUnsignedShort(vertice);
    }
}
