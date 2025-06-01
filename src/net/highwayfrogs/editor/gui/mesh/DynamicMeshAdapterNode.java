package net.highwayfrogs.editor.gui.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * This represents an individual component which supports creating mesh data from a single kind of value.
 * Many values of this type can be tracked in this one single object.
 * Created by Kneesnap on 9/25/2023.
 */
public abstract class DynamicMeshAdapterNode<TDataSource> extends DynamicMeshNode {
    private final Map<TDataSource, DynamicMeshTypedDataEntry> entriesByDataSource = new HashMap<>();

    public DynamicMeshAdapterNode(DynamicMesh mesh) {
        super(mesh);
        if (mesh == null)
            throw new NullPointerException("mesh");
    }

    @Override
    public void clear() {
        super.clear();
        this.entriesByDataSource.clear();
    }

    /**
     * Gets the tracked typed data entry corresponding to the provided face index.
     * @param index The index of the face.
     * @return typedDataEntry, or null.
     */
    @SuppressWarnings("unchecked") // This warning seems to be a bug.
    public DynamicMeshTypedDataEntry getTypedDataEntryByFaceIndex(int index) {
        DynamicMeshDataEntry dataEntry = getDataEntryByFaceIndex(index);
        if (!(dataEntry instanceof DynamicMeshAdapterNode.DynamicMeshTypedDataEntry))
            return null;

        return (DynamicMeshTypedDataEntry) dataEntry;
    }

    /**
     * Gets the tracked typed data source corresponding to the provided face index.
     * @param index The index of the face.
     * @return typedDataSource, or null.
     */
    public TDataSource getDataSourceByFaceIndex(int index) {
        DynamicMeshTypedDataEntry typedDataEntry = getTypedDataEntryByFaceIndex(index);
        return typedDataEntry != null ? typedDataEntry.getDataSource() : null;
    }

    /**
     * Gets the tracked data entry corresponding to the provided data source, if there is one.
     * @param source The source to look up the data entry from.
     * @return typedDataEntry, or null.
     */
    public DynamicMeshTypedDataEntry getDataEntry(TDataSource source) {
        return this.entriesByDataSource.get(source);
    }

    /**
     * Adds a data source to the mesh.
     * @param source The source to add.
     */
    public final boolean add(TDataSource source) {
        DynamicMeshTypedDataEntry newEntry;

        try {
            newEntry = this.writeValuesToArrayAndCreateEntry(source);
        } catch (Throwable th) {
            throw new IllegalArgumentException("Failed to write mesh values for " + source, th);
        }

        if (newEntry == null)
            throw new IllegalStateException(Utils.getSimpleName(this) + " returned null from writeValuesToArrayAndCreateEntry(TDataSource).");

        DynamicMeshTypedDataEntry oldEntry = this.entriesByDataSource.put(source, newEntry);
        boolean addSuccess = addUnlinkedEntry(newEntry);

        // Add failure, restore the source to how it was before.
        if (!addSuccess) {
            if (oldEntry != null) {
                this.entriesByDataSource.put(source, oldEntry);
            } else {
                this.entriesByDataSource.remove(source, newEntry);
            }
        }

        return addSuccess;
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
        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.remove(source);
        if (entry == null)
            return false; // No entry removed.

        boolean removeSuccess = removeUnlinkedEntry(entry);
        if (!removeSuccess)
            this.entriesByDataSource.put(source, entry);

        return removeSuccess;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onEntryRemoved(DynamicMeshDataEntry entry) {
        super.onEntryRemoved(entry);
        if (entry instanceof DynamicMeshAdapterNode.DynamicMeshTypedDataEntry) {
            DynamicMeshTypedDataEntry typedDataEntry = (DynamicMeshTypedDataEntry) entry;
            this.entriesByDataSource.remove(typedDataEntry.getDataSource(), typedDataEntry);
        }
    }

    /**
     * Update all the texture coordinate values stored for a particular data source.
     * @param source The source to update texture coordinates for.
     */
    public void updateTexCoords(TDataSource source) {
        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot update mesh data on an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.get(source);
        if (entry == null)
            throw new RuntimeException("Cannot update texture coordinates for source " + Utils.getSimpleName(this) + ", because it isn't tracked as part of the mesh!");

        // Update each tex coord.
        this.mesh.getEditableTexCoords().startBatchingUpdates();
        for (int i = 0; i < entry.getWrittenTexCoordCount(); i++)
            this.updateTexCoord(entry, i);
        this.mesh.getEditableTexCoords().endBatchingUpdates();
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

        if (localTexCoordIndex < 0 || localTexCoordIndex >= entry.getWrittenTexCoordCount()) {
            if (entry.getWrittenTexCoordCount() == 0)
                throw new RuntimeException("Cannot update local texture coordinate index " + localTexCoordIndex + ", since there are no texture coordinates tracked for the " + Utils.getSimpleName(source));

            throw new RuntimeException("Cannot update local texture coordinate index " + localTexCoordIndex + ", since it was not in the texture coordinate range [0, " + entry.getWrittenTexCoordCount() + ") tracked for the " + Utils.getSimpleName(source));
        }

        this.updateTexCoord(entry, localTexCoordIndex);
    }

    /**
     * Update all the vertex position values stored for a particular data source.
     * @param source The source to update vertices for.
     */
    public void updateVertices(TDataSource source) {
        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot update mesh data on an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.get(source);
        if (entry == null)
            throw new RuntimeException("Cannot update vertices for source " + Utils.getSimpleName(this) + ", because it isn't tracked as part of the mesh!");

        // Update each vertex.
        for (int i = 0; i < entry.getWrittenVertexCount(); i++)
            this.updateVertex(entry, i);
    }

    /**
     * Update the vertex position for the provided data source.
     * @param source The data source to update a vertex for.
     * @param localVertexIndex The local vertex index to the data source / entry.
     */
    public void updateVertex(TDataSource source, int localVertexIndex) {
        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot update mesh data on an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.get(source);
        if (entry == null)
            throw new RuntimeException("Cannot update vertices for source " + Utils.getSimpleName(this) + ", because it isn't tracked as part of the mesh!");

        if (localVertexIndex < 0 || localVertexIndex >= entry.getWrittenVertexCount()) {
            if (entry.getWrittenVertexCount() == 0)
                throw new RuntimeException("Cannot update local vertex index " + localVertexIndex + ", since there are no vertices tracked for the " + Utils.getSimpleName(source));

            throw new RuntimeException("Cannot update local vertex index " + localVertexIndex + ", since it was not in the local vertex ID range [0, " + entry.getWrittenVertexCount() + ") tracked for the " + Utils.getSimpleName(source));
        }

        this.updateVertex(entry, localVertexIndex);
    }

    /**
     * Update the face data for the provided data entry.
     * @param entry The data entry to write updated face data for.
     * @param localFaceIndex The face ID to update.
     */
    public void updateFace(DynamicMeshTypedDataEntry entry, int localFaceIndex) {
        // Do nothing by default.
    }

    /**
     * Update all the texture coordinate values stored for a particular data source.
     * @param source The source to update texture coordinates for.
     */
    public void updateFaces(TDataSource source) {
        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot update mesh data on an inactive node.");

        DynamicMeshTypedDataEntry entry = this.entriesByDataSource.get(source);
        if (entry == null)
            throw new RuntimeException("Cannot update faces for source " + Utils.getSimpleName(this) + ", because it isn't tracked as part of the mesh!");

        // Update each face.
        this.mesh.getEditableFaces().startBatchingUpdates();
        for (int i = 0; i < entry.getWrittenFaceCount(); i++)
            this.updateFace(entry, i);
        this.mesh.getEditableFaces().endBatchingUpdates();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean updateFace(DynamicMeshDataEntry entry, int localFaceIndex) {
        if (entry instanceof DynamicMeshAdapterNode.DynamicMeshTypedDataEntry) {
            updateFace((DynamicMeshTypedDataEntry) entry, localFaceIndex);
            return true;
        }

        return super.updateFace(entry, localFaceIndex);
    }

    /**
     * Update the tex coord value for the provided data entry.
     * @param entry The data entry to write the updated texture coordinates for.
     * @param localTexCoordIndex The tex coord to update.
     */
    public abstract void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex);

    @Override
    @SuppressWarnings("unchecked")
    public boolean updateTexCoord(DynamicMeshDataEntry entry, int localTexCoordIndex) {
        if (entry instanceof DynamicMeshAdapterNode.DynamicMeshTypedDataEntry) {
            updateTexCoord((DynamicMeshTypedDataEntry) entry, localTexCoordIndex);
            return true;
        }

        return false;
    }

    /**
     * Update the vertex position for the provided data entry.
     * @param entry The data entry to write updated vertex positions for.
     * @param localVertexIndex The vertex ID to update.
     */
    public abstract void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex);

    @Override
    @SuppressWarnings("unchecked")
    public boolean updateVertex(DynamicMeshDataEntry entry, int localVertexIndex) {
        if (entry instanceof DynamicMeshAdapterNode.DynamicMeshTypedDataEntry) {
            updateVertex((DynamicMeshTypedDataEntry) entry, localVertexIndex);
            return true;
        }

        return false;
    }

    @Getter
    public class DynamicMeshTypedDataEntry extends DynamicMeshDataEntry {
        private final TDataSource dataSource;

        public DynamicMeshTypedDataEntry(DynamicMesh mesh, TDataSource dataSource) {
            super(mesh);
            this.dataSource = dataSource;
        }
    }
}