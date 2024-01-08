package net.highwayfrogs.editor.gui.mesh;

/**
 * Represents a node which contains no special behavior, and is controlled by external code.
 * Created by Kneesnap on 1/3/2024.
 */
public class DynamicMeshUnmanagedNode extends DynamicMeshNode {
    public DynamicMeshUnmanagedNode(DynamicMesh mesh) {
        super(mesh);
    }

    /**
     * Registers a dynamic mesh data entry.
     * @param entry The entry to register
     * @return true, if the registration was successful
     */
    public boolean addEntry(DynamicMeshDataEntry entry) {
        return super.addUnlinkedEntry(entry);
    }

    /**
     * Removes a dynamic mesh data entry.
     * @param entry The entry to remove
     * @return true, if the entry was successfully removed
     */
    public boolean removeEntry(DynamicMeshDataEntry entry) {
        return super.removeUnlinkedEntry(entry);
    }
}
