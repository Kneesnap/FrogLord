package net.highwayfrogs.editor.gui.texture;

import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * Represents a texture which comes directly from a <see cref="IVorkTextureSource"/> without modification.
 * TODO: We need to sort out how texture padding works. I don't think the ITextureSource uses it the same way this does.
 * Created by Kneesnap on 9/23/2023.
 */
public class SimpleTexture extends Texture {
    public SimpleTexture(ITextureSource source) {
        super(source);
    }

    public SimpleTexture(Function<Texture, ITextureSource> source) {
        super(source);
    }

    @Override
    public Vector2f getUV(Texture heldTexture, int x, int y, Vector2f result) {
        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (heldTexture != this)
            throw new IllegalArgumentException("Cannot get UV for texture " + Utils.getSimpleName(heldTexture) + ", because " + Utils.getSimpleName(this) + " doesn't support held textures.");

        result.setX((float) (heldTexture.getLeftPadding() + x) / this.getPaddedWidth());
        result.setY((float) (heldTexture.getUpPadding() + y) / this.getPaddedHeight());
        return result;
    }

    @Override
    public Vector2f getUV(Texture heldTexture, Vector2f localUv, Vector2f result) {
        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (heldTexture != this)
            throw new IllegalArgumentException("Cannot get UV for texture " + Utils.getSimpleName(heldTexture) + ", because " + Utils.getSimpleName(this) + " doesn't support held textures.");

        // The input UV range of [0, 1] represents the range between the center of the first pixel, and the center of the last pixel.
        result.setX((heldTexture.getLeftPadding() + (localUv.getX() * getWidthWithoutPadding())) / getPaddedWidth());
        result.setY((heldTexture.getUpPadding() + (localUv.getY() * getHeightWithoutPadding())) / getPaddedHeight());
        return result;
    }
}