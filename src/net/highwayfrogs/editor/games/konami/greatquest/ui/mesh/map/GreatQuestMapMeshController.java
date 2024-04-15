package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Controls the map mesh for Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestMapMeshController extends MeshViewController<GreatQuestMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 500;
    private static final double DEFAULT_MOVEMENT_SPEED = 100;

    private static final PhongMaterial VERTEX_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial CONNECTION_MATERIAL = Utils.makeSpecialMaterial(Color.LIMEGREEN);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getFirstPersonCamera().getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getFirstPersonCamera().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);

        AmbientLight mainLight = new AmbientLight(Color.WHITE);
        mainLight.getScope().add(getMeshView());
        mainLight.getScope().addAll(getAxisDisplayList().getNodes());
        getRenderManager().createDisplayList().add(mainLight);
        
        getComboBoxMeshCullFace().setValue(CullFace.NONE); // Great Quest has no back-face culling.
        getMeshView().setVisible(false); // Hide real mesh view.

        // Create mesh views necessary
        // TODO: Consider tracking these better.
        DisplayList mapMeshList = getRenderManager().createDisplayListWithNewGroup();
        addMaterialMesh(mapMeshList, null);
        for (kcMaterial material : getMap().getSceneManager().getMaterials())
            addMaterialMesh(mapMeshList, material);

        // Add mesh click listener.
        getMeshScene().setOnMouseClicked(evt -> {
            PickResult result = evt.getPickResult();
            if (result == null || !(result.getIntersectedNode() instanceof MeshView))
                return; // No pick result, or the thing that was clicked was not the main mesh.

            Mesh mesh = ((MeshView) result.getIntersectedNode()).getMesh();
            if (!(mesh instanceof GreatQuestMapMaterialMesh))
                return;

            GreatQuestMapMaterialMesh materialMesh = (GreatQuestMapMaterialMesh) mesh;
            kcMaterial material = materialMesh.getMapMaterial();
            getLogger().info("Clicked on " + (material != null ? "'" + material.getMaterialName() + "'/'" + material.getTextureFileName() + "'" : "NULL"));
            DynamicMeshDataEntry entry = materialMesh.getMainNode().getDataEntryByFaceIndex(evt.getPickResult().getIntersectedFace());
            if (entry == null)
                return;

            int faceStartIndex = entry.getFaceMeshArrayIndex(evt.getPickResult().getIntersectedFace() - entry.getFaceStartIndex());
            float texCoord1U = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 1));
            float texCoord1V = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 1) + 1);
            float texCoord2U = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 3));
            float texCoord2V = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 3) + 1);
            float texCoord3U = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 5));
            float texCoord3V = materialMesh.getTexCoords().get(materialMesh.getFaces().get(faceStartIndex + 5) + 1);
            getLogger().info(" - UV0: [" + texCoord1U + ", " + texCoord1V + "]");
            getLogger().info(" - UV1: [" + texCoord2U + ", " + texCoord2V + "]");
            getLogger().info(" - UV2: [" + texCoord3U + ", " + texCoord3V + "]");
        });

    }

    private void addMaterialMesh(DisplayList displayList, kcMaterial material) {
        GreatQuestMapMaterialMesh materialMesh = new GreatQuestMapMaterialMesh(getMap(), material);

        MeshView meshView = new MeshView();
        meshView.setDrawMode(getComboBoxMeshDrawMode().getValue());
        meshView.setCullFace(getComboBoxMeshCullFace().getValue());
        getCheckBoxShowMesh().selectedProperty().bindBidirectional(meshView.visibleProperty());
        getComboBoxMeshDrawMode().getItems().setAll(DrawMode.values());
        getComboBoxMeshDrawMode().valueProperty().bindBidirectional(meshView.drawModeProperty());
        getComboBoxMeshCullFace().getItems().setAll(CullFace.values());
        getComboBoxMeshCullFace().valueProperty().bindBidirectional(meshView.cullFaceProperty());

        materialMesh.addView(meshView);
        displayList.add(meshView);
        getMainLight().getScope().add(meshView);
    }

    @Override
    protected void setupManagers() {
        addManager(new GreatQuestEntityManager(this));
        // TODO: Setup managers.
    }

    @Override
    public String getMeshDisplayName() {
        return getMap().getDebugName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        // TODO: Come up with default camera position.
    }

    /**
     * Gets the map file which the mesh represents.
     */
    public GreatQuestChunkedFile getMap() {
        return getMesh().getMap();
    }
}