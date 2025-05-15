package net.highwayfrogs.editor.gui.texture.atlas;

import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.QuadConsumer;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.objects.SortedList;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Represents a page containing many textures.
 * Sometimes called a 'texture atlas'.
 * The width / height of a texture page should always be a power of two.
 * Back-ported from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
public abstract class BasicTextureAtlas<TTexture extends AtlasTexture> extends TextureAtlas {
    private final BiFunction<TextureAtlas, ITextureSource, TTexture> atlasTextureConstructor;
    private final QuadConsumer<Texture, BufferedImage, BufferedImage, Boolean> imageChangeListener;
    private final Map<ITextureSource, TTexture> texturesBySource = new HashMap<>();
    private final SortedList<TTexture> sortedTextures;
    private TTexture fallbackTexture;
    private boolean cachedPositionsInvalid = true;
    private boolean cachedTextureSizesInvalid;

    public BasicTextureAtlas(int startingWidth, int startingHeight, boolean allowAutomaticResizing, BiFunction<TextureAtlas, ITextureSource, TTexture> atlasTextureConstructor) {
        super(atlas -> new AtlasBuilderTextureSource((TextureAtlas) atlas, false, true), startingWidth, startingHeight, allowAutomaticResizing);
        this.atlasTextureConstructor = atlasTextureConstructor;
        this.sortedTextures = makeSortedTextureList();
        this.imageChangeListener = this::onTextureChange;
    }

    @Override
    public void registerTexture() {
        super.registerTexture();
        this.sortedTextures.forEach(TTexture::registerTexture);
    }

    @Override
    public void unregisterTexture() {
        super.unregisterTexture();
        this.sortedTextures.forEach(TTexture::unregisterTexture);
    }

    @Override
    public void disposeTexture() {
        super.disposeTexture();
        for (int i = 0; i < this.sortedTextures.size(); i++) {
            TTexture texture = this.sortedTextures.get(i);
            texture.getImageChangeListeners().remove(this.imageChangeListener);
            texture.disposeTexture();
        }

        // Clear data.
        this.texturesBySource.clear();
        this.sortedTextures.clear();
        this.fallbackTexture = null;
    }

    /**
     * Makes a sorted texture list.
     */
    protected SortedList<TTexture> makeSortedTextureList() {
        return new SortedList<>(Comparator.comparingInt((TTexture texture) -> texture.getPaddedWidth() * texture.getPaddedHeight()).reversed()
                .thenComparingLong(Texture::getUniqueId)); // Need something instant and constant for sorting/lookup order consistency.
    }

    @Override
    public SortedList<? extends AtlasTexture> getSortedTextureList() {
        return this.sortedTextures;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<AtlasTexture> getTextures() {
        ensureNotDisposed();
        return (Iterable<AtlasTexture>) this.sortedTextures;
    }

    @Override
    @SuppressWarnings({"SuspiciousMethodCalls"})
    public boolean containsTexture(AtlasTexture texture) {
        return this.sortedTextures.contains(texture);
    }

    @Override
    public TTexture getTextureFromSource(ITextureSource textureSource) {
        ensureNotDisposed();
        return this.texturesBySource.getOrDefault(textureSource, this.fallbackTexture);
    }

    @Override
    public TTexture getNullTextureFromSource(ITextureSource textureSource) {
        ensureNotDisposed();
        return this.texturesBySource.get(textureSource);
    }

    @Override // This is a duplicate method since this method inherits from Texture, instead of just TextureAtlas.
    public Texture getChildTextureBySource(ITextureSource source, boolean throwErrorIfNotFound) {
        ensureNotDisposed();
        TTexture texture = this.texturesBySource.get(source);
        if (texture != null)
            return texture;

        return super.getChildTextureBySource(source, throwErrorIfNotFound);
    }

    @Override
    public Vector2f getUV(Texture heldTexture, int x, int y, Vector2f result) {
        if (heldTexture == this)
            return super.getUV(heldTexture, x, y, result);

        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (!(heldTexture instanceof AtlasTexture))
            throw new IllegalArgumentException("Provided texture " + Utils.getSimpleName(heldTexture) + "is not held by this atlas.");
        // This check causes a pretty significant slowdown when updating texture UVs.
        // Additionally, if the texture isn't in the atlas, it displays as an unknown texture, making it easy to identify.
        //if (((AtlasTexture) heldTexture).getAtlas() != this || !this.texturesBySource.containsKey(heldTexture.getTextureSource()))
        //    throw new IllegalStateException("Tried to get the UV of a texture which wasn't held by this atlas!");

        AtlasTexture atlasTexture = (AtlasTexture) heldTexture;
        int xPos = atlasTexture.getX() + atlasTexture.getLeftPaddingEmpty() + atlasTexture.getLeftPadding() + x;
        int yPos = atlasTexture.getY() + atlasTexture.getUpPaddingEmpty() + atlasTexture.getUpPadding() + y;
        result.setX((float) xPos / getAtlasWidth());
        result.setY((float) yPos / getAtlasHeight());
        return result;
    }

    @Override
    public Vector2f getUV(Texture heldTexture, Vector2f localUv, Vector2f result) {
        if (heldTexture == this)
            return super.getUV(heldTexture, localUv, result);

        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (!(heldTexture instanceof AtlasTexture))
            throw new IllegalArgumentException("Provided texture " + Utils.getSimpleName(heldTexture) + " is not held by this atlas.");
        // This check causes a pretty significant slowdown when updating texture UVs.
        // Additionally, if the texture isn't in the atlas, it displays as an unknown texture, making it easy to identify.
        // if (((AtlasTexture) heldTexture).getAtlas() != this || !this.texturesBySource.containsKey(heldTexture.getTextureSource()))
        //    throw new IllegalStateException("Tried to get the UV of a texture which wasn't held by this atlas!");

        AtlasTexture atlasTexture = (AtlasTexture) heldTexture;
        int baseX = (atlasTexture.getX() + atlasTexture.getLeftPaddingEmpty() + atlasTexture.getLeftPadding());
        int baseY = (atlasTexture.getY() + atlasTexture.getUpPaddingEmpty() + atlasTexture.getUpPadding());
        result.setX((baseX + (localUv.getX() * heldTexture.getWidthWithoutPadding())) / getAtlasWidth());
        result.setY((baseY + (localUv.getY() * heldTexture.getHeightWithoutPadding())) / getAtlasHeight());
        return result;
    }

    private void onTextureChange(Texture texture, BufferedImage oldImage, BufferedImage newImage, boolean didOldImageHaveAnyTransparency) {
        if (getTextureSource().isCurrentlyBuildingTexture())
            return; // Ignore texture changes while atlas building occurs.

        ensureNotDisposed();
        if (oldImage == null || (oldImage.getWidth() != newImage.getWidth()) || (oldImage.getHeight() != newImage.getHeight()))
            this.markTextureSizesDirty(false);

        markImageDirty();
        if (!shouldDisableUpdates())
           getTextureSource().fireChangeEvent();
    }

    @Override
    public TTexture getFallbackTexture() {
        return this.fallbackTexture;
    }

    @Override
    public void setFallbackTexture(ITextureSource texture) {
        this.fallbackTexture = addTexture(texture);
    }

    @Override
    public TTexture addTexture(ITextureSource textureSource) {
        ensureNotDisposed();
        if (textureSource == null)
            throw new NullPointerException("textureSource");

        TTexture existingTexture = this.getNullTextureFromSource(textureSource);
        if (existingTexture != null)
            return existingTexture; // If there's already a texture for this source, just return it.

        if (textureSource.getWidth() <= 0 || textureSource.getHeight() <= 0)
            throw new RuntimeException("The " + Utils.getSimpleName(textureSource) + " gave invalid dimensions of " + textureSource.getWidth() + "x" + textureSource.getHeight());

        TTexture newTexture = this.atlasTextureConstructor.apply(this, textureSource);
        if (!this.sortedTextures.add(newTexture) || this.texturesBySource.put(textureSource, newTexture) != null)
            throw new RuntimeException("The texture atlas didn't think the texture source was registered, but when we tried to register it, it was.");

        newTexture.registerTexture();
        newTexture.getImageChangeListeners().add(this.imageChangeListener);
        if (placeTexture(newTexture)) {
            if (!shouldDisableUpdates())
                getTextureSource().fireChangeEvent();
        } else {
            this.markPositionsDirty();
        }

        return newTexture;
    }

    /**
     * Removes a texture from this atlas.
     * @param textureSource The texture source to remove.
     * @return The texture which used that texture source that was removed, if one was removed.
     */
    @Override
    public TTexture removeTexture(ITextureSource textureSource) {
        ensureNotDisposed();
        if (textureSource == null)
            throw new NullPointerException("textureSource");

        TTexture texture = getNullTextureFromSource(textureSource);
        if (texture != null && !this.removeTexture(texture))
            throw new RuntimeException("Failed to remove textureSource. (Internal error?)");

        return texture;
    }

    /**
     * Removes a texture from this atlas.
     * @param texture The texture to remove.
     * @return Whether the texture was removed.
     */
    @Override
    @SuppressWarnings({"SuspiciousMethodCalls", "unchecked"})
    public boolean removeTexture(AtlasTexture texture) {
        ensureNotDisposed();
        if (texture == null)
            throw new NullPointerException("texture");

        if (this.sortedTextures.remove(texture) && this.texturesBySource.remove(texture.getTextureSource(), texture)) {
            texture.unregisterTexture();
            texture.getImageChangeListeners().remove(this.imageChangeListener);
            if (this.fallbackTexture == texture)
                this.fallbackTexture = null;

            freeTexture((TTexture) texture);
            return true;
        }

        // No need to invalidate, since removing an image doesn't actually make anything not work.
        return false;
    }

    @Override
    public void rebuild() {
        ensureNotDisposed();
        this.cachedPositionsInvalid = true;
        this.cachedTextureSizesInvalid = true;
        this.update();
    }

    /**
     * Marks texture sizes as dirty, potentially causing an update.
     */
    @Override
    public void markTextureSizesDirty(boolean fireEvents) {
        ensureNotDisposed();
        this.cachedTextureSizesInvalid = true;
        this.cachedPositionsInvalid = true;
        if (fireEvents && !shouldDisableUpdates())
            getTextureSource().fireChangeEvent();
    }

    /**
     * Marks texture positions as dirty, potentially causing an update.
     */
    @Override
    public void markPositionsDirty(boolean fireEvents) {
        ensureNotDisposed();
        this.cachedPositionsInvalid = true;
        if (fireEvents && !shouldDisableUpdates())
            getTextureSource().fireChangeEvent();
    }

    @Override
    public void update(BufferedImage newImage) {
        ensureNotDisposed();
        if (shouldDisableUpdates())
            return;

        // Update image.
        prepareImageGeneration();
        super.update(newImage);
    }

    /**
     * Prepares the data for image generation.
     */
    public void prepareImageGeneration() {
        ensureNotDisposed();
        boolean cachedTextureSizesInvalid = this.cachedTextureSizesInvalid;

        // Update texture sizes.
        if (this.cachedTextureSizesInvalid) {
            this.cachedTextureSizesInvalid = false;
            this.sortedTextures.update();
        }

        // Update positions.
        if (cachedTextureSizesInvalid || this.cachedPositionsInvalid)
            this.rebuildTexturePositions();
    }

    @Override
    public void rebuildTexturePositions() {
        ensureNotDisposed();
        this.pushDisableUpdates();

        boolean ranOutOfSpace = !this.updatePositions(this.sortedTextures);
        if (ranOutOfSpace && this.sortedTextures.size() > 0) {
            if (!isAutomaticResizingEnabled())
                throw new RuntimeException("The texture atlas is full, and automatic resizing is disabled.");

            setAtlasWidth(getAtlasWidth() * 2);
            setAtlasHeight(getAtlasHeight() * 2);
            this.rebuildTexturePositions();
        }

        this.cachedPositionsInvalid = false; // Prevents recursive looping.
        this.markImageDirty();
        this.popDisableUpdates();
    }

    /**
     * Algorithmic implementation which updates positions of all textures.
     * @param sortedTextureList The list of textures to place, sorted from largest to smallest.
     * @return Returns true if the atlas ran out of space.
     */
    protected boolean updatePositions(SortedList<TTexture> sortedTextureList) {
        ensureNotDisposed();
        for (int i = 0; i < sortedTextureList.size(); i++) {
            TTexture texture = sortedTextureList.get(i);
            if (!placeTexture(texture))
                return false; // Ran out of space.
        }

        return true;
    }

    /**
     * Algorithmic implementation which updates positions of a texture.
     * @param texture the texture to place
     * @return Returns true if the texture was placed successfully.
     */
    protected abstract boolean placeTexture(TTexture texture);

    /**
     * Algorithmic implementation which frees the positional area covered by the texture.
     * @param texture the texture to free
     */
    protected abstract void freeTexture(TTexture texture);
}