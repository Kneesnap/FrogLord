package net.highwayfrogs.editor.gui.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This represents an individual component which supports creating mesh data from a single kind of value.
 * Many different values of this type can be tracked in this one single object.
 * Created by Kneesnap on 9/25/2023.
 */
public abstract class DynamicMeshAdapterNode<TDataSource> extends DynamicMeshNode {
    @Getter private final DynamicMesh mesh;
    private final Map<TDataSource, DynamicMeshTypedDataEntry> entriesByDataSource = new HashMap<>();
    private final List<DynamicMeshTypedDataEntry> dataEntries = new ArrayList<>();

    public DynamicMeshAdapterNode(DynamicMesh mesh) {
        super(mesh);
        if (mesh == null)
            throw new NullPointerException("mesh");

        this.mesh = mesh;
    }

    /**
     * Gets the tracked data entry corresponding to the provided data source index.
     * @param index The index of the data entry.
     * @return typedDataEntry, or null.
     */
    public DynamicMeshTypedDataEntry getDataEntry(int index) {
        return index >= 0 && index < this.dataEntries.size() ? this.dataEntries.get(index) : null;
    }

    /**
     * Gets the tracked data entry corresponding to the provided data source, if there is one.
     * @param source The source to lookup the data entry from.
     * @return typedDataEntry, or null.
     */
    public DynamicMeshTypedDataEntry getDataEntry(TDataSource source) {
        return this.entriesByDataSource.get(source);
    }

    /**
     * Adds a data source to the mesh.
     * @param source The source to add.
     */
    public final void add(TDataSource source) {
        if (source == null)
            return; // Can't add null.

        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot add mesh data to an inactive node.");

        if (this.entriesByDataSource.containsKey(source))
            throw new RuntimeException("This data source (" + Utils.getSimpleName(source) + ") is already part of the " + Utils.getSimpleName(this) + ".");

        DynamicMeshTypedDataEntry newEntry = this.writeValuesToArrayAndCreateEntry(source);
        if (newEntry == null)
            throw new IllegalStateException(Utils.getSimpleName(this) + " returned null from writeValuesToArrayAndCreateEntry(TDataSource).");

        this.entriesByDataSource.put(source, newEntry);
        this.dataEntries.add(newEntry);
        this.onEntryAdded(newEntry);
    }

    /**
     * Writes the mesh values to the end of all the mesh arrays and creates a new entry object representing this data.
     * @param data The data source to write from.
     * @return dataEntry
     */
    protected abstract DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(TDataSource data);

    /**
     * Removes the data source and any associated mesh data from the mesh.
     * @param source The source to remove.
     * @return If the data was removed successfully.
     */
    public final boolean remove(TDataSource source) {
        if (source == null)
            return false; // No data to remove.

        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot add remove data from an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.remove(source);
        if (entry == null)
            return false; // No entry removed.

        // Remove entry from other tracking.
        this.dataEntries.remove(entry);

        // Remove hook.
        this.onEntryRemoved(entry);

        // Remove Vertices
        // TODO

        // Remove Tex Coords
        // TODO

        // Remove Faces
        // TODO...?

        return true;
    }

    /**
     * Update all of the vertex position values stored for a particular data source.
     * @param source The source to update vertices for.
     */
    public void updateVertices(TDataSource source) {
        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot update mesh data on an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.get(source);
        if (entry == null)
            throw new RuntimeException("Cannot update vertices for source " + Utils.getSimpleName(this) + ", because it isn't tracked as part of the mesh!");

        for (int i = 0; i < entry.getVertexCount(); i++)
            this.updateVertex(entry, i);
    }

    /**
     * Update the vertex position for the provided data source.
     * @param source           The data source to update a vertex for.
     * @param localVertexIndex The local vertex index to the data source / entry.
     */
    public void updateVertex(TDataSource source, int localVertexIndex) {
        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot update mesh data on an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.get(source);
        if (entry == null)
            throw new RuntimeException("Cannot update vertices for source " + Utils.getSimpleName(this) + ", because it isn't tracked as part of the mesh!");

        if (localVertexIndex < 0 || localVertexIndex >= entry.getVertexCount()) {
            if (entry.getVertexCount() == 0)
                throw new RuntimeException("Cannot update local vertex index " + localVertexIndex + ", since there are no vertices tracked for the " + Utils.getSimpleName(source));

            throw new RuntimeException("Cannot update local vertex index " + localVertexIndex + ", since it was not in the local vertex ID range [0, " + entry.getVertexCount() + ") tracked for the " + Utils.getSimpleName(source));
        }

        this.updateVertex(entry, localVertexIndex);
    }

    /**
     * Update the vertex position for the provided data entry.
     * @param entry            The data entry to write updated vertex positions for.
     * @param localVertexIndex The vertex ID to update.
     */
    public abstract void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex);

    /**
     * Update all of the texture coordinate values stored for a particular data source.
     * @param source The source to update texture coordinates for.
     */
    public void updateTexCoords(TDataSource source) {
        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot update mesh data on an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.get(source);
        if (entry == null)
            throw new RuntimeException("Cannot update texture coordinates for source " + Utils.getSimpleName(this) + ", because it isn't tracked as part of the mesh!");

        for (int i = 0; i < entry.getTexCoordCount(); i++)
            this.updateTexCoord(entry, i);
    }

    /**
     * Update the texture coordinates for the provided data source.
     * @param source             The data source to update texture coordinates for.
     * @param localTexCoordIndex The local tex coord index to the data source / entry.
     */
    public void updateTexCoord(TDataSource source, int localTexCoordIndex) {
        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot update mesh data on an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.get(source);
        if (entry == null)
            throw new RuntimeException("Cannot update texture coordinates for source " + Utils.getSimpleName(this) + ", because it isn't tracked as part of the mesh!");

        if (localTexCoordIndex < 0 || localTexCoordIndex >= entry.getVertexCount()) {
            if (entry.getTexCoordCount() == 0)
                throw new RuntimeException("Cannot update local texture coordinate index " + localTexCoordIndex + ", since there are no texture coordinates tracked for the " + Utils.getSimpleName(source));

            throw new RuntimeException("Cannot update local texture coordinate index " + localTexCoordIndex + ", since it was not in the texture coordinate range [0, " + entry.getTexCoordCount() + ") tracked for the " + Utils.getSimpleName(source));
        }

        this.updateVertex(entry, localTexCoordIndex);
    }

    /**
     * Update the tex coord value for the provided data entry.
     * @param entry              The data entry to write the updated texture coordinates for.
     * @param localTexCoordIndex The tex coord to update.
     */
    public abstract void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex);

    @Getter
    public class DynamicMeshTypedDataEntry extends DynamicMeshDataEntry {
        private final TDataSource dataSource;

        public DynamicMeshTypedDataEntry(DynamicMesh mesh, TDataSource dataSource) {
            super(mesh);
            if (dataSource == null)
                throw new NullPointerException("dataSource");

            this.dataSource = dataSource;
        }

        public DynamicMeshTypedDataEntry(DynamicMesh mesh, TDataSource dataSource, int vertexCount, int texCoordCount, int faceCount) {
            super(mesh, vertexCount, texCoordCount, faceCount);
            if (dataSource == null)
                throw new NullPointerException("dataSource");

            this.dataSource = dataSource;
        }
    }
}