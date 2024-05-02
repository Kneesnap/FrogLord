package net.highwayfrogs.editor.gui.mesh;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArrayBatcher;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This represents a triangle mesh which has functionality to dynamically create, update, and change mesh data.
 * Created by Kneesnap on 9/24/2023.
 */
public class DynamicMesh extends TriangleMesh implements IDynamicMeshHelper {
    private final String meshName;
    @Getter private final TextureAtlas textureAtlas;
    @Getter private final PSXMeshShadedTextureManager<?> shadedTextureManager;
    @Getter private final FXIntArrayBatcher editableFaces;
    @Getter private final DynamicMeshFloatArray editableTexCoords;
    @Getter private final DynamicMeshFloatArray editableVertices;
    @Getter private final List<DynamicMeshNode> nodes = new ArrayList<>();
    @Getter private final List<DynamicMeshDataEntry> dataEntries = new ArrayList<>();
    @Getter private final List<MeshView> meshViews = new ArrayList<>(); // Tracks all views which are viewing this mesh.
    @Getter private PhongMaterial material;
    private Logger cachedLogger;

    public DynamicMesh(TextureAtlas atlas) {
        this(atlas, VertexFormat.POINT_TEXCOORD, null);
    }

    public DynamicMesh(TextureAtlas atlas, String meshName) {
        this(atlas, VertexFormat.POINT_TEXCOORD, meshName);
    }

    public DynamicMesh(TextureAtlas atlas, VertexFormat format, String meshName) {
        super(format);
        this.meshName = meshName;
        this.textureAtlas = atlas;
        if (atlas != null) {
            this.textureAtlas.getTextureSource().setMesh(this);
            this.textureAtlas.getImageChangeListeners().add(this::onTextureChange);
            updateMaterial(atlas.getImage());
        }

        // Setup editable array batches.
        this.editableFaces = new FXIntArrayBatcher(new FXIntArray(), getFaces());
        this.editableTexCoords = new DynamicMeshFloatArray(this, "texCoord", getTexCoords(), format.getTexCoordIndexOffset(), getTexCoordElementSize());
        this.editableVertices = new DynamicMeshFloatArray(this, "vertex", getPoints(), format.getPointIndexOffset(), getPointElementSize());

        this.shadedTextureManager = createShadedTextureManager();
    }

    /**
     * Creates the shaded texture manager for this mesh.
     */
    protected PSXMeshShadedTextureManager<?> createShadedTextureManager() {
        return null;
    }

    /**
     * Gets the logger used for this mesh.
     */
    public Logger getLogger() {
        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = Logger.getLogger(Utils.getSimpleName(this) + ".Name='" + getMeshName() + "'");
    }

    /**
     * Gets the mesh name.
     */
    public String getMeshName() {
        return this.meshName != null ? this.meshName : Utils.getSimpleName(this);
    }

    @Override
    public DynamicMesh getMesh() {
        return this;
    }

    @Override
    public boolean updateTexCoord(DynamicMeshDataEntry entry, int localTexCoordIndex) {
        return entry != null && entry.getMeshNode() != null && entry.getMeshNode().updateTexCoord(entry, localTexCoordIndex);
    }

    @Override
    public boolean updateVertex(DynamicMeshDataEntry entry, int localVertexIndex) {
        return entry != null && entry.getMeshNode() != null && entry.getMeshNode().updateVertex(entry, localVertexIndex);
    }

    @Override
    public boolean updateFace(DynamicMeshDataEntry entry, int localFaceIndex) {
        return entry != null && entry.getMeshNode() != null && entry.getMeshNode().updateFace(entry, localFaceIndex);
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
        this.editableFaces.applyToFxArray();
        this.editableTexCoords.applyToFxArray();
        this.editableVertices.applyToFxArray();
    }

    /**
     * Enable batch operations for all mesh array wrappers.
     */
    public void pushBatchOperations() {
        pushBatchUpdates();
        pushBatchRemovals();
        pushBatchInsertions();
    }

    /**
     * Enable batch array updates for all mesh array wrappers.
     */
    public void pushBatchUpdates() {
        this.editableFaces.startBatchingUpdates();
        this.editableTexCoords.startBatchingUpdates();
        this.editableVertices.startBatchingUpdates();
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
        popBatchUpdates();
        popBatchRemovals();
        popBatchInsertions();
    }

    /**
     * Disable batch array updates for all mesh array wrappers.
     * Performs a single update for each array that needs an update.
     */
    public void popBatchUpdates() {
        this.editableVertices.endBatchingUpdates();
        this.editableTexCoords.endBatchingUpdates();
        this.editableFaces.endBatchingUpdates();
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
        pushBatchOperations();
        node.onAddedToMesh();
        popBatchOperations();
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

        pushBatchOperations();
        node.onRemovedFromMesh();
        popBatchOperations();
        return true;
    }

    /**
     * Adds a view as actively displaying this mesh.
     * @param view The view to add.
     * @return true if added successfully
     */
    public boolean addView(MeshView view) {
        if (view == null)
            throw new NullPointerException("view");

        if (this.meshViews.contains(view))
            return false; // Already registered.

        // Register the texture.
        if (this.meshViews.isEmpty() && this.textureAtlas != null)
            this.textureAtlas.registerTexture();

        this.meshViews.add(view);
        view.setMesh(this);
        view.setMaterial(this.material);
        return true;
    }

    /**
     * Removes a view which was actively displaying this mesh.
     * @param view The view to remove.
     * @return true iff the view was removed successfully
     */
    public boolean removeView(MeshView view) {
        if (view == null)
            throw new NullPointerException("view");

        if (!this.meshViews.remove(view))
            return false; // Not registered.

        view.setMesh(null);
        view.setMaterial(null);

        // Attempt to free the texture.
        if (this.meshViews.isEmpty()) {
            if (this instanceof IPSXShadedMesh)
                ((IPSXShadedMesh) this).getShadedTextureManager().onDispose();
            if (this.textureAtlas != null)
                this.textureAtlas.releaseTexture();
        }

        return true;
    }

    /**
     * Apply a new image to the material
     * @param newImage the image to apply
     */
    protected PhongMaterial updateMaterial(BufferedImage newImage) {
        if (this.material == null) {
            this.material = Utils.makeDiffuseMaterial(Utils.toFXImage(newImage, false));

            // Apply newly created material to meshes.
            for (int i = 0; i < this.meshViews.size(); i++)
                this.meshViews.get(i).setMaterial(this.material);

            return this.material;
        }

        // Update material image.
        this.material.setDiffuseMap(Utils.toFXImage(newImage, false));
        return this.material;
    }

    private void onTextureChange(Texture texture, BufferedImage oldImage, BufferedImage newImage, boolean didOldImageHaveAnyTransparentPixels) {
        updateMaterial(newImage); // Apply the image to the mesh now.
    }

    // Common updates. (Useful to be here to reduce code duplication)

    /**
     * Updates the texture coordinates of faces which are not otherwise updated when the atlas changes.
     */
    public void updateNonShadedPolygonTexCoords(Set<DynamicMeshNode> skippedNodes) {
        this.editableTexCoords.startBatchingUpdates();
        for (int i = 0; i < this.nodes.size(); i++) {
            DynamicMeshNode node = this.nodes.get(i);
            if (skippedNodes == null || !skippedNodes.contains(node))
                node.updateTexCoords();
        }
        this.editableTexCoords.endBatchingUpdates();
    }
}