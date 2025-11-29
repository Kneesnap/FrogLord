package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.RawColorTextureSource;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh.kcCFace;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh.kcCTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapUIManager.GreatQuestMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshUnmanagedNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages map collision displays in 3D space.
 * Created by Kneesnap on 4/17/2024.
 */
public class GreatQuestMapCollisionManager extends GreatQuestMapListManager<kcCResourceTriMesh, MeshView> {
    private final DisplayList meshViewList;
    private GreatQuestMapCollisionMesh mapCollisionMesh;
    private boolean highlightSlippyPolygons;
    private kcCTriMesh selectedMesh;

    public GreatQuestMapCollisionManager(MeshViewController<GreatQuestMapMesh> controller) {
        super(controller);
        this.meshViewList = getRenderManager().createDisplayListWithNewGroup(); // Creating a group allows all nodes to be part of a node before transparent stuff is added.
    }

    @Override
    public void onSetup() {
        super.onSetup();

        // Enable selecting individual parts of the map collision mesh.
        // Add mesh click listener.
        getController().getInputManager().addMouseListener(MouseEvent.MOUSE_CLICKED, (manager, event, deltaX, deltaY) -> {
            PickResult result = event.getPickResult();
            if (result == null || !(result.getIntersectedNode() instanceof MeshView) || manager.getMouseTracker().isSignificantMouseDragRecorded())
                return; // No pick result, or the thing that was clicked was not the main mesh.

            Mesh mesh = ((MeshView) result.getIntersectedNode()).getMesh();
            if (!(mesh instanceof GreatQuestMapCollisionMesh) || mesh != this.mapCollisionMesh)
                return;

            GreatQuestMapCollisionMesh collisionMesh = (GreatQuestMapCollisionMesh) mesh;
            kcCTriMesh triMesh = collisionMesh.getMainNode().getDataSourceByFaceIndex(result.getIntersectedFace());
            if (triMesh != null) {
                event.consume();
                setSelectedMesh((triMesh != this.selectedMesh) ? triMesh : null);
            }
        });
    }

    @Override
    public void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);

        MeshView meshView = new MeshView();
        MeshView meshViewOutline = new MeshView();
        this.mapCollisionMesh = new GreatQuestMapCollisionMesh(this, getMap().getSceneManager().getCollisionMeshes(), "Map Collision");
        this.mapCollisionMesh.addView(meshView, getController().getMeshTracker());
        this.mapCollisionMesh.addView(meshViewOutline, getController().getMeshTracker());
        setupMeshView(meshView);
        setupMeshView(meshViewOutline);
        meshView.setVisible(false);
        meshViewOutline.setVisible(false);
        meshViewOutline.setDrawMode(DrawMode.LINE);
        meshViewOutline.setMaterial(Scene3DUtils.makeUnlitSharpMaterial(javafx.scene.paint.Color.BLACK));

        getMainGrid().addCheckBox("Show Map Collision", false, visible -> {
            meshView.setVisible(visible);
            meshViewOutline.setVisible(visible);
        });
        getMainGrid().addCheckBox("Show Slippy Polygons", this.highlightSlippyPolygons, this::setHighlightSlippyPolygons);
    }

    @Override
    public String getTitle() {
        return "Environment Collision";
    }

    @Override
    public String getValueName() {
        return "Environment Collision Mesh";
    }

    @Override
    protected String getListDisplayName(int index, kcCResourceTriMesh triMesh) {
        return triMesh != null ? triMesh.getName() : super.getListDisplayName(index, null);
    }

    @Override
    public List<kcCResourceTriMesh> getValues() {
        List<kcCResourceTriMesh> entities = new ArrayList<>();
        for (kcCResource resource : getMap().getChunks())
            if (resource instanceof kcCResourceTriMesh && GreatQuestEntityManager.isFileNameEnvironmentalMesh(resource.getName()))
                entities.add(((kcCResourceTriMesh) resource));

        return entities;
    }

    @Override
    protected MeshView setupDisplay(kcCResourceTriMesh triMesh) {
        MeshView meshView = new MeshView();
        new GreatQuestMapCollisionMesh(this, triMesh).addView(meshView, getController().getMeshTracker());
        meshView.setOnMouseClicked(event -> handleClick(event, triMesh));
        setupMeshView(meshView);
        return meshView;
    }

    private void setupMeshView(MeshView meshView) {
        getController().getMainLight().getScope().add(meshView);
        meshView.setCullFace(CullFace.NONE);
        this.meshViewList.add(meshView);
    }

    @Override
    protected void updateEditor(kcCResourceTriMesh triMesh) {
        // TODO: IMPLEMENT
    }

    @Override
    protected void setVisible(kcCResourceTriMesh triMesh, MeshView meshView, boolean visible) {
        meshView.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(kcCResourceTriMesh oldTriMesh, MeshView oldMeshView, kcCResourceTriMesh newTriMesh, MeshView newMeshView) {
        if (oldMeshView != null && oldMeshView.getMesh() instanceof GreatQuestMapCollisionMesh)
            oldMeshView.setMaterial(((GreatQuestMapCollisionMesh) oldMeshView.getMesh()).getMaterial());
        if (newMeshView != null && newMeshView.getMesh() instanceof GreatQuestMapCollisionMesh)
            newMeshView.setMaterial(((GreatQuestMapCollisionMesh) newMeshView.getMesh()).getHighlightedMaterial());
    }

    @Override
    protected kcCResourceTriMesh createNewValue() {
        return new kcCResourceTriMesh(getMap());
    }

    @Override
    protected void onDelegateRemoved(kcCResourceTriMesh triMesh, MeshView meshView) {
        if (meshView != null) {
            getController().getMainLight().getScope().remove(meshView);
            this.meshViewList.remove(meshView);
        }
    }

    /**
     * Set whether slippy polygons should be highlighted.
     * @param highlightSlippyPolygons if true, they will be highlighted
     */
    public void setHighlightSlippyPolygons(boolean highlightSlippyPolygons) {
        if (this.highlightSlippyPolygons == highlightSlippyPolygons)
            return;

        this.highlightSlippyPolygons = highlightSlippyPolygons;
        this.mapCollisionMesh.updateFaces(); // Update the texCoords used by each face.
    }

    /**
     * Set the mesh which is selected.
     * @param triMesh the mesh to select
     */
    public void setSelectedMesh(kcCTriMesh triMesh) {
        if (triMesh == this.selectedMesh)
            return;

        this.selectedMesh = triMesh;
        this.mapCollisionMesh.updateFaces();
    }

    @Getter
    public static class GreatQuestMapCollisionMesh extends DynamicMesh {
        private final GreatQuestMapCollisionManager manager;
        private final List<kcCTriMesh> triMeshes;
        private final DynamicMeshDataEntry texCoordEntry;
        private final GreatQuestMapCollisionMeshNode mainNode;
        private final AtlasTexture defaultTexture;
        private final AtlasTexture cameraWireframeRenderDebugTexture;
        private final AtlasTexture cameraRaycastSkipTexture;
        private final AtlasTexture climbableTexture;
        private final AtlasTexture slippyTexture; // This is not actually a flag, but still viewable. Slippy faces are ones Frogger slips off, and cannot jump from.
        private final AtlasTexture selectedTexture;
        private final int defaultTextureIndex;
        private final int cameraWireframeRenderDebugTextureIndex;
        private final int cameraRaycastSkipTextureIndex;
        private final int climbableTextureIndex;
        private final int slippyTextureIndex;
        private final int selectedTextureIndex;
        private PhongMaterial highlightedMaterial;

        public GreatQuestMapCollisionMesh(GreatQuestMapCollisionManager manager, kcCResourceTriMesh triMesh) {
            this(manager, Collections.singletonList(triMesh.getTriMesh()), triMesh.getName());
        }

        public GreatQuestMapCollisionMesh(GreatQuestMapCollisionManager manager, List<kcCTriMesh> triMeshes, String name) {
            super(new SequentialTextureAtlas(64, 64, true), DynamicMeshTextureQuality.LIT_BLURRY, name);
            this.manager = manager;
            this.triMeshes = triMeshes;
            getTextureAtlas().startBulkOperations();
            this.defaultTexture = getTextureAtlas().addTexture(createImageSource(Color.GREEN));
            this.climbableTexture = getTextureAtlas().addTexture(createImageSource(new Color(0x6E260E)));
            this.cameraRaycastSkipTexture = getTextureAtlas().addTexture(createImageSource(Color.GRAY));
            this.cameraWireframeRenderDebugTexture = getTextureAtlas().addTexture(createImageSource(Color.RED));
            this.slippyTexture = getTextureAtlas().addTexture(createImageSource(Color.CYAN));
            this.selectedTexture = getTextureAtlas().addTexture(createImageSource(Color.YELLOW));
            getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
            getTextureAtlas().endBulkOperations();

            pushBatchOperations();
            DynamicMeshUnmanagedNode textureNode = new DynamicMeshUnmanagedNode(this);
            addNode(textureNode);
            this.texCoordEntry = new DynamicMeshDataEntry(this);
            this.defaultTextureIndex = addTexture(this.defaultTexture);
            this.climbableTextureIndex = addTexture(this.climbableTexture);
            this.cameraRaycastSkipTextureIndex = addTexture(this.cameraRaycastSkipTexture);
            this.cameraWireframeRenderDebugTextureIndex = addTexture(this.cameraWireframeRenderDebugTexture);
            this.slippyTextureIndex = addTexture(this.slippyTexture);
            this.selectedTextureIndex = addTexture(this.selectedTexture);
            textureNode.addEntry(this.texCoordEntry);

            this.mainNode = new GreatQuestMapCollisionMeshNode(this);
            addNode(this.mainNode);
            popBatchOperations();
        }

        private int addTexture(AtlasTexture texture) {
            // Add texture UVs.
            TextureAtlas textureAtlas = getTextureAtlas();
            int texCoordGroupStartIndex = this.texCoordEntry.addTexCoordValue(textureAtlas.getUV(texture, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
            this.texCoordEntry.addTexCoordValue(textureAtlas.getUV(texture, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
            this.texCoordEntry.addTexCoordValue(textureAtlas.getUV(texture, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F
            this.texCoordEntry.addTexCoordValue(textureAtlas.getUV(texture, Vector2f.ONE)); // uvBottomRight, 1F, 1F
            return texCoordGroupStartIndex;
        }

        @Override
        protected PhongMaterial updateMaterial(BufferedImage newImage) {
            PhongMaterial parentMaterial = super.updateMaterial(newImage);
            if (this.highlightedMaterial != null)
                this.highlightedMaterial = Scene3DUtils.updateHighlightMaterial(this.highlightedMaterial, newImage);
            return parentMaterial;
        }

        /**
         * Test if slippy faces should be highlighted.
         */
        public boolean shouldHighlightSlippyFaces() {
            return this.manager != null && this.manager.highlightSlippyPolygons;
        }

        /**
         * Gets or creates the highlighted material.
         */
        public PhongMaterial getHighlightedMaterial() {
            if (this.highlightedMaterial == null)
                this.highlightedMaterial = Scene3DUtils.createHighlightMaterial(getMaterial());

            return this.highlightedMaterial;
        }

        private static ITextureSource createImageSource(Color color) {
            return new RawColorTextureSource(color);
        }
    }

    public static class GreatQuestMapCollisionMeshNode extends DynamicMeshAdapterNode<kcCTriMesh> {

        public GreatQuestMapCollisionMeshNode(GreatQuestMapCollisionMesh mesh) {
            super(mesh);
        }

        @Override
        public GreatQuestMapCollisionMesh getMesh() {
            return (GreatQuestMapCollisionMesh) super.getMesh();
        }

        @Override
        protected void onAddedToMesh() {
            super.onAddedToMesh();
            getMesh().getTriMeshes().forEach(this::add);
        }

        @Override
        protected DynamicMeshAdapterNode<kcCTriMesh>.DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcCTriMesh triMesh) {
            DynamicMeshTypedDataEntry newEntry = new DynamicMeshTypedDataEntry(getMesh(), triMesh);

            List<kcVector4> vertices = triMesh.getVertices();
            for (int i = 0; i < vertices.size(); i++) {
                kcVector4 vertex = vertices.get(i);
                newEntry.addVertexValue(vertex.getX(), vertex.getY(), vertex.getZ());
            }

            // Vertice IDs are the same IDs seen in the map data.
            int vertexStartIndex = newEntry.getPendingVertexStartIndex();
            for (int i = 0; i < triMesh.getFaces().size(); i++) {
                kcCFace face = triMesh.getFaces().get(i);
                int baseTexCoordIndex = getBaseTextureCoordinateIndex(getMesh(), triMesh, face);
                int vtxIndex1 = vertexStartIndex + face.getVertices()[0];
                int vtxIndex2 = vertexStartIndex + face.getVertices()[1];
                int vtxIndex3 = vertexStartIndex + face.getVertices()[2];

                // JavaFX uses counter-clockwise winding order.
                newEntry.addFace(vtxIndex3, baseTexCoordIndex + 2, vtxIndex2, baseTexCoordIndex + 1, vtxIndex1, baseTexCoordIndex);
            }

            return newEntry;
        }

        @Override
        public void updateTexCoord(DynamicMeshAdapterNode<kcCTriMesh>.DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
            // Do nothing.
        }

        @Override
        public void updateVertex(DynamicMeshAdapterNode<kcCTriMesh>.DynamicMeshTypedDataEntry entry, int localVertexIndex) {
            kcVector4 vertex = entry.getDataSource().getVertices().get(localVertexIndex);
            entry.writeVertexXYZ(localVertexIndex, vertex.getX(), vertex.getY(), vertex.getZ());
        }

        @Override
        public void updateFace(DynamicMeshAdapterNode<kcCTriMesh>.DynamicMeshTypedDataEntry entry, int localFaceIndex) {
            int baseTexCoordIndex = getBaseTextureCoordinateIndex(getMesh(), entry.getDataSource(), entry.getDataSource().getFaces().get(localFaceIndex));
            entry.writeFace(localFaceIndex, Integer.MIN_VALUE, baseTexCoordIndex + 2, Integer.MIN_VALUE, baseTexCoordIndex + 1, Integer.MIN_VALUE, baseTexCoordIndex);
        }

        private int getBaseTextureCoordinateIndex(GreatQuestMapCollisionMesh mesh, kcCTriMesh triMesh, kcCFace face) {
            if (mesh.getManager() != null && triMesh == mesh.getManager().selectedMesh) {
                return mesh.getSelectedTextureIndex();
            } else if ((face.getFlags() & kcCFace.FLAG_CLIMBABLE) != 0) {
                return mesh.getClimbableTextureIndex();
            } else if (face.getNormal().getY() < GreatQuestInstance.JUMP_SLOPE_THRESHOLD && getMesh().shouldHighlightSlippyFaces()) {
                // This goes here since climbable surfaces override slippy behavior.
                return mesh.getSlippyTextureIndex();
            } else if ((face.getFlags() & kcCFace.FLAG_SKIP_CAMERA_RAYCAST) != 0) {
                return mesh.getCameraRaycastSkipTextureIndex();
            } else if ((face.getFlags() & kcCFace.FLAG_DEBUG_USE_OFFSET_WHEN_DRAWING_WIREFRAME) != 0) {
                return mesh.getCameraWireframeRenderDebugTextureIndex();
            } else {
                return mesh.getDefaultTextureIndex();
            }
        }
    }
}