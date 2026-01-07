package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.*;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Controls the map mesh for pre-recode frogger.
 * Created by Kneesnap on 12/8/2023.
 */
public class OldFroggerMapMeshController extends MeshViewController<OldFroggerMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 5000;
    private static final double DEFAULT_MOVEMENT_SPEED = 400;
    @Getter private OldFroggerLightManager lightManager;

    public OldFroggerMapMeshController(OldFroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getFirstPersonCamera().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);

        AmbientLight mainLight = new AmbientLight(Color.WHITE);
        mainLight.getScope().add(getMeshView());
        mainLight.getScope().addAll(getAxisDisplayList().getNodes());
        getRenderManager().createDisplayList().add(mainLight);

        // Shading should always be enabled.
        if (getCheckBoxEnablePsxShading() != null)
            getCheckBoxEnablePsxShading().setDisable(true);
    }

    @Override
    protected void setupManagers() {
        addManager(new OldFroggerGeneralDataManager(this));
        addManager(new OldFroggerCameraHeightFieldManager(this));
        addManager(new OldFroggerEntityManager(this));
        addManager(new OldFroggerPathManager(this));
        addManager(new OldFroggerFormUIManager(this));
        addManager(this.lightManager = new OldFroggerLightManager(this));
        addManager(new OldFroggerGridManager(this));
        addManager(new OldFroggerVertexManager(this));
        addManager(new OldFroggerZoneManager(this));
        addManager(new OldFroggerLandscapeUIManager(this));
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