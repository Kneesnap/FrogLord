package net.highwayfrogs.editor.games.sony.shared.map.mesh;

import javafx.scene.AmbientLight;
import javafx.scene.SubScene;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a map mesh controller for one of the later Sony Cambridge PSX games.
 * Created by Kneesnap on 5/8/2024.
 */
@Getter
public class SCMapMeshController extends MeshViewController<SCMapMesh> {
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
        SCMapPolygonPacket<?> polygonPacket = getMap().getPolygonPacket();
        if (polygonPacket != null) {
            Set<SVector> unusedVertices = new HashSet<>(polygonPacket.getVertices());

            for (SCMapPolygon polygon : polygonPacket.getPolygons())
                for (int i = 0; i < polygon.getPolygonType().getVerticeCount(); i++)
                    unusedVertices.remove(polygonPacket.getVertices().get(polygon.getVertices()[i]));

            for (SVector vertex : unusedVertices)
                this.vertexDisplayList.addSphere(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ(), 1, VERTEX_MATERIAL, false);
        }

        // Add mesh click listener.
        getMeshScene().setOnMouseClicked(evt -> {
            PickResult result = evt.getPickResult();
            if (result == null || !(result.getIntersectedNode() instanceof MeshView))
                return; // No pick result, or the thing that was clicked was not the main mesh.

            Mesh mesh = ((MeshView) result.getIntersectedNode()).getMesh();
            if (!(mesh instanceof SCMapMesh))
                return;

            SCMapMesh mapMesh = (SCMapMesh) mesh;
            DynamicMeshDataEntry entry = mapMesh.getMainNode().getDataEntryByFaceIndex(result.getIntersectedFace());
            if (entry == null)
                return;

            if (entry instanceof DynamicMeshAdapterNode.DynamicMeshTypedDataEntry) { // TODO: Replace with a polygon editor later.
                SCMapPolygon mapPolygon = ((DynamicMeshAdapterNode<SCMapPolygon>.DynamicMeshTypedDataEntry) entry).getDataSource();
                getLogger().info("Face " + result.getIntersectedFace() + ":");
                for (int localVtxId = 0; localVtxId < mapPolygon.getVertexCount(); localVtxId++) {
                    int mapVertexId = mapPolygon.getVertices()[localVtxId];
                    SVector vertex = getMap().getPolygonPacket().getVertices().get(mapVertexId);
                    getLogger().info(" Vertex " + localVtxId + "/" + mapVertexId + " -> Pad: " + Utils.toHexString(vertex.getUnsignedPadding()));
                }
            }
        });
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
        /*for (MediEvilMapEntity entity : getMap().getEntitiesPacket().getEntities()) {
            MediEvilEntityDefinition entityDefinition = entity.getEntityDefinition();
            if (entityDefinition == null || !"Dan".equalsIgnoreCase(entityDefinition.getName()))
                continue; // If this isn't Dan, skip it.

            // Once we've found Dan, come up with a camera position
            double rotationYaw = entity.getRotationYInRadians();
            double xMultiple = Math.cos(-rotationYaw - (Math.PI / 2));
            double zMultiple = Math.sin(-rotationYaw - (Math.PI / 2));
            SVector danPos = entity.getPosition();
            getFirstPersonCamera().setPos(danPos.getFloatX() + (xMultiple * 50), danPos.getFloatY() - 35, danPos.getFloatZ() + (zMultiple * 50));
            getFirstPersonCamera().setCameraLookAt(danPos.getFloatX(), danPos.getFloatY(), danPos.getFloatZ());
        }*/

        // TODO: IMPLEMENT
    }

    /**
     * Gets the map file which the mesh represents.
     */
    public SCMapFile<?> getMap() {
        return getMesh().getMap();
    }
}