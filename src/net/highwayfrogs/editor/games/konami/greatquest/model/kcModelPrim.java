package net.highwayfrogs.editor.games.konami.greatquest.model;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the 'kcModelPrim' concept found in the PS2 PAL debug symbols.
 * NOTE: It seems the model bones per primitive is always 0 or 1.
 *  -> ALSO, WEIGHT4F is ALWAYS used if bones per primitive is 1, never WEIGHT1F on PS2 NTSC. Why? Not sure.
 * Created by Kneesnap on 6/22/2023.
 */
public class kcModelPrim extends GameData<GreatQuestInstance> {
    @Getter private final kcModel model;
    @Getter private final List<kcVertex> vertices = new ArrayList<>();
    @Getter private long materialId; // uint
    @Getter private kcPrimitiveType primitiveType = kcPrimitiveType.TRIANGLE_LIST; // TRIANGLE_LIST and TRIANGLE_STRIP are the only ones used in any known build. It is unknown if the other primitive types are implemented.
    @Getter @Setter private short[] boneIds;
    private transient long loadedVertexCount = -1;
    @Getter private transient kcModelNode parentNode; // The node which draws this primitive.

    public kcModelPrim(kcModel model) {
        super(model.getGameInstance());
        this.model = model;
    }

    @Override
    public void load(DataReader reader) {
        this.materialId = reader.readUnsignedIntAsLong();
        this.primitiveType = kcPrimitiveType.values()[reader.readInt()];
        this.loadedVertexCount = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.materialId);
        writer.writeUnsignedInt(this.primitiveType.ordinal());
        writer.writeUnsignedInt(this.vertices.size());
    }

    void setParentNode(kcModelNode parentNode, boolean warnIfAlreadySet) {
        if (this.parentNode == parentNode)
            return;

        // Let the primitive know which node is drawing it.
        if (this.parentNode != null && warnIfAlreadySet)
            getLogger().warning("The kcModelPrim was already drawn by another kcModelNode.");

        this.parentNode = parentNode;
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
            vertex.load(reader, this.model.getComponents(), this.model.getFvf(), true);
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
            this.vertices.get(i).save(writer, this.model.getComponents(), this.model.getFvf(), true);
    }
}