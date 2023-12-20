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
 * TODO: System for bulking changes to arrays.
 * TODO: Once things are functional, consider ways of optimizing.
 * - Removals and additions may be bulked together for index updates and fast adding / removal.
 * - But overall all changes (including in-place ones like updating values) should operate on a wrapped array. If setAll() is the fastest way to update things in JavaFX, we'll probably go exclusively that route. I think we need to figure out what the fastest ways of updating things are though.
 * Created by Kneesnap on 9/24/2023.
 */
@Getter
public abstract class DynamicMesh extends TriangleMesh {
    private final TextureAtlas textureAtlas;
    private final List<DynamicMeshNode> nodes = new ArrayList<>();
    private final List<DynamicMeshDataEntry> dataEntries = new ArrayList<>();
    private final List<MeshView> meshViews = new ArrayList<>(); // Tracks all of the views which can view this mesh.
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