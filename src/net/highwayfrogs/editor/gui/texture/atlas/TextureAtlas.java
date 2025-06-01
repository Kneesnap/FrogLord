package net.highwayfrogs.editor.gui.texture.atlas;

import javafx.scene.image.Image;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.SimpleTexture;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.objects.SortedList;

import java.util.function.Function;

/**
 * Represents a page containing many textures.
 * Sometimes called a 'texture page' or 'texture sheet'.
 * Back-ported from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
@Getter
public abstract class TextureAtlas extends SimpleTexture {
    @Setter(AccessLevel.PACKAGE) private int atlasWidth;
    @Setter(AccessLevel.PACKAGE) private int atlasHeight;
    @Setter private boolean automaticResizingEnabled;

    @SuppressWarnings("unchecked")
    public TextureAtlas(Function<Texture, ? super AtlasBuilderTextureSource> source, int width, int height, boolean allowAutomaticResizing) {
        super((Function<Texture, ITextureSource>) source);
        if (width < 1)
            throw new IllegalArgumentException("Width of texture atlas cannot be set below one! (Got: " + width + ")");
        if (height < 1)
            throw new IllegalArgumentException("Height of texture atlas cannot be set below one! (Got: " + height + ")");

        this.atlasWidth = width;
        this.atlasHeight = height;
        this.automaticResizingEnabled = allowAutomaticResizing;
    }

    @Override
    public AtlasBuilderTextureSource getTextureSource() {
        return (AtlasBuilderTextureSource) super.getTextureSource();
    }

    @Override
    public void disposeTexture() {
        super.disposeTexture();
        getTextureSource().releaseResources();
    }

    @Override
    protected void ensureNotDisposed() {
        if (!isDisposed())
            return;

        DynamicMesh mesh = getTextureSource().getMesh();
        if (mesh != null)
            mesh.getLogger().severe("The associated %s has been freed, but something is still using it!", Utils.getSimpleName(this));

        super.ensureNotDisposed();
    }

    /**
     * Gets the updated JavaFX image containing the full texture sheet.
     */
    public Image getFxImage() {
        ensureNotDisposed();
        return getTextureSource().getCachedFxImage();
    }

    /**
     * Gets an iterable object which iterates through all textures registered to the atlas.
     */
    public abstract Iterable<AtlasTexture> getTextures();

    /**
     * Gets a sorted list of textures, however configuration has occurred to sort them.
     */
    public abstract SortedList<? extends AtlasTexture> getSortedTextureList();

    /**
     * Runs the algorithm which determines where all the textures go.
     */
    public abstract void rebuildTexturePositions();

    /**
     * Checks if a given texture is in this atlas.
     * @param texture The texture to check.
     * @return isTexturePresent
     */
    public abstract boolean containsTexture(AtlasTexture texture);

    /**
     * Adds a texture to this atlas.
     * @param textureSource The texture source to add.
     * @return The texture which has been added.
     */
    public abstract AtlasTexture addTexture(ITextureSource textureSource);

    /**
     * Removes a texture from this atlas.
     * @param textureSource The texture source to remove.
     * @return The texture which used that texture source that was removed. Null if no texture was removed
     */
    public abstract AtlasTexture removeTexture(ITextureSource textureSource);

    /**
     * Removes a texture from this atlas and disposes it.
     * @param textureSource The texture source to remove.
     * @return Whether the texture was removed.
     */
    public boolean removeAndDisposeTexture(ITextureSource textureSource) {
        AtlasTexture texture = removeTexture(textureSource);
        if (texture == null)
            return false;

        texture.disposeTexture();
        return true;
    }

    /**
     * Removes a texture from this atlas.
     * @param texture The texture to remove.
     * @return Whether the texture was removed.
     */
    public abstract boolean removeTexture(AtlasTexture texture);

    /**
     * Removes a texture from this atlas, and disposes it.
     * @param texture The texture to remove.
     * @return Whether the texture was removed.
     */
    public boolean removeAndDisposeTexture(AtlasTexture texture) {
        if (!removeTexture(texture))
            return false;

        texture.disposeTexture();
        return true;
    }

    /**
     * Gets the texture which is returned if a texture is not found.
     * @return The currently active fallback texture.
     */
    public abstract AtlasTexture getFallbackTexture();

    /**
     * Sets the texture which is returned if a texture is not found.
     * Registered into the atlas automatically.
     * @param texture The texture to set as the fallback texture.
     */
    public abstract void setFallbackTexture(ITextureSource texture);

    /**
     * Gets a texture from its source, returning the fallback texture if no such texture is tracked.
     * @param textureSource The texture source to get the texture from.
     * @return The texture, if it is found. If the source does not have anything tracked, the fallback is provided.
     */
    public abstract AtlasTexture getTextureFromSource(ITextureSource textureSource);

    /**
     * Gets a texture from its source, returning null if no such texture is tracked.
     * @param textureSource The texture source to get the texture from.
     * @return The texture, if it is found. If the source does not have anything tracked, null is provided.
     */
    public abstract AtlasTexture getNullTextureFromSource(ITextureSource textureSource);

    /**
     * Gets a texture from its source, returning the fallback texture if no such texture is tracked.
     * @param textureSource The texture source to get the texture from.
     * @return The texture, if it is found. If the source does not have anything tracked, the fallback texture is provided.
     */
    public AtlasTexture getTextureFromSourceOrFallback(ITextureSource textureSource) {
        ensureNotDisposed();
        AtlasTexture texture = getNullTextureFromSource(textureSource);
        return texture != null ? texture : getFallbackTexture();
    }

    /**
     * Marks texture sizes as dirty, potentially causing an update.
     * Events will be fired.
     */
    public final void markTextureSizesDirty() {
        markTextureSizesDirty(true);
    }

    /**
     * Marks texture sizes as dirty, potentially causing an update.
     * @param fireEvents If events should be fired.
     */
    public abstract void markTextureSizesDirty(boolean fireEvents);

    /**
     * Marks texture positions as dirty, potentially causing an update.
     * Events will be fired.
     */
    public final void markPositionsDirty() {
        markPositionsDirty(true);
    }

    /**
     * Marks texture positions as dirty, potentially causing an update.
     * @param fireEvents If events should be fired.
     */
    public abstract void markPositionsDirty(boolean fireEvents);

    /**
     * Rebuilds the atlas according to the implemented algorithm.
     */
    public abstract void rebuild();

    /**
     * Prepares the data for image generation.
     */
    public abstract void prepareImageGeneration();
}