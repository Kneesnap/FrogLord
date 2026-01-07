package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerPickupData;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerPickupData.PickupAnimationFrame;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerCentralUIManager.FroggerCentralMapListManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathSegmentPreview;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.MeshViewFrameTimer.MeshViewFixedFrameRateTimerTask;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages entities in a Frogger map.
 * Created by Kneesnap on 6/1/2024.
 */
public class FroggerUIMapEntityManager extends FroggerCentralMapListManager<FroggerMapEntity, MeshView> {
    private final float[] posCache = new float[6];
    private final float[] selectedMouseEntityPosition = new float[6];
    private final Map<MRModel, MRModelMesh> meshCache = new HashMap<>();
    @Getter private DisplayList litEntityRenderList;
    @Getter private DisplayList unlitEntityRenderList;
    private FroggerMapEntity selectedMouseEntity;
    private MeshViewFixedFrameRateTimerTask<FroggerUIMapEntityManager> pickupAnimationUpdateTask;
    private int pickupAnimationFrameCounter;

    public static final float ENTITY_PLACEHOLDER_SPRITE_SIZE = 16F;
    public static final TriangleMesh ENTITY_PLACEHOLDER_SPRITE_MESH = Scene3DUtils.createSpriteMesh(ENTITY_PLACEHOLDER_SPRITE_SIZE);
    public static final PhongMaterial ENTITY_PLACEHOLDER_SPRITE_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(ImageResource.SQUARE_LETTER_E_128.getFxImage());
    public static final PhongMaterial ENTITY_HIGHLIGHTED_PLACEHOLDER_SPRITE_MATERIAL = Scene3DUtils.updateHighlightMaterial(null, ImageResource.SQUARE_LETTER_E_128.getAwtImage());

    public FroggerUIMapEntityManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        // The map renders after entities (except for sprite entities) because transparent entities are exceedingly rare in Frogger.
        // Even though there are some (such as the sky cloud patch), making them display properly would hide all entities underwater, so it's preferable to do it like this.
        // Situations such as transparent water layers should show the entities under the water too.
        this.litEntityRenderList = getRenderManager().createDisplayListWithNewGroup();
        this.litEntityRenderList.add(getController().getGeneralManager().getPlayerCharacterView());
        this.litEntityRenderList.add(getController().getGeneralManager().getFrogLight());
        super.onSetup();
        setPickupAnimationsVisible(true);

        getController().getInputManager().addMouseListener(this::handleSelectedEntityMouseInput);
    }

    @Override
    public void setupNodesWhichRenderLast() {
        super.setupNodesWhichRenderLast();

        // Unlit entities currently are just sprites.
        // Sprites have transparency, so they need to render after the map has rendered.
        this.unlitEntityRenderList = getRenderManager().createDisplayListWithNewGroup();
        getController().getMainLight().getScope().add(this.unlitEntityRenderList.getRoot());
        setupUnlitEntities(); // Update the unlit entities.
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        getMainGrid().addCheckBox("Show Pickup Animations", true, this::setPickupAnimationsVisible);
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);
    }

    private void setPickupAnimationsVisible(boolean visible) {
        if (visible && this.pickupAnimationUpdateTask == null) {
            // The frame-rate of 2 comes from Pickup_move(), which only increments the counter if MRFrameIndex is 0, which happens every other frame.
            this.pickupAnimationUpdateTask = getFrameTimer(getGameInstance().getFPS()).addTask(2, this, FroggerUIMapEntityManager::tickPickupAnimations);
        } else if (!visible && this.pickupAnimationUpdateTask != null) {
            this.pickupAnimationUpdateTask.cancel();
            this.pickupAnimationUpdateTask = null;
        }
    }

    @Override
    public String getTitle() {
        return "Entities";
    }

    @Override
    public String getValueName() {
        return "Entity";
    }

    @Override
    public List<FroggerMapEntity> getValues() {
        return getMap().getEntityPacket().getEntities();
    }

    @Override
    protected MeshView setupDisplay(FroggerMapEntity entity) {
        MeshView newView = new MeshView();
        newView.setCullFace(CullFace.NONE);
        newView.setDrawMode(DrawMode.FILL);
        updateEntityMesh(entity, newView);
        updateEntityPositionRotation(entity, newView);

        getController().getBakedGeometryManager().setupMeshViewHighlightSkipHover(newView);
        newView.setOnMouseClicked(evt -> handleClick(evt, entity));
        return newView;
    }

    @Override // Only handle entity clicks when the path selector is not active, to prevent deselecting an entity relating to the active selection.
    protected void handleClick(MouseEvent event, FroggerMapEntity entity) {
        event.consume();
        if (!getController().getPathManager().getPathSelector().isPromptActive())
            super.handleClick(event, entity);
    }

    @Override
    protected void setVisible(FroggerMapEntity oldEntity, MeshView meshView, boolean visible) {
        if (meshView != null)
            meshView.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(FroggerMapEntity oldEntity, MeshView oldMeshView, FroggerMapEntity newEntity, MeshView newMeshView) {
        if (oldEntity != null && oldMeshView != null)
            updateEntityMesh(oldEntity, oldMeshView); // Restore original material.
        if (newEntity != null && newMeshView != null)
            updateEntityMesh(newEntity, newMeshView); // Apply new highlight material.
    }

    @Override
    protected FroggerMapEntity createNewValue() {
        // If an entity is selected, create a clone of the current entity.
        FroggerMapEntity selectedEntity = getSelectedValue();
        if (selectedEntity != null)
            return selectedEntity.clone();

        // The entity ID will be automatically generated when the entity is added, so we do not need to set it here.
        // The form grid ID will also be set (if possible) when the map form entry is created.
        // The red checkpoint frog has been picked because it's more visually noticeable on most maps. (If the user adds an entity while zoomed out, it might be hard to see it)
        FroggerMapEntity newEntity = new FroggerMapEntity(getMap(), getGameInstance().getMapFormEntry(FroggerMapTheme.GENERAL, 4));
        final float angleInRadians = (float) Math.toRadians(180F);
        newEntity.getMatrixEntityData().getMatrix().updateMatrix(angleInRadians, 0, angleInRadians); // Rotate the baby frog to face the default camera direction.
        return newEntity;
    }

    @Override
    protected boolean tryAddValue(FroggerMapEntity entity) {
        if (entity == null || !getMap().getEntityPacket().addEntity(entity))
            return false;

        setSelectedMouseEntity(entity);
        return true;
    }

    @Override
    protected boolean tryRemoveValue(FroggerMapEntity entity) {
        if (entity == null || !getMap().getEntityPacket().removeEntity(entity))
            return false;

        if (this.selectedMouseEntity == entity)
            setSelectedMouseEntity(null);
        return true;
    }

    @Override
    protected void updateEditor(FroggerMapEntity entity) {
        entity.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void onDelegateRemoved(FroggerMapEntity entity, MeshView meshView) {
        if (meshView != null) {
            this.litEntityRenderList.remove(meshView);
            this.unlitEntityRenderList.remove(meshView);
            getController().getLightManager().clearAmbientLighting(meshView);
        }
    }

    private void handleSelectedEntityMouseInput(InputManager manager, MouseEvent event, double deltaX, double deltaY) {
        if (this.selectedMouseEntity == null)
            return;

        if (!manager.getMouseTracker().isSignificantMouseDragRecorded())
            event.consume(); // Allow the event to go through if a mouse drag is occurring.

        PickResult pickResult = event.getPickResult();
        if (pickResult == null)
            return;

        // Tie the entity position to the cursor position.
        Point3D intersectedPoint = pickResult.getIntersectedPoint();
        Node intersectedNode = pickResult.getIntersectedNode();

        // Prevent interacting with the MeshView itself.
        MeshView entityMeshView = getDelegatesByValue().get(this.selectedMouseEntity);
        if (entityMeshView == intersectedNode) {
            entityMeshView.setMouseTransparent(true); // Prevent mouse interaction with the entity while it is selected.
            return;
        }

        if (this.selectedMouseEntity.getPathInfo() == null && intersectedPoint != null && intersectedNode != null) {
            Point3D newWorldPos = intersectedNode.localToScene(intersectedPoint);
            this.selectedMouseEntityPosition[0] = (float) newWorldPos.getX();
            this.selectedMouseEntityPosition[1] = (float) newWorldPos.getY();
            this.selectedMouseEntityPosition[2] = (float) newWorldPos.getZ();
            if (getController().getInputManager().isKeyPressed(KeyCode.CONTROL))
                for (int i = 0; i < 3; i++)
                    this.selectedMouseEntityPosition[i] = Math.round(this.selectedMouseEntityPosition[i] / 8F) * 8F;
        }

        updateEntityPositionRotation(this.selectedMouseEntity); // Update display position.
    }

    /**
     * Sets the entity currently active for mouse-based selection.
     * @param newEntity the entity to set active on the mouse
     */
    public void setSelectedMouseEntity(FroggerMapEntity newEntity) {
        if (this.selectedMouseEntity == newEntity)
            return; // No change.

        FroggerMapEntity oldSelectedMouseEntity = this.selectedMouseEntity;
        this.selectedMouseEntity = newEntity;

        // Clean up/deselect old entity.
        if (oldSelectedMouseEntity != null) {
            MeshView oldMeshView = getDelegatesByValue().get(oldSelectedMouseEntity);
            oldMeshView.setMouseTransparent(false);
            updateEntityMesh(oldSelectedMouseEntity, oldMeshView); // Reset any visual changes.
            updateEntityPositionRotation(oldSelectedMouseEntity, oldMeshView); // Reset the position to the new desired position.
            updateEditor(); // Update the entity editor display, update path slider, etc.
        }

        // Select new entity.
        if (newEntity == null)
            return;

        // We can't set the MeshView as mouse transparent yet.
        newEntity.getPositionAndRotation(this.selectedMouseEntityPosition); // Set the temporary mouse position to be the entity's real position.

        if (newEntity.getPathInfo() != null) {
            // Listen for path clicks
            getController().getPathManager().getPathSelector().promptPath(selection -> {
                applyPathSelectionToMouse(selection);
                setSelectedMouseEntity(null);
            }, this::applyPathSelectionToMouse, () -> setSelectedMouseEntity(null));
        } else {
            // Listen for mouse clicks.
            getController().getBakedGeometryManager().getPolygonSelector().activate(clickedPolygon -> {
                this.selectedMouseEntity.getMatrixEntityData().applyPositionData(this.selectedMouseEntityPosition);
                setSelectedMouseEntity(null);
            }, () -> setSelectedMouseEntity(null));
        }
    }

    private void applyPathSelectionToMouse(FroggerPathSegmentPreview selection) {
        FroggerPathInfo pathInfo = this.selectedMouseEntity.getPathInfo();
        boolean didPathChange = selection.getPath() != pathInfo.getPath();

        if (didPathChange)
            getMap().getPathPacket().removeEntityFromPathTracking(this.selectedMouseEntity);
        pathInfo.setPath(selection.getPath(), selection.getPathSegment());
        pathInfo.setSegmentDistance(selection.getSegmentDistance());
        if (didPathChange)
            getMap().getPathPacket().addEntityToPathTracking(this.selectedMouseEntity);

        // Update the entity position.
        this.selectedMouseEntity.getPathEntityData().getPositionAndRotation(this.selectedMouseEntityPosition);
        updateEntityPositionRotation(this.selectedMouseEntity);
    }

    /**
     * Update all entity meshes.
     */
    public void updateAllEntityMeshes() {
        getValues().forEach(this::updateEntityMesh);
    }

    /**
     * Update the mesh displayed for the given entity.
     * @param entity The entity to update the mesh for.s
     */
    public void updateEntityMesh(FroggerMapEntity entity) {
        MeshView entityMesh = getDelegatesByValue().get(entity);
        if (entityMesh != null)
            updateEntityMesh(entity, entityMesh);
    }

    private void updateEntityMesh(FroggerMapEntity entity, MeshView entityMeshView) {
        boolean isEntitySelected = (getSelectedValue() == entity);
        boolean isEntityHighlighted = isEntitySelected && (this.selectedMouseEntity != entity);

        // Reset entity scaling.
        Scene3DUtils.setNodeScale(entityMeshView, 1D, 1D, 1D);

        // Attempt to apply from 3D model.
        IFroggerFormEntry formEntry = entity.getFormEntry();
        MRModel model = formEntry != null ? formEntry.getEntityModel(entity) : null;
        if (model != null) {
            // Update MeshView.
            MRModelMesh modelMesh = this.meshCache.computeIfAbsent(model, MRModel::createMeshWithDefaultAnimation);
            if (modelMesh.getEditableFaces().size() > 0) {
                DynamicMesh.tryRemoveMesh(entityMeshView);
                modelMesh.addView(entityMeshView, getController().getMeshTracker(), isEntityHighlighted, !getController().getLightManager().isLightingAppliedToEntities());
                entityMeshView.setCullFace(CullFace.BACK);

                // Update entity display material and such.
                if (isEntityHighlighted) {
                    registerUnlitEntity(entityMeshView);
                } else {
                    registerLitEntity(entity, entityMeshView);
                }

                return;
            }
        }

        // Unregister the attached mesh as we're about to potentially
        DynamicMesh.tryRemoveMesh(entityMeshView);

        // Attempt to use a fly sprite.
        FroggerGameInstance config = getMap().getGameInstance();
        FroggerPickupData pickupData = config.getPickupData(entity.getFlyScoreType());
        if (pickupData != null && pickupData.getFrames().size() > 0) {
            PickupAnimationFrame pickupAnimationFrame = pickupData.getFrames().get(Math.max(0, this.pickupAnimationFrameCounter) % pickupData.getFrames().size());
            if (pickupAnimationFrame != null && pickupAnimationFrame.applyToMeshView(entityMeshView, isEntityHighlighted)) {
                registerUnlitEntity(entityMeshView);
                return;
            }
        }

        // Setup entity placeholder sprite mesh.
        entityMeshView.setMesh(ENTITY_PLACEHOLDER_SPRITE_MESH);
        entityMeshView.setMaterial(isEntityHighlighted ? ENTITY_HIGHLIGHTED_PLACEHOLDER_SPRITE_MATERIAL : ENTITY_PLACEHOLDER_SPRITE_MATERIAL);
        registerUnlitEntity(entityMeshView);
    }

    private void registerUnlitEntity(MeshView meshView) {
        getController().getLightManager().clearAmbientLighting(meshView);
        if (this.unlitEntityRenderList == null || this.unlitEntityRenderList.addIfMissing(meshView))
            this.litEntityRenderList.remove(meshView);
    }

    private void registerLitEntity(FroggerMapEntity entity, MeshView meshView) {
        FroggerUIMapLightManager lightManager = getController().getLightManager();
        boolean lightingEnabled = lightManager.isLightingAppliedToEntities();
        if (lightingEnabled) {
            lightManager.setAmbientLightingMode(meshView, !entity.getConfig().isAtOrBeforeBuild40() && "CHECKPOINT".equals(entity.getTypeName())); // 'CHECKPOINT' is valid starting from PSX Alpha to retail.
        } else {
            lightManager.clearAmbientLighting(meshView);
        }

        if (this.litEntityRenderList.addIfMissing(meshView) && this.unlitEntityRenderList != null)
            this.unlitEntityRenderList.remove(meshView);
    }

    /**
     * Updates the displayed position / rotation of the entity.
     * @param entity The entity to get positional data from.
     */
    public void updateEntityPositionRotation(FroggerMapEntity entity) {
        updateEntityPositionRotation(entity, getDelegatesByValue().get(entity));
    }

    /**
     * Updates the displayed position / rotation of the entity.
     * @param entity         The entity to get positional data from.
     * @param entityMeshView The mesh view to update position for.
     */
    public void updateEntityPositionRotation(FroggerMapEntity entity, MeshView entityMeshView) {
        if (entity == null || entity.getEntityData() == null || entityMeshView == null)
            return; // No data to update position from.

        // Determine position to display entity at.
        float[] positionData;
        if (entity == this.selectedMouseEntity) { // Mouse selections are bound to the mouse entity position.
            positionData = this.selectedMouseEntityPosition;
        } else {
            FroggerPathInfo pathPreview = getController().getPathManager().getPathRunnerPreviews().get(entity);
            if (pathPreview != null && FroggerEntityDataPathInfo.evaluatePathInfoToEntityArray(pathPreview, entity, this.posCache)) {
                // If the entity is shown as following its path currently, use the path preview position.
                positionData = this.posCache;
            } else {
                // Otherwise, use the true entity position.
                positionData = entity.getPositionAndRotation(this.posCache);
            }
        }

        float pitch = positionData != null ? positionData[3] : 0;
        float yaw = positionData != null ? positionData[4] : 0;
        float roll = positionData != null ? positionData[5] : 0;

        int foundRotations = 0;
        for (Transform transform : entityMeshView.getTransforms()) { // Update existing rotations.
            if (!(transform instanceof Rotate))
                continue;

            foundRotations++;
            Rotate rotate = (Rotate) transform;
            if (rotate.getAxis() == Rotate.X_AXIS) {
                rotate.setAngle(Math.toDegrees(pitch));
            } else if (rotate.getAxis() == Rotate.Y_AXIS) {
                rotate.setAngle(Math.toDegrees(yaw));
            } else if (rotate.getAxis() == Rotate.Z_AXIS) {
                rotate.setAngle(Math.toDegrees(roll));
            } else {
                foundRotations--;
            }
        }

        if (foundRotations == 0) { // There are no rotations, so add rotations.
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(roll), Rotate.Z_AXIS));
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(yaw), Rotate.Y_AXIS));
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(pitch), Rotate.X_AXIS));
        }

        entityMeshView.setTranslateX(positionData != null ? positionData[0] : 0);
        entityMeshView.setTranslateY(positionData != null ? positionData[1] : 0);
        entityMeshView.setTranslateZ(positionData != null ? positionData[2] : 0);
    }

    private void tickPickupAnimations(MeshViewFixedFrameRateTimerTask<?> timerTask) {
        this.pickupAnimationFrameCounter += timerTask.getDeltaFrames();

        // Update all pickups to show updated animation states.
        List<FroggerMapEntity> entities = getValues();
        for (int i = 0; i < entities.size(); i++) {
            FroggerMapEntity entity = entities.get(i);
            FroggerFlyScoreType flyScoreType = entity.getFlyScoreType();
            if (flyScoreType != null)
                updateEntityMesh(entity);
        }
    }

    private void setupUnlitEntities() {
        // Update all the unlit entities.
        List<FroggerMapEntity> entities = getValues();
        for (int i = 0; i < entities.size(); i++) {
            FroggerMapEntity entity = entities.get(i);
            MeshView entityMeshView = getDelegatesByValue().get(entity);
            if (entityMeshView != null && !this.litEntityRenderList.contains(entityMeshView))
                updateEntityMesh(entity, entityMeshView);
        }
    }
}