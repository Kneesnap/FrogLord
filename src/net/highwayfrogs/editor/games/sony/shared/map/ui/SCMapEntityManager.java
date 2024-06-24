package net.highwayfrogs.editor.games.sony.shared.map.ui;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.data.SCMapEntity;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMesh;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.map.ui.SCMapUIManager.SCMapListManager;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages entities in 3D space.
 * TODO: Sync with the level section system to always show entities based on which section(s) are visible.
 * Created by Kneesnap on 5/14/2024.
 */
public class SCMapEntityManager<TMapMesh extends SCMapMesh> extends SCMapListManager<TMapMesh, SCMapEntity, MeshView> {
    private final Map<MOFHolder, MOFMesh> meshCache = new HashMap<>();
    public SCMapEntityManager(SCMapMeshController<TMapMesh> controller) {
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
    public List<SCMapEntity> getValues() {
        if (getMap().getEntityPacket() != null && getMap().getEntityPacket().isActive())
            return getMap().getEntityPacket().getEntities();

        SCMapFile<? extends SCGameInstance> parentMap = getMap().getParentMap();
        if (parentMap != null && parentMap.getEntityPacket() != null && parentMap.getEntityPacket().isActive())
            return parentMap.getEntityPacket().getEntities();

        return Collections.emptyList();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);
    }

    @Override
    protected MeshView setupDisplay(SCMapEntity entity) {
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
    public void updateEntityMesh(SCMapEntity entity) {
        MeshView entityMesh = getDelegatesByValue().get(entity);
        if (entityMesh != null)
            updateEntityMesh(entity, entityMesh);
    }

    private void updateEntityMesh(SCMapEntity entity, MeshView entityMesh) {
        /*MOFHolder holder = entity.getMof();
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
        }*/ // TODO: Get 3D models working.

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
    public void updateEntityPositionRotation(SCMapEntity entity) {
        updateEntityPositionRotation(entity, getDelegatesByValue().get(entity));
    }

    /**
     * Updates the displayed position / rotation of the entity.
     * @param entity         The entity to get positional data from.
     * @param entityMeshView The mesh view to update position for.
     */
    public void updateEntityPositionRotation(SCMapEntity entity, MeshView entityMeshView) {
        if (entity == null || entityMeshView == null)
            return; // No data to update position from.


        float roll = (float) entity.getRotationXInRadians();
        float pitch = (float) entity.getRotationYInRadians();
        float yaw = (float) entity.getRotationZInRadians();

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
    protected void updateEditor(SCMapEntity entity) {
        entity.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void onDelegateRemoved(SCMapEntity removedEntity, MeshView oldMeshView) {
        if (oldMeshView != null) {
            getRenderManager().getRoot().getChildren().remove(oldMeshView);
            getController().getLightingGroup().getChildren().add(oldMeshView);
        }
    }

    @Override
    protected void setVisible(SCMapEntity medievilMapEntity, MeshView meshView, boolean visible) {
        meshView.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(SCMapEntity oldEntity, MeshView oldMeshView, SCMapEntity newEntity, MeshView newMeshView) {
        if (oldEntity != null && oldMeshView != null)
            updateEntityMesh(oldEntity, oldMeshView); // Restore original material.
        if (newEntity != null && newMeshView != null)
            updateEntityMesh(newEntity, newMeshView); // Apply new highlight material.
    }

    @Override
    protected SCMapEntity createNewValue() {
        return new SCMapEntity(getMap());
    }
}