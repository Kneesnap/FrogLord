package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.config.exe.PickupData;
import net.highwayfrogs.editor.file.config.exe.PickupData.PickupAnimationFrame;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerCentralUIManager.FroggerCentralMapListManager;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.MeshViewFrameTimer.MeshViewFixedFrameRateTimerTask;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages entities in a Frogger map.
 * Created by Kneesnap on 6/1/2024.
 */
public class FroggerUIMapEntityManager extends FroggerCentralMapListManager<FroggerMapEntity, MeshView> {
    private final float[] posCache = new float[6];
    private final Map<MOFHolder, MOFMesh> meshCache = new HashMap<>();
    @Getter private DisplayList litEntityRenderList;
    @Getter private DisplayList unlitEntityRenderList;
    private MeshViewFixedFrameRateTimerTask<FroggerUIMapEntityManager> pickupAnimationUpdateTask;
    private int pickupAnimationFrameCounter;

    public static final float ENTITY_PLACEHOLDER_SPRITE_SIZE = 16F;
    public static final TriangleMesh ENTITY_PLACEHOLDER_SPRITE_MESH = Scene3DUtils.createSpriteMesh(ENTITY_PLACEHOLDER_SPRITE_SIZE);
    public static final PhongMaterial ENTITY_PLACEHOLDER_SPRITE_MATERIAL = Utils.makeUnlitSharpMaterial(ImageResource.SQUARE_LETTER_E_128.getFxImage());
    public static final PhongMaterial ENTITY_HIGHLIGHTED_PLACEHOLDER_SPRITE_MATERIAL = Scene3DUtils.updateHighlightMaterial(null, ImageResource.SQUARE_LETTER_E_128.getAwtImage());

    public FroggerUIMapEntityManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        // The map renders after entities (except for sprite entities) because transparent entities are exceedingly rare in Frogger (I'm not even sure there's a single one in the retail builds).
        // Situations such as transparent water layers should show the entities under the water too.
        this.litEntityRenderList = getRenderManager().createDisplayListWithNewGroup();
        super.onSetup();
        setPickupAnimationsVisible(true);
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

    @Override
    protected void setVisible(FroggerMapEntity oldEntity, MeshView meshView, boolean visible) {
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
        return new FroggerMapEntity(getMap());
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
        }
    }

    /**
     * Update the mesh displayed for the given entity.
     * @param entity The entity to update the mesh for.
     */
    public void updateEntityMesh(FroggerMapEntity entity) {
        MeshView entityMesh = getDelegatesByValue().get(entity);
        if (entityMesh != null)
            updateEntityMesh(entity, entityMesh);
    }

    private void updateEntityMesh(FroggerMapEntity entity, MeshView entityMeshView) {
        boolean isEntitySelected = (getSelectedValue() == entity);

        // Reset entity scaling.
        Scene3DUtils.setNodeScale(entityMeshView, 1D, 1D, 1D);

        // Attempt to apply from 3D model.
        IFroggerFormEntry formEntry = entity.getFormEntry();
        WADEntry modelEntry = entity.getEntityModel();
        if (modelEntry != null && modelEntry.getFile() instanceof MOFHolder) {
            MOFHolder holder = ((MOFHolder) modelEntry.getFile()).getOverride();

            // Set VLO archive to the map VLO if currently unset.
            VLOArchive vlo = getMap().getConfig().getForcedVLO(getMap().getGameInstance(), modelEntry.getDisplayName());
            if (vlo == null) {
                ThemeBook themeBook = getMap().getGameInstance().getThemeBook(formEntry.getTheme());
                if (themeBook != null)
                    vlo = themeBook.getVLO(getMap());
            }
            holder.setVloFile(vlo);

            // Update MeshView.
            MOFMesh modelMesh = this.meshCache.computeIfAbsent(holder, MOFHolder::makeMofMesh);
            if (modelMesh.getFaceCount() > 0) {
                DynamicMesh.tryRemoveMesh(entityMeshView);
                entityMeshView.setMesh(modelMesh);
                // TODO: Future: Register mesh properly once we redo MOF support to use the new system. (DynamicMesh.addMeshView)

                // Update entity display material and such.
                TextureMap textureSheet = modelMesh.getTextureMap();
                if (isEntitySelected) {
                    entityMeshView.setMaterial(textureSheet.getDiffuseHighlightedMaterial());
                    registerUnlitEntity(entityMeshView);
                } else {
                    entityMeshView.setMaterial(textureSheet.getDiffuseMaterial());
                    registerLitEntity(entityMeshView);
                }

                return;
            }
        }

        // Unregister the attached mesh as we're about to potentially
        DynamicMesh.tryRemoveMesh(entityMeshView);

        // Attempt to use a fly sprite.
        FroggerGameInstance config = getMap().getGameInstance();
        PickupData pickupData = config.getPickupData(entity.getFlyScoreType());
        if (pickupData != null && pickupData.getFrames().size() > 0) {
            PickupAnimationFrame pickupAnimationFrame = pickupData.getFrames().get(Math.max(0, this.pickupAnimationFrameCounter) % pickupData.getFrames().size());
            if (pickupAnimationFrame != null && pickupAnimationFrame.applyToMeshView(entityMeshView, isEntitySelected)) {
                registerUnlitEntity(entityMeshView);
                return;
            }
        }

        // Setup entity placeholder sprite mesh.
        entityMeshView.setMesh(ENTITY_PLACEHOLDER_SPRITE_MESH);
        entityMeshView.setMaterial(isEntitySelected ? ENTITY_HIGHLIGHTED_PLACEHOLDER_SPRITE_MATERIAL : ENTITY_PLACEHOLDER_SPRITE_MATERIAL);
        registerUnlitEntity(entityMeshView);
    }

    private void registerUnlitEntity(MeshView meshView) {
        if (this.unlitEntityRenderList == null || this.unlitEntityRenderList.addIfMissing(meshView))
            this.litEntityRenderList.remove(meshView);
    }

    private void registerLitEntity(MeshView meshView) {
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

        float[] positionData = entity.getPositionAndRotation(this.posCache);

        float roll = positionData != null ? positionData[3] : 0;
        float pitch = positionData != null ? positionData[4] : 0;
        float yaw = positionData != null ? positionData[5] : 0;

        int foundRotations = 0;
        for (Transform transform : entityMeshView.getTransforms()) { // Update existing rotations.
            if (!(transform instanceof Rotate))
                continue;

            foundRotations++;
            Rotate rotate = (Rotate) transform;
            if (rotate.getAxis() == Rotate.X_AXIS) {
                rotate.setAngle(Math.toDegrees(roll));
            } else if (rotate.getAxis() == Rotate.Y_AXIS) {
                rotate.setAngle(Math.toDegrees(pitch));
            } else if (rotate.getAxis() == Rotate.Z_AXIS) {
                rotate.setAngle(Math.toDegrees(yaw));
            } else {
                foundRotations--;
            }
        }

        if (foundRotations == 0) { // There are no rotations, so add rotations.
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(yaw), Rotate.Z_AXIS));
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(pitch), Rotate.Y_AXIS));
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(roll), Rotate.X_AXIS));
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

    // TODO: Here's some old code for adding new entities. We should learn from this when improving the editor to actually have a new entity.
    /*private void addNewEntity(IFroggerFormEntry entry) {
        FroggerMapEntity newEntity = new FroggerMapEntity(getMap(), entry);

        if (newEntity.getMatrixInfo() != null) { // Lets you select a polygon to place the new entity on.
            for (FroggerGridStack stack : getMap().getGridStacks())
                for (FroggerGridSquare square : stack.getGridSquares())
                    if (square.getPolygon() != null)
                        getController().renderOverPolygon(square.getPolygon(), MapMesh.GENERAL_SELECTION);
            MeshData data = getMesh().getManager().addMesh();

            getController().getGeometryManager().selectPolygon(poly -> {
                getMesh().getManager().removeMesh(data);

                // Set entity position to the clicked polygon.
                PSXMatrix matrix = newEntity.getMatrixInfo();

                SVector pos = MAPPolygon.getCenterOfPolygon(getMesh(), poly);
                matrix.getTransform()[0] = Utils.floatToFixedPointInt4Bit(pos.getFloatX());
                matrix.getTransform()[1] = Utils.floatToFixedPointInt4Bit(pos.getFloatY());
                matrix.getTransform()[2] = Utils.floatToFixedPointInt4Bit(pos.getFloatZ());

                // Add entity.
                addEntityToMap(newEntity);
            }, () -> getMesh().getManager().removeMesh(data));
            return;
        }

        if (newEntity.getPathState() != null) {
            if (getMap().getPathPacket().getPaths().isEmpty()) {
                Utils.makePopUp("Path entities cannot be added if there are no paths present! Add a path.", AlertType.WARNING);
                return;
            }

            // User selects the path.
            getController().getPathManager().getPathSelector().promptPath((path, segment, segDistance) -> {
                newEntity.getPathState().setPath(path, segment);
                newEntity.getPathState().setSegmentDistance(segDistance);
                newEntity.getPathState().setSpeed(10); // Default speed.
                addEntityToMap(newEntity);
            }, null);
            return;
        }

        addEntityToMap(newEntity);
    }

    private void addEntityToMap(FroggerMapEntity entity) {
        if (entity.getUniqueId() == -1) { // Default entity id, update it to something new.
            boolean isPath = entity.getMatrixInfo() != null;

            // Use the largest entity id + 1.
            for (FroggerMapEntity tempEntity : getEntities())
                if (tempEntity.getUniqueId() >= entity.getUniqueId() && (isPath == (tempEntity.getMatrixInfo() != null)))
                    entity.setUniqueId(tempEntity.getUniqueId() + 1);
        }

        if (entity.getFormGridId() == -1) { // Default form id, make it something.
            int[] formCounts = new int[getForms().size()];
            for (FroggerMapEntity testEntity : getEntities())
                if (testEntity.getFormEntry() == entity.getFormEntry())
                    formCounts[testEntity.getFormGridId()]++;

            int maxCount = -1;
            for (int i = 0; i < formCounts.length; i++) {
                if (formCounts[i] > maxCount) {
                    maxCount = formCounts[i];
                    entity.setFormGridId(i);
                }
            }
        }

        getEntities().add(entity);
        showEntityInfo(entity);
        updateEntities();
    }*/
}