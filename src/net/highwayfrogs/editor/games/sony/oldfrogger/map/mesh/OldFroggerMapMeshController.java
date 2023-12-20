package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerFormUIManager;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapVertexManager;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerPathManager;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Controls the map mesh for pre-recode frogger.
 * Created by Kneesnap on 12/8/2023.
 */
public class OldFroggerMapMeshController extends MeshViewController<OldFroggerMapMesh> {
    private static final double DEFAULT_FAR_CLIP = 5000;
    private static final double DEFAULT_MOVEMENT_SPEED = 400;


    private static final PhongMaterial MATERIAL_GREEN = Utils.makeSpecialMaterial(Color.LIME);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getCameraFPS().getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getCameraFPS().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);

        AmbientLight mainLight = new AmbientLight(Color.WHITE);
        mainLight.getScope().add(getMeshView());
        mainLight.getScope().addAll(getAxisDisplayList().getNodes());
        getRenderManager().createDisplayList().add(mainLight);

        // TODO: Put into separate controller.
        /*if (getMap().getCameraHeightFieldPacket() != null) {
            DisplayList heightFieldVertices = getRenderManager().createDisplayList();
            for (int z = 0; z < getMap().getCameraHeightFieldPacket().getHeightMap().length; z++)
                for (int x = 0; x < getMap().getCameraHeightFieldPacket().getHeightMap()[z].length; x++)
                    heightFieldVertices.addSphere(x, Utils.fixedPointIntToFloat4Bit(getMap().getCameraHeightFieldPacket().getHeightMap()[z][x]), z, 1, MATERIAL_GREEN, false);
        }*/

        // TODO: !
        getMeshScene().setOnMouseClicked(evt -> {
            int intersectedFace = evt.getPickResult().getIntersectedFace();
            if (intersectedFace < 0)
                return;

            // TODO: In the future let's track which faces go to which entries, so we can do a much easier lookup. Eg: We track vertices and faces, instead of just doing math here.
            DynamicMeshAdapterNode<OldFroggerMapPolygon>.DynamicMeshTypedDataEntry entry = getMesh().getMainNode().getDataEntry(intersectedFace / 2);
            if (entry == null)
                return;

            OldFroggerMapPolygon polygon = entry.getDataSource();
            if (polygon.getPolygonType().isTextured()) {
                System.out.println("Texture ID: " + polygon.getTextureId());
                System.out.println("UV-0: " + getUVDisplay(polygon.getTextureUvs()[0]));
                System.out.println("UV-1: " + getUVDisplay(polygon.getTextureUvs()[1]));
                System.out.println("UV-2: " + getUVDisplay(polygon.getTextureUvs()[2]));
                System.out.println("UV-3: " + getUVDisplay(polygon.getTextureUvs()[3]));
            } else {
                System.out.println("Not Textured");
            }
        });
    }

    private String getUVDisplay(SCByteTextureUV uv) {
        return "[U: " + uv.getU() + ", " + uv.getV() + "], Floats: " + uv.getFloatU() + ", " + uv.getFloatV();
    }

    @Override
    protected void setupManagers() {
        addManager(new OldFroggerEntityManager(this));
        addManager(new OldFroggerPathManager(this));
        addManager(new OldFroggerFormUIManager(this));
        addManager(new OldFroggerMapVertexManager(this));
        // TODO: Add managers
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
    public OldFroggerMapFile getMap() {
        return getMesh().getMap();
    }
}