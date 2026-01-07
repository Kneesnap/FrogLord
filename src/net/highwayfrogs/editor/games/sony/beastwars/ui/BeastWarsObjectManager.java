package net.highwayfrogs.editor.games.sony.beastwars.ui;

import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.beastwars.map.data.BeastWarsMapObject;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapMesh;
import net.highwayfrogs.editor.games.sony.beastwars.ui.BeastWarsMapUIManager.BeastWarsMapListManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the objects (entities) for a given map.
 * Created by Kneesnap on 3/5/2025.
 */
public class BeastWarsObjectManager extends BeastWarsMapListManager<BeastWarsMapObject, MeshView> {
    private final Map<MRModel, MRModelMesh> meshCache = new HashMap<>();

    public BeastWarsObjectManager(MeshViewController<BeastWarsMapMesh> controller) {
        super(controller);
    }

    @Override
    public String getTitle() {
        return "Objects";
    }

    @Override
    public String getValueName() {
        return "Object";
    }

    @Override
    public List<BeastWarsMapObject> getValues() {
        return getMap().getObjects();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);
    }

    @Override
    protected MeshView setupDisplay(BeastWarsMapObject object) {
        MeshView newView = new MeshView();
        newView.setCullFace(CullFace.BACK);
        newView.setDrawMode(DrawMode.FILL);
        updateObjectMesh(object, newView);
        updateObjectPositionRotation(object, newView);
        getRenderManager().getRoot().getChildren().add(newView);
        getController().getLightingGroup().getChildren().add(newView);

        newView.setOnMouseClicked(evt -> handleClick(evt, object));
        return newView;
    }

    /**
     * Update the mesh displayed for the given object.
     * @param object The object to update the mesh for.
     */
    public void updateObjectMesh(BeastWarsMapObject object) {
        MeshView objectMesh = getDelegatesByValue().get(object);
        if (objectMesh != null)
            updateObjectMesh(object, objectMesh);
    }

    private void updateObjectMesh(BeastWarsMapObject object, MeshView objectMesh) {
        DynamicMesh.tryRemoveMesh(objectMesh);

        MRModel model = object.getModel();
        if (model != null) {
            // Update MeshView.
            MRModelMesh modelMesh = this.meshCache.computeIfAbsent(model, MRModel::createMeshWithDefaultAnimation);
            modelMesh.addView(objectMesh, getController().getMeshTracker(), getSelectedValue() == object, true);
            objectMesh.setCullFace(CullFace.BACK);
            return;
        }

        // NOTE: Maybe this could be a single tri mesh, local to this manager, and we just update its points in updateEntities().
        TriangleMesh triMesh = Scene3DUtils.createSpriteMesh(FroggerUIMapEntityManager.ENTITY_PLACEHOLDER_SPRITE_SIZE);
        objectMesh.setCullFace(CullFace.NONE);
        objectMesh.setMesh(triMesh);
        objectMesh.setMaterial(FroggerUIMapEntityManager.ENTITY_PLACEHOLDER_SPRITE_MATERIAL);
    }

    /**
     * Updates the displayed position / rotation of the object.
     * @param object The object to get positional data from.
     */
    public void updateObjectPositionRotation(BeastWarsMapObject object) {
        updateObjectPositionRotation(object, getDelegatesByValue().get(object));
    }

    /**
     * Updates the displayed position / rotation of the object.
     * @param object         The object to get positional data from.
     * @param entityMeshView The mesh view to update position for.
     */
    public void updateObjectPositionRotation(BeastWarsMapObject object, MeshView entityMeshView) {
        if (object == null || entityMeshView == null)
            return; // No data to update position from.

        SVector rotation = object.getRotation();
        float roll = (float) (2 * Math.PI * rotation.getFloatX(12));
        float pitch = (float) (2 * Math.PI * rotation.getFloatY(12));
        float yaw = (float) (2 * Math.PI * rotation.getFloatZ(12));

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
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(roll), Rotate.X_AXIS));
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(pitch), Rotate.Y_AXIS));
            entityMeshView.getTransforms().add(new Rotate(Math.toDegrees(yaw), Rotate.Z_AXIS));
        }

        entityMeshView.setTranslateX(object.getWorldPositionX());
        entityMeshView.setTranslateY(object.getWorldPositionY());
        entityMeshView.setTranslateZ(object.getWorldPositionZ());
        SVector scale = object.getScale();
        entityMeshView.setScaleX(scale.getFloatX(12));
        entityMeshView.setScaleY(scale.getFloatY(12));
        entityMeshView.setScaleZ(scale.getFloatZ(12));
    }

    @Override
    protected void updateEditor(BeastWarsMapObject object) {
        object.setupEditor(getEditorGrid(), this);
    }

    @Override
    protected void setVisible(BeastWarsMapObject object, MeshView meshView, boolean visible) {
        meshView.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(BeastWarsMapObject oldObject, MeshView oldMeshView, BeastWarsMapObject newObject, MeshView newMeshView) {
        if (oldObject != null && oldMeshView != null)
            updateObjectMesh(oldObject, oldMeshView); // Restore original material.
        if (newObject != null && newMeshView != null)
            updateObjectMesh(newObject, newMeshView); // Apply new highlight material.
    }

    @Override
    protected BeastWarsMapObject createNewValue() {
        return new BeastWarsMapObject(getMap());
    }

    @Override
    protected void onDelegateRemoved(BeastWarsMapObject object, MeshView meshView) {
        if (meshView != null) {
            getRenderManager().getRoot().getChildren().remove(meshView);
            getController().getLightingGroup().getChildren().remove(meshView);
        }
    }
}
