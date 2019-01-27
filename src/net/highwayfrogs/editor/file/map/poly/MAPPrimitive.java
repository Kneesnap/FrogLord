package net.highwayfrogs.editor.file.map.poly;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a MAP primitive.
 * Created by Kneesnap on 1/26/2019.
 */
@Getter
public abstract class MAPPrimitive extends PSXGPUPrimitive {
    private int[] vertices;
    private MAPPrimitiveType type;

    public MAPPrimitive(MAPPrimitiveType type, int verticeCount) {
        this.type = type;
        this.vertices = new int[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (int vertice : getVertices())
            writer.writeUnsignedShort(vertice);
    }

    /**
     * Get the number of vertices stored by this primitive.
     * @return verticeCount
     */
    public int getVerticeCount() {
        return vertices.length;
    }
}
