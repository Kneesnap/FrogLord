package net.highwayfrogs.editor.games.sony.shared.shading;

import lombok.Getter;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshNode;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;

import java.util.*;

/**
 * Tracks shaded textures to their polygons.
 * Created by Kneesnap on 12/19/2023.
 */
public abstract class PSXShadedTextureManager<TPolygon> {
    private final Map<PSXShadeTextureDefinition, List<TPolygon>> polygonsByShadedTexture = new HashMap<>();
    private final Map<TPolygon, PSXShadeTextureDefinition> shadedTexturesByPolygon = new HashMap<>();

    /**
     * Gets a list of polygons belonging to a shaded texture.
     * @param shadedTexture The shaded texture to get polygons from.
     * @return polygons, can be null.
     */
    public List<TPolygon> getPolygonsFromShadedTexture(PSXShadeTextureDefinition shadedTexture) {
        return this.polygonsByShadedTexture.get(shadedTexture);
    }

    /**
     * Gets a cached shaded texture created by the polygon.
     * @param polygon The polygon to get the shaded texture from.
     * @return shaded texture.
     */
    public PSXShadeTextureDefinition getShadedTexture(TPolygon polygon) {
        return this.shadedTexturesByPolygon.get(polygon);
    }

    /**
     * Adds a polygon to the tracker.
     * @param polygon The polygon to add.
     */
    public void addPolygon(TPolygon polygon) {
        if (this.shadedTexturesByPolygon.containsKey(polygon))
            throw new RuntimeException("The provided polygon is already tracked.");

        PSXShadeTextureDefinition newDefinition = createShadedTexture(polygon);
        if (isValid(newDefinition, polygon))
            addPolygon(polygon, newDefinition);
    }

    /**
     * Removes a polygon from the tracker.
     * @param polygon The polygon to add.
     */
    public boolean removePolygon(TPolygon polygon) {
        PSXShadeTextureDefinition shadedTexture = this.shadedTexturesByPolygon.remove(polygon);
        if (shadedTexture == null)
            return false; // Not registered.

        List<TPolygon> polygonList = this.polygonsByShadedTexture.get(shadedTexture);
        if (polygonList == null || !polygonList.remove(polygon))
            return false; // Bugged?

        // If the list is empty, clean up.
        if (polygonList.isEmpty()) {
            this.polygonsByShadedTexture.remove(shadedTexture);
            onShadedTextureRemoved(shadedTexture);
        }

        return true;
    }

    /**
     * Called to free the textures tracked by this manager
     */
    public void onDispose() {
        for (PSXShadeTextureDefinition textureDefinition : this.polygonsByShadedTexture.keySet())
            textureDefinition.onDispose();

        this.shadedTexturesByPolygon.clear();
        this.polygonsByShadedTexture.clear();
    }

    /**
     * Updates a polygon to use a new shade definition.
     * @param polygon The polygon to update.
     */
    public boolean updatePolygon(TPolygon polygon, PSXShadeTextureDefinition newShadedTexture) {
        if (newShadedTexture == null)
            throw new NullPointerException("newShadedTexture");

        PSXShadeTextureDefinition oldShadedTexture = this.shadedTexturesByPolygon.get(polygon);
        if (oldShadedTexture == null)
            return false; // Not registered.

        if (Objects.equals(oldShadedTexture, newShadedTexture))
            return false; // We've got the same shaded polygon, no need to update.

        // Remove from old polygon list.
        List<TPolygon> oldPolygonList = this.polygonsByShadedTexture.get(oldShadedTexture);
        List<TPolygon> newPolygonList = this.polygonsByShadedTexture.get(newShadedTexture);
        if (oldPolygonList.size() == 1 && Objects.equals(oldPolygonList.get(0), polygon)) {
            this.polygonsByShadedTexture.remove(oldShadedTexture, oldPolygonList);
            onShadedTextureRemoved(oldShadedTexture);

            // If the destination list doesn't exist, re-register the one we just got unregistered.
            // But register it to the new shading definition.
            if (newPolygonList == null) {
                this.polygonsByShadedTexture.put(newShadedTexture, oldPolygonList);
                this.shadedTexturesByPolygon.put(polygon, newShadedTexture);
                onShadedTextureAdded(newShadedTexture, polygon);
                applyTextureShading(polygon, newShadedTexture);
                return true;
            }
        } else {
            oldPolygonList.remove(polygon);
        }

        if (newPolygonList != null) {
            // Re-use the same object instance between different polygons.
            this.shadedTexturesByPolygon.put(polygon, this.shadedTexturesByPolygon.get(newPolygonList.get(0)));
        } else {
            // This shading definition isn't currently tracked, let's register it.
            newPolygonList = new ArrayList<>();
            this.polygonsByShadedTexture.put(newShadedTexture, newPolygonList);
            this.shadedTexturesByPolygon.put(polygon, newShadedTexture);
            onShadedTextureAdded(newShadedTexture, polygon);
        }

        newPolygonList.add(polygon);
        applyTextureShading(polygon, newShadedTexture);
        return true;
    }

    private void addPolygon(TPolygon polygon, PSXShadeTextureDefinition newShadedTexture) {
        List<TPolygon> newPolygonList = this.polygonsByShadedTexture.get(newShadedTexture);

        if (newPolygonList != null) {
            newPolygonList.add(polygon);
            this.shadedTexturesByPolygon.put(polygon, this.shadedTexturesByPolygon.get(newPolygonList.get(0)));
        } else {
            newPolygonList = new ArrayList<>();
            newPolygonList.add(polygon);
            this.polygonsByShadedTexture.put(newShadedTexture, newPolygonList);
            this.shadedTexturesByPolygon.put(polygon, newShadedTexture);
            onShadedTextureAdded(newShadedTexture, polygon);
        }
    }

    /**
     * Creates a shaded texture for the given polygon.
     * @param polygon The polygon to create the texture for.
     * @return newShadedTexture
     */
    protected abstract PSXShadeTextureDefinition createShadedTexture(TPolygon polygon);

    /**
     * Applies texture shading from a shaded texture back to the given polygon.
     * @param polygon The polygon to create the texture for.
     * @param shadedTexture The shaded texture to apply to the polygon.
     */
    protected abstract void applyTextureShading(TPolygon polygon, PSXShadeTextureDefinition shadedTexture);

    /**
     * Test if a definition is valid.
     * @param definition The definition to test validity of
     * @param polygon The polygon it belongs to.
     * @return isValid
     */
    protected boolean isValid(PSXShadeTextureDefinition definition, TPolygon polygon) {
        return definition != null && (!definition.isTextured() || definition.getTextureSource() != null);
    }

    /**
     * Called when a shaded texture is added.
     * @param shadedTexture The shaded texture which has been added.
     * @param firstPolygon  The first polygon associated with this shaded texture.
     */
    protected void onShadedTextureAdded(PSXShadeTextureDefinition shadedTexture, TPolygon firstPolygon) {
        shadedTexture.onRegister();
    }

    /**
     * Called when a shaded texture is added.
     * @param shadedTexture The shaded texture which has been added.
     */
    protected void onShadedTextureRemoved(PSXShadeTextureDefinition shadedTexture) {
        shadedTexture.onDispose();
    }

    @Getter
    public static abstract class PSXShadedTextureAtlasManager<TPolygon> extends PSXShadedTextureManager<TPolygon> {
        private final TextureAtlas textureAtlas;

        public PSXShadedTextureAtlasManager(TextureAtlas atlas) {
            this.textureAtlas = atlas;
        }

        @Override
        protected void onShadedTextureAdded(PSXShadeTextureDefinition shadedTexture, TPolygon firstPolygon) {
            super.onShadedTextureAdded(shadedTexture, firstPolygon);
            this.textureAtlas.addTexture(shadedTexture);
        }

        @Override
        protected void onShadedTextureRemoved(PSXShadeTextureDefinition shadedTexture) {
            super.onShadedTextureRemoved(shadedTexture);
            this.textureAtlas.removeTexture(shadedTexture);
        }
    }

    @Getter
    public static abstract class PSXMeshShadedTextureManager<TPolygon> extends PSXShadedTextureAtlasManager<TPolygon> {
        private final DynamicMesh mesh;

        public PSXMeshShadedTextureManager(DynamicMesh mesh) {
            super(mesh.getTextureAtlas());
            this.mesh = mesh;
        }

        /**
         * Updates all polygons tracked to be using the shade texture definition.
         * @param shadeTexture the shade texture to update.
         * @param seenNodes a set containing all nodes that use the shade texture definition on at least a single face.
         */
        @SuppressWarnings("unchecked")
        public void updateTextureCoordinates(PSXShadeTextureDefinition shadeTexture, Set<DynamicMeshNode> seenNodes) {
            List<TPolygon> polygonsUsingTexture = getPolygonsFromShadedTexture(shadeTexture);
            if (polygonsUsingTexture == null || polygonsUsingTexture.isEmpty())
                return;

            // Update each polygon's entry.
            this.mesh.getEditableTexCoords().startBatchingUpdates();
            for (int i = 0; i < this.mesh.getNodes().size(); i++) {
                DynamicMeshNode node = this.mesh.getNodes().get(i);
                if (!(node instanceof DynamicMeshAdapterNode<?>))
                    continue;

                // Careful here. We may need to add some kind of type-checking if it gets much worse.
                DynamicMeshAdapterNode<TPolygon> nodeForcedType = (DynamicMeshAdapterNode<TPolygon>) node;
                for (int j = 0; j < polygonsUsingTexture.size(); j++) {
                    TPolygon polygon = polygonsUsingTexture.get(j);
                    if (nodeForcedType.getDataEntry(polygon) != null) {
                        nodeForcedType.updateTexCoords(polygon);
                        if (seenNodes != null)
                            seenNodes.add(node);
                    }
                }
            }

            this.mesh.getEditableTexCoords().endBatchingUpdates();
        }
    }
}