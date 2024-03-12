package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.ui.MediEvilEntityManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Controls the map mesh for MediEvil.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
@Getter
public class MediEvilMapMeshController extends MeshViewController<MediEvilMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 5000;
    private static final double DEFAULT_MOVEMENT_SPEED = 250;
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

        // Display unused vertices.
        this.vertexDisplayList = getRenderManager().createDisplayList();
        Set<SVector> unusedVertices = new HashSet<>(getMap().getGraphicsPacket().getVertices());

        for (MediEvilMapPolygon polygon : getMap().getGraphicsPacket().getPolygons())
            for (int i = 0; i < polygon.getPolygonType().getVerticeCount(); i++)
                unusedVertices.remove(getMap().getGraphicsPacket().getVertices().get(polygon.getVertices()[i]));

        for (SVector vertex : unusedVertices)
            this.vertexDisplayList.addSphere(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ(), 1, VERTEX_MATERIAL, false);

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