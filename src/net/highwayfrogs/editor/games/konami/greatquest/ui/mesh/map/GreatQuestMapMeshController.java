package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import javafx.scene.SubScene;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Controls the map mesh for Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestMapMeshController extends MeshViewController<GreatQuestMapMesh> {
    private GreatQuestMapMeshCollection meshViewCollection;
    private static final double DEFAULT_FAR_CLIP = 1000; // Far enough away to see the skybox.
    private static final double DEFAULT_MOVEMENT_SPEED = 25;

    private static final PhongMaterial VERTEX_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial CONNECTION_MATERIAL = Utils.makeSpecialMaterial(Color.LIMEGREEN);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getFirstPersonCamera().getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getFirstPersonCamera().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);
        getComboBoxMeshCullFace().setValue(CullFace.NONE); // Great Quest has no back-face culling.

        // Create map mesh.
        this.meshViewCollection = new GreatQuestMapMeshCollection(this);
        this.meshViewCollection.setMesh(getMesh().getActualMesh());

        // Add mesh click listener.
        getMeshScene().setOnMouseClicked(evt -> {
            PickResult result = evt.getPickResult();
            if (result == null || !(result.getIntersectedNode() instanceof MeshView))
                return; // No pick result, or the thing that was clicked was not the main mesh.

            Mesh mesh = ((MeshView) result.getIntersectedNode()).getMesh();
            if (!(mesh instanceof GreatQuestMapMaterialMesh))
                return;

            GreatQuestMapMaterialMesh materialMesh = (GreatQuestMapMaterialMesh) mesh;
            kcMaterial material = materialMesh.getMapMaterial();
            getLogger().info("Clicked on " + (material != null ? "'" + material.getMaterialName() + "'/'" + material.getTextureFileName() + "'" : "NULL"));
            DynamicMeshDataEntry entry = materialMesh.getMainNode().getDataEntryByFaceIndex(evt.getPickResult().getIntersectedFace());
            if (entry == null)
                return;

            int faceStartIndex = entry.getFaceMeshArrayIndex(evt.getPickResult().getIntersectedFace() - entry.getFaceStartIndex());
            float texCoord1U = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 1));
            float texCoord1V = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 1) + 1);
            float texCoord2U = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 3));
            float texCoord2V = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 3) + 1);
            float texCoord3U = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 5));
            float texCoord3V = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 5) + 1);
            getLogger().info(" - UV0: [" + texCoord1U + ", " + texCoord1V + "]");
            getLogger().info(" - UV1: [" + texCoord2U + ", " + texCoord2V + "]");
            getLogger().info(" - UV2: [" + texCoord3U + ", " + texCoord3V + "]");
        });

    }

    @Override
    protected void setupManagers() {
        addManager(new GreatQuestEntityManager(this));
        // TODO: Setup managers.
    }

    @Override
    public String getMeshDisplayName() {
        return getMap().getDebugName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        // TODO: Come up with default camera position.
    }

    @Override
    protected double getAxisDisplayLength() {
        return 3;
    }

    @Override
    protected double getAxisDisplaySize() {
        return 1;
    }

    /**
     * Gets the map file which the mesh represents.
     */
    public GreatQuestChunkedFile getMap() {
        return getMesh().getMap();
    }

    public static class GreatQuestMapMeshCollection extends MeshViewCollection<GreatQuestMapMaterialMesh> {
        private final MeshViewController<?> viewController;

        public GreatQuestMapMeshCollection(MeshViewController<?> viewController) {
            super(viewController.getRenderManager().createDisplayListWithNewGroup());
            this.viewController = viewController;
        }

        @Override
        protected void onMeshViewSetup(int meshIndex, GreatQuestMapMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            MeshViewController.bindMeshSceneControls(this.viewController, meshView);
            this.viewController.getMainLight().getScope().add(meshView);
        }

        @Override
        protected void onMeshViewCleanup(int meshIndex, GreatQuestMapMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewCleanup(meshIndex, mesh, meshView);
            MeshViewController.unbindMeshSceneControls(this.viewController, meshView);
            this.viewController.getMainLight().getScope().remove(meshView);
        }
    }
}