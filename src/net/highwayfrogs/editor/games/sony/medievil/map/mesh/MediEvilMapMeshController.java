package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.*;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Controls the map mesh for MediEvil.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
public class MediEvilMapMeshController extends MeshViewController<OldFroggerMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 5000;
    private static final double DEFAULT_MOVEMENT_SPEED = 400;
    private DisplayList vertexDisplayList;


    private static final PhongMaterial MATERIAL_GREEN = Utils.makeSpecialMaterial(Color.LIME);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_RED = Utils.makeSpecialMaterial(Color.RED);
    private static final PhongMaterial MATERIAL_BLUE = Utils.makeSpecialMaterial(Color.BLUE);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getFirstPersonCamera().getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getFirstPersonCamera().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);

        AmbientLight mainLight = new AmbientLight(Color.WHITE);
        mainLight.getScope().add(getMeshView());
        mainLight.getScope().addAll(getAxisDisplayList().getNodes());
        getRenderManager().createDisplayList().add(mainLight);

        this.vertexDisplayList = getRenderManager().createDisplayList();

        // TODO: Improve this.
        getMeshScene().setOnMouseClicked(evt -> {
            PickResult result = evt.getPickResult();
            if (result == null || result.getIntersectedNode() != getMeshView())
                return; // No pick result, or the thing that was clicked was not the main mesh.

            int intersectedFace = result.getIntersectedFace();
            if (intersectedFace < 0)
                return;

            OldFroggerMapPolygon polygon = getMesh().getMainNode().getDataSourceByFaceIndex(intersectedFace);
            if (polygon == null)
                return;

            getLogger().info("Polygon: " + polygon.getPolygonType());
            if (polygon.getPolygonType().isTextured()) {
                getLogger().info("Texture ID: " + polygon.getTextureId());
                getLogger().info("UV-0: " + getUVDisplay(polygon.getTextureUvs()[0]));
                getLogger().info("UV-1: " + getUVDisplay(polygon.getTextureUvs()[1]));
                getLogger().info("UV-2: " + getUVDisplay(polygon.getTextureUvs()[2]));
                getLogger().info("UV-3: " + getUVDisplay(polygon.getTextureUvs()[3]));
            }
            for (int i = 0; i < polygon.getColors().length; i++)
                getLogger().info("Color " + i + ": " + polygon.getColors()[i].toString());

            this.vertexDisplayList.clear();
            SVector vertex0 = getMap().getVertexPacket().getVertices().get(polygon.getVertices()[0]);
            SVector vertex1 = getMap().getVertexPacket().getVertices().get(polygon.getVertices()[1]);
            SVector vertex2 = getMap().getVertexPacket().getVertices().get(polygon.getVertices()[2]);
            SVector vertex3 = getMap().getVertexPacket().getVertices().get(polygon.getVertices()[3]);
            this.vertexDisplayList.addSphere(vertex0.getFloatX(), vertex0.getFloatY(), vertex0.getFloatZ(), 1, MATERIAL_YELLOW, false);
            this.vertexDisplayList.addSphere(vertex1.getFloatX(), vertex1.getFloatY(), vertex1.getFloatZ(), 1, MATERIAL_GREEN, false);
            this.vertexDisplayList.addSphere(vertex2.getFloatX(), vertex2.getFloatY(), vertex2.getFloatZ(), 1, MATERIAL_RED, false);
            this.vertexDisplayList.addSphere(vertex3.getFloatX(), vertex3.getFloatY(), vertex3.getFloatZ(), 1, MATERIAL_BLUE, false);
        });
    }

    private String getUVDisplay(SCByteTextureUV uv) {
        return "[U: " + uv.getU() + ", " + uv.getV() + "], Floats: " + uv.getFloatU() + ", " + uv.getFloatV();
    }

    @Override
    protected void setupManagers() {
        addManager(new OldFroggerZoneManager(this));
        addManager(new OldFroggerVertexManager(this));
        addManager(new OldFroggerGridManager(this));
        addManager(this.lightManager = new OldFroggerLightManager(this));
        addManager(new OldFroggerFormUIManager(this));
        addManager(new OldFroggerPathManager(this));
        addManager(new OldFroggerEntityManager(this));
        addManager(new OldFroggerCameraHeightFieldManager(this));
        addManager(new OldFroggerGeneralDataManager(this));
    }

    @Override
    public String getMeshDisplayName() {
        return getMap().getFileDisplayName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        IVector froggerPos = getMap().getLevelSpecificDataPacket().getFroggerStartPosition();
        SVector cameraOffset = getMap().getStandardPacket().getCameraOffset();
        getFirstPersonCamera().setPos(froggerPos.getFloatX() + cameraOffset.getFloatX(), froggerPos.getFloatY() + cameraOffset.getFloatY(), froggerPos.getFloatZ() + cameraOffset.getFloatZ());
        getFirstPersonCamera().setCameraLookAt(froggerPos.getFloatX(), froggerPos.getFloatY(), froggerPos.getFloatZ());
    }

    /**
     * Gets the map file which the mesh represents.
     */
    public OldFroggerMapFile getMap() {
        return getMesh().getMap();
    }
}