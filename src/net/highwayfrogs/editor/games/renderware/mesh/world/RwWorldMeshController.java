package net.highwayfrogs.editor.games.renderware.mesh.world;

import javafx.scene.SubScene;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.chunks.RwWorldChunk;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.utils.Scene3DUtils;

/**
 * Implements a world mesh view controller for RenderWare world meshes.
 * Created by Kneesnap on 8/18/2024.
 */
@Getter
public class RwWorldMeshController extends MeshViewController<RwWorldCombinedMesh> {
    private RwWorldMeshCollection meshViewCollection;
    private static final double DEFAULT_FAR_CLIP = 3500; // Far enough away to see the end of Frogger Beyond Mountain.
    private static final double DEFAULT_MOVEMENT_SPEED = 50;

    private static final PhongMaterial VERTEX_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial CONNECTION_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.LIMEGREEN);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        // Create map mesh before super, so the map is registered before the skybox / transparent water / entities, thus allowing transparency to work right.
        this.meshViewCollection = new RwWorldMeshCollection(this);
        this.meshViewCollection.setMesh(getMesh().getActualMesh());

        super.setupBindings(subScene3D, meshView);
        getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getFirstPersonCamera().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);
        getComboBoxMeshCullFace().setValue(CullFace.BACK);
        //getMainLight().getScope().add(meshView);

        // TODO: TOSS
        /*DisplayList displayList = getRenderManager().createDisplayList();
        for (RwAtomicSectorChunk worldSector : getWorld().getWorldSectors()) {
            for (RwV3d vertexPos : worldSector.getVertices())
                displayList.addSphere(vertexPos.getX(), vertexPos.getY(), vertexPos.getZ(), 1, VERTEX_MATERIAL, false);
        }*/

        // Add mesh click listener.
        getMeshScene().setOnMouseClicked(evt -> {
            PickResult result = evt.getPickResult();
            if (result == null || !(result.getIntersectedNode() instanceof MeshView))
                return; // No pick result, or the thing that was clicked was not the main mesh.

            Mesh mesh = ((MeshView) result.getIntersectedNode()).getMesh();
            if (!(mesh instanceof RwWorldMesh))
                return;

            RwWorldMesh materialMesh = (RwWorldMesh) mesh;
            /*kcMaterial material = materialMesh.getMapMaterial();
            // TODO: getLogger().info("Clicked on " + (material != null ? "'" + material.getMaterialName() + "'/'" + material.getTextureFileName() + "'" : "NULL"));
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
            getLogger().info(" - UV2: [" + texCoord3U + ", " + texCoord3V + "]");*/
        });

    }

    @Override
    protected void setupManagers() {
        // TODO: Setup managers.
    }

    @Override
    public String getMeshDisplayName() {
        return getWorld().getStreamFile().getLocationName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        setupDefaultInverseCamera();
    }

    @Override
    protected double getAxisDisplayLength() {
        return 3;
    }

    @Override
    protected double getAxisDisplaySize() {
        return 1;
    }

    @Override
    protected boolean mapRendersFirst() {
        // Gives preference to transparent entities, since the map is rarely (if ever?) transparent.
        return true;
    }

    /**
     * Gets the world which the mesh represents.
     */
    public RwWorldChunk getWorld() {
        return getMesh().getWorld();
    }

    public static class RwWorldMeshCollection extends MeshViewCollection<RwWorldMaterialMesh> {
        private final MeshViewController<?> viewController;

        public RwWorldMeshCollection(MeshViewController<?> viewController) {
            super(viewController.getRenderManager().createDisplayListWithNewGroup());
            this.viewController = viewController;
        }

        @Override
        protected void onMeshViewSetup(int meshIndex, RwWorldMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            MeshViewController.bindMeshSceneControls(this.viewController, meshView);
            this.viewController.getMainLight().getScope().add(meshView);
        }

        @Override
        protected void onMeshViewCleanup(int meshIndex, RwWorldMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewCleanup(meshIndex, mesh, meshView);
            MeshViewController.unbindMeshSceneControls(this.viewController, meshView);
            this.viewController.getMainLight().getScope().remove(meshView);
        }
    }
}