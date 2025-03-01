package net.highwayfrogs.editor.gui.mesh;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This represents a triangle mesh which has functionality to dynamically create, update, and change mesh data.
 * Created by Kneesnap on 9/24/2023.
 */
public class DynamicMesh extends TriangleMesh implements IDynamicMeshHelper {
    private final String meshName;
    @Getter private final DynamicMeshTextureQuality textureQuality;
    @Getter private final TextureAtlas textureAtlas;
    @Getter private final DynamicMeshIntArray editableFaces;
    @Getter private final DynamicMeshFloatArray editableTexCoords;
    @Getter private final DynamicMeshFloatArray editableVertices;
    @Getter private final List<DynamicMeshNode> nodes = new ArrayList<>();
    @Getter private final List<DynamicMeshDataEntry> dataEntries = new ArrayList<>();
    @Getter private final List<MeshView> meshViews = new ArrayList<>(); // Tracks all views which are viewing this mesh.
    @Getter private Image materialFxImage;
    @Getter private PhongMaterial material;
    private ILogger cachedLogger;

    // It's possible to disable smoothing, using a non-empty array where the first element is zero.
    // This feature was not documented anywhere I could find, but I found the code responsible at com.sun.prism.impl.BaseMesh.checkSmoothingGroup()
    // Apply this to a mesh and smoothing should be disabled.
    private static final int[] SMOOTHING_ARRAY_DISABLE_SMOOTHING = new int[0];

    public DynamicMesh(TextureAtlas atlas, DynamicMeshTextureQuality textureQuality) {
        this(atlas, textureQuality, VertexFormat.POINT_TEXCOORD, null);
    }

    public DynamicMesh(TextureAtlas atlas, DynamicMeshTextureQuality textureQuality, String meshName) {
        this(atlas, textureQuality, VertexFormat.POINT_TEXCOORD, meshName);
    }

    public DynamicMesh(TextureAtlas atlas, DynamicMeshTextureQuality textureQuality, VertexFormat format, String meshName) {
        super(format);
        this.textureQuality = textureQuality;
        this.meshName = meshName;
        this.textureAtlas = atlas;
        if (atlas != null) {
            this.textureAtlas.getTextureSource().setMesh(this);
            this.textureAtlas.getImageChangeListeners().add(this::onTextureChange);
            updateMaterial(atlas.getImage());
        }

        // Setup editable array batches.
        this.editableFaces = new DynamicMeshIntArray(this, "face", getFaces(), getFaceElementSize(), this::updateEntryFacesAfterWrite);
        this.editableTexCoords = new DynamicMeshFloatArray(this, "texCoord", getTexCoords(), format.getTexCoordIndexOffset(), getTexCoordElementSize(), this::updateEntryTexCoordsAfterWrite);
        this.editableVertices = new DynamicMeshFloatArray(this, "vertex", getPoints(), format.getPointIndexOffset(), getPointElementSize(), this::updateEntryVerticesAfterWrite);
        getFaceSmoothingGroups().setAll(SMOOTHING_ARRAY_DISABLE_SMOOTHING); // Disable smoothing.
    }

    /**
     * Gets the logger used for this mesh.
     */
    public ILogger getLogger() {
        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = new LazyInstanceLogger(null, DynamicMesh::getLoggerInfo, this);
    }

    /**
     * Gets the logger info.
     */
    public String getLoggerInfo() {
        return Utils.getSimpleName(this) + ".Name='" + getMeshName() + "'";
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
     * Updates entry vertex start indices for all entries, as a response to buffered data being applied.
     * This must be called AFTER the changes have been applied.
     */
    void updateEntryVerticesAfterWrite() {
        int vertexStartIndex = 0;
        for (int i = 0; i < this.dataEntries.size(); i++) {
            DynamicMeshDataEntry entry = this.dataEntries.get(i);
            entry.onVerticesWritten(vertexStartIndex);
            vertexStartIndex += entry.getWrittenVertexCount();
        }
    }

    /**
     * Updates entry texCoord start indices for all entries, as a response to buffered data being applied.
     * This must be called AFTER the changes have been applied.
     */
    void updateEntryTexCoordsAfterWrite() {
        int texCoordStartIndex = 0;
        for (int i = 0; i < this.dataEntries.size(); i++) {
            DynamicMeshDataEntry entry = this.dataEntries.get(i);
            entry.onTexCoordsWritten(texCoordStartIndex);
            texCoordStartIndex += entry.getWrittenTexCoordCount();
        }
    }

    /**
     * Updates entry face start indices for all entries, as a response to buffered data being applied.
     * This must be called AFTER the changes have been applied.
     */
    void updateEntryFacesAfterWrite() {
        int faceStartIndex = 0;
        for (int i = 0; i < this.dataEntries.size(); i++) {
            DynamicMeshDataEntry entry = this.dataEntries.get(i);
            entry.onFacesWritten(faceStartIndex);
            faceStartIndex += entry.getWrittenFaceCount();
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
        if (this.meshViews.isEmpty())
            onFree();

        return true;
    }

    /**
     * Called when the mesh has been free'd.
     */
    protected void onFree() {
        if (this.textureAtlas != null)
            this.textureAtlas.releaseTexture();
    }

    /**
     * Apply a new image to the material
     * @param newImage the image to apply
     */
    protected PhongMaterial updateMaterial(BufferedImage newImage) {
        return updateMaterial(FXUtils.toFXImage(newImage, false));
    }

    /**
     * Apply a new image to the material.
     * @param newFxImage the image to apply
     */
    protected PhongMaterial updateMaterial(Image newFxImage) {
        if (this.material == null) {
            this.material = Scene3DUtils.makePhongMaterial(this.materialFxImage = newFxImage, this.textureQuality);

            // Apply newly created material to meshes.
            for (int i = 0; i < this.meshViews.size(); i++)
                this.meshViews.get(i).setMaterial(this.material);

            return this.material;
        }

        // Update material image.
        if (this.materialFxImage != newFxImage) {
            this.material = Scene3DUtils.updatePhongMaterial(this.material, newFxImage, this.textureQuality);
            this.materialFxImage = newFxImage;
        }
        return this.material;
    }

    private void onTextureChange(Texture texture, BufferedImage oldImage, BufferedImage newImage, boolean didOldImageHaveAnyTransparentPixels) {
        // Apply the image to the mesh now.
        if (this.textureAtlas.getFxImage() != null) {
            updateMaterial(this.textureAtlas.getFxImage());
        } else {
            updateMaterial(newImage);
        }
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

    /**
     * Prints debugging information about the mesh.
     */
    @SuppressWarnings("CommentedOutCode")
    public void printDebugMeshInfo() {
        ILogger logger = getLogger();
        logger.info("Mesh Information" + (this.meshName != null ? "[" + this.meshName + "]:" : ":"));
        logger.info(" Texture Atlas: " + (this.textureAtlas != null ? this.textureAtlas.getAtlasWidth() + "x" + this.textureAtlas.getAtlasHeight() + " (" + this.textureAtlas.getSortedTextureList().size() + " entries)" : "None"));
        logger.info(" Vertices[EditableSize=" + this.editableVertices.size() + ",EditablePendingSize=" + this.editableVertices.pendingSize() + ",FxArraySize=" + getPoints().size() + "]");
        logger.info(" TexCoords[EditableSize=" + this.editableTexCoords.size() + ",EditablePendingSize=" + this.editableTexCoords.pendingSize() + ",FxArraySize=" + getTexCoords().size() + "]");
        logger.info(" Faces[EditableSize=" + this.editableFaces.size() + ",EditablePendingSize=" + this.editableFaces.pendingSize() + ",FxArraySize=" + getFaces().size() + "]");
        logger.info(" Active MeshViews: " + this.meshViews.size());
        logger.info(" DynamicMeshDataEntry Count: " + this.dataEntries.size());
        logger.info(" Nodes[" + this.nodes.size() + "]:");
        for (int i = 0; i < this.nodes.size(); i++) {
            DynamicMeshNode node = this.nodes.get(i);
            node.printDebugMeshNodeInfo(logger, "  - ");
        }

        // This is disabled by default, since it spams the console, but it can be helpful for debugging.
        //logger.info(" Vertices: " + Arrays.toString(this.editableVertices.getArray().toArray()));
        //logger.info(" TexCoords: " + Arrays.toString(this.editableTexCoords.getArray().toArray()));
        //logger.info(" Faces: " + Arrays.toString(this.editableFaces.getArray().toArray()));
    }

    /**
     * Unregisters the MeshView from its parent DynamicMesh, IF the parent mesh is a DynamicMesh
     * @param meshView the MeshView to unregister
     * @return success
     */
    public static boolean tryRemoveMesh(MeshView meshView) {
        return meshView != null && meshView.getMesh() instanceof DynamicMesh && ((DynamicMesh) meshView.getMesh()).removeView(meshView);
    }

    public enum DynamicMeshTextureQuality {
        LIT_BLURRY,
        UNLIT_SHARP
    }
}