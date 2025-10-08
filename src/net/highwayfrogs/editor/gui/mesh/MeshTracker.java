package net.highwayfrogs.editor.gui.mesh;

import net.highwayfrogs.editor.utils.objects.CountMap;

import java.util.ArrayList;

/**
 * Tracks meshes which should be disposed together.
 * Created by Kneesnap on 5/11/2025.
 */
public class MeshTracker {
    private final CountMap<DynamicMesh> trackedMeshes = new CountMap<>();

    /**
     * Tracks a DynamicMesh for scene disposal.
     * @param mesh the mesh track
     * @return true if the mesh was just added to the tracking
     */
    public boolean trackMesh(DynamicMesh mesh) {
        return this.trackedMeshes.getAndAdd(mesh) == 0;
    }

    /**
     * Removes a DynamicMesh from tracking.
     * @param mesh the mesh track
     * @return if the mesh was fully removed from the tracking
     */
    public boolean stopTrackingMesh(DynamicMesh mesh) {
        return this.trackedMeshes.subtractAndGet(mesh) == 0;
    }

    /**
     * Disposes all tracked meshes.
     */
    public void disposeMeshes() {
        // Remove all attached MeshViews, and dispose the mesh.
        // Avoid ConcurrentModificationException from removeMesh().
        for (DynamicMesh mesh : new ArrayList<>(this.trackedMeshes.keySet())) {
            while (mesh.getMeshViews().size() > 0)
                mesh.removeView(mesh.getMeshViews().get(0));

            mesh.dispose();
        }

        this.trackedMeshes.clear();
    }
}
