package net.highwayfrogs.editor.games.sony.shared.landscaper;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a node which can provide landscape data.
 * Created by Kneesnap on 7/16/2024.
 */
public abstract class LandscapeNode<TVertex extends LandscapeVertex, TPolygon extends LandscapePolygon> extends LandscapeBase<TVertex, TPolygon> {
    @Getter private final Landscape landscape;

    protected LandscapeNode(Landscape landscape) {
        super(landscape.getGameInstance());
        this.landscape = landscape;
    }

    /**
     * Returns true iff the node is currently registered to the landscape.
     */
    public boolean isRegistered() {
        return getLandscape() != null && getLandscape().getNodes().contains(this);
    }

    /**
     * Adds a vertex to the node.
     * @param vertex the vertex to add to the node
     * @return true iff the vertex was added successfully
     */
    public boolean addVertex(TVertex vertex) {
        if (vertex == null)
            throw new NullPointerException("vertex");
        if (vertex.getLandscape() != getLandscape())
            throw new RuntimeException("The provided vertex is not usable within the target Landscape!");
        if (vertex.isRegistered())
            return false; // Already registered.

        this.vertices.add(vertex);
        if (isRegistered())
            getLandscape().addNodeVertex(this, vertex);

        return true;
    }

    /**
     * Removes a vertex from the node AND from the landscape.
     * @param vertex the vertex to remove from the node
     * @return true iff the vertex was removed successfully
     */
    public boolean removeVertex(TVertex vertex) {
        if (!removeVertexFromNode(vertex))
            return false;

        if (isRegistered())
            getLandscape().removeNodeVertex(this, vertex, null);

        return true;
    }

    /**
     * Removes a vertex from the node, but not the landscape.
     * @param vertex the vertex to remove from the node
     * @return true iff the vertex was removed successfully
     */
    boolean removeVertexFromNode(TVertex vertex) {
        if (vertex == null)
            throw new NullPointerException("vertex");
        if (vertex.getConnectedPolygons().size() > 0)
            throw new RuntimeException(vertex.getConnectedPolygons().size() + " polygon(s) depend on the vertex, so it cannot be removed.");
        if (!this.vertices.remove(vertex))
            return false; // This could probably be binary searched if the node is registered.

        vertex.onRemoveFromNode();
        return true;
    }

    /**
     * Adds a polygon to the node.
     * @param polygon the polygon to add to the node
     * @return true iff the polygon was added successfully
     */
    public boolean addPolygon(TPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (polygon.getLandscape() != getLandscape())
            throw new RuntimeException("The provided polygon is not usable within the target Landscape!");
        if (polygon.isRegistered())
            return false; // Already registered.

        this.polygons.add(polygon);
        if (isRegistered())
            getLandscape().addNodePolygon(this, polygon);

        return true;
    }

    /**
     * Removes a polygon from the node.
     * @param polygon the polygon to remove from the node
     * @return true iff the polygon was removed successfully
     */
    public boolean removePolygon(TPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (!this.polygons.remove(polygon))
            return false; // This could probably be binary searched if the node is registered.

        if (isRegistered())
            getLandscape().removeNodePolygon(this, polygon, null);

        return true;
    }

    /**
     * Read the node's vertices from the vertex block.
     * Assumes the reader is at the correct spot within the vertex block.
     * @param reader the reader to read the vertex data from
     */
    protected void readVertices(DataReader reader) {
        this.vertices.clear();
        int vertexCount = reader.readInt();
        for (int i = 0; i < vertexCount; i++) {
            TVertex nextVertex = createNextVertex(reader); // We assume createNextVertex() will set the owner.
            nextVertex.load(reader);
            if (!addVertex(nextVertex))
                throw new RuntimeException("Failed to register vertex during readVertices()!");
        }
    }

    /**
     * Write the node's vertices to the vertex block.
     * Assumes the writer is at the correct spot within the vertex block.
     * @param writer the writer to write the vertex data to
     */
    protected void writeVertices(DataWriter writer) {
        writer.writeInt(this.vertices.size());
        for (int i = 0; i < this.vertices.size(); i++)
            this.vertices.get(i).save(writer);
    }

    /**
     * Read the polygons from the polygon block.
     * Assumes the reader is at the correct spot within the polygon block.
     * @param reader the reader to read the polygon data from
     */
    protected void readPolygons(DataReader reader) {
        this.polygons.clear();
        int polygonCount = reader.readInt();
        for (int i = 0; i < polygonCount; i++) {
            TPolygon nextPolygon = createNextPolygon(reader);
            nextPolygon.load(reader);
            if (!addPolygon(nextPolygon))
                throw new RuntimeException("Failed to register polygon during readPolygons()!");
        }
    }

    /**
     * Writes the polygons to the polygon block.
     * Assumes the writer is at the correct spot within the polygon block.
     * @param writer the writer to write the polygon data to
     */
    protected void writePolygons(DataWriter writer) {
        writer.writeInt(this.polygons.size());
        for (int i = 0; i < this.polygons.size(); i++)
            this.polygons.get(i).save(writer);
    }

    /**
     * Read the node data from the node block.
     * Assumes the reader is at the correct spot within the node block.
     * @param reader the reader to read the node data from
     */
    protected abstract void readNodeData(DataReader reader);

    /**
     * Writes the node data to the node block.
     * Assumes the writer is at the correct spot within the node block.
     * @param writer the writer to write the node data to
     */
    protected abstract void writeNodeData(DataWriter writer);

    /**
     * Creates a new landscape vertex based on heuristics or data read from the reader.
     * @return nextVertex
     */
    protected abstract TVertex createNextVertex(DataReader reader);

    /**
     * Creates a new landscape polygon based on heuristics or data read from the reader.
     * @return nextPolygon
     */
    protected abstract TPolygon createNextPolygon(DataReader reader);
}
