package net.highwayfrogs.editor.games.sony.medievil.map.ui;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.entity.MediEvilMapEntity;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.ui.MediEvilMapUIManager.MediEvilMapListManager;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the display of entities in old Frogger maps.
 * Created by Kneesnap on 12/12/2023.
 */
public class MediEvilEntityManager extends MediEvilMapListManager<MediEvilMapEntity, MeshView> {
    private final Map<MOFHolder, MOFMesh> meshCache = new HashMap<>();
    public MediEvilEntityManager(MeshViewController<MediEvilMapMesh> controller) {
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
    public List<MediEvilMapEntity> getValues() {
        return getMap().getEntitiesPacket().getEntities();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);
    }

    @Override
    protected MeshView setupDisplay(MediEvilMapEntity entity) {
        MeshView newView = new MeshView();
        newView.setCullFace(CullFace.NONE);
        newView.setDrawMode(DrawMode.FILL);
        updateEntityMesh(entity, newView);
        updateEntityPositionRotation(entity, newView);
        getRenderManager().getRoot().getChildren().add(newView);
        getController().getLightingGroup().getChildren().add(newView);

        newView.setOnMouseClicked(evt -> handleClick(evt, entity));
        return newView;
    }

    /**
     * Update the mesh displayed for the given entity.
     * @param entity The entity to update the mesh for.
     */
    public void updateEntityMesh(MediEvilMapEntity entity) {
        MeshView entityMesh = getDelegatesByValue().get(entity);
        if (entityMesh != null)
            updateEntityMesh(entity, entityMesh);
    }

    private void updateEntityMesh(MediEvilMapEntity entity, MeshView entityMesh) {
        MOFHolder holder = entity.getMof();
        if (holder != null) {

            // Set VLO archive to the map VLO if currently unset.
            if (holder.getVloFile() == null) {
                MediEvilLevelTableEntry levelTableEntry = entity.getMap().getLevelTableEntry();
                if (levelTableEntry != null)
                    holder.setVloFile(levelTableEntry.getVloFile());
            }

            // Update MeshView.
            MOFMesh modelMesh = this.meshCache.computeIfAbsent(holder, MOFHolder::makeMofMesh);
            entityMesh.setMesh(modelMesh);

            // Update material.
            TextureMap textureSheet = modelMesh.getTextureMap();
            entityMesh.setMaterial((getSelectedValue() == entity) ? textureSheet.getDiffuseHighlightedMaterial() : textureSheet.getDiffuseMaterial());
            return;
        }

        // Couldn't find a model to use, so instead we'll display as a 2D sprite.
        float entityIconSize = MapUIController.ENTITY_ICON_SIZE;

        // Attempt to apply 2d textures, instead of the default texture.
        PhongMaterial material = EntityManager.MATERIAL_ENTITY_ICON;

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
    public void updateEntityPositionRotation(MediEvilMapEntity entity) {
        updateEntityPositionRotation(entity, getDelegatesByValue().get(entity));
    }

    /**
     * Updates the displayed position / rotation of the entity.
     * @param entity         The entity to get positional data from.
     * @param entityMeshView The mesh view to update position for.
     */
    public void updateEntityPositionRotation(MediEvilMapEntity entity, MeshView entityMeshView) {
        if (entity == null || entityMeshView == null)
            return; // No data to update position from.

        float roll = (float) (2 * Math.PI * (entity.getRotationX() / 256F));
        float pitch = (float) (2 * Math.PI * (entity.getRotationY() / 256F));
        float yaw = (float) (2 * Math.PI * (entity.getRotationZ() / 256F));

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

        SVector position = entity.getPosition();
        entityMeshView.setTranslateX(Utils.fixedPointIntToFloat4Bit(position.getX()));
        entityMeshView.setTranslateY(Utils.fixedPointIntToFloat4Bit(position.getY()));
        entityMeshView.setTranslateZ(Utils.fixedPointIntToFloat4Bit(position.getZ()));
    }


    @Override
    protected void updateEditor(MediEvilMapEntity entity) {
        entity.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void onDelegateRemoved(MediEvilMapEntity removedEntity, MeshView oldMeshView) {
        if (oldMeshView != null) {
            getRenderManager().getRoot().getChildren().remove(oldMeshView);
            getController().getLightingGroup().getChildren().add(oldMeshView);
        }
    }

    @Override
    protected void setVisible(MediEvilMapEntity medievilMapEntity, MeshView meshView, boolean visible) {
        meshView.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(MediEvilMapEntity oldEntity, MeshView oldMeshView, MediEvilMapEntity newEntity, MeshView newMeshView) {
        if (oldEntity != null && oldMeshView != null)
            updateEntityMesh(oldEntity, oldMeshView); // Restore original material.
        if (newEntity != null && newMeshView != null)
            updateEntityMesh(newEntity, newMeshView); // Apply new highlight material.
    }

    @Override
    protected MediEvilMapEntity createNewValue() {
        return new MediEvilMapEntity(getMap());
    }
}