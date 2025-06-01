package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.geometry.HPos;
import javafx.scene.DepthTest;
import javafx.scene.SubScene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTrack;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestActionSequencePlayback.SequenceStatus;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.fx.wrapper.LazyFXListCell;

/**
 * Controls a model mesh for Great Quest.
 * TODO: Allow switching between FPS camera and rotation camera?
 * Created by Kneesnap on 4/15/2024.
 */
@Getter
public class GreatQuestModelViewController extends MeshViewController<GreatQuestModelMesh> {
    private GreatQuestActionSequencePlayback sequencePlayback;
    private GreatQuestModelMeshViewCollection meshViewCollection;
    private ComboBox<kcCActionSequence> actionSequenceComboBox;
    private ComboBox<kcCResourceTrack> animationComboBox;
    private CheckBox showSkeletonCheckBox;
    private CheckBox forceRepeatCheckBox;
    private final MeshView skeletonMeshView = new MeshView();

    private static final double DEFAULT_FAR_CLIP = 50;
    private static final double DEFAULT_NEAR_CLIP = 0.1;
    private static final double DEFAULT_MOVEMENT_SPEED = 3;

    private static final PhongMaterial VERTEX_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial CONNECTION_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.LIMEGREEN);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);

        this.sequencePlayback = new GreatQuestActionSequencePlayback(getMesh());

        // Setup skeleton view toggle.
        this.showSkeletonCheckBox = new CheckBox("Show Skeleton");
        this.showSkeletonCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> this.skeletonMeshView.setVisible(newValue));
        this.forceRepeatCheckBox = new CheckBox("Force Repeat");
        GridPane.setHalignment(this.forceRepeatCheckBox, HPos.RIGHT);
        addToViewSettingsGrid(this.showSkeletonCheckBox, this.forceRepeatCheckBox);

        // Setup animation UI.
        this.animationComboBox = new ComboBox<>(getMesh().getAvailableAnimations());
        this.animationComboBox.setButtonCell(new LazyFXListCell<>(kcCResourceTrack::getName, "No Animation"));
        this.animationComboBox.setCellFactory(listView -> new LazyFXListCell<>(kcCResourceTrack::getName, "No Animation"));
        GridPane.setHgrow(this.animationComboBox, Priority.ALWAYS);
        if (getMesh().getAvailableAnimations().size() > 0) {
            this.animationComboBox.getSelectionModel().selectFirst();
            this.animationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue != newValue && !this.animationComboBox.isDisable()) {
                    if (newValue != null) {
                        this.actionSequenceComboBox.getSelectionModel().clearSelection();
                        this.actionSequenceComboBox.setValue(null);
                    }

                    getMesh().setActiveAnimation(newValue, this.forceRepeatCheckBox.isSelected());
                }
            });
        } else {
            this.animationComboBox.setDisable(true);
        }

        addToViewSettingsGrid(new Label("Animation"), this.animationComboBox);

        // Setup sequence UI.
        this.actionSequenceComboBox = new ComboBox<>(getMesh().getAvailableSequences());
        this.actionSequenceComboBox.setButtonCell(new LazyFXListCell<>(kcCActionSequence::getName, "No Sequence"));
        this.actionSequenceComboBox.setCellFactory(listView -> new LazyFXListCell<>(kcCActionSequence::getName, "No Sequence"));
        GridPane.setHgrow(this.actionSequenceComboBox, Priority.ALWAYS);
        if (getMesh().getAvailableSequences().size() > 0) {
            this.actionSequenceComboBox.getSelectionModel().selectFirst();
            this.actionSequenceComboBox.setValue(null);
            this.actionSequenceComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null)
                    this.animationComboBox.getSelectionModel().clearSelection();

                if (oldValue != newValue)
                    this.sequencePlayback.setSequence(newValue);
            });
        } else {
            this.actionSequenceComboBox.setDisable(true);
        }

        addToViewSettingsGrid(new Label("Action Sequence"), this.actionSequenceComboBox);


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
        if (this.forceRepeatCheckBox.isSelected() && this.sequencePlayback.getStatus() == SequenceStatus.FINISHED && !getMesh().isPlayingAnimation())
            this.sequencePlayback.restart();

        getMesh().tickAnimation(deltaTime);
        this.sequencePlayback.tick();

        // Update UI
        boolean hasAnimations = !getMesh().getAvailableAnimations().isEmpty();
        boolean hasSequences = !getMesh().getAvailableSequences().isEmpty();
        boolean isPlayingAnimation = hasAnimations && getMesh().isPlayingAnimation();
        boolean isPlayingSequence = hasSequences && (this.sequencePlayback.getStatus() != SequenceStatus.FINISHED || (this.actionSequenceComboBox.getValue() != null && isPlayingAnimation));

        // Update disable repeat box.
        boolean shouldDisableRepeatBox = (!hasAnimations && !hasSequences);
        if (this.forceRepeatCheckBox.isDisabled() != shouldDisableRepeatBox)
            this.forceRepeatCheckBox.setDisable(shouldDisableRepeatBox);

        boolean shouldDisableAnimationsBox = isPlayingSequence || !hasAnimations;
        if (this.animationComboBox.isDisable() != shouldDisableAnimationsBox)
            this.animationComboBox.setDisable(shouldDisableAnimationsBox);

        boolean shouldDisableSequenceBox = !hasSequences;
        if (this.actionSequenceComboBox.isDisable() != shouldDisableSequenceBox)
            this.actionSequenceComboBox.setDisable(shouldDisableSequenceBox);
    }

    @Override
    protected void setupManagers() {
        // No managers.
    }

    @Override
    public String getMeshDisplayName() {
        return getMesh().getMeshName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        getFirstPersonCamera().setInvertY(true);
        getFirstPersonCamera().setPos(0, 1, 3);
        getFirstPersonCamera().setPitchAndYaw(180, 0);
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