package net.highwayfrogs.editor.gui.mesh;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents a triangle mesh which has functionality to dynamically setup, update, and change mesh data.
 * Created by Kneesnap on 9/24/2023.
 */
@Getter
public abstract class DynamicMesh extends TriangleMesh {
    private final TextureAtlas textureAtlas;
    private final FXIntArray editableFaces = new FXIntArray();
    private final FXFloatArray editableTexCoords = new FXFloatArray();
    private final FXFloatArray editableVertices = new FXFloatArray();
    private final List<DynamicMeshNode> nodes = new ArrayList<>();
    private final List<DynamicMeshDataEntry> dataEntries = new ArrayList<>();
    private final List<MeshView> meshViews = new ArrayList<>(); // Tracks all views which can view this mesh.
    private PhongMaterial material;

    public DynamicMesh(TextureAtlas atlas) {
        this(atlas, VertexFormat.POINT_TEXCOORD);
    }

    public DynamicMesh(TextureAtlas atlas, VertexFormat format) {
        super(format);
        this.textureAtlas = atlas;
        this.textureAtlas.getImageChangeListeners().add(this::onTextureChange);
        updateMaterial(atlas.getImage());
    }

    /**
     * Updates mesh arrays with our data.
     */
    public void updateMeshArrays() {
        if (!this.editableFaces.isBulkRemovalEnabled())
            this.editableFaces.apply(getFaces());
        if (!this.editableTexCoords.isBulkRemovalEnabled())
            this.editableTexCoords.apply(getTexCoords());
        if (!this.editableVertices.isBulkRemovalEnabled())
            this.editableVertices.apply(getPoints());
    }

    /**
     * Enable bulk removals for all mesh array wrappers.
     */
    public void pushBulkRemovals() {
        this.editableVertices.startBulkRemovals();
        this.editableTexCoords.startBulkRemovals();
        this.editableFaces.startBulkRemovals();
    }

    /**
     * Disable bulk removals for all mesh array wrappers.
     * Attempts to update the wrapped FX arrays for any array wrapper which has an empty bulk removal stack count.
     */
    public void popBulkRemovals() {
        // End bulk removals.
        this.editableVertices.endBulkRemovals();
        this.editableTexCoords.endBulkRemovals();
        this.editableFaces.endBulkRemovals();

        // Update mesh.
        updateMeshArrays();
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