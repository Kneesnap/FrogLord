package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Controls the map mesh for Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestMapMeshController extends MeshViewController<GreatQuestMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 5000;
    private static final double DEFAULT_MOVEMENT_SPEED = 250;

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
    }

    @Override
    protected void setupManagers() {
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