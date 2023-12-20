package net.highwayfrogs.editor.gui.texture.atlas;

import lombok.Getter;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a texture which is part of a TextureAltas.
 * Created by Kneesnap on 9/23/2023.
 */
@Getter
public class AtlasTexture extends Texture {
    private final TextureAtlas atlas;
    private int x;
    private int y;
    private int upPaddingEmpty;
    private int downPaddingEmpty;
    private int leftPaddingEmpty;
    private int rightPaddingEmpty;

    /**
     * Creates a new AtlasTexture
     * @param atlas  The atlas which holds the texture.
     * @param source The texture source.
     */
    public AtlasTexture(TextureAtlas atlas, ITextureSource source) {
        super(source);
        this.atlas = atlas;
    }

    /**
     * Gets the width of this texture with image padding included, but not empty padding.
     */
    public int getNonEmptyPaddedWidth() {
        return super.getPaddedWidth();
    }

    /**
     * Gets the height of this texture with image padding included, but not empty padding.
     */
    public int getNonEmptyPaddedHeight() {
        return super.getPaddedHeight();
    }

    /**
     * Gets the width of this texture with padding included.
     */
    @Override
    public int getPaddedWidth() {
        return super.getPaddedWidth() + this.leftPaddingEmpty + this.rightPaddingEmpty;
    }

    /**
     * Gets the height of this texture with padding included.
     */
    @Override
    public int getPaddedHeight() {
        return super.getPaddedHeight() + this.upPaddingEmpty + this.downPaddingEmpty;
    }

    /**
     * Sets the x position of the top left-hand corner of this texture (with padding) in the atlas.
     * @param value The new x coordinate value
     */
    public void setX(int value) {
        if (value < 0)
            throw new IndexOutOfBoundsException("X cannot be set below 0! (Got: " + value + ")");

        if (this.x != value) {
            this.x = value;
            this.atlas.markPositionsDirty();
        }
    }

    /**
     * Sets the y position of the top left-hand corner of this texture (with padding) in the atlas.
     * @param value The new y coordinate value.
     */
    public void setY(int value) {
        if (value < 0)
            throw new IndexOutOfBoundsException("Y cannot be set below 0! (Got: " + value + ")");

        if (this.y != value) {
            this.y = value;
            this.atlas.markPositionsDirty();
        }
    }

    /**
     * Sets the position which this texture resides at in the atlas.
     * Only performs one update.
     * @param x The new x position.
     * @param y The new y position.
     * @throws IndexOutOfBoundsException Thrown if an invalid value is provided.
     */
    public void setPosition(int x, int y) {
        if (x < 0)
            throw new IndexOutOfBoundsException("X cannot be set below 0! (Got: " + x + ")");
        if (y < 0)
            throw new IndexOutOfBoundsException("Y cannot be set below 0! (Got: " + y + ")");

        boolean positionChanged = (this.x != x) || (this.x != y);

        this.x = x;
        this.y = y;
        if (positionChanged)
            this.atlas.markPositionsDirty();
    }

    /**
     * The number of empty padding pixels which extend upward from the texture.
     */
    public void setUpPaddingEmpty(int value) {
        if (value < 0)
            throw new IndexOutOfBoundsException("UpPadding cannot be set below 0! (Got: " + value + ")");

        if (this.upPaddingEmpty != value) {
            this.upPaddingEmpty = value;
            this.atlas.markTextureSizesDirty();
        }
    }

    /**
     * The number of empty padding pixels which extend downward from the texture.
     */
    public void setDownPaddingEmpty(int value) {
        if (value < 0)
            throw new IndexOutOfBoundsException("DownPadding cannot be set below 0! (Got: " + value + ")");

        if (this.downPaddingEmpty != value) {
            this.downPaddingEmpty = value;
            this.atlas.markTextureSizesDirty();
        }
    }

    /**
     * The number of empty padding pixels which extend to the left from the texture.
     */
    public void setLeftPaddingEmpty(int value) {
        if (value < 0)
            throw new IndexOutOfBoundsException("LeftPadding cannot be set below 0! (Got: " + value + ")");

        if (this.leftPaddingEmpty != value) {
            this.leftPaddingEmpty = value;
            this.atlas.markTextureSizesDirty();
        }
    }

    /**
     * The number of empty padding pixels which extend to the right from the texture.
     */
    public void setRightPaddingEmpty(int value) {
        if (value < 0)
            throw new IndexOutOfBoundsException("RightPadding cannot be set below 0! (Got: " + value + ")");

        if (this.rightPaddingEmpty != value) {
            this.rightPaddingEmpty = value;
            this.atlas.markTextureSizesDirty();
        }
    }

    /**
     * Sets the empty padding values which this texture keeps in the atlas.
     * Only performs one update.
     * @param up    The amount of padding (in pixels) used in the up direction.
     * @param down  The amount of padding (in pixels) used in the down direction.
     * @param left  The amount of padding (in pixels) used in the left direction.
     * @param right The amount of padding (in pixels) used in the right direction.
     */
    public void setEmptyPadding(int up, int down, int left, int right) {
        if (up < 0)
            throw new IndexOutOfBoundsException("UpPadding cannot be set below 0! (Got: " + up + ")");
        if (down < 0)
            throw new IndexOutOfBoundsException("DownPadding cannot be set below 0! (Got: " + down + ")");
        if (left < 0)
            throw new IndexOutOfBoundsException("LeftPadding cannot be set below 0! (Got: " + left + ")");
        if (right < 0)
            throw new IndexOutOfBoundsException("RightPadding cannot be set below 0! (Got: " + right + ")");

        boolean paddingChanged = (this.upPaddingEmpty != up) || (this.downPaddingEmpty != down)
                || (this.leftPaddingEmpty != left) || (this.rightPaddingEmpty != right);

        this.upPaddingEmpty = up;
        this.downPaddingEmpty = down;
        this.leftPaddingEmpty = left;
        this.rightPaddingEmpty = right;
        if (paddingChanged)
            this.atlas.markTextureSizesDirty();
    }

    @Override
    public Vector2f getUV(Texture heldTexture, int x, int y) {
        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (heldTexture != this)
            throw new IllegalArgumentException("Cannot get UV for texture " + Utils.getSimpleName(heldTexture) + ", because " + Utils.getSimpleName(this) + " doesn't support held textures.");

        float u = (float) (x + heldTexture.getLeftPadding()) / this.getNonEmptyPaddedWidth();
        float v = (float) (y + heldTexture.getUpPadding()) / this.getNonEmptyPaddedHeight();
        return new Vector2f(u, v);
    }

    @Override
    public Vector2f getUV(Texture heldTexture, Vector2f localUv) {
        // We only want to throw when this one is called, the other GetUV has reasonable use-cases.
        throw new UnsupportedOperationException("getUV of a " + Utils.getSimpleName(this) + " does not exist. To get the UV for an atlas texture, call this method in the atlas texture itself.");
    }
}