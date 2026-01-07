package net.highwayfrogs.editor.gui.texture;

import lombok.Getter;
import net.highwayfrogs.editor.system.QuadConsumer;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a texture which can be used by FrogLord's 3D system.
 * Sometimes we refer to textures as holding other textures.
 * A texture always holds itself, but other textures like texture sheets/atlases can hold other textures too.
 * This is a backport from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
public abstract class Texture {
    private static long nextAvailableTextureId = 0;
    @Getter private final ITextureSource textureSource;
    private final Consumer<BufferedImage> updateHook = this::update;
    @Getter private final long uniqueId = nextAvailableTextureId++;
    private BufferedImage cachedImage;
    private BufferedImage cachedImageWithoutPadding;
    private boolean cachedImageInvalid; // This should start as false, as to avoid updates before the image is even used.
    private int disableUpdateCount;
    @Getter private int upPadding;
    @Getter private int downPadding;
    @Getter private int leftPadding;
    @Getter private int rightPadding;
    @Getter private boolean disposed; // The texture should never be used again.

    /**
     * Whether bulk mode is enabled.
     * If bulk mode is enabled, updates should not occur until it is disabled.
     * This is to prevent unnecessary updates (causing slowdown) when doing many operations in bulk.
     */
    @Getter
    private boolean bulkMode;

    /**
     * Whether the texture has even a single pixel which is not full alpha.
     */
    @Getter private boolean hasAnyTransparentPixels;

    /**
     * This method performs a sanity check that the atlas is currently not in a state of disposal.
     */
    protected void ensureNotDisposed() {
        if (this.disposed)
            throw new RuntimeException("A(n) " + Utils.getSimpleName(this) + " has been freed, but something is still using it!");
    }

    /**
     * Whether updates should be disabled for now.
     */
    public final boolean shouldDisableUpdates() {
        return this.bulkMode || this.disableUpdateCount > 0;
    }

    /**
     * Gets the width (in pixels) of the texture without padding.
     */
    public int getWidthWithoutPadding() {
        return getPaddedWidth() - (getLeftPadding() + getRightPadding());
    }

    /**
     * Gets the height (in pixels) of the texture without padding.
     */
    public int getHeightWithoutPadding() {
        return getPaddedHeight() - (getUpPadding() + getDownPadding());
    }

    /**
     * Gets the image which this texture represents.
     * This can return null if updates are disabled and the image has not been generated before.
     */
    public BufferedImage getImage() {
        ensureNotDisposed();
        if (this.cachedImage == null)
            this.cachedImageInvalid = true;

        if (this.cachedImageInvalid)
            this.update();

        return this.cachedImage;
    }

    /**
     * The image represented by this texture without padding.
     */
    public BufferedImage getImageWithoutPadding() {
        ensureNotDisposed();
        if (this.cachedImage == null)
            this.cachedImageInvalid = true;

        if (this.cachedImageInvalid)
            this.update();

        if (this.cachedImage != null && this.cachedImageWithoutPadding == null)
            this.cachedImageWithoutPadding = ImageUtils.trimEdges(this.cachedImage, getUpPadding(), getDownPadding(), getLeftPadding(), getRightPadding());

        return this.cachedImageWithoutPadding;
    }

    /**
     * Gets the width of this texture with padding included.
     */
    public int getPaddedWidth() {
        return ((this.cachedImage != null && !this.cachedImageInvalid) ? this.cachedImage.getWidth() : this.textureSource.getWidth());
    }

    /**
     * Gets the height of this texture with padding included.
     */
    public int getPaddedHeight() {
        return ((this.cachedImage != null && !this.cachedImageInvalid) ? this.cachedImage.getHeight() : this.textureSource.getHeight());
    }

    /**
     * Called when the texture image changes.
     * Texture texture, BufferedImage oldImage, BufferedImage newImage, boolean didOldImageHaveAnyTransparentPixels
     */
    @Getter
    private final List<QuadConsumer<Texture, BufferedImage, BufferedImage, Boolean>> imageChangeListeners = new ArrayList<>();

    /**
     * Creates a new VorkTexture.
     * @param source The source which creates the underlying image.
     */
    public Texture(ITextureSource source) {
        if (source == null)
            throw new NullPointerException("source");

        this.textureSource = source;
    }

    /**
     * Creates a new VorkTexture
     * @param source The creator of the source which creates the underlying image.
     */
    public Texture(Function<Texture, ITextureSource> source) {
        if (source == null)
            throw new NullPointerException("source");
        this.textureSource = source.apply(this);
    }

    /**
     * Registers the texture to the texture source.
     */
    public void registerTexture() {
        ensureNotDisposed();
        if (this.textureSource != null && !this.textureSource.getImageChangeListeners().contains(this.updateHook))
            this.textureSource.getImageChangeListeners().add(this.updateHook);
    }

    /**
     * Called when the texture is unregistered.
     */
    public void unregisterTexture() {
        if (this.textureSource != null)
            this.textureSource.getImageChangeListeners().remove(this.updateHook);
    }

    /**
     * Called when the texture should be disposed, never to be used again.
     */
    public void disposeTexture() {
        unregisterTexture();
        this.disposed = true;
        this.cachedImage = this.cachedImageWithoutPadding = null;
    }

    /**
     * Called to generate an image to use as the cached image.
     */
    protected BufferedImage makeImageForCache() {
        return this.textureSource.makeImage();
    }

    /**
     * Replaces the cached image, generating the image from the source.
     */
    protected final void updateCachedImage() {
        updateCachedImage(null);
    }

    /**
     * Replaces the cached image
     * @param newImage The new image.
     */
    protected void updateCachedImage(BufferedImage newImage) {
        ensureNotDisposed();
        this.cachedImageInvalid = false;

        if (newImage == null)
            newImage = makeImageForCache();

        BufferedImage oldImage = this.cachedImage;
        boolean wereAnyPixelsTransparent = this.hasAnyTransparentPixels;
        this.cachedImage = newImage;
        this.upPadding = this.textureSource.getUpPadding();
        this.downPadding = this.textureSource.getDownPadding();
        this.leftPadding = this.textureSource.getLeftPadding();
        this.rightPadding = this.textureSource.getRightPadding();

        this.hasAnyTransparentPixels = this.textureSource.hasAnyTransparentPixels(newImage);

        this.cachedImageWithoutPadding = null;
        if (oldImage != null)
            for (int i = 0; i < this.imageChangeListeners.size(); i++)
                this.imageChangeListeners.get(i).accept(this, oldImage, newImage, wereAnyPixelsTransparent);
    }

    /**
     * Performs updates to this texture.
     */
    public final void update() {
        this.update(null);
    }

    /**
     * Performs updates to this texture.
     * @param newImage The image to update.
     */
    public void update(BufferedImage newImage) {
        ensureNotDisposed();
        if (newImage != null) // If an image was provided, the image must be dirty.
            this.cachedImageInvalid = true; // We set this to true so that if updates are disabled this will still be considered dirty when the updates are re-enabled.

        if (!shouldDisableUpdates() && this.cachedImageInvalid)
            updateCachedImage(newImage);
    }

    /**
     * Marks the image dirty for rebuilding.
     */
    public void markImageDirty() {
        ensureNotDisposed();
        if (this.cachedImage != null) // Don't consider an image which doesn't exist yet to be "dirty".
            this.cachedImageInvalid = true;
    }

    /**
     * Starts bulk operation mode.
     * @throws UnsupportedOperationException Thrown if bulk operation mode is already enabled.
     */
    public void startBulkOperations() {
        ensureNotDisposed();
        if (this.bulkMode)
            throw new UnsupportedOperationException("BulkMode is already enabled!");

        this.bulkMode = true;
    }

    /**
     * Ends bulk operation mode and performs the awaited updates.
     * @throws UnsupportedOperationException Thrown if bulk operation mode is not currently enabled.
     */
    public void endBulkOperations() {
        ensureNotDisposed();
        if (!this.bulkMode)
            throw new UnsupportedOperationException("BulkMode was not enabled!");

        this.bulkMode = false;
        this.update(); // Upon reaching zero, update! (This will only update if the image was marked as dirty).
    }

    /**
     * Pushes a signal to disable updates, until popped.
     */
    public final void pushDisableUpdates() {
        ensureNotDisposed();
        this.disableUpdateCount++;
    }

    /**
     * Pops a signal to disable updates.
     */
    public final void popDisableUpdates() {
        ensureNotDisposed();
        if (this.disableUpdateCount <= 0)
            throw new UnsupportedOperationException("Cannot pop disable updates, because there's nothing to pop.");

        // Decrease counter.
        --this.disableUpdateCount;

        // At one point this method was capable of updating the image.
        // However, after some consideration, image updates should be user-facing, not implicit to functions used by the texture system itself.
        // If the texturing system wants an update, it should call it more explicitly to avoid calling an image update during another image update.
    }

    /**
     * Gets the UV values for a particular xy pixel position for a texture held by this one.
     * @param heldTexture The held texture to get the UV for.
     * @param x           The x position to get the 'u' value from.
     * @param y           The y position to get the 'v' value from.
     * @return The vector containing the uv values.
     */
    public Vector2f getUV(Texture heldTexture, int x, int y) {
        return getUV(heldTexture, x, y, new Vector2f());
    }

    /**
     * Gets the UV values for a particular xy pixel position for a texture held by this one.
     * @param heldTexture The held texture to get the UV for.
     * @param x The x position to get the 'u' value from.
     * @param y The y position to get the 'v' value from.
     * @param output The output vector to save the data to
     * @return The vector containing the uv values.
     */
    public abstract Vector2f getUV(Texture heldTexture, int x, int y, Vector2f output);

    /**
     * Gets the UV values for a 'local' uv for a texture held by this one.
     * @param heldTexture The held texture to get the UV for
     * @param localUv     The 'local' uv to get the full UV from.
     * @return The vector containing the uv values.
     */
    public Vector2f getUV(Texture heldTexture, Vector2f localUv) {
        return getUV(heldTexture, localUv, new Vector2f());
    }

    /**
     * Gets the UV values for a 'local' uv for a texture held by this one.
     * @param heldTexture The held texture to get the UV for
     * @param localUv The 'local' uv to get the full UV from.
     * @param output The output vector to save the data to
     * @return The vector containing the uv values.
     */
    public abstract Vector2f getUV(Texture heldTexture, Vector2f localUv, Vector2f output);

    /**
     * Gets a texture nested inside of this texture which uses the provided source.
     * @param source               The source to find a texture from.
     * @param throwErrorIfNotFound If the child texture is not found, should an error be thrown?
     * @return childTexture
     */
    public Texture getChildTextureBySource(ITextureSource source, boolean throwErrorIfNotFound) {
        ensureNotDisposed();
        if (source == this.textureSource)
            return this;
        if (throwErrorIfNotFound)
            throw new RuntimeException("The source '" + Utils.getSimpleName(source) + "' was not found as a child inside '" + Utils.getSimpleName(this) + "'.");
        return null;
    }
}