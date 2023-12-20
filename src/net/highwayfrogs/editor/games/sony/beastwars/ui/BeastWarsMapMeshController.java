package net.highwayfrogs.editor.games.sony.beastwars.ui;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapMesh;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Manages the UI which is displayed when a mesh is viewed.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public class BeastWarsMapMeshController extends MeshViewController<BeastWarsMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 5000;
    private static final double DEFAULT_MOVEMENT_SPEED = 400;

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getCameraFPS().getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getCameraFPS().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);

        AmbientLight mainLight = new AmbientLight(Color.WHITE);
        mainLight.getScope().add(getMeshView());
        mainLight.getScope().addAll(getAxisDisplayList().getNodes());
        getRenderManager().createDisplayList().add(mainLight);
    }

    @Override
    protected void setupManagers() {
        addManager(new BeastWarsCollprimManager(this));
        addManager(new BeastWarsLineManager(this));
        addManager(new BeastWarsZoneManager(this));
        // TODO
    }

    @Override
    public String getMeshDisplayName() {
        return getMap().getFileDisplayName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        // TODO
        //getCameraFPS().setPos();
        //getCameraFPS().setCameraLookAt();
    }

    /**
     * Gets the map file which the mesh represents.
     */
    public BeastWarsMapFile getMap() {
        return getMesh().getMap();
    }
}