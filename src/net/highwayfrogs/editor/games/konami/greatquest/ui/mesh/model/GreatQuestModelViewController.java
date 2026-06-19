package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.scene.DepthTest;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.system.math.Vector3f;

/**
 * Controls a model mesh for Great Quest.
 * TODO: Allow switching between FPS camera and rotation camera?
 * Created by Kneesnap on 4/15/2024.
 */
@Getter
public class GreatQuestModelViewController extends MeshViewController<GreatQuestModelMesh> {
    private GreatQuestActionSequencePlayback sequencePlayback;
    private GreatQuestModelMeshViewCollection meshViewCollection;
    private final MeshView skeletonMeshView = new MeshView();
    @Setter
    private boolean animationTickingPaused = false;
    @Setter
    private float animationSpeedMultiplier = 1.0f;

    public static final double DEFAULT_FAR_CLIP = 1000;
    public static final double DEFAULT_NEAR_CLIP = 0.1;
    public static final Vector3f DEFAULT_CAMERA_OFFSET = new Vector3f(0, 1, 3);
    public static final float DEFAULT_CAMERA_PITCH = 180;;
    public static final float DEFAULT_CAMERA_YAW = 0;
    public static final double DEFAULT_ZOOM_FACTOR = 0.05;

    private static final double DEFAULT_MOVEMENT_SPEED = 0.75;

    public GreatQuestModelViewController(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);

        this.sequencePlayback = new GreatQuestActionSequencePlayback(getMesh());

        // Setup camera stuff.
        getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getCamera().setNearClip(DEFAULT_NEAR_CLIP);
        getFirstPersonCamera().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);

        getLightingGroup().getChildren().add(getMeshView());
        getComboBoxMeshCullFace().setValue(CullFace.NONE); // Great Quest has no back-face culling.
        getColorPickerLevelBackground().setValue(Color.GRAY); // Gray is better for viewing models.

        // Create mesh views necessary to display. (Must happen before registering skeleton)
        if (getModel() != null && getMesh().getActualMesh() != null) {
            this.meshViewCollection = new GreatQuestModelMeshViewCollection(this);
            this.meshViewCollection.setMesh(getMesh().getActualMesh());

            boolean applySkeletonRotation = getMesh().isSkeletonAxisRotationApplied();
            GreatQuestUtils.setEntityRotation(this.skeletonMeshView, 0, 0, 0, applySkeletonRotation);
            for (int i = 0; i < this.meshViewCollection.getMeshViews().size(); i++)
                GreatQuestUtils.setEntityRotation(this.meshViewCollection.getMeshViews().get(i), 0, 0, 0, applySkeletonRotation);
        }

        // Setup skeleton.
        this.skeletonMeshView.setVisible(false);
        this.skeletonMeshView.setDepthTest(DepthTest.DISABLE);
        getRenderManager().getRoot().getChildren().add(this.skeletonMeshView);
        getMainLight().getScope().add(this.skeletonMeshView);
        if (getMesh().getSkeletonMesh() != null)
            getMesh().getSkeletonMesh().addView(this.skeletonMeshView, getMeshTracker());

        // Tick animations.
        getFrameTimer().addPerFrameTask(this::onTick);
    }

    private void onTick(float deltaTime) {
        if (!this.animationTickingPaused)
            getMesh().tickAnimation(deltaTime * this.animationSpeedMultiplier);
        this.sequencePlayback.tick();
    }

    @Override
    protected void setupManagers() {
        addManager(new GreatQuestAnimationEditor(this));
    }

    @Override
    public String getMeshDisplayName() {
        return getMesh().getMeshName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        getFirstPersonCamera().setInvertY(true);
        getFirstPersonCamera().setPos(DEFAULT_CAMERA_OFFSET.getX(), DEFAULT_CAMERA_OFFSET.getY(), DEFAULT_CAMERA_OFFSET.getZ());
        getFirstPersonCamera().setPitchAndYaw(DEFAULT_CAMERA_PITCH, DEFAULT_CAMERA_YAW);
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

        public GreatQuestModelMeshViewCollection(MeshViewController<?> viewController) {
            super(viewController, viewController.getRenderManager().createDisplayList());
        }

        @Override
        protected void onMeshViewSetup(int meshIndex, GreatQuestModelMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            MeshViewController.bindMeshSceneControls(getController(), meshView);
            getController().getMainLight().getScope().add(meshView);
            if (mesh.isSkeletonAxisRotationApplied())
                GreatQuestUtils.setEntityRotation(meshView, 0, 0, 0, true);
        }

        @Override
        protected void onMeshViewCleanup(int meshIndex, GreatQuestModelMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewCleanup(meshIndex, mesh, meshView);
            MeshViewController.unbindMeshSceneControls(getController(), meshView);
            getController().getMainLight().getScope().remove(meshView);
            if (mesh.isSkeletonAxisRotationApplied())
                GreatQuestUtils.setEntityRotation(meshView, 0, 0, 0, false);
        }
    }
}