package net.highwayfrogs.editor.gui.mesh;

import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a collection of dynamic meshes
 * Created by Kneesnap on 4/15/2024.
 */
public class DynamicMeshCollection<TMesh extends DynamicMesh> {
    private final String meshName;
    @Getter private final List<TMesh> meshes = new ArrayList<>();
    @Getter private final List<MeshViewCollection<? super TMesh>> viewCollections = new ArrayList<>();

    public DynamicMeshCollection(String meshName) {
        this.meshName = meshName;
    }

    /**
     * Gets the name of this mesh.
     */
    public String getMeshName() {
        return this.meshName != null ? this.meshName : String.format("%s@%X", Utils.getSimpleName(this), hashCode());
    }

    /**
     * Adds a mesh to the collection.
     * @param mesh the mesh to add
     * @return true iff the mesh was added successfully
     */
    public boolean addMesh(TMesh mesh) {
        if (mesh == null)
            throw new NullPointerException("mesh");

        if (this.meshes.contains(mesh))
            return false;

        int newMeshIndex = this.meshes.size();
        this.meshes.add(mesh);

        // Setup mesh views.
        for (int i = 0; i < this.viewCollections.size(); i++)
            this.viewCollections.get(i).setupMeshView(newMeshIndex);

        return true;
    }

    /**
     * Removes a mesh from the collection.
     * @param mesh the mesh to remove
     * @return true iff the mesh was removed successfully
     */
    public boolean removeMesh(TMesh mesh) {
        if (mesh == null)
            return false;

        int meshIndex = this.meshes.lastIndexOf(mesh);
        if (meshIndex < 0)
            return false; // Wasn't registered.

        // Remove mesh views.
        for (int i = 0; i < this.viewCollections.size(); i++)
            this.viewCollections.get(i).removeMeshView(meshIndex);

        // Remove from the list.
        // NOTE: THIS MUST BE DONE AFTER CLEANING UP THE MESH VIEWS, SINCE THEY WILL ACCESS THE LIST TO GET THE REMOVED MESH.
        TMesh removedMesh = this.meshes.remove(meshIndex);
        if (removedMesh != mesh) // This is a warning which will let us know if we start modifying the list during the mesh view removal step. In theory, we can modify this code slightly to support this behavior, but I don't think we need to support this.
            throw new AssertionError("One or more of the MeshViews cleaned up modified the list of meshes. (Added or removed a mesh). This code needs to be modified slightly to support this scenario.");

        return true;
    }

    @Getter
    public static class MeshViewCollection<TMesh extends DynamicMesh> {
        private DynamicMeshCollection<TMesh> meshCollection;
        private final List<MeshView> meshViews = new ArrayList<>();
        private final DisplayList displayList;

        public MeshViewCollection(DisplayList displayList) {
            this.displayList = displayList;
        }

        /**
         * Gets the logger for this collection view.
         */
        public Logger getLogger() {
            return Logger.getLogger(Utils.getSimpleName(this) + "{currentMeshCollection="
                    + (this.meshCollection != null ? "'" + this.meshCollection.getMeshName() + "'" : "null") + "}");
        }

        /**
         * Set the mesh which is currently displayed
         * @param newMeshCollection the mesh to display
         */
        public void setMesh(DynamicMeshCollection<TMesh> newMeshCollection) {
            if (this.meshCollection == newMeshCollection)
                return; // No change.

            // Remove old mesh.
            if (this.meshCollection != null) {
                this.meshCollection.getViewCollections().remove(this);
                for (int i = 0; i < this.meshViews.size(); i++)
                    removeMeshView(i, true);
            }

            // Add new mesh.
            this.meshCollection = newMeshCollection;
            if (newMeshCollection != null) {
                newMeshCollection.getViewCollections().add(this);
                for (int i = 0; i < newMeshCollection.getMeshes().size(); i++)
                    setupMeshView(i);
            }

            // Remove unused MeshViews.
            int newMeshViewCount = newMeshCollection != null ? newMeshCollection.getMeshes().size() : 0;
            while (this.meshViews.size() > newMeshViewCount) {
                MeshView removedView = this.meshViews.remove(this.meshViews.size() - 1);
                if (removedView != null)
                    this.displayList.remove(removedView);
            }
        }

        /**
         * Removes the MeshView at the given index
         * @param meshIndex the index of the mesh to remove
         */
        final void removeMeshView(int meshIndex) {
            removeMeshView(meshIndex, false);
        }

        /**
         * Removes the MeshView at the given index
         * @param meshIndex the index of the mesh to remove
         * @param keepMeshViewTracked if true, the mesh view will stay tracked for re-use
         */
        private void removeMeshView(int meshIndex, boolean keepMeshViewTracked) {
            if (this.meshCollection == null)
                throw new IllegalStateException("Cannot cleanup a mesh view when there is no active mesh to view.");
            if (meshIndex < 0 || meshIndex >= this.meshViews.size())
                throw new ArrayIndexOutOfBoundsException("The mesh collection does not have a mesh at index " + meshIndex + ".");

            MeshView meshView = keepMeshViewTracked ? this.meshViews.get(meshIndex) : this.meshViews.remove(meshIndex);
            if (meshView == null)
                return; // The MeshView was never actually setup.

            TMesh mesh = this.meshCollection.getMeshes().get(meshIndex);
            if (!mesh.removeView(meshView))
                return; // MeshView is already setup.

            // Remove MeshView from the group.
            if (!keepMeshViewTracked)
                this.displayList.remove(meshView);

            // Call cleanup hook.
            try {
                onMeshViewCleanup(meshIndex, mesh, meshView);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Encountered an error in the MeshView cleanup hook, onMeshViewCleanup().");
            }
        }

        /**
         * Sets up a mesh view for the mesh index
         * @param meshIndex the mesh index to create
         */
        final void setupMeshView(int meshIndex) {
            if (this.meshCollection == null)
                throw new IllegalStateException("Cannot setup a mesh view when there is no active mesh to view.");
            if (meshIndex < 0 || meshIndex >= this.meshCollection.getMeshes().size())
                throw new ArrayIndexOutOfBoundsException("The mesh collection does not have a mesh at index " + meshIndex + ".");

            // Ensure size.
            while (this.meshViews.size() > meshIndex + 1)
                this.meshViews.add(null);

            // Get/add a mesh view.
            MeshView meshView;
            if (this.meshViews.size() == meshIndex) {
                this.meshViews.add(meshView = new MeshView());
                this.displayList.add(meshView);
            } else {
                meshView = this.meshViews.get(meshIndex);
                if (meshView == null) {
                    this.meshViews.set(meshIndex, meshView = new MeshView());
                    this.displayList.add(meshView);
                }
            }

            // Setup the mesh view.
            TMesh mesh = this.meshCollection.getMeshes().get(meshIndex);
            if (!mesh.addView(meshView))
                return; // MeshView is already setup.

            try {
                onMeshViewSetup(meshIndex, mesh, meshView);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Encountered an error in the MeshView setup hook, onMeshViewSetup().");
            }
        }

        /**
         * Hook to allow listening for the setup of a single MeshView.
         * @param meshIndex the index of the MeshView/DynamicMesh
         * @param mesh the DynamicMesh which the MeshView displays
         * @param meshView the MeshView which has been set up
         */
        protected void onMeshViewSetup(int meshIndex, TMesh mesh, MeshView meshView) {
            // Do nothing by default.
        }

        /**
         * Hook to allow listening for the cleanup of a single MeshView.
         * @param meshIndex the index of the MeshView/DynamicMesh
         * @param mesh the DynamicMesh which the MeshView displays
         * @param meshView the MeshView which has been removed
         */
        protected void onMeshViewCleanup(int meshIndex, TMesh mesh, MeshView meshView) {
            // Do nothing by default.
        }

        /**
         * Control if the currently registered meshviews are visible.
         * @param visible the desired visibility state
         */
        public void setVisible(boolean visible) {
            for (int i = 0; i < this.meshViews.size(); i++)
                this.meshViews.get(i).setVisible(visible);
        }

        /**
         * Set the position of all attached MeshViews
         * @param x the new x position
         * @param y the new y position
         * @param z the new z position
         */
        public void setPosition(double x, double y, double z) {
            for (int i = 0; i < this.meshViews.size(); i++)
                Scene3DUtils.setNodePosition(this.meshViews.get(i), x, y, z);
        }

        /**
         * Set the scale of all attached MeshViews
         * @param x the new x scale
         * @param y the new y scale
         * @param z the new z scale
         */
        public void setScale(double x, double y, double z) {
            for (int i = 0; i < this.meshViews.size(); i++)
                Scene3DUtils.setNodeScale(this.meshViews.get(i), x, y, z);
        }

        /**
         * Set the rotation of all attached MeshViews
         * @param x the new x rotation
         * @param y the new y rotation
         * @param z the new z rotation
         */
        public void setRotation(double x, double y, double z) {
            for (int i = 0; i < this.meshViews.size(); i++)
                Scene3DUtils.setNodeRotation(this.meshViews.get(i), x, y, z);
        }
    }
}