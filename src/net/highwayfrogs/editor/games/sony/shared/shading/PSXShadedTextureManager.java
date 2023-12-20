package net.highwayfrogs.editor.games.sony.shared.shading;

import lombok.Getter;
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

        addPolygon(polygon, createShadedTexture(polygon));
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
        if (polygonList.size() == 0) {
            this.polygonsByShadedTexture.remove(shadedTexture);
            onShadedTextureRemoved(shadedTexture);
        }

        return true;
    }

    /**
     * Updates a polygon to ensure it renders properly.
     * @param polygon The polygon to update.
     */
    public void updatePolygon(TPolygon polygon) {
        PSXShadeTextureDefinition oldShadedTexture = this.shadedTexturesByPolygon.get(polygon);
        if (oldShadedTexture == null)
            return; // Not registered.

        PSXShadeTextureDefinition newShadedTexture = createShadedTexture(polygon);
        if (Objects.equals(oldShadedTexture, newShadedTexture))
            return; // We've got the same shaded polygon, no need to update.

        this.shadedTexturesByPolygon.put(polygon, newShadedTexture);

        // Remove from old polygon list.
        List<TPolygon> oldPolygonList = this.polygonsByShadedTexture.get(oldShadedTexture);
        oldPolygonList.remove(polygon);
        if (oldPolygonList.isEmpty())
            this.polygonsByShadedTexture.remove(oldShadedTexture, oldPolygonList);

        // Add new polygon state.
        addPolygon(polygon, newShadedTexture);
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
}