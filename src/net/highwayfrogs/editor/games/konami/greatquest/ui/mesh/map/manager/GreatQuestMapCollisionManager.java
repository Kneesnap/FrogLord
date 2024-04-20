package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
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

    public GreatQuestMapCollisionManager(MeshViewController<GreatQuestMapMesh> controller) {
        super(controller);
        this.meshViewList = getRenderManager().createDisplayListWithNewGroup(); // Creating a group allows all nodes to be part of a node before transparent stuff is added.
    }

    @Override
    public void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);

        MeshView meshView = new MeshView();
        GreatQuestMapCollisionMesh mapCollisionMesh = new GreatQuestMapCollisionMesh(getMap().getSceneManager().getCollisionMeshes(), "Map Collision");
        mapCollisionMesh.addView(meshView);
        setupMeshView(meshView);
        meshView.setVisible(false);
        getMainGrid().addCheckBox("Show Map Collision", false, meshView::setVisible);
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
        new GreatQuestMapCollisionMesh(triMesh).addView(meshView);
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

    @Getter
    public static class GreatQuestMapCollisionMesh extends DynamicMesh {
        private final List<kcCTriMesh> triMeshes;
        private PhongMaterial highlightedMaterial;

        public GreatQuestMapCollisionMesh(kcCResourceTriMesh triMesh) {
            this(Collections.singletonList(triMesh.getTriMesh()), triMesh.getName());
        }

        public GreatQuestMapCollisionMesh(List<kcCTriMesh> triMeshes, String name) {
            super(null, name);
            this.triMeshes = triMeshes;
            updateMaterial(UnknownTextureSource.GREEN_INSTANCE.makeImage());

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
            this.vertexEntry.addTexCoordValue(Vector2f.ZERO); // uvTopLeft, 0F, 0F
            this.vertexEntry.addTexCoordValue(Vector2f.UNIT_X); // uvTopRight, 1F, 0F
            this.vertexEntry.addTexCoordValue(Vector2f.UNIT_Y); // uvBottomLeft, 0F, 1F
            this.vertexEntry.addTexCoordValue(Vector2f.ONE); // uvBottomRight, 1F, 1F

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
            int vtxIndex1 = this.vertexEntry.getVertexStartIndex() + face.getVertices()[0];
            int vtxIndex2 = this.vertexEntry.getVertexStartIndex() + face.getVertices()[1];
            int vtxIndex3 = this.vertexEntry.getVertexStartIndex() + face.getVertices()[2];

            // JavaFX uses counter-clockwise winding order.
            entry.addFace(vtxIndex3, 2, vtxIndex2, 1, vtxIndex1, 0);
            return entry;
        }

        @Override
        public void updateTexCoord(DynamicMeshAdapterNode<kcCFace>.DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {

        }

        @Override
        public void updateVertex(DynamicMeshAdapterNode<kcCFace>.DynamicMeshTypedDataEntry entry, int localVertexIndex) {
            kcVector4 vertex = this.triMesh.getVertices().get(localVertexIndex);
            this.vertexEntry.writeVertexXYZ(localVertexIndex, vertex.getX(), vertex.getY(), vertex.getZ());
        }
    }
}