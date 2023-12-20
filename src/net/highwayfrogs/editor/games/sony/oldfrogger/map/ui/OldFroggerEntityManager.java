package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.AmbientLight;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig.OldFroggerFormConfigEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapForm;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the display of entities in old Frogger maps.
 * Created by Kneesnap on 12/12/2023.
 */
public class OldFroggerEntityManager extends OldFroggerMapListManager<OldFroggerMapEntity, MeshView> {
    private final float[] posCache = new float[6];
    private final Map<MOFHolder, MOFMesh> meshCache = new HashMap<>();
    private DisplayList entityDisplayList;
    private AmbientLight mainLight;

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
    public void onSetup() {
        this.entityDisplayList = getRenderManager().createDisplayList();

        // Temporary lighting for entities. TODO: Hm.
        this.mainLight = new AmbientLight(Color.WHITE);
        this.entityDisplayList.add(this.mainLight);

        super.onSetup();
    }

    @Override
    protected void setupMainGridEditor(VBox editorBox) {
        super.setupMainGridEditor(editorBox);
        getShowValuesCheckBox().selectedProperty().set(true);
    }

    @Override
    protected MeshView setupDisplay(OldFroggerMapEntity entity) {
        MeshView newView = new MeshView();
        newView.setCullFace(CullFace.NONE);
        newView.setDrawMode(DrawMode.FILL);
        updateEntityMesh(entity, newView);
        updateEntityPositionRotation(entity, newView);
        this.mainLight.getScope().add(newView);
        this.entityDisplayList.add(newView);

        newView.setOnMouseClicked(evt -> getValueSelectionBox().getSelectionModel().select(entity));
        return newView;
    }

    private void updateEntityMesh(OldFroggerMapEntity entity, MeshView entityMesh) {
        OldFroggerMapForm form = entity.getForm();
        MOFHolder holder = form.getMofFile();
        if (holder != null) {

            // Set VLO archive to the map VLO if currently unset.
            if (holder.getVloFile() == null) {
                OldFroggerLevelTableEntry levelTableEntry = entity.getMap().getLevelTableEntry();
                if (levelTableEntry != null)
                    holder.setVloFile(levelTableEntry.getMainVLOArchive());
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


    @Override
    protected void updateEditor(OldFroggerMapEntity entity) {
        getEditorGrid().addLabel("Form ID ", String.valueOf(entity.getFormTypeId()));
        getEditorGrid().addLabel("Entity ID", String.valueOf(entity.getEntityId()));

        OldFroggerMapForm form = entity.getForm();
        if (form != null && form.getMofFile() != null)
            getEditorGrid().addLabel("MOF / Model", form.getMofFile().getFileDisplayName());

        OldFroggerFormConfig formConfig = getMap().getFormConfig();
        OldFroggerFormConfigEntry formConfigEntry = formConfig != null ? formConfig.getFormByType(entity.getFormTypeId()) : null;
        if (formConfigEntry != null)
            getEditorGrid().addLabel("Form Name", formConfigEntry.getDisplayName());

        getEditorGrid().addLabel("Difficulty", Utils.toHexString(entity.getDifficulty()));

        if (entity.getEntityData() != null) {
            getEditorGrid().addSeparator();
            try {
                entity.getEntityData().setupEditor(this, getEditorGrid());
            } catch (Throwable th) {
                getEditorGrid().addNormalLabel("Encountered an error setting up the editor.");
                th.printStackTrace();
            }
        }

        // TODO: The "Remove" button likely belongs next to the "Add" button. Consider centering them.

        // TODO: Cleanup this mess.
    }

    @Override
    protected void onDelegateRemoved(OldFroggerMapEntity removedEntity, MeshView oldMeshView) {
        if (oldMeshView != null) {
            this.entityDisplayList.remove(oldMeshView);
            this.mainLight.getScope().remove(oldMeshView);
        }
    }

    @Override
    protected void setValuesVisible(boolean valuesVisible) {
        this.entityDisplayList.setVisible(valuesVisible);
    }

    @Override
    protected void onSelectedValueChange(OldFroggerMapEntity oldEntity, MeshView oldMeshView, OldFroggerMapEntity newEntity, MeshView newMeshView) {
        // TODO: Maybe?
    }

    @Override
    protected OldFroggerMapEntity createNewValue() {
        return new OldFroggerMapEntity(getMap());
    }
}