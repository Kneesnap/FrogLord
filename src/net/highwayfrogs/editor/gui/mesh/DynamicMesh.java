package net.highwayfrogs.editor.gui.mesh;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArrayBatcher;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents a triangle mesh which has functionality to dynamically setup, update, and change mesh data.
 * Created by Kneesnap on 9/24/2023.
 */
@Getter
public abstract class DynamicMesh extends TriangleMesh {
    @Getter private final TextureAtlas textureAtlas;
    @Getter private final FXIntArrayBatcher editableFaces;
    @Getter private final DynamicMeshFloatArray editableTexCoords;
    @Getter private final DynamicMeshFloatArray editableVertices;
    @Getter private final List<DynamicMeshNode> nodes = new ArrayList<>();
    @Getter private final List<DynamicMeshDataEntry> dataEntries = new ArrayList<>();
    @Getter private final List<MeshView> meshViews = new ArrayList<>(); // Tracks all views which are viewing this mesh.
    @Getter private PhongMaterial material;

    public DynamicMesh(TextureAtlas atlas) {
        this(atlas, VertexFormat.POINT_TEXCOORD);
    }

    public DynamicMesh(TextureAtlas atlas, VertexFormat format) {
        super(format);
        this.textureAtlas = atlas;
        this.textureAtlas.getImageChangeListeners().add(this::onTextureChange);
        updateMaterial(atlas.getImage());

        // Setup editable array batches.
        this.editableFaces = new FXIntArrayBatcher(new FXIntArray(), getFaces());
        this.editableTexCoords = new DynamicMeshFloatArray(this, getTexCoords(), format.getTexCoordIndexOffset());
        this.editableVertices = new DynamicMeshFloatArray(this, getPoints(), format.getPointIndexOffset());
    }

    /**
     * Updates entry start indices.
     */
    public void updateEntryStartIndices() {
        int faceStartIndex = 0;
        int texCoordStartIndex = 0;
        int vertexStartIndex = 0;
        for (int i = 0; i < this.dataEntries.size(); i++) {
            DynamicMeshDataEntry entry = this.dataEntries.get(i);
            entry.updateStartIndices(faceStartIndex, texCoordStartIndex, vertexStartIndex);

            faceStartIndex += entry.getWrittenFaceCount();
            texCoordStartIndex += entry.getWrittenTexCoordCount();
            vertexStartIndex += entry.getWrittenVertexCount();
        }
    }

    /**
     * Update the mesh arrays.
     */
    public void updateMeshArrays() {
        this.editableFaces.applyToFxArrayIfReady();
        this.editableTexCoords.applyToFxArrayIfReady();
        this.editableVertices.applyToFxArrayIfReady();
    }

    /**
     * Enable batch operations for all mesh array wrappers.
     */
    public void pushBatchOperations() {
        pushBatchRemovals();
        pushBatchInsertions();
    }

    /**
     * Enable batch insertion for all mesh array wrappers.
     */
    public void pushBatchInsertions() {
        this.editableFaces.startBatchInsertion();
        this.editableTexCoords.startBatchInsertion();
        this.editableVertices.startBatchInsertion();
    }

    /**
     * Enable batch removals for all mesh array wrappers.
     */
    public void pushBatchRemovals() {
        this.editableFaces.startBatchRemoval();
        this.editableTexCoords.startBatchRemoval();
        this.editableVertices.startBatchRemoval();
    }

    /**
     * Disable batch operations for all mesh array wrappers.
     * Updates the mesh arrays if necessary.
     */
    public void popBatchOperations() {
        popBatchRemovals();
        popBatchInsertions();
    }

    /**
     * Disable batch insertions for all mesh array wrappers.
     * Updates the mesh arrays if necessary.
     */
    public void popBatchInsertions() {
        this.editableVertices.endBatchInsertion();
        this.editableTexCoords.endBatchInsertion();
        this.editableFaces.endBatchInsertion();
    }

    /**
     * Disable batch removals for all mesh array wrappers.
     * Updates the mesh arrays if necessary.
     */
    public void popBatchRemovals() {
        this.editableVertices.endBatchRemoval();
        this.editableTexCoords.endBatchRemoval();
        this.editableFaces.endBatchRemoval();
    }

    /**
     * Test if a node is registered (active) on this mesh.
     * @param meshNode The node to test.
     * @return isActive
     */
    public boolean isActive(DynamicMeshNode meshNode) {
        return this.nodes.contains(meshNode);
    }

    /**
     * Adds the node to the mesh.
     * @param node The node to add.
     * @return If the node was added successfully.
     */
    public boolean addNode(DynamicMeshNode node) {
        if (this.nodes.contains(node))
            return false;

        this.nodes.add(node);
        node.onAddedToMesh();
        return true;
    }

    /**
     * Removes the node from the mesh.
     * @param node The node to remove.
     * @return If the node was removed successfully.
     */
    public boolean removeNode(DynamicMeshNode node) {
        if (!this.nodes.remove(node))
            return false;

        node.onRemovedFromMesh();
        return true;
    }

    /**
     * Adds a view as actively displaying this mesh.
     * @param view The view to add.
     */
    public void addView(MeshView view) {
        if (view == null)
            throw new NullPointerException("view");

        if (this.meshViews.contains(view))
            return; // Already registered.

        this.meshViews.add(view);
        view.setMesh(this);
        view.setMaterial(this.material);
    }

    /**
     * Removes a view which was actively displaying this mesh.
     * @param view The view to remove.
     */
    public void removeView(MeshView view) {
        if (view == null)
            throw new NullPointerException("view");

        if (!this.meshViews.remove(view))
            return; // Already registered.

        view.setMesh(null);
        view.setMaterial(null);
    }

    private void updateMaterial(BufferedImage newImage) {
        if (this.material == null) {
            this.material = Utils.makeDiffuseMaterial(Utils.toFXImage(newImage, false));

            // Apply newly created material to meshes.
            for (int i = 0; i < this.meshViews.size(); i++)
                this.meshViews.get(i).setMaterial(this.material);

            return;
        }

        // Update material image.
        this.material.setDiffuseMap(Utils.toFXImage(newImage, false));
    }

    private void onTextureChange(Texture texture, BufferedImage oldImage, BufferedImage newImage, boolean didOldImageHaveAnyTransparentPixels) {
        updateMaterial(newImage); // Apply the image to the mesh now.
    }
}