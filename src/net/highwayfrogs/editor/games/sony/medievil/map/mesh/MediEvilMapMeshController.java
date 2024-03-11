package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.ui.MediEvilEntityManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Controls the map mesh for MediEvil.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
@Getter
public class MediEvilMapMeshController extends MeshViewController<MediEvilMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 5000;
    private static final double DEFAULT_MOVEMENT_SPEED = 400;
    private DisplayList vertexDisplayList;

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

        this.vertexDisplayList = getRenderManager().createDisplayList();
        /*for (SVector vertex : getMap().getGraphicsPacket().getVertices())
            this.vertexDisplayList.addSphere(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ(), 1, VERTEX_MATERIAL, false);

        for (MediEvilMapPolygon polygon : getMap().getGraphicsPacket().getPolygons()) {
            for (int i = 1; i < polygon.getPolygonType().getVerticeCount(); i++) {
                SVector vertex1 = getMap().getGraphicsPacket().getVertices().get(polygon.getVertices()[i - 1]);
                SVector vertex2 = getMap().getGraphicsPacket().getVertices().get(polygon.getVertices()[i]);
                this.vertexDisplayList.addLine(vertex1.getFloatX(), vertex1.getFloatY(), vertex1.getFloatZ(), vertex2.getFloatX(), vertex2.getFloatY(), vertex2.getFloatZ(), 1, CONNECTION_MATERIAL);
            }
        }*/
    }

    @Override
    protected void setupManagers() {
        addManager(new MediEvilEntityManager(this));
        // TODO: Setup more managers.
    }

    @Override
    public String getMeshDisplayName() {
        return getMap().getFileDisplayName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        // TODO: CREATE
    }

    /**
     * Gets the map file which the mesh represents.
     */
    public MediEvilMapFile getMap() {
        return getMesh().getMap();
    }
}