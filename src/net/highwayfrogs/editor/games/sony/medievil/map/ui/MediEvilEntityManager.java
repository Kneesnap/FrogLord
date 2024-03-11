package net.highwayfrogs.editor.games.sony.medievil.map.ui;

import javafx.scene.layout.VBox;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.entity.MediEvilMapEntity;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the display of entities in old Frogger maps.
 * Created by Kneesnap on 12/12/2023.
 */
public class MediEvilEntityManager extends MediEvilMapUIManager.MediEvilMapListManager<MediEvilMapEntity, MeshView> {
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
    protected void setupMainGridEditor(VBox editorBox) {
        super.setupMainGridEditor(editorBox);
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
        newView.setOnMouseClicked(evt -> getValueSelectionBox().getSelectionModel().select(entity));
        return newView;
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
            entityMesh.setMaterial(modelMesh.getTextureMap().getDiffuseMaterial());
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
     * @param entity         The entity to get positional data from.
     * @param entityMeshView The mesh view to update position for.
     */
    public void updateEntityPositionRotation(MediEvilMapEntity entity, MeshView entityMeshView) {
        if (entity == null || entityMeshView == null)
            return; // No data to update position from.

        SVector positionData = entity.getInitialPosition();

        entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(entity.getRotationZ()), Rotate.Z_AXIS)); // TODO: Check this is correct.
        entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(entity.getRotationY()), Rotate.Y_AXIS));
        entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(entity.getRotationX()), Rotate.X_AXIS));

        entityMeshView.setTranslateX(positionData != null ? positionData.getX() / 16.0: 0);
        entityMeshView.setTranslateY(positionData != null ? positionData.getY() / 16.0 : 0);
        entityMeshView.setTranslateZ(positionData != null ? positionData.getZ() / 16.0: 0);
}


    @Override
    protected void updateEditor(MediEvilMapEntity entity) {
        /*entity.setupEditor(this, getEditorGrid());*/
    }

    @Override
    protected void onDelegateRemoved(MediEvilMapEntity removedEntity, MeshView oldMeshView) {
        if (oldMeshView != null)
            getRenderManager().getRoot().getChildren().remove(oldMeshView);
    }

    @Override
    protected void setVisible(MediEvilMapEntity medievilMapEntity, MeshView meshView, boolean visible) {
        meshView.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(MediEvilMapEntity oldEntity, MeshView oldMeshView, MediEvilMapEntity newEntity, MeshView newMeshView) {
        // TODO: Maybe highlight?
    }

    @Override
    protected MediEvilMapEntity createNewValue() {
        return new MediEvilMapEntity(getMap());
    }
}