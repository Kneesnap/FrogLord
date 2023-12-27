package net.highwayfrogs.editor.gui.mesh;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents a basic component in a mesh.
 * Created by Kneesnap on 9/25/2023.
 */
@Getter
public class DynamicMeshNode {
    private final DynamicMesh mesh;
    private final List<DynamicMeshDataEntry> dataEntries = new ArrayList<>();

    protected DynamicMeshNode(DynamicMesh mesh) {
        this.mesh = mesh;
    }

    /**
     * Called when this node is added to the mesh.
     */
    protected void onAddedToMesh() {
        getMesh().updateMeshArrays();
    }

    /**
     * Called when this node is removed from the mesh.
     */
    protected void onRemovedFromMesh() {
        clear();
    }

    /**
     * Clears all data entries from this node, and subsequently, the mesh.
     */
    public void clear() {
        // Enable bulk removals.
        this.mesh.pushBulkRemovals();

        // Remove all data entries.
        for (int i = 0; i < this.dataEntries.size(); i++) {
            DynamicMeshDataEntry dataEntry = this.dataEntries.get(i);
            this.onEntryRemoved(dataEntry);
            dataEntry.onRemovedFromNode();
        }

        // Clear all tracking.
        this.dataEntries.clear();

        // Update mesh.
        this.mesh.popBulkRemovals();
    }

    /**
     * Gets the tracked data entry corresponding to the provided face index.
     * @param faceIndex The index of the face.
     * @return dataEntry, or null.
     */
    public DynamicMeshDataEntry getDataEntryByFaceIndex(int faceIndex) {
        int left = 0;
        int right = this.dataEntries.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            DynamicMeshDataEntry midEntry = this.dataEntries.get(mid);

            if (faceIndex >= midEntry.getFaceStartIndex() && faceIndex < midEntry.getFaceStartIndex() + midEntry.getFaceCount()) {
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
     * Adds an unlinked data entry to the mesh.
     * Unlinked data entries are not tied to a TDataSource, and are expected to be managed by the extension class.
     * @param entry The entry to add.
     */
    protected final boolean addUnlinkedEntry(DynamicMeshDataEntry entry) {
        if (entry == null)
            return false; // Can't add null.

        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot add mesh data to an inactive node.");

        if (this.dataEntries.contains(entry))
            return false; // Already registered.

        // Register the entry.
        this.dataEntries.add(entry);
        this.onEntryAdded(entry);
        entry.onAddedToNode();
        return true;
    }

    /**
     * Removes the mesh data from the mesh.
     * Unlinked entries are not tied to a specific TDataSource, they are expected to be managed by the extension class.
     * @param entry The mesh data entry to remove.
     * @return If the data was removed successfully.
     */
    protected final boolean removeUnlinkedEntry(DynamicMeshDataEntry entry) {
        if (entry == null)
            return false; // No data to remove.

        if (!this.mesh.isActive(this))
            throw new RuntimeException("Cannot add remove data from an inactive node.");

        // Remove entry from other tracking.
        if (!this.dataEntries.remove(entry))
            return false;

        // Remove hook.
        this.onEntryRemoved(entry);
        entry.onRemovedFromNode();
        return true;
    }


    /**
     * Called when a data entry is added to this node.
     */
    protected void onEntryAdded(DynamicMeshDataEntry entry) {
        // Do nothing.
    }

    /**
     * Called when a data entry is removed from this node.
     */
    protected void onEntryRemoved(DynamicMeshDataEntry entry) {
        // Do nothing.
    }
}