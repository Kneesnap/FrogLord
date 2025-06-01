package net.highwayfrogs.editor.games.renderware.mesh.clump.material;

import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.chunks.RwClumpChunk;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.utils.Scene3DUtils;

/**
 * Implements a world mesh view controller for RenderWare clump.
 * Created by Kneesnap on 8/27/2024.
 */
@Getter
public class RwClumpCombinedMeshController extends MeshViewController<RwClumpCombinedMesh> {
    private RwClumpMeshCollection meshViewCollection;

    private static final PhongMaterial VERTEX_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial CONNECTION_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.LIMEGREEN);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        // Create map mesh before super, so the map is registered before the skybox / transparent water / entities, thus allowing transparency to work right.
        this.meshViewCollection = new RwClumpMeshCollection(this);
        this.meshViewCollection.setMesh(getMesh().getActualMesh());

        super.setupBindings(subScene3D, meshView);
        getComboBoxMeshCullFace().setValue(CullFace.BACK);
    }

    @Override
    protected void setupManagers() {
        // TODO: Setup managers.
    }

    @Override
    public String getMeshDisplayName() {
        return getClump().getCollectionViewDisplayName();
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
     * Gets the clump which the mesh represents.
     */
    public RwClumpChunk getClump() {
        return getMesh().getClump();
    }

    public static class RwClumpMeshCollection extends MeshViewCollection<RwClumpMaterialMesh> {
        public RwClumpMeshCollection(MeshViewController<?> viewController) {
            super(viewController, viewController.getRenderManager().createDisplayListWithNewGroup());
        }

        @Override
        protected void onMeshViewSetup(int meshIndex, RwClumpMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            MeshViewController.bindMeshSceneControls(getController(), meshView);
            getController().getMainLight().getScope().add(meshView);
        }

        @Override
        protected void onMeshViewCleanup(int meshIndex, RwClumpMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewCleanup(meshIndex, mesh, meshView);
            MeshViewController.unbindMeshSceneControls(getController(), meshView);
            getController().getMainLight().getScope().remove(meshView);
        }
    }
}