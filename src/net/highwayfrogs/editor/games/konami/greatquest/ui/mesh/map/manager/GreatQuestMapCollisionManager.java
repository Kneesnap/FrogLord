package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceTriMesh.kcCFace;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceTriMesh.kcCTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapUIManager.GreatQuestMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Scene3DUtils;

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

    public GreatQuestMapCollisionManager(MeshViewController<GreatQuestMapMesh> controller) {
        super(controller);
        this.meshViewList = getRenderManager().createDisplayListWithNewGroup(); // Creating a group allows all nodes to be part of a node before transparent stuff is added.
    }

    @Override
    public void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);

        MeshView meshView = new MeshView();
        this.mapCollisionMesh = new GreatQuestMapCollisionMesh(this, getMap().getSceneManager().getCollisionMeshes(), "Map Collision");
        this.mapCollisionMesh.addView(meshView);
        setupMeshView(meshView);
        meshView.setVisible(false);
        getMainGrid().addCheckBox("Show Map Collision", false, meshView::setVisible);
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
        new GreatQuestMapCollisionMesh(this, triMesh).addView(meshView);
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

    @Getter
    public static class GreatQuestMapCollisionMesh extends DynamicMesh {
        private final GreatQuestMapCollisionManager manager;
        private final List<kcCTriMesh> triMeshes;
        private final AtlasTexture defaultTexture;
        private final AtlasTexture cameraWireframeRenderDebugTexture;
        private final AtlasTexture cameraRaycastSkipTexture;
        private final AtlasTexture climbableTexture;
        private final AtlasTexture slippyTexture; // This is not actually a flag, but still viewable. Slippy faces are ones Frogger slips off, and cannot jump from.
        private PhongMaterial highlightedMaterial;

        public GreatQuestMapCollisionMesh(GreatQuestMapCollisionManager manager, kcCResourceTriMesh triMesh) {
            this(manager, Collections.singletonList(triMesh.getTriMesh()), triMesh.getName());
        }

        public GreatQuestMapCollisionMesh(GreatQuestMapCollisionManager manager, List<kcCTriMesh> triMeshes, String name) {
            super(new SequentialTextureAtlas(32, 32, true), name);
            this.manager = manager;
            this.triMeshes = triMeshes;
            getTextureAtlas().startBulkOperations();
            this.defaultTexture = getTextureAtlas().addTexture(UnknownTextureSource.GREEN_INSTANCE);
            this.climbableTexture = getTextureAtlas().addTexture(UnknownTextureSource.BROWN_INSTANCE);
            this.cameraRaycastSkipTexture = getTextureAtlas().addTexture(UnknownTextureSource.YELLOW_INSTANCE);
            this.cameraWireframeRenderDebugTexture = getTextureAtlas().addTexture(UnknownTextureSource.GRAY_INSTANCE);
            this.slippyTexture = getTextureAtlas().addTexture(UnknownTextureSource.CYAN_INSTANCE);
            getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
            getTextureAtlas().endBulkOperations();

            for (int i = 0; i < triMeshes.size(); i++) {
                GreatQuestMapCollisionMeshNode newNode = new GreatQuestMapCollisionMeshNode(this, triMeshes.get(i));
                addNode(newNode);
            }
        }

        @Override
        protected PhongMaterial updateMaterial(BufferedImage newImage) {
            PhongMaterial parentMaterial = super.updateMaterial(newImage);
            this.highlightedMaterial = Scene3DUtils.updateHighlightMaterial(this.highlightedMaterial, newImage);
            return parentMaterial;
        }

        /**
         * Test if slippy faces should be highlighted.
         */
        public boolean shouldHighlightSlippyFaces() {
            return this.manager != null && this.manager.highlightSlippyPolygons;
        }
    }

    public static class GreatQuestMapCollisionMeshNode extends DynamicMeshAdapterNode<kcCFace> {
        private DynamicMeshDataEntry vertexEntry;
        private final kcCTriMesh triMesh;

        public GreatQuestMapCollisionMeshNode(GreatQuestMapCollisionMesh mesh, kcCTriMesh triMesh) {
            super(mesh);
            this.triMesh = triMesh;
        }

        @Override
        public GreatQuestMapCollisionMesh getMesh() {
            return (GreatQuestMapCollisionMesh) super.getMesh();
        }

        @Override
        protected void onAddedToMesh() {
            super.onAddedToMesh();
            // Setup vertices.
            this.vertexEntry = new DynamicMeshDataEntry(getMesh());

            // Add texture UVs.
            TextureAtlas textureAtlas = getMesh().getTextureAtlas();
            for (AtlasTexture texture : textureAtlas.getSortedTextureList()) {
                this.vertexEntry.addTexCoordValue(textureAtlas.getUV(texture, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
                this.vertexEntry.addTexCoordValue(textureAtlas.getUV(texture, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
                this.vertexEntry.addTexCoordValue(textureAtlas.getUV(texture, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F
                this.vertexEntry.addTexCoordValue(textureAtlas.getUV(texture, Vector2f.ONE)); // uvBottomRight, 1F, 1F
            }

            List<kcVector4> vertices = this.triMesh.getVertices();
            for (int i = 0; i < vertices.size(); i++) {
                kcVector4 vertex = vertices.get(i);
                this.vertexEntry.addVertexValue(vertex.getX(), vertex.getY(), vertex.getZ());
            }
            addUnlinkedEntry(this.vertexEntry);

            // Setup polygons.
            this.triMesh.getFaces().forEach(this::add);
        }

        @Override
        protected DynamicMeshAdapterNode<kcCFace>.DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcCFace face) {
            DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), face);

            // Vertice IDs are the same IDs seen in the map data.
            int baseTexCoordIndex = getBaseTextureCoordinateIndex(getMesh(), face);
            int vtxIndex1 = this.vertexEntry.getVertexStartIndex() + face.getVertices()[0];
            int vtxIndex2 = this.vertexEntry.getVertexStartIndex() + face.getVertices()[1];
            int vtxIndex3 = this.vertexEntry.getVertexStartIndex() + face.getVertices()[2];

            // JavaFX uses counter-clockwise winding order.
            entry.addFace(vtxIndex3, baseTexCoordIndex + 2, vtxIndex2, baseTexCoordIndex + 1, vtxIndex1, baseTexCoordIndex);
            return entry;
        }

        @Override
        public void updateTexCoord(DynamicMeshAdapterNode<kcCFace>.DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
            // Do nothing.
        }

        @Override
        public void updateVertex(DynamicMeshAdapterNode<kcCFace>.DynamicMeshTypedDataEntry entry, int localVertexIndex) {
            kcVector4 vertex = this.triMesh.getVertices().get(localVertexIndex);
            this.vertexEntry.writeVertexXYZ(localVertexIndex, vertex.getX(), vertex.getY(), vertex.getZ());
        }

        @Override
        public void updateFace(DynamicMeshAdapterNode<kcCFace>.DynamicMeshTypedDataEntry entry, int localFaceIndex) {
            int baseTexCoordIndex = getBaseTextureCoordinateIndex(getMesh(), entry.getDataSource());
            entry.writeFace(localFaceIndex, Integer.MIN_VALUE, baseTexCoordIndex + 2, Integer.MIN_VALUE, baseTexCoordIndex + 1, Integer.MIN_VALUE, baseTexCoordIndex);
        }

        private int getBaseTextureCoordinateIndex(GreatQuestMapCollisionMesh mesh, kcCFace face) {
            AtlasTexture texture;
            if ((face.getFlags() & kcCFace.FLAG_CLIMBABLE) != 0) {
                texture = mesh.getClimbableTexture();
            } else if (face.getNormal().getY() < GreatQuestInstance.JUMP_SLOPE_THRESHOLD && getMesh().shouldHighlightSlippyFaces()) {
                // This goes here since climbable surfaces override slippy behavior.
                texture = mesh.getSlippyTexture();
            } else if ((face.getFlags() & kcCFace.FLAG_SKIP_CAMERA_RAYCAST) != 0) {
                texture = mesh.getCameraRaycastSkipTexture();
            } else if ((face.getFlags() & kcCFace.FLAG_DEBUG_USE_OFFSET_WHEN_DRAWING_WIREFRAME) != 0) {
                texture = mesh.getCameraWireframeRenderDebugTexture();
            } else {
                texture = mesh.getDefaultTexture();
            }

            return (4 * mesh.getTextureAtlas().getSortedTextureList().indexOf(texture));
        }
    }
}