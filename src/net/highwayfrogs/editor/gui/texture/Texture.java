package net.highwayfrogs.editor.gui.texture;

import lombok.Getter;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.system.QuadConsumer;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents a texture which can be used by FrogLord's 3D system.
 * Sometimes we refer to textures as holding other textures.
 * A texture always holds itself, but other textures like texture sheets/atlases can hold other textures too.
 * This is a backport from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
public abstract class Texture {
    @Getter private final ITextureSource textureSource;
    private BufferedImage cachedImage;
    private BufferedImage cachedImageWithoutPadding;
    private boolean cachedImageInvalid; // This should start as false, as to avoid updates before the image is even used.
    private int disableUpdateCount;
    @Getter private int upPadding;
    @Getter private int downPadding;
    @Getter private int leftPadding;
    @Getter private int rightPadding;

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
     * Whether updates should be disabled for now.
     */
    public final boolean shouldDisableUpdates() {
        return this.bulkMode || this.disableUpdateCount > 0;
    }

    /**
     * Gets the width (in pixels) of the texture without padding.
     */
    public int getWidth() {
        return ((this.cachedImage != null && !this.cachedImageInvalid) ? this.cachedImage.getWidth() : this.textureSource.getWidth())
                - (getLeftPadding() + getRightPadding());
    }

    /**
     * Gets the height (in pixels) of the texture without padding.
     */
    public int getHeight() {
        return ((this.cachedImage != null && !this.cachedImageInvalid) ? this.cachedImage.getHeight() : this.textureSource.getHeight())
                - (getUpPadding() + getDownPadding());
    }

    /**
     * Gets the image which this texture represents.
     * This can return null if updates are disabled and the image has not been generated before.
     */
    public BufferedImage getImage() {
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
        if (this.cachedImage == null)
            this.cachedImageInvalid = true;

        if (this.cachedImageInvalid)
            this.update();

        if (this.cachedImage != null && this.cachedImageWithoutPadding == null)
            this.cachedImageWithoutPadding = ImageWorkHorse.trimEdges(this.cachedImage, getUpPadding(), getDownPadding(), getLeftPadding(), getRightPadding());

        return this.cachedImageWithoutPadding;
    }

    /**
     * Gets the width of this texture with padding included.
     */
    public int getPaddedWidth() {
        return getWidth() + getLeftPadding() + getRightPadding();
    }

    /**
     * Gets the height of this texture with padding included.
     */
    public int getPaddedHeight() {
        return getHeight() + getUpPadding() + getDownPadding();
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
        this.textureSource.getImageChangeListeners().add(this::update); // TODO: Memory leak, this is never removed.
    }

    /**
     * Creates a new VorkTexture
     * @param source The creator of the source which creates the underlying image.
     */
    public Texture(Function<Texture, ITextureSource> source) {
        if (source == null)
            throw new NullPointerException("source");
        this.textureSource = source.apply(this);
        this.textureSource.getImageChangeListeners().add(this::update);
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
        this.cachedImageInvalid = false;

        if (newImage == null)
            newImage = this.textureSource.makeImage();

        BufferedImage oldImage = this.cachedImage;
        boolean wereAnyPixelsTransparent = this.hasAnyTransparentPixels;
        this.cachedImage = newImage;
        this.upPadding = this.textureSource.getUpPadding();
        this.downPadding = this.textureSource.getDownPadding();
        this.leftPadding = this.textureSource.getLeftPadding();
        this.rightPadding = this.textureSource.getRightPadding();

        boolean foundTransparentPixel = false;
        for (int y = 0; y < this.cachedImage.getHeight() && !foundTransparentPixel; y++)
            for (int x = 0; x < this.cachedImage.getWidth() && !foundTransparentPixel; x++)
                if (Utils.getAlpha(this.cachedImage.getRGB(x, y)) != (byte) 0xFF)
                    foundTransparentPixel = true;
        this.hasAnyTransparentPixels = foundTransparentPixel;

        this.cachedImageWithoutPadding = null;
        if (oldImage != null)
            for (int i = 0; i < this.imageChangeListeners.size(); i++)
                this.imageChangeListeners.get(i).accept(this, oldImage, this.cachedImage, wereAnyPixelsTransparent);
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
        if (newImage != null) // If an image was provided, the image must be dirty.
            this.cachedImageInvalid = true; // We set this to true so that if updates are disabled this will still be considered dirty when the updates are re-enabled.

        if (!shouldDisableUpdates() && this.cachedImageInvalid)
            updateCachedImage(newImage);
    }

    /**
     * Marks the image dirty for rebuilding.
     */
    public void markImageDirty() {
        if (this.cachedImage != null) // Don't consider an image which doesn't exist yet "dirty".
            this.cachedImageInvalid = true;
    }

    /**
     * Starts bulk operation mode.
     * @throws UnsupportedOperationException Thrown if bulk operation mode is already enabled.
     */
    public void startBulkOperations() {
        if (this.bulkMode)
            throw new UnsupportedOperationException("BulkMode is already enabled!");

        this.bulkMode = true;
    }

    /**
     * Ends bulk operation mode and performs the awaited updates.
     * @throws UnsupportedOperationException Thrown if bulk operation mode is not currently enabled.
     */
    public void endBulkOperations() {
        if (!this.bulkMode)
            throw new UnsupportedOperationException("BulkMode was not enabled!");

        this.bulkMode = false;
        this.update();
    }

    /**
     * Pushes a signal to disable updates, until popped.
     */
    protected final void pushDisableUpdates() {
        this.disableUpdateCount++;
    }

    /**
     * Pops a signal to disable updates.
     */
    protected final void popDisableUpdates() {
        if (this.disableUpdateCount <= 0)
            throw new UnsupportedOperationException("Cannot pop disable updates, because there's nothing to pop.");
        this.disableUpdateCount--;
    }

    /**
     * Gets the UV values for a particular xy pixel position for a texture held by this one.
     * @param heldTexture The held texture to get the UV for.
     * @param x           The x position to get the 'u' value from.
     * @param y           The y position to get the 'v' value from.
     * @return The vector containing the uv values.
     */
    public abstract Vector2f getUV(Texture heldTexture, int x, int y);

    /**
     * Gets the UV values for a 'local' uv for a texture held by this one.
     * @param heldTexture The held texture to get the UV for
     * @param localUv     The 'local' uv to get the full UV from.
     * @return The vector containing the uv values.
     */
    public abstract Vector2f getUV(Texture heldTexture, Vector2f localUv);

    /**
     * Gets a texture nested inside of this texture which uses the provided source.
     * @param source               The source to find a texture from.
     * @param throwErrorIfNotFound If the child texture is not found, should an error be thrown?
     * @return childTexture
     */
    public Texture getChildTextureBySource(ITextureSource source, boolean throwErrorIfNotFound) {
        if (source == this.textureSource)
            return this;
        if (throwErrorIfNotFound)
            throw new RuntimeException("The source '" + Utils.getSimpleName(source) + "' was not found as a child inside '" + Utils.getSimpleName(this) + "'.");
        return null;
    }
}