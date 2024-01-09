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
     * Gets the tracked data entry corresponding to the provided face index.
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

            if (faceIndex >= midEntry.getFaceStartIndex() && faceIndex < midEntry.getFaceStartIndex() + midEntry.getWrittenFaceCount()) {
                return midEntry;
            } else if (midEntry.getFaceStartIndex() > faceIndex) {
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