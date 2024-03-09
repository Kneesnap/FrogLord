package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Controls the map mesh for MediEvil.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
@Getter
public class MediEvilMapMeshController extends MeshViewController<MediEvilMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 5000;
    private static final double DEFAULT_MOVEMENT_SPEED = 400;
    private DisplayList vertexDisplayList;

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
    }

    @Override
    protected void setupManagers() {
        // TODO: Setup managers.
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