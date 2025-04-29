package net.highwayfrogs.editor.games.sony.shared.landscaper.heightfield;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.landscaper.Landscape;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapeVertex;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a vertex column/vertex stack in an advanced height-field.
 * TODO: Are we using multiple height-fields or multiple root vertices?
 *  -> With separate fused height-fields, we do have the question of having height-fields with differing sizes. (Hmm, not sure how I feel about this yet)
 *    -> One option is to make the setting for the height-field editable on a per-scene basis. Yeah I'm fine with that.
 *  -> Pro: takes full advantage of the height-field system.
 *  -> With a single height-field,
 *    -> We can make each vertex track whether it creates a grid square or not. Perhaps a true/false flag, and the first polygon is the root one or something.
 *    -> Pro: Very simple
 *    -> Con: I think it would be harder to create new geometry for the user. Eg: Harder to make a good UI. Consider this further.
 * Created by Kneesnap on 7/15/2024.
 */
public class AdvancedHeightFieldVertexColumn extends SCSharedGameData {
    @Getter private final AdvancedHeightField heightField;
    private final List<LandscapeVertex> sortedVertices = new ArrayList<>(); // The root vertex part of this.
    private final List<LandscapeVertex> immutableSortedVertices = Collections.unmodifiableList(this.sortedVertices);
    @Getter private LandscapeVertex rootVertex;
    @Getter private transient int gridX;
    @Getter private transient int gridZ;

    public AdvancedHeightFieldVertexColumn(AdvancedHeightField heightField, int gridX, int gridZ) {
        super(heightField.getGameInstance());
        this.heightField = heightField;
        this.gridX = gridX;
        this.gridZ = gridZ;
    }

    @Override
    public void load(DataReader reader) {
        int rootVertexId = reader.readInt();
        this.rootVertex = rootVertexId >= 0 ? getLandscape().getVertices().get(rootVertexId) : null;

        this.sortedVertices.clear();
        short vertexCount = reader.readUnsignedByteAsShort(); // Due to the limitation of how many polygons can be in a group, most likely this is
        for (int i = 0; i < vertexCount; i++) {
            int vertexId = reader.readInt();
            LandscapeVertex vertex = getLandscape().getVertices().get(vertexId);
            this.sortedVertices.add(vertex);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.rootVertex != null ? this.rootVertex.getVertexId() : -1);
        writer.writeUnsignedByte((short) this.sortedVertices.size());
        for (int i = 0; i < this.sortedVertices.size(); i++)
            writer.writeInt(this.sortedVertices.get(i).getVertexId());
    }

    /**
     * Get the landscape which the height-field column is registered to.
     */
    public Landscape getLandscape() {
        return this.heightField != null ? this.heightField.getLandscape() : null;
    }

    /**
     * Gets the sorted vertices by height on this column.
     */
    public List<LandscapeVertex> getSortedVertices() {
        return this.immutableSortedVertices;
    }

    /**
     * Removes a vertex from the column.
     * @param vertex the vertex to remove from the column
     */
    public void removeVertex(LandscapeVertex vertex) {
        this.sortedVertices.remove(vertex);
        if (this.rootVertex == vertex)
            this.rootVertex = null;
    }

    /**
     * Store the origin position of the column into the output vector.
     * @param output the vector to store the position in. If null is provided, a new vector will be created.
     * @return originPosition
     */
    public Vector3f getOriginPosition(Vector3f output) {
        if (output == null)
            output = new Vector3f();

        if (this.heightField != null) {
            output.setXYZ(this.heightField.getOriginPosition());
            output.setX(output.getX() + (this.gridX * this.heightField.getXSquareSize()));
            output.setZ(output.getZ() + (this.gridZ * this.heightField.getZSquareSize()));
        }

        return output;
    }
}
