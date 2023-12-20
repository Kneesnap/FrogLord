package net.highwayfrogs.editor.gui.mesh;

import lombok.Getter;

/**
 * This represents a basic component in a mesh.
 * Created by Kneesnap on 9/25/2023.
 */
@Getter
public class DynamicMeshNode {
    private final DynamicMesh mesh;

    protected DynamicMeshNode(DynamicMesh mesh) {
        this.mesh = mesh;
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
        // Do nothing.
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