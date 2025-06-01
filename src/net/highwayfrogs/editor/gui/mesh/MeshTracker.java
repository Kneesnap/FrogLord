package net.highwayfrogs.editor.gui.mesh;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks meshes which should be disposed together.
 * Created by Kneesnap on 5/11/2025.
 */
public class MeshTracker {
    private final Set<DynamicMesh> trackedMeshes = new HashSet<>();

    /**
     * Tracks a DynamicMesh for scene disposal.
     * @param mesh the mesh track
     * @return if the mesh was added to the tracking
     */
    public boolean trackMesh(DynamicMesh mesh) {
        return this.trackedMeshes.add(mesh);
    }

    /**
     * Removes a DynamicMesh from tracking.
     * @param mesh the mesh track
     * @return if the mesh was removed from the tracking
     */
    public boolean stopTrackMesh(DynamicMesh mesh) {
        return this.trackedMeshes.remove(mesh);
    }

    /**
     * Disposes all tracked meshes.
     */
    public void disposeMeshes() {
        // Remove all attached MeshViews, and dispose the mesh.
        for (DynamicMesh mesh : this.trackedMeshes) {
            while (mesh.getMeshViews().size() > 0)
                mesh.removeView(mesh.getMeshViews().get(0));

            mesh.dispose();
        }

        this.trackedMeshes.clear();
    }
}
