package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapForm;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the display of entities in old Frogger maps.
 * Created by Kneesnap on 12/12/2023.
 */
public class OldFroggerEntityManager extends OldFroggerMapListManager<OldFroggerMapEntity, MeshView> {
    private final float[] posCache = new float[6];
    private final Map<MRModel, MRModelMesh> meshCache = new HashMap<>();

    public OldFroggerEntityManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
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
    public List<OldFroggerMapEntity> getValues() {
        return getMap().getEntityMarkerPacket().getEntities();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);
    }

    @Override
    protected MeshView setupDisplay(OldFroggerMapEntity entity) {
        MeshView newView = new MeshView();
        newView.setCullFace(CullFace.NONE);
        newView.setDrawMode(DrawMode.FILL);
        updateEntityMesh(entity, newView);
        updateEntityPositionRotation(entity, newView);
        getController().getLightManager().getLightingGroup().getChildren().add(newView);

        newView.setOnMouseClicked(evt -> handleClick(evt, entity));
        return newView;
    }

    /**
     * Update the mesh displayed for the given entity.
     * @param entity The entity to update the mesh for.
     */
    public void updateEntityMesh(OldFroggerMapEntity entity) {
        MeshView entityMesh = getDelegatesByValue().get(entity);
        if (entityMesh != null)
            updateEntityMesh(entity, entityMesh);
    }

    private void updateEntityMesh(OldFroggerMapEntity entity, MeshView entityMesh) {
        OldFroggerMapForm form = entity.getForm();
        MRModel model = form.getModel();
        if (model != null) {
            DynamicMesh.tryRemoveMesh(entityMesh);

            // Update MeshView.
            MRModelMesh modelMesh = this.meshCache.computeIfAbsent(model, MRModel::createMeshWithDefaultAnimation);
            modelMesh.addView(entityMesh, getController().getMeshTracker(), (getSelectedValue() == entity), true);
            entityMesh.setCullFace(CullFace.BACK);
            return;
        }

        // Couldn't find a model to use, so instead we'll display as a 2D sprite.
        float entityIconSize = FroggerUIMapEntityManager.ENTITY_PLACEHOLDER_SPRITE_SIZE;

        // Attempt to apply 2d textures, instead of the default texture.
        PhongMaterial material = FroggerUIMapEntityManager.ENTITY_PLACEHOLDER_SPRITE_MATERIAL;

        // NOTE: Maybe this could be a single tri mesh, local to this manager, and we just update its points in updateEntities().
        TriangleMesh triMesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        triMesh.getPoints().addAll(-entityIconSize * 0.5f, entityIconSize * 0.5f, 0, -entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, entityIconSize * 0.5f, 0);
        triMesh.getTexCoords().addAll(0, 1, 0, 0, 1, 0, 1, 1);
        triMesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 0, 0);

        // Update mesh.
        entityMesh.setMesh(triMesh);
        entityMesh.setMaterial(material);
    }

    /**
     * Updates the displayed position / rotation of the entity.
     * @param entity The entity to get positional data from.
     */
    public void updateEntityPositionRotation(OldFroggerMapEntity entity) {
        updateEntityPositionRotation(entity, getDelegatesByValue().get(entity));
    }

    /**
     * Updates the displayed position / rotation of the entity.
     * @param entity         The entity to get positional data from.
     * @param entityMeshView The mesh view to update position for.
     */
    public void updateEntityPositionRotation(OldFroggerMapEntity entity, MeshView entityMeshView) {
        if (entity == null || entity.getEntityData() == null || entityMeshView == null)
            return; // No data to update position from.

        float[] positionData = entity.getEntityData().getPosition(this.posCache);

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


    @Override
    protected void updateEditor(OldFroggerMapEntity entity) {
        entity.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void onDelegateRemoved(OldFroggerMapEntity removedEntity, MeshView oldMeshView) {
        if (oldMeshView != null)
            getController().getLightManager().getLightingGroup().getChildren().remove(oldMeshView);
    }

    @Override
    protected void setVisible(OldFroggerMapEntity oldFroggerMapEntity, MeshView meshView, boolean visible) {
        if (meshView != null)
            meshView.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(OldFroggerMapEntity oldEntity, MeshView oldMeshView, OldFroggerMapEntity newEntity, MeshView newMeshView) {
        if (oldEntity != null && oldMeshView != null)
            updateEntityMesh(oldEntity, oldMeshView); // Restore original material.
        if (newEntity != null && newMeshView != null)
            updateEntityMesh(newEntity, newMeshView); // Apply new highlight material.
    }

    @Override
    protected OldFroggerMapEntity createNewValue() {
        return new OldFroggerMapEntity(getMap());
    }
}