package net.highwayfrogs.editor.games.konami.greatquest.model;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the 'kcModelPrim' concept found in the PS2 PAL debug symbols.
 * Created by Kneesnap on 6/22/2023.
 */
@Getter
public class kcModelPrim extends GameObject {
    private final kcModel model;
    private final List<kcVertex> vertices = new ArrayList<>();
    private long materialId; // uint
    private kcPrimitiveType primType;
    @Setter private short[] boneIds;
    private transient long loadedVertexCount = -1;

    public kcModelPrim(kcModel model) {
        this.model = model;
    }

    @Override
    public void load(DataReader reader) {
        this.materialId = reader.readUnsignedIntAsLong();
        this.primType = kcPrimitiveType.values()[reader.readInt()];
        this.loadedVertexCount = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.materialId);
        writer.writeUnsignedInt(this.primType.ordinal());
        writer.writeUnsignedInt(this.vertices.size());
    }

    /**
     * Gets the number of vertices this prim holds.
     * This will return the correct value, regardless of if vertices have loaded or not.
     * @return vertexCount
     */
    public long getVertexCount() {
        return this.loadedVertexCount != -1 ? this.loadedVertexCount : this.vertices.size();
    }

    /**
     * Loads the vertices individually from the raw vertex data.
     * @param reader The reader to read data from.
     * @return loadedVertexCount
     */
    public int loadVertices(DataReader reader) {
        if (this.loadedVertexCount == -1)
            throw new RuntimeException("Cannot load vertices, the loading execution flow wasn't correct.");

        for (int i = 0; i < this.loadedVertexCount; i++) {
            kcVertex vertex = new kcVertex();
            vertex.load(reader, this.model.getComponents(), this.model.getFvf());
            this.vertices.add(vertex);
        }

        this.loadedVertexCount = -1;
        return this.vertices.size();
    }

    /**
     * Writes the vertices individually to raw vertex data.
     * @param writer The writer to write data from.
     */
    public void saveVertices(DataWriter writer) {
        for (int i = 0; i < this.vertices.size(); i++)
            this.vertices.get(i).save(writer, this.model.getComponents(), this.model.getFvf());
    }
}