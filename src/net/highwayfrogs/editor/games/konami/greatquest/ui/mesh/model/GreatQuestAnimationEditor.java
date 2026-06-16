package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.shape.MeshView;
import javafx.stage.Stage;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKey;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector.kcTrackKeyLinearPosition;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector.kcTrackKeyLinearRotation;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTrack;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestActionSequencePlayback.SequenceStatus;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.fxobject.RotationGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.RotationGizmo.IRotationChangeListener;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo.IPositionChangeListener;
import net.highwayfrogs.editor.system.math.Matrix4x4f;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.fx.wrapper.LazyFXListCell;

/**
 * Animation editing UI manager for the Great Quest model viewer.
 * Provides:
 *  - A timeline panel at the bottom of the 3D view for track/keyframe visualisation and editing
 *  - A side accordion panel for keyframe value editing and animation creation
 *  - A 3D TranslationGizmo for visually editing LINEAR_POSITION keyframe positions
 * Created by Kneesnap on 6/12/2026.
 */
public class GreatQuestAnimationEditor extends MeshUIManager<GreatQuestModelMesh> {

    // UI components
    private GreatQuestAnimationTimelinePanel timelinePanel;
    private UISidePanel sidePanel;
    private GUIEditorGrid infoGrid;

    // Animation playback controls
    private CheckBox showSkeletonCheckBox;
    private CheckBox forceRepeatCheckBox;
    private ComboBox<kcCResourceTrack> animationComboBox;
    private ComboBox<kcCActionSequence> actionSequenceComboBox;

    // 3D position gizmo
    private TranslationGizmo positionGizmo;
    private MeshView positionGizmoView;
    // 3D rotation gizmo
    private RotationGizmo rotationGizmo;
    private MeshView rotationGizmoView;
    private DisplayList gizmoDisplayList;

    // State tracking
    private kcCResourceTrack lastKnownAnimation;
    private boolean preScrubAnimationTickingPaused;
    private double playbackStartTick; // Tick at which the most recent playback started (for SPACE-to-pause restore)
    private EventHandler<KeyEvent> spaceKeyFilter;

    /** Height of the timeline panel in pixels. */
    public static final double TIMELINE_HEIGHT = 200.0;
    /** Scale applied to the translation gizmo so it matches the scene scale. */
    private static final double GIZMO_SCALE = 0.025;

    public GreatQuestAnimationEditor(GreatQuestModelViewController controller) {
        super(controller);
    }

    @Override
    public GreatQuestModelViewController getController() {
        return (GreatQuestModelViewController) super.getController();
    }

    @Override
    public GreatQuestInstance getGameInstance() {
        return (GreatQuestInstance) super.getGameInstance();
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    public void onSetup() {
        setupSidePanel();
        setupTimeline();
        setupGizmoDisplayList();

        // Register SPACE as a Stage-level capture filter so it fires regardless of
        // whether the 2D side-panel or the 3D SubScene has focus (the SubScene is an
        // event-dispatch boundary that blocks scene-level addEventHandler from firing).
        Stage stage = getController().getOverwrittenStage();
        if (stage != null) {
            this.spaceKeyFilter = this::handleKeyPress;
            stage.addEventFilter(KeyEvent.KEY_PRESSED, this.spaceKeyFilter);
        }

        // Per-frame task to keep the timeline scrubber in sync
        getController().getFrameTimer().addPerFrameTask(this::onTick);

        // Initialise with the mesh's current animation (if any)
        syncTimelineToMesh();
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() != KeyCode.SPACE)
            return;

        // Don't intercept SPACE while the user is typing in a text field.
        Stage stage = getController().getOverwrittenStage();
        Node focusOwner = stage != null && stage.getScene() != null ? stage.getScene().getFocusOwner() : null;
        if (focusOwner instanceof TextInputControl)
            return;

        boolean currentlyPaused = getController().isAnimationTickingPaused();
        if (currentlyPaused) {
            // Starting playback — record the tick we're playing from
            this.playbackStartTick = getMesh().getAnimationTick();
        } else {
            // Pausing — return the scrubber to the tick playback started from
            getMesh().setAnimationTick(this.playbackStartTick);
        }
        getController().setAnimationTickingPaused(!currentlyPaused);
        event.consume();
    }

    @Override
    public void onRemove() {
        if (this.spaceKeyFilter != null) {
            Stage stage = getController().getOverwrittenStage();
            if (stage != null)
                stage.removeEventFilter(KeyEvent.KEY_PRESSED, this.spaceKeyFilter);
            this.spaceKeyFilter = null;
        }

        hidePositionGizmo();
        hideRotationGizmo();

        // Remove the timeline panel and restore the main pane's bottom anchor
        if (this.timelinePanel != null) {
            AnchorPane root2D = getController().getRoot2D();
            root2D.getChildren().remove(this.timelinePanel);
            if (!root2D.getChildren().isEmpty())
                AnchorPane.setBottomAnchor(root2D.getChildren().get(0), 0.0);
            this.timelinePanel = null;
        }

        super.onRemove();
    }

    // =========================================================================
    // Setup helpers
    // =========================================================================

    private void setupSidePanel() {
        this.sidePanel = getController().createSidePanel("Animation Editor", true);
        this.sidePanel.getScrollPane().setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Top grid: animation-level controls
        this.infoGrid = this.sidePanel.makeEditorGrid();

        if (getMesh().getSkeleton() != null)
            this.infoGrid.addButton("Create New Animation", this::createNewAnimation);

        setupAnimationControls();

        // Separator then keyframe editor grid
        this.sidePanel.add(new Separator());
    }

    private void setupAnimationControls() {
        // --- Show Skeleton / Force Repeat ---
        this.showSkeletonCheckBox = new CheckBox("Show Skeleton");
        this.showSkeletonCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                getController().getSkeletonMeshView().setVisible(newVal));
        this.forceRepeatCheckBox = new CheckBox("Force Repeat");
        this.forceRepeatCheckBox.setSelected(true);
        GridPane.setHalignment(this.forceRepeatCheckBox, HPos.RIGHT);
        this.infoGrid.setupNode(this.showSkeletonCheckBox);
        this.infoGrid.setupSecondNode(this.forceRepeatCheckBox, false);
        this.infoGrid.addRow(25);

        // --- Animation combo box ---
        this.animationComboBox = new ComboBox<>(getMesh().getAvailableAnimations());
        this.animationComboBox.setButtonCell(new LazyFXListCell<>(kcCResourceTrack::getName, "No Animation"));
        this.animationComboBox.setCellFactory(lv -> new LazyFXListCell<>(kcCResourceTrack::getName, "No Animation")
                .setWithoutIndexStyleHandler(a -> (a == null || getMesh().getSourceAnimations().contains(a)) ? null : FXUtils.STYLE_LIST_CELL_RED_BACKGROUND));
        if (getMesh().getAvailableAnimations().isEmpty())
            this.animationComboBox.setDisable(true);
        else
            this.animationComboBox.getSelectionModel().selectFirst();

        // --- Action Sequence combo box ---
        this.actionSequenceComboBox = new ComboBox<>(getMesh().getAvailableSequences());
        this.actionSequenceComboBox.setButtonCell(new LazyFXListCell<>(kcCActionSequence::getName, "No Sequence"));
        this.actionSequenceComboBox.setCellFactory(lv -> new LazyFXListCell<>(kcCActionSequence::getName, "No Sequence")
                .setWithoutIndexStyleHandler(s -> s == null || (getMesh().getActionSequenceTable() != null && getMesh().getActionSequenceTable().contains(s)) ? null : FXUtils.STYLE_LIST_CELL_RED_BACKGROUND));
        if (getMesh().getAvailableSequences().isEmpty()) {
            this.actionSequenceComboBox.setDisable(true);
        } else {
            this.actionSequenceComboBox.getSelectionModel().selectFirst();
            this.actionSequenceComboBox.setValue(null);
        }

        // Add listeners after both combos are fully created (avoids NPE on cross-references)
        if (!getMesh().getAvailableAnimations().isEmpty()) {
            this.animationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (oldVal != newVal && !this.animationComboBox.isDisable()) {
                    if (newVal != null) {
                        this.actionSequenceComboBox.getSelectionModel().clearSelection();
                        this.actionSequenceComboBox.setValue(null);
                    }
                    getMesh().setActiveAnimation(newVal, this.forceRepeatCheckBox.isSelected(), false, false);
                }
            });
        }
        if (!getMesh().getAvailableSequences().isEmpty()) {
            this.actionSequenceComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null)
                    this.animationComboBox.getSelectionModel().clearSelection();
                if (oldVal != newVal)
                    getController().getSequencePlayback().setSequence(newVal);
            });
        }

        // Add rows to the info grid
        this.infoGrid.setupNode(new Label("Animation"));
        this.infoGrid.setupSecondNode(this.animationComboBox, false);
        GridPane.setHgrow(this.animationComboBox, Priority.ALWAYS);
        this.infoGrid.addRow(25);

        this.infoGrid.setupNode(new Label("Action Sequence"));
        this.infoGrid.setupSecondNode(this.actionSequenceComboBox, false);
        GridPane.setHgrow(this.actionSequenceComboBox, Priority.ALWAYS);
        this.infoGrid.addRow(25);

        // --- Playback speed slider (0.1× – 4×, default 1×) ---
        Slider speedSlider = new Slider(0.1, 4.0, 1.0);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(1.0);
        speedSlider.setMinorTickCount(3);
        speedSlider.setSnapToTicks(false);
        speedSlider.setTooltip(new Tooltip("Animation playback speed multiplier"));
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                getController().setAnimationSpeedMultiplier(newVal.floatValue()));
        GridPane.setHgrow(speedSlider, Priority.ALWAYS);
        this.infoGrid.setupNode(new Label("Speed"));
        this.infoGrid.setupSecondNode(speedSlider, false);
        this.infoGrid.addRow(35);
    }

    private void setupTimeline() {
        this.timelinePanel = new GreatQuestAnimationTimelinePanel(this);

        AnchorPane root2D = getController().getRoot2D();

        // Push the main SplitPane upward to make room
        if (!root2D.getChildren().isEmpty()) {
            Node mainPane = root2D.getChildren().get(0);
            AnchorPane.setBottomAnchor(mainPane, TIMELINE_HEIGHT);
        }

        AnchorPane.setLeftAnchor(this.timelinePanel, 0.0);
        AnchorPane.setRightAnchor(this.timelinePanel, 0.0);
        AnchorPane.setBottomAnchor(this.timelinePanel, 0.0);
        this.timelinePanel.setPrefHeight(TIMELINE_HEIGHT);
        root2D.getChildren().add(this.timelinePanel);
    }

    private void setupGizmoDisplayList() {
        this.gizmoDisplayList = getController().getTransparentRenderManager().createDisplayList();
    }

    // =========================================================================
    // Per-frame update
    // =========================================================================

    private void onTick(float deltaTime) {
        // Force-repeat: restart the sequence when it finishes
        if (this.forceRepeatCheckBox != null && this.forceRepeatCheckBox.isSelected()
                && getController().getSequencePlayback().getStatus() == SequenceStatus.FINISHED
                && !getMesh().isPlayingAnimation())
            getController().getSequencePlayback().restart();

        // Update combo/checkbox enable states
        if (this.animationComboBox != null) {
            boolean hasAnimations = !getMesh().getAvailableAnimations().isEmpty();
            boolean hasSequences = !getMesh().getAvailableSequences().isEmpty();
            boolean isPlayingAnimation = hasAnimations && getMesh().isPlayingAnimation();
            boolean isPlayingSequence = hasSequences && (getController().getSequencePlayback().getStatus() != SequenceStatus.FINISHED
                    || (this.actionSequenceComboBox.getValue() != null && isPlayingAnimation));

            boolean shouldDisableRepeat = !hasAnimations && !hasSequences;
            if (this.forceRepeatCheckBox.isDisabled() != shouldDisableRepeat)
                this.forceRepeatCheckBox.setDisable(shouldDisableRepeat);

            boolean shouldDisableAnims = isPlayingSequence || !hasAnimations;
            if (this.animationComboBox.isDisable() != shouldDisableAnims)
                this.animationComboBox.setDisable(shouldDisableAnims);

            boolean shouldDisableSeqs = !hasSequences;
            if (this.actionSequenceComboBox.isDisable() != shouldDisableSeqs)
                this.actionSequenceComboBox.setDisable(shouldDisableSeqs);
        }

        // Detect animation changes (e.g. the user selected a different animation)
        kcCResourceTrack current = getMesh().getActiveAnimation();
        if (current != this.lastKnownAnimation) {
            this.lastKnownAnimation = current;
            syncTimelineToMesh();
        }

        // Keep timeline scrubber in sync with the mesh (unless the user is dragging it)
        if (this.timelinePanel != null && !this.timelinePanel.isDraggingScrubber())
            this.timelinePanel.setScrubberTick(getMesh().getAnimationTick());

        // Update the gizmo position (the bone moves as the animation plays)
        refreshGizmoPosition();
    }

    private void syncTimelineToMesh() {
        if (this.timelinePanel != null)
            this.timelinePanel.setAnimation(getMesh().getActiveAnimation(), getMesh().getSkeleton());
    }

    // =========================================================================
    // Timeline event callbacks (called by GreatQuestAnimationTimelinePanel)
    // =========================================================================

    /** User started dragging the scrubber → pause automatic animation ticking. */
    public void onTimelineScrubStart() {
        this.preScrubAnimationTickingPaused = getController().isAnimationTickingPaused();
        getController().setAnimationTickingPaused(true);
    }

    /** User released the scrubber → restore the pause state that existed before scrubbing started. */
    void onTimelineScrubEnd() {
        getController().setAnimationTickingPaused(this.preScrubAnimationTickingPaused);
    }

    /** Scrubber moved to a new position → seek animation. */
    void onTimelineScrubberMoved(double newTick) {
        getMesh().setAnimationTick(newTick);
    }

    /** A bone label was clicked in the timeline → show skeleton and select that bone. */
    void onBoneLabelClicked(kcTrack track) {
        // Ensure the skeleton is visible
        if (this.showSkeletonCheckBox != null && !this.showSkeletonCheckBox.isSelected())
            this.showSkeletonCheckBox.setSelected(true);

        // Highlight the bone in the 3D skeleton mesh
        updateBoneHighlight(track);

        // Hide any active gizmos (no keyframe is selected)
        hidePositionGizmo();
        hideRotationGizmo();
    }

    /** A keyframe was clicked in the timeline. */
    void onKeyframeSelected(kcTrack track, kcTrackKey<?> key) {
        updateBoneHighlight(track);
        if (key != null && track != null) {
            getMesh().setAnimationTick(key.getTick());
            showOrUpdatePositionGizmo(track, key);
            showOrUpdateRotationGizmo(track, key);
        } else {
            hidePositionGizmo();
            hideRotationGizmo();
        }
    }

    /**
     * The user dragged a keyframe to a new tick in the timeline.
     * We move it only if the target tick is not already taken by another keyframe.
     */
    void onKeyframeMoveRequested(kcTrack track, kcTrackKey<?> key, int newTick) {
        if (key.getTick() == newTick)
            return; // Tick will not change.

        // Check for tick collision.
        kcTrackKey<?> other = track.getKeyForTick(newTick);
        if (other != key && other.getTick() == newTick)
            return; // This would collide with another key!

        track.setTrackKeyTick(key, newTick);

        // Snap animation to the keyframe's new position and refresh the mesh
        getMesh().setAnimationTick(newTick);

        if (this.timelinePanel != null)
            this.timelinePanel.redraw();
    }

    /** Add a new keyframe to a track at the given tick. */
    void onKeyframeAddRequested(kcTrack track, int tick) {
        kcCResourceSkeleton skeleton = getMesh().getSkeleton();
        if (skeleton == null)
            return;

        kcNode node = skeleton.getNodeByTag(track.getTag());
        track.createKeyAtTick(node, tick);
        if (this.timelinePanel != null)
            this.timelinePanel.onAnimationModified();

        getMesh().updateMeshes();
    }

    /** Delete a keyframe from a track. */
    void onKeyframeDeleteRequested(kcTrack track, kcTrackKey<?> key) {
        if (this.timelinePanel != null && this.timelinePanel.getSelectedKeyframe() == key) {
            this.timelinePanel.selectKeyframe(null, null);
            onKeyframeSelected(null, null);
        }

        track.removeKey(key);

        if (this.timelinePanel != null)
            this.timelinePanel.onAnimationModified();

        getMesh().updateMeshes();
    }

    private void onKeyframeValueChanged() {
        // Keyframe data changed in-place at the current tick — bypass the tick-equality
        // early-exit in setAnimationTick and force a full mesh refresh.
        getMesh().updateMeshes();
    }

    /**
     * Keeps the rotation gizmo's stored quaternion in sync with the keyframe's vector.
     * Called from text field change handlers so that subsequent gizmo drags start from the
     * up-to-date value.
     */
    private void syncRotationGizmoFromKeyframe(kcTrackKeyVector vecKey) {
        if (this.rotationGizmoView != null && this.rotationGizmo != null) {
            kcVector4 vec = vecKey.getVector();
            this.rotationGizmo.setRotation(this.rotationGizmoView, vec.getX(), vec.getY(), vec.getZ(), vec.getW(), false);
        }
    }

    // =========================================================================
    // Bone highlighting
    // =========================================================================

    private void updateBoneHighlight(kcTrack track) {
        GreatQuestModelSkeletonMesh skeletonMesh = getMesh().getSkeletonMesh();
        if (skeletonMesh == null)
            return;

        kcCResourceSkeleton skeleton = getMesh().getSkeleton();
        kcNode bone = (skeleton != null && track != null) ? skeleton.getNodeByTag(track.getTag()) : null;
        skeletonMesh.setSelectedBone(bone);
    }

    // =========================================================================
    // Position gizmo
    // =========================================================================

    /**
     * Shows (or repositions) the translation gizmo for a LINEAR_POSITION keyframe.
     * For other key types the gizmo is hidden.
     */
    private void showOrUpdatePositionGizmo(kcTrack track, kcTrackKey<?> key) {
        if (!(key instanceof kcTrackKeyLinearPosition)) {
            hidePositionGizmo();
            return;
        }

        kcTrackKeyLinearPosition vecKey = (kcTrackKeyLinearPosition) key;

        // Lazily create the gizmo and its MeshView
        if (this.positionGizmoView == null) {
            this.positionGizmo = new TranslationGizmo();
            this.positionGizmoView = new MeshView();
            this.positionGizmoView.setDepthTest(DepthTest.DISABLE);
            this.positionGizmo.addView(this.positionGizmoView, getController(), null);
            Scene3DUtils.setNodeScale(this.positionGizmoView, GIZMO_SCALE, GIZMO_SCALE, GIZMO_SCALE);
            getController().getMainLight().getScope().add(this.positionGizmoView);
            this.gizmoDisplayList.add(this.positionGizmoView);
        }

        // Wire the change listener: update keyframe values when the gizmo moves
        IPositionChangeListener listener = (mv, oldX, oldY, oldZ, newX, newY, newZ, flags) -> {
            // Convert gizmo world-space movement back to bone-local space
            Vector3f localPos = worldToLocal(track.getTag(), (float) newX, (float) newY, (float) newZ);
            vecKey.getVector().setXYZW(localPos.getX(), localPos.getY(), localPos.getZ(), vecKey.getVector().getW());
            onKeyframeValueChanged();
        };
        this.positionGizmo.setChangeListener(this.positionGizmoView, listener);

        // Position the gizmo at the bone's current world location
        placePositionGizmoAtBone(track.getTag());
    }

    private void hidePositionGizmo() {
        if (this.positionGizmoView != null) {
            DynamicMesh.tryRemoveMesh(this.positionGizmoView);
            this.gizmoDisplayList.remove(this.positionGizmoView);
            getController().getMainLight().getScope().remove(this.positionGizmoView);
            this.positionGizmoView = null;
            this.positionGizmo = null;
        }
    }

    // =========================================================================
    // Rotation gizmo
    // =========================================================================

    /**
     * Shows (or repositions) the rotation gizmo for a rotation keyframe.
     * For non-rotation key types the gizmo is hidden.
     */
    private void showOrUpdateRotationGizmo(kcTrack track, kcTrackKey<?> key) {
        if (!(key instanceof kcTrackKeyLinearRotation)) {
            hideRotationGizmo();
            return;
        }

        kcTrackKeyLinearRotation vecKey = (kcTrackKeyLinearRotation) key;

        // Lazily create the gizmo and its MeshView
        if (this.rotationGizmoView == null) {
            this.rotationGizmo = new RotationGizmo();
            // The gizmo rings are in JavaFX world space, but GQ bone quaternions are in game space.
            // The model mesh has Rx(-90°) applied, so: JavaFX-Y = game-Z, JavaFX-Z = game-(-Y).
            // Supply the game-space equivalents so that dragging each ring rotates the correct axis.
            // Empirically: with the skeleton Rx(-90°) applied to the model mesh,
            // game-Z produces the visual X-axis rotation and game-X produces visual Y-axis rotation.
            this.rotationGizmo.setRotationAxes(
                    new Vector3f(0, 0, 1),   // red   ring: game Z → visual X rotation
                    new Vector3f(1, 0, 0),   // green ring: game X → visual Y rotation
                    new Vector3f(0, -1, 0)); // blue  ring: game −Y → visual Z rotation (unchanged)
            this.rotationGizmoView = new MeshView();
            this.rotationGizmoView.setDepthTest(DepthTest.DISABLE);
            this.rotationGizmo.addView(this.rotationGizmoView, getController(), null);
            Scene3DUtils.setNodeScale(this.rotationGizmoView, GIZMO_SCALE, GIZMO_SCALE, GIZMO_SCALE);
            getController().getMainLight().getScope().add(this.rotationGizmoView);
            this.gizmoDisplayList.add(this.rotationGizmoView);
        }

        // Sync stored quaternion with the keyframe's current values
        kcVector4 vec = vecKey.getVector();
        this.rotationGizmo.setRotation(this.rotationGizmoView, vec.getX(), vec.getY(), vec.getZ(), vec.getW(), false);

        // Wire the change listener: update keyframe values when the gizmo rotates
        IRotationChangeListener listener = (mv, oldX, oldY, oldZ, oldW, newX, newY, newZ, newW, flags) -> {
            vecKey.getVector().setXYZW(newX, newY, newZ, newW);
            onKeyframeValueChanged();
        };
        this.rotationGizmo.setChangeListener(this.rotationGizmoView, listener);

        // Position the gizmo at the bone's current world location
        placeRotationGizmoAtBone(track.getTag());
    }

    private void hideRotationGizmo() {
        if (this.rotationGizmoView != null) {
            DynamicMesh.tryRemoveMesh(this.rotationGizmoView);
            this.gizmoDisplayList.remove(this.rotationGizmoView);
            getController().getMainLight().getScope().remove(this.rotationGizmoView);
            this.rotationGizmoView = null;
            this.rotationGizmo = null;
        }
    }

    /** Repositions gizmos to follow the bone as the animation plays. */
    private void refreshGizmoPosition() {
        kcTrack track = this.timelinePanel != null ? this.timelinePanel.getSelectedTrack() : null;
        if (track == null)
            return;
        if (this.positionGizmoView != null && this.positionGizmo != null)
            placePositionGizmoAtBone(track.getTag());
        if (this.rotationGizmoView != null && this.rotationGizmo != null)
            placeRotationGizmoAtBone(track.getTag());
    }

    private void placePositionGizmoAtBone(int boneTag) {
        Matrix4x4f boneTransform = getMesh().getBoneTransform(boneTag);
        if (boneTransform == null)
            return;

        float wx = boneTransform.getTranslationX();
        float wy = boneTransform.getTranslationY();
        float wz = boneTransform.getTranslationZ();
        this.positionGizmo.setPosition(this.positionGizmoView, wx, wy, wz, false);
    }

    private void placeRotationGizmoAtBone(int boneTag) {
        Matrix4x4f boneTransform = getMesh().getBoneTransform(boneTag);
        if (boneTransform == null)
            return;

        float wx = boneTransform.getTranslationX();
        float wy = boneTransform.getTranslationY();
        float wz = boneTransform.getTranslationZ();
        this.rotationGizmo.setPosition(this.rotationGizmoView, wx, wy, wz);
    }

    /**
     * Converts a world-space position back into the local space of the given bone's parent,
     * which is the coordinate system in which LINEAR_POSITION keyframes are expressed.
     *
     * <p>Matrix4x4f stores translation in row 3, so the matrix product G = L * P gives:
     * G.translation = P_upper^T * L.translation + P.translation
     * Recovering L.translation requires: P_upper * (G.translation - P.translation)
     * which is computed via (P.multiply(offset) - P.translation) where offset = worldPos - P.translation.
     * Using inverse(P) * worldPos is incorrect because it applies P_upper^T twice instead of once.</p>
     *
     * @param boneTag the tag of the bone whose keyframe we are editing
     * @param wx world X
     * @param wy world Y
     * @param wz world Z
     * @return position in parent-bone local space
     */
    private Vector3f worldToLocal(int boneTag, float wx, float wy, float wz) {
        kcCResourceSkeleton skeleton = getMesh().getSkeleton();
        if (skeleton != null) {
            kcNode node = skeleton.getNodeByTag(boneTag);
            if (node != null && node.getParent() != null) {
                int parentTag = node.getParent().getTag();

                // Consider how to reduce allocations.
                Matrix4x4f parentWorld = getMesh().getBoneTransform(parentTag);
                float ptx = parentWorld.getTranslationX();
                float pty = parentWorld.getTranslationY();
                float ptz = parentWorld.getTranslationZ();
                // Compute P_upper * (worldPos - P.translation):
                //   parentWorld.multiply(offset) = P_upper * offset + P.translation
                //   subtract P.translation to isolate P_upper * offset = L.translation
                Vector3f offset = new Vector3f(wx - ptx, wy - pty, wz - ptz);
                Vector3f result = parentWorld.multiply(offset, new Vector3f());
                result.setXYZ(result.getX() - ptx, result.getY() - pty, result.getZ() - ptz);
                return result;
            }
        }
        // Root bone (or unavailable parent): world == local
        return new Vector3f(wx, wy, wz);
    }

    // =========================================================================
    // Create animation
    // =========================================================================

    private void createNewAnimation() {
        kcCResourceSkeleton skeleton = getMesh().getSkeleton();
        if (skeleton == null) {
            FXUtils.showPopup(AlertType.ERROR, "Cannot create animation", "No skeleton is associated with this model.");
            return;
        }

        kcModelWrapper modelWrapper = getMesh().getModelWrapper();
        if (modelWrapper == null) {
            FXUtils.showPopup(AlertType.ERROR, "Cannot create animation", "No modelWrapper is associated with this model.");
            return;
        }

        // Generate a default name for the animation file.
        String defaultName = null;
        String filePath = modelWrapper.getFilePath();
        int lastSlash = StringUtils.isNullOrWhiteSpace(filePath) ? -1 : filePath.lastIndexOf('\\');
        if (lastSlash > 0) {
            int secondLastSlash = filePath.lastIndexOf('\\', lastSlash - 1);
            if (secondLastSlash >= 0)
                defaultName = filePath.substring(secondLastSlash + 1, lastSlash) + ".bae";
        }

        // Prompt the user to input a file name.
        InputMenu.promptInput(getGameInstance(),
                "Enter a name for the new animation file:", defaultName, newName -> {
            if (newName == null || newName.trim().isEmpty())
                return;

            String trimmedName = newName.trim();
            GreatQuestChunkedFile parentFile = skeleton.getParentFile();
            kcCResourceTrack newTrack = new kcCResourceTrack(parentFile);

            try {
                newTrack.setName(trimmedName);
                parentFile.addResource(newTrack);
            } catch (Exception ex) {
                FXUtils.showPopup(AlertType.ERROR, "Failed to create animation", "Could not create '" + trimmedName + "': " + ex.getMessage());
                return;
            }

            // Make it available for selection in the animation combo box
            getMesh().getAvailableAnimations().add(newTrack);
        });
    }
}
