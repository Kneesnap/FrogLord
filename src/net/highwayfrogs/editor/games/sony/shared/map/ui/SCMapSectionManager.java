package net.highwayfrogs.editor.games.sony.shared.map.ui;

import javafx.application.Platform;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.games.sony.c12.C12GameInstance;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMesh;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMeshController;
import net.highwayfrogs.editor.games.sony.shared.map.section.SCLevelDefinition;
import net.highwayfrogs.editor.games.sony.shared.map.section.SCLevelSectionDefinition;
import net.highwayfrogs.editor.games.sony.shared.map.ui.SCMapUIManager.SCMapListManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;

import java.util.Collections;
import java.util.List;

/**
 * Allows choosing if sections should display.
 * Created by Kneesnap on 5/14/2024.
 */
public class SCMapSectionManager<TMapMesh extends SCMapMesh> extends SCMapListManager<TMapMesh, SCLevelSectionDefinition, MeshView> {
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
    public List<SCLevelSectionDefinition> getValues() {
        ISCLevelTableEntry levelTableEntry = getMap().getLevelTableEntry();
        if (levelTableEntry == null)
            return Collections.emptyList();

        SCLevelDefinition levelDefinition = levelTableEntry.getLevelDefinition();
        if (levelDefinition == null)
            return Collections.emptyList();

        return levelDefinition.getLevelSections();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        if (getGameInstance() instanceof C12GameInstance) {
            Platform.runLater(getValueSelectionBox().getSelectionModel()::selectFirst); // Schedule this after the values have been added.
        } else {
            getValueDisplaySetting().setValue(ListDisplayType.ALL); // This does not run for C-12, because C-12 has lots of lag due to the sheer amount of shaded texture data generated.
        }
    }

    @Override
    protected MeshView setupDisplay(SCLevelSectionDefinition sectionDef) {
        MeshView newView = new MeshView();
        newView.setCullFace(CullFace.BACK);
        newView.setDrawMode(DrawMode.FILL);
        updateEntityPositionRotation(sectionDef, newView);
        getRenderManager().getRoot().getChildren().add(newView);
        getController().getLightingGroup().getChildren().add(newView);

        newView.setOnMouseClicked(evt -> {
            evt.consume();
            getValueSelectionBox().getSelectionModel().select(sectionDef);
        });
        return newView;
    }

    /**
     * Updates the displayed position / rotation of the level section.
     * @param sectionDef The level section to get positional data from.
     */
    public void updateEntityPositionRotation(SCLevelSectionDefinition sectionDef) {
        updateEntityPositionRotation(sectionDef, getDelegatesByValue().get(sectionDef));
    }

    /**
     * Updates the displayed position / rotation of the level section.
     * @param sectionDef         The level section to get positional data from.
     * @param sectionMeshView The mesh view to update position for.
     */
    public void updateEntityPositionRotation(SCLevelSectionDefinition sectionDef, MeshView sectionMeshView) {
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
    protected void updateEditor(SCLevelSectionDefinition sectionDef) {
        // No editor for now.
    }

    @Override
    protected void onDelegateRemoved(SCLevelSectionDefinition removedSectionDef, MeshView oldMeshView) {
        if (oldMeshView != null) {
            setVisible(removedSectionDef, oldMeshView, false);
            getRenderManager().getRoot().getChildren().remove(oldMeshView);
            getController().getLightingGroup().getChildren().remove(oldMeshView);
        }
    }

    @Override
    protected void setVisible(SCLevelSectionDefinition sectionDef, MeshView meshView, boolean visible) {
        meshView.setVisible(visible);

        // Create/remove map meshes on visibility control, as to avoid the major lag seen in places like C-12 Final Resistance due to the sheer scale of its maps.
        if (visible && !(meshView.getMesh() instanceof SCMapMesh)) {
            MeshViewController.bindMeshSceneControls(getController(), meshView);
            SCMapMesh mapMesh = new SCMapMesh(sectionDef.getMapFile());
            mapMesh.addView(meshView, getController().getMeshTracker());
        } else if (!visible && (meshView.getMesh() instanceof SCMapMesh)) {
            MeshViewController.unbindMeshSceneControls(getController(), meshView);
            DynamicMesh.tryRemoveMesh(meshView);
            System.gc(); // This is here as a prayer to avoid increasing the max heap size when switching between sections.
        }
    }

    @Override
    protected void onSelectedValueChange(SCLevelSectionDefinition oldSectionDef, MeshView oldMeshView, SCLevelSectionDefinition newSectionDef, MeshView newMeshView) {
        // Do nothing for now.
    }

    @Override
    protected SCLevelSectionDefinition createNewValue() {
        throw new RuntimeException("Cannot create new level section.");
    }
}