package net.highwayfrogs.editor.gui.texture.atlas;

import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.QuadConsumer;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.SortedList;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
        super(atlas -> new AtlasBuilderTextureSource((TextureAtlas) atlas), startingWidth, startingHeight, allowAutomaticResizing);
        this.atlasTextureConstructor = atlasTextureConstructor;
        this.sortedTextures = makeSortedTextureList();
        this.imageChangeListener = this::onTextureChange;
    }

    /**
     * Makes a sorted texture list.
     */
    protected SortedList<TTexture> makeSortedTextureList() {
        return new SortedList<>(Comparator.comparingInt((TTexture texture) -> texture.getPaddedWidth() * texture.getPaddedHeight()).reversed().thenComparingInt(Objects::hashCode));
    }

    @Override
    public SortedList<? extends AtlasTexture> getSortedTextureList() {
        return this.sortedTextures;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<AtlasTexture> getTextures() {
        return (Iterable<AtlasTexture>) this.sortedTextures;
    }

    @Override
    @SuppressWarnings({"SuspiciousMethodCalls"})
    public boolean containsTexture(AtlasTexture texture) {
        return this.sortedTextures.contains(texture);
    }

    @Override
    public TTexture getTextureFromSource(ITextureSource textureSource) {
        return this.texturesBySource.getOrDefault(textureSource, this.fallbackTexture);
    }

    @Override
    public TTexture getNullTextureFromSource(ITextureSource textureSource) {
        return this.texturesBySource.get(textureSource);
    }

    @Override // This is a duplicateish method since this method inherits from Texture, instead of just TextureAtlas.
    public Texture getChildTextureBySource(ITextureSource source, boolean throwErrorIfNotFound) {
        TTexture texture = this.texturesBySource.get(source);
        if (texture != null)
            return texture;

        return super.getChildTextureBySource(source, throwErrorIfNotFound);
    }

    @Override
    public Vector2f getUV(Texture heldTexture, int x, int y) {
        if (heldTexture == this)
            return super.getUV(heldTexture, x, y);

        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (!(heldTexture instanceof AtlasTexture))
            throw new IllegalArgumentException("Provided texture " + Utils.getSimpleName(heldTexture) + "is not held by this atlas.");
        if (!this.texturesBySource.containsKey(heldTexture.getTextureSource()))
            throw new IllegalStateException("Tried to get the UV of a texture which wasn't held by this atlas!");

        AtlasTexture atlasTexture = (AtlasTexture) heldTexture;
        float xPos = atlasTexture.getX() + atlasTexture.getLeftPadding() + atlasTexture.getLeftPaddingEmpty() + x;
        float yPos = atlasTexture.getY() + atlasTexture.getUpPadding() + atlasTexture.getUpPaddingEmpty() + y;
        return new Vector2f(xPos / getPaddedWidth(), yPos / getPaddedHeight());
    }

    @Override
    public Vector2f getUV(Texture heldTexture, Vector2f localUv) {
        if (heldTexture == this)
            return super.getUV(heldTexture, localUv);

        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (!(heldTexture instanceof AtlasTexture))
            throw new IllegalArgumentException("Provided texture " + Utils.getSimpleName(heldTexture) + " is not held by this atlas.");
        if (!this.texturesBySource.containsKey(heldTexture.getTextureSource()))
            throw new IllegalStateException("Tried to get the UV of a texture which wasn't held by this atlas!");

        AtlasTexture atlasTexture = (AtlasTexture) heldTexture;
        float baseU = (float) (atlasTexture.getX() + atlasTexture.getLeftPadding() + atlasTexture.getLeftPaddingEmpty()) / this.getPaddedWidth();
        float baseV = (float) (atlasTexture.getY() + atlasTexture.getUpPadding() + atlasTexture.getUpPaddingEmpty()) / this.getPaddedHeight();
        float localU = (localUv.getX() * heldTexture.getWidth()) / this.getPaddedWidth();
        float localV = (localUv.getY() * heldTexture.getHeight()) / this.getPaddedHeight();
        return new Vector2f(baseU + localU, baseV + localV);
    }

    private void onTextureChange(Texture texture, BufferedImage oldImage, BufferedImage newImage, boolean didOldImageHaveAnyTransparency) {
        if (oldImage == null || (oldImage.getWidth() != newImage.getWidth()) || (oldImage.getHeight() != newImage.getHeight()))
            this.markTextureSizesDirty(false);
        getTextureSource().fireChangeEvent();
    }

    @Override
    public Texture getFallbackTexture() {
        return this.fallbackTexture;
    }

    @Override
    public void setFallbackTexture(ITextureSource texture) {
        this.fallbackTexture = addTexture(texture);
    }

    @Override
    public TTexture addTexture(ITextureSource textureSource) {
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

        this.markPositionsDirty();
        newTexture.getImageChangeListeners().add(this.imageChangeListener);
        return newTexture;
    }

    /**
     * Removes a texture from this atlas.
     * @param textureSource The texture source to remove.
     * @return The texture which used that texture source that was removed, if one was removed.
     */
    @Override
    public TTexture removeTexture(ITextureSource textureSource) {
        if (textureSource == null)
            throw new NullPointerException("textureSource");

        TTexture texture = getNullTextureFromSource(textureSource);
        if (texture != null)
            this.removeTexture(texture);

        return texture;
    }

    /**
     * Removes a texture from this atlas.
     * @param texture The texture to remove.
     * @return Whether the texture was removed.
     */
    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean removeTexture(AtlasTexture texture) {
        if (texture == null)
            throw new NullPointerException("texture");

        if (this.sortedTextures.remove(texture)) {
            this.texturesBySource.remove(texture.getTextureSource());
            texture.getImageChangeListeners().remove(this.imageChangeListener);
            if (this.fallbackTexture == texture)
                this.fallbackTexture = null;
            return true;
        }

        // No need to invalidate, since removing an image doesn't actually make anything not work.
        return false;
    }

    @Override
    public void rebuild() {
        this.cachedPositionsInvalid = true;
        this.cachedTextureSizesInvalid = true;
        this.update();
    }

    /**
     * Marks texture sizes as dirty, potentially causing an update.
     */
    @Override
    public void markTextureSizesDirty(boolean fireEvents) {
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
        this.cachedPositionsInvalid = true;
        if (fireEvents && !shouldDisableUpdates())
            getTextureSource().fireChangeEvent();
    }

    @Override
    public void update(BufferedImage newImage) {
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
        boolean cachedTextureSizesInvalid = this.cachedTextureSizesInvalid;

        // Update texture sizes.
        if (this.cachedTextureSizesInvalid) {
            this.cachedTextureSizesInvalid = false;
            this.sortedTextures.update();
        }

        // Update positions.
        if (cachedTextureSizesInvalid || this.cachedPositionsInvalid) {
            this.cachedPositionsInvalid = false;
            this.rebuildTexturePositions();
            this.markImageDirty();
        }
    }

    @Override
    public void rebuildTexturePositions() {
        this.pushDisableUpdates();

        boolean ranOutOfSpace = this.updatePositions(this.sortedTextures);
        if (ranOutOfSpace) {
            if (!isAutomaticResizingEnabled())
                throw new RuntimeException("The texture atlas is full, and automatic resizing is disabled.");

            setAtlasWidth(getAtlasWidth() * 2);
            setAtlasHeight(getAtlasHeight() * 2);
            this.rebuildTexturePositions();
        }

        this.popDisableUpdates();
    }

    /**
     * Algorithmic implementation which updates positions of all textures.
     * @param sortedTextureList The list of textures to place, sorted from largest to smallest.
     * @return Returns true if the atlas ran out of space.
     */
    protected abstract boolean updatePositions(SortedList<TTexture> sortedTextureList);
}