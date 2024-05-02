package net.highwayfrogs.editor.gui.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This represents a basic component in a mesh.
 * Created by Kneesnap on 9/25/2023.
 */

public abstract class DynamicMeshNode implements IDynamicMeshHelper {
    @Getter private final DynamicMesh mesh;
    @Getter private final List<DynamicMeshDataEntry> dataEntries = new ArrayList<>();
    private Logger cachedLogger;

    protected DynamicMeshNode(DynamicMesh mesh) {
        this.mesh = mesh;
    }

    /**
     * Gets the logger available to this mesh node.
     */
    public Logger getLogger() {
        if (this.mesh != null) {
            Logger logger = this.mesh.getLogger();
            if (logger != null)
                return logger;
        }

        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = Logger.getLogger(Utils.getSimpleName(this));
    }

    /**
     * Called when this node is added to the mesh.
     */
    protected void onAddedToMesh() {
        // Do nothing.
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
        this.mesh.pushBatchOperations();

        // Remove all data entries.
        for (int i = 0; i < this.dataEntries.size(); i++) {
            DynamicMeshDataEntry dataEntry = this.dataEntries.get(i);
            this.onEntryRemoved(dataEntry);
            dataEntry.onRemovedFromNode();
        }

        // Clear all tracking.
        this.dataEntries.clear();

        // Update mesh.
        this.mesh.popBatchOperations();
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

        if (getMesh().getDataEntries().contains(entry))
            throw new IllegalArgumentException("The provided entry is registered to the mesh via another node.");

        // Register the entry.
        this.dataEntries.add(entry);
        getMesh().getDataEntries().add(entry);
        this.mesh.pushBatchOperations();
        this.onEntryAdded(entry);
        entry.onAddedToNode(this);
        this.mesh.popBatchOperations();
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
        getMesh().getDataEntries().remove(entry);
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

    @Override
    public boolean updateFace(DynamicMeshDataEntry entry, int localFaceIndex) {
        // Default implementation does nothing and returns false.
        return false;
    }
}