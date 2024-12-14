package net.highwayfrogs.editor.games.sony.shared.map.ui;

import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.games.sony.medievil2.IMediEvil2LevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2LevelDefinition;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2LevelDefinition.MediEvil2LevelSectionDefinition;
import net.highwayfrogs.editor.games.sony.medievil2.map.MediEvil2Map;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMesh;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.map.ui.SCMapUIManager.SCMapListManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;

import java.util.Collections;
import java.util.List;

/**
 * Allows choosing if sections should display.
 * TODO: Generalize this to work with C-12 too.
 * Created by Kneesnap on 5/14/2024.
 */
public class SCMapSectionManager<TMapMesh extends SCMapMesh> extends SCMapListManager<TMapMesh, MediEvil2LevelSectionDefinition, MeshView> {
    public SCMapSectionManager(SCMapMeshController<TMapMesh> controller) {
        super(controller);
        this.disableRemoveButton = true;
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getAddValueButton().setDisable(true);
    }

    @Override
    public String getTitle() {
        return "Level Sections";
    }

    @Override
    public String getValueName() {
        return "Level Section";
    }

    @Override
    public MediEvil2Map getMap() {
        return (MediEvil2Map) super.getMap();
    }

    @Override
    public List<MediEvil2LevelSectionDefinition> getValues() {
        IMediEvil2LevelTableEntry levelTableEntry = getMap().getLevelTableEntry();
        if (levelTableEntry == null)
            return Collections.emptyList();

        MediEvil2LevelDefinition levelDefinition = levelTableEntry.getLevelDefinition();
        return levelDefinition.getLevelSections();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        getValueDisplaySetting().setValue(ListDisplayType.ALL);
    }

    @Override
    protected MeshView setupDisplay(MediEvil2LevelSectionDefinition sectionDef) {
        MeshView newView = new MeshView();
        newView.setCullFace(CullFace.BACK);
        newView.setDrawMode(DrawMode.FILL);
        MeshViewController.bindMeshSceneControls(getController(), newView);
        SCMapMesh mapMesh = new SCMapMesh(sectionDef.getMapFile());
        mapMesh.addView(newView);
        updateEntityPositionRotation(sectionDef, newView);
        getRenderManager().getRoot().getChildren().add(newView);
        getController().getLightingGroup().getChildren().add(newView);

        newView.setOnMouseClicked(evt -> handleClick(evt, sectionDef));
        return newView;
    }

    /**
     * Updates the displayed position / rotation of the level section.
     * @param sectionDef The level section to get positional data from.
     */
    public void updateEntityPositionRotation(MediEvil2LevelSectionDefinition sectionDef) {
        updateEntityPositionRotation(sectionDef, getDelegatesByValue().get(sectionDef));
    }

    /**
     * Updates the displayed position / rotation of the level section.
     * @param sectionDef         The level section to get positional data from.
     * @param sectionMeshView The mesh view to update position for.
     */
    public void updateEntityPositionRotation(MediEvil2LevelSectionDefinition sectionDef, MeshView sectionMeshView) {
        /*if (entity == null || entityMeshView == null)
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
        entityMeshView.setTranslateZ(Utils.fixedPointIntToFloat4Bit(position.getZ()));*/
        // TODO: Figure out.
    }


    @Override
    protected void updateEditor(MediEvil2LevelSectionDefinition sectionDef) {
        // No editor for now.
    }

    @Override
    protected void onDelegateRemoved(MediEvil2LevelSectionDefinition removedSectionDef, MeshView oldMeshView) {
        if (oldMeshView != null) {
            getRenderManager().getRoot().getChildren().remove(oldMeshView);
            MeshViewController.unbindMeshSceneControls(getController(), oldMeshView);
            getController().getLightingGroup().getChildren().add(oldMeshView);
        }
    }

    @Override
    protected void setVisible(MediEvil2LevelSectionDefinition sectionDef, MeshView meshView, boolean visible) {
        meshView.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(MediEvil2LevelSectionDefinition oldSectionDef, MeshView oldMeshView, MediEvil2LevelSectionDefinition newSectionDef, MeshView newMeshView) {
        // Do nothing for now.
    }

    @Override
    protected MediEvil2LevelSectionDefinition createNewValue() {
        throw new RuntimeException("Cannot create new level section.");
    }
}