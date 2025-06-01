package net.highwayfrogs.editor.gui.mesh;

import java.util.List;

/**
 * Contains helper functions shared between a DynamicMesh, or a subset of a DynamicMesh such as DynamicMeshNode.
 * Created by Kneesnap on 1/7/2024.
 */
public interface IDynamicMeshHelper {
    /**
     * Gets the data entries tracked for the purpose of the helper functions.
     */
    List<DynamicMeshDataEntry> getDataEntries();

    /**
     * Gets the mesh this object is associated with.
     */
    DynamicMesh getMesh();

    /**
     * Updates the face data for the given local face index, if supported.
     * If unsupported, behavior is either to do nothing and return false or to throw an exception.
     * @param entry the entry containing the face to update
     * @param localFaceIndex the local index of the face to update
     * @return true, iff the local face was updated.
     */
    boolean updateFace(DynamicMeshDataEntry entry, int localFaceIndex);

    /**
     * Update all the face data associated with the entry.
     * @param entry The entry to update faces for.
     */
    default void updateFaces(DynamicMeshDataEntry entry) {
        if (entry == null)
            throw new NullPointerException("entry");
        if (!entry.isActive())
            throw new RuntimeException("Cannot update mesh data on an inactive entry.");

        // Update each texCoord.
        getMesh().getEditableFaces().startBatchingUpdates();
        try {
            for (int i = 0; i < entry.getWrittenFaceCount(); i++)
                this.updateFace(entry, i);
        } finally {
            getMesh().getEditableFaces().endBatchingUpdates();
        }
    }

    /**
     * Update all faces held by entries tracked by this object.
     */
    default void updateFaces() {
        List<DynamicMeshDataEntry> entries = getDataEntries();
        getMesh().getEditableFaces().startBatchingUpdates();
        try {
            for (int i = 0; i < entries.size(); i++) {
                DynamicMeshDataEntry entry = entries.get(i);
                for (int j = 0; j < entry.getWrittenFaceCount(); j++)
                    this.updateFace(entry, j);
            }
        } finally {
            getMesh().getEditableFaces().endBatchingUpdates();
        }
    }

    /**
     * Updates the texCoord data for the given local texCoord index.
     * @param entry the entry containing the texCoord to update
     * @param localTexCoordIndex the local index of the texCoord to update
     * @return true, iff the local texture coordinate was updated.
     */
    boolean updateTexCoord(DynamicMeshDataEntry entry, int localTexCoordIndex);

    /**
     * Update all the texCoord data associated with the entry.
     * @param entry The entry to update texCoords for.
     */
    default void updateTexCoords(DynamicMeshDataEntry entry) {
        if (entry == null)
            throw new NullPointerException("entry");
        if (!entry.isActive())
            throw new RuntimeException("Cannot update mesh data on an inactive entry.");

        // Update each texCoord.
        getMesh().getEditableTexCoords().startBatchingUpdates();
        for (int i = 0; i < entry.getWrittenTexCoordCount(); i++)
            this.updateTexCoord(entry, i);
        getMesh().getEditableTexCoords().endBatchingUpdates();
    }

    /**
     * Update all texCoords held by entries tracked by this object.
     */
    default void updateTexCoords() {
        List<DynamicMeshDataEntry> entries = getDataEntries();
        getMesh().getEditableTexCoords().startBatchingUpdates();
        for (int i = 0; i < entries.size(); i++) {
            DynamicMeshDataEntry entry = entries.get(i);
            for (int j = 0; j < entry.getWrittenTexCoordCount(); j++)
                this.updateTexCoord(entry, j);
        }
        getMesh().getEditableTexCoords().endBatchingUpdates();
    }

    /**
     * Updates the vertex position data for the given local vertex index.
     * @param entry the entry containing the vertex to update
     * @param localVertexIndex the local index of the vertex to update
     * @return true, iff the local vertex was updated.
     */
    boolean updateVertex(DynamicMeshDataEntry entry, int localVertexIndex);

    /**
     * Update all the vertex position data associated with the entry.
     * @param entry The entry to update vertices for.
     */
    default void updateVertices(DynamicMeshDataEntry entry) {
        if (entry == null)
            throw new NullPointerException("entry");
        if (!entry.isActive())
            throw new RuntimeException("Cannot update mesh data on an inactive entry.");

        // Update each vertex.
        getMesh().getEditableVertices().startBatchingUpdates();
        for (int i = 0; i < entry.getWrittenVertexCount(); i++)
            this.updateVertex(entry, i);
        getMesh().getEditableVertices().endBatchingUpdates();
    }

    /**
     * Update all vertices held by entries tracked by this object.
     */
    default void updateVertices() {
        List<DynamicMeshDataEntry> entries = getDataEntries();
        getMesh().getEditableVertices().startBatchingUpdates();
        for (int i = 0; i < entries.size(); i++) {
            DynamicMeshDataEntry entry = entries.get(i);
            for (int j = 0; j < entry.getWrittenVertexCount(); j++)
                this.updateVertex(entry, j);
        }
        getMesh().getEditableVertices().endBatchingUpdates();
    }

    /**
     * Gets the tracked data entry corresponding to the provided face index.
     * This will not search pending operations, it will only search the currently written data.
     * @param faceIndex The index of the face.
     * @return dataEntry, or null.
     */
    default DynamicMeshDataEntry getDataEntryByFaceIndex(int faceIndex) {
        List<DynamicMeshDataEntry> dataEntries = getDataEntries();
        int left = 0;
        int right = dataEntries.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            DynamicMeshDataEntry midEntry = dataEntries.get(mid);
            int midStartIndex = midEntry.getCurrentFaceStartIndex();

            if (faceIndex >= midStartIndex && faceIndex < midStartIndex + midEntry.getWrittenFaceCount()) {
                return midEntry;
            } else if (midStartIndex > faceIndex) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return null;
    }

    /**
     * Gets the tracked data entry corresponding to the provided vertex index.
     * This will not search pending operations, it will only search the currently written data.
     * @param vertexIndex The index of the vertex.
     * @return dataEntry, or null.
     */
    default DynamicMeshDataEntry getDataEntryByVertexIndex(int vertexIndex) {
        List<DynamicMeshDataEntry> dataEntries = getDataEntries();
        int left = 0;
        int right = dataEntries.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            DynamicMeshDataEntry midEntry = dataEntries.get(mid);
            int midStartIndex = midEntry.getCurrentVertexStartIndex();

            if (vertexIndex >= midStartIndex && vertexIndex < midStartIndex + midEntry.getWrittenVertexCount()) {
                return midEntry;
            } else if (midStartIndex > vertexIndex) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return null;
    }

    /**
     * Gets the tracked data entry corresponding to the provided texCoord index.
     * This will not search pending operations, it will only search the currently written data.
     * @param texCoordIndex The index of the texCoord.
     * @return dataEntry, or null.
     */
    default DynamicMeshDataEntry getDataEntryByTexCoordIndex(int texCoordIndex) {
        List<DynamicMeshDataEntry> dataEntries = getDataEntries();
        int left = 0;
        int right = dataEntries.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            DynamicMeshDataEntry midEntry = dataEntries.get(mid);
            int midStartIndex = midEntry.getCurrentTexCoordStartIndex();

            if (texCoordIndex >= midStartIndex && texCoordIndex < midStartIndex + midEntry.getWrittenTexCoordCount()) {
                return midEntry;
            } else if (midStartIndex > texCoordIndex) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return null;
    }

    /**
     * Update all mesh entry faces that use the oldTextureUvIndex to use the newTextureUvIndex.
     * @param oldTextureUvIndex textureUvIndex to replace
     * @param newTextureUvIndex replacement textureUvIndex
     */
    default void updateTextureIndex(int oldTextureUvIndex, int newTextureUvIndex) {
        if (oldTextureUvIndex < 0 || newTextureUvIndex < 0)
            return; // Invalid textures, so skip em.

        List<DynamicMeshDataEntry> dataEntries = getDataEntries();
        getMesh().getEditableFaces().startBatchingUpdates();

        // Update Texture UVs.
        for (int i = 0; i < dataEntries.size(); i++)
            dataEntries.get(i).updateTextureIndex(oldTextureUvIndex, newTextureUvIndex);

        // We're done bulking changes.
        getMesh().getEditableFaces().endBatchingUpdates();
    }
}