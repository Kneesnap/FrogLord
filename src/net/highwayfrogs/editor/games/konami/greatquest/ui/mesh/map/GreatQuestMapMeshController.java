package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr.kcVtxBufFileStruct;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcBox4;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapCollisionManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapEnvironmentEditor;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapSceneManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.system.math.Matrix4x4f;
import net.highwayfrogs.editor.system.math.Quaternion;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.List;

/**
 * Controls the map mesh for Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestMapMeshController extends MeshViewController<GreatQuestMapMesh> {
    private GreatQuestMapMeshCollection meshViewCollection;
    private kcVtxBufFileStruct selectedVertexBuffer;
    private GreatQuestMapMaterialMesh highlightedVertexBufferMesh;
    private final Box selectedVertexBufferBoundingBox = new Box();
    private static final double DEFAULT_FAR_CLIP = 1000; // Far enough away to see the skybox.
    private static final double DEFAULT_NEAR_CLIP = .1; // Great Quest needs a fairly small near clip as the map geometry is shown at a small scale.
    private static final double DEFAULT_MOVEMENT_SPEED = 25;

    private static final PhongMaterial BOUNDING_BOX_OUTLINE = Scene3DUtils.makeUnlitSharpMaterial(Color.RED);

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        // Create map mesh before super, so the map is registered before the skybox / transparent water / entities, thus allowing transparency to work right.
        this.meshViewCollection = new GreatQuestMapMeshCollection(this);
        this.meshViewCollection.setMesh(getMesh().getActualMesh());

        super.setupBindings(subScene3D, meshView);
        getCamera().setNearClip(DEFAULT_NEAR_CLIP);
        getCamera().setFarClip(DEFAULT_FAR_CLIP);
        getFirstPersonCamera().setDefaultMoveSpeed(DEFAULT_MOVEMENT_SPEED);
        getComboBoxMeshCullFace().setValue(CullFace.NONE); // Great Quest has no back-face culling.

        // Add mesh click listener.
        getInputManager().addMouseListener(MouseEvent.MOUSE_CLICKED, (manager, event, deltaX, deltaY) -> {
            PickResult result = event.getPickResult();
            if (result == null || !(result.getIntersectedNode() instanceof MeshView) || manager.getMouseTracker().isSignificantMouseDragRecorded())
                return; // No pick result, or the thing that was clicked was not the main mesh.

            Mesh mesh = ((MeshView) result.getIntersectedNode()).getMesh();
            if (!(mesh instanceof GreatQuestMapMaterialMesh))
                return;

            GreatQuestMapMaterialMesh materialMesh = (GreatQuestMapMaterialMesh) mesh;
            kcMaterial material = materialMesh.getMapMaterial();
            if (material != null) {
                List<?> vertexBuffers = getMap().getSceneManager().getVertexBuffersForMaterial(material);
                getLogger().info("Clicked on '%s'/'%s' (%s buffers)", material.getMaterialName(), material.getTextureFileName(), (vertexBuffers != null ? vertexBuffers.size() : 0));
            }

            kcVtxBufFileStruct vtxBuf = materialMesh.getMainNode().getDataSourceByFaceIndex(result.getIntersectedFace());
            if (vtxBuf == null)
                return;

            boolean clickedSelected = (vtxBuf == this.selectedVertexBuffer);
            if (this.selectedVertexBuffer != null) {
                getMesh().getActualMesh().removeMesh(this.highlightedVertexBufferMesh);
                this.selectedVertexBufferBoundingBox.setVisible(false);
                this.selectedVertexBuffer = null;
                this.highlightedVertexBufferMesh = null;
            }

            if (!clickedSelected) {
                this.highlightedVertexBufferMesh = new GreatQuestMapMaterialMesh(getMap(), vtxBuf);
                getMesh().getActualMesh().addMesh(this.highlightedVertexBufferMesh);
                this.selectedVertexBuffer = vtxBuf;

                // Update bounding box.
                kcBox4 boundingBox = vtxBuf.getBoundingBox();
                float boxX = (boundingBox.getMax().getX() + boundingBox.getMin().getX()) * .5F;
                float boxY = (boundingBox.getMax().getY() + boundingBox.getMin().getY()) * .5F;
                float boxZ = (boundingBox.getMax().getZ() + boundingBox.getMin().getZ()) * .5F;
                float boxWidth = Math.abs(boundingBox.getMax().getX() - boundingBox.getMin().getX());
                float boxHeight = Math.abs(boundingBox.getMax().getY() - boundingBox.getMin().getY());
                float boxDepth = Math.abs(boundingBox.getMax().getZ() - boundingBox.getMin().getZ());

                Scene3DUtils.setNodePosition(this.selectedVertexBufferBoundingBox, boxX, boxY, boxZ);
                this.selectedVertexBufferBoundingBox.setWidth(boxWidth);
                this.selectedVertexBufferBoundingBox.setHeight(boxHeight);
                this.selectedVertexBufferBoundingBox.setDepth(boxDepth);
                this.selectedVertexBufferBoundingBox.setVisible(true);
            }
        });

        // Setup bounding box.
        this.selectedVertexBufferBoundingBox.setMaterial(BOUNDING_BOX_OUTLINE);
        this.selectedVertexBufferBoundingBox.setMouseTransparent(true);
        this.selectedVertexBufferBoundingBox.setDrawMode(DrawMode.LINE);
        this.selectedVertexBufferBoundingBox.setCullFace(CullFace.NONE);
        this.selectedVertexBufferBoundingBox.setVisible(false);
        getRenderManager().getRoot().getChildren().add(this.selectedVertexBufferBoundingBox);
    }

    @Override
    protected void setupManagers() {
        addManager(new GreatQuestMapEnvironmentEditor(this));
        addManager(new GreatQuestEntityManager(this));
        addManager(new GreatQuestMapCollisionManager(this));

        kcCResOctTreeSceneMgr manager = getMesh().getMap().getSceneManager();
        if (manager != null)
            addManager(new GreatQuestMapSceneManager(this, manager));
    }

    @Override
    public String getMeshDisplayName() {
        return getMap().getDebugName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        kcCResourceEntityInst playerEntity = getMap().getResourceByHash(kcEntityInst.PLAYER_ENTITY_HASH);
        if (playerEntity != null && playerEntity.getInstance() instanceof kcEntity3DInst) {
            kcEntity3DInst playerEntity3D = (kcEntity3DInst) playerEntity.getInstance();
            Vector3f position = new Vector3f(playerEntity3D.getPosition().getX(), playerEntity3D.getPosition().getY(), playerEntity3D.getPosition().getZ());

            getFirstPersonCamera().setInvertY(true);
            Vector3f cameraOffset = new Vector3f(0, 0, 3);
            Matrix4x4f.createFromQuaternion(Quaternion.fromAxisAngle(Vector3f.UNIT_Y, playerEntity3D.getRotation().getY()))
                    .multiply(cameraOffset, cameraOffset); // Apply Y rotation.

            getFirstPersonCamera().setPos(position.getX() + cameraOffset.getX(), position.getY() + cameraOffset.getY() + 2, position.getZ() + cameraOffset.getZ());
            getFirstPersonCamera().setCameraLookAt(position.getX(), position.getY(), position.getZ());
        } else {
            setupDefaultInverseCamera();
        }
    }

    @Override
    protected double getAxisDisplayLength() {
        return 3;
    }

    @Override
    protected double getAxisDisplaySize() {
        return 1;
    }

    @Override
    protected boolean mapRendersFirst() {
        // Gives preference to transparent entities, since the map is rarely (if ever?) transparent.
        return true;
    }

    /**
     * Gets the map file which the mesh represents.
     */
    public GreatQuestChunkedFile getMap() {
        return getMesh().getMap();
    }

    public static class GreatQuestMapMeshCollection extends MeshViewCollection<GreatQuestMapMaterialMesh> {
        public GreatQuestMapMeshCollection(MeshViewController<?> viewController) {
            super(viewController, viewController.getRenderManager().createDisplayListWithNewGroup());}

        @Override
        protected void onMeshViewSetup(int meshIndex, GreatQuestMapMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            MeshViewController.bindMeshSceneControls(getController(), meshView);
            getController().getMainLight().getScope().add(meshView);
        }

        @Override
        protected void onMeshViewCleanup(int meshIndex, GreatQuestMapMaterialMesh mesh, MeshView meshView) {
            super.onMeshViewCleanup(meshIndex, mesh, meshView);
            MeshViewController.unbindMeshSceneControls(getController(), meshView);
            getController().getMainLight().getScope().remove(meshView);
        }
    }
}