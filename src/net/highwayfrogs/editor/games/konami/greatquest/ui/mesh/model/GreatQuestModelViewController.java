package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Controls a model mesh for Great Quest.
 * TODO: Allow switching between FPS camera and rotation camera?
 * Created by Kneesnap on 4/15/2024.
 */
@Getter
public class GreatQuestModelViewController extends MeshViewController<GreatQuestModelMesh> {
    private GreatQuestModelMeshViewCollection meshViewCollection;

    private static final double DEFAULT_FAR_CLIP = 50;
    private static final double DEFAULT_NEAR_CLIP = 0.1;
    private static final double DEFAULT_MOVEMENT_SPEED = 3;

    private static final PhongMaterial VERTEX_MATERIAL = Utils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial CONNECTION_MATERIAL = Utils.makeUnlitSharpMaterial(Color.LIMEGREEN);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getFirstPersonCamera().getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getFirstPersonCamera().getCamera().setNearClip(DEFAULT_NEAR_CLIP);
        getFirstPersonCamera().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);

        getLightingGroup().getChildren().add(getMeshView());
        getComboBoxMeshCullFace().setValue(CullFace.NONE); // Great Quest has no back-face culling.
        getColorPickerLevelBackground().setValue(Color.GRAY); // Gray is better for viewing models.

        // Create mesh views necessary to display.
        if (getModel() != null && getMesh().getActualMesh() != null) {
            this.meshViewCollection = new GreatQuestModelMeshViewCollection(this);
            this.meshViewCollection.setMesh(getMesh().getActualMesh());
        }
    }

    @Override
    protected void setupManagers() {
        // TODO: Setup managers.
    }

    @Override
    public String getMeshDisplayName() {
        return getMesh().getMeshName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        setupDefaultInverseCamera(0, 0, 0, 2.5);
    }

    @Override
    protected double getAxisDisplayLength() {
        return .3333;
    }

    @Override
    protected double getAxisDisplaySize() {
        return .015;
    }

    /**
     * Gets the model which the mesh represents.
     */
    public kcModel getModel() {
        return getMesh().getModel();
    }

    /**
     * Tracks the viewers for the model.
     */
    public static class GreatQuestModelMeshViewCollection extends MeshViewCollection<GreatQuestModelMaterialMesh> {
        private final MeshViewController<?> viewController;

        public GreatQuestModelMeshViewCollection(MeshViewController<?> viewController) {
            super(viewController.getRenderManager().createDisplayList());
            this.viewController = viewController;
        }

        @Override
        protected void onMeshViewSetup(int meshIndex, GreatQuestModelMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            MeshViewController.bindMeshSceneControls(this.viewController, meshView);
            this.viewController.getMainLight().getScope().add(meshView);
        }

        @Override
        protected void onMeshViewCleanup(int meshIndex, GreatQuestModelMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewCleanup(meshIndex, mesh, meshView);
            MeshViewController.unbindMeshSceneControls(this.viewController, meshView);
            this.viewController.getMainLight().getScope().remove(meshView);
        }
    }
}