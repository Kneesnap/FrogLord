package net.highwayfrogs.editor.gui.texture;

import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * Represents a texture which comes directly from a <see cref="IVorkTextureSource"/> without modification.
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
    public Vector2f getUV(Texture heldTexture, int x, int y) {
        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (heldTexture != this)
            throw new IllegalArgumentException("Cannot get UV for texture " + Utils.getSimpleName(heldTexture) + ", because " + Utils.getSimpleName(this) + " doesn't support held textures.");

        float u = (float) (x + heldTexture.getLeftPadding()) / this.getPaddedWidth();
        float v = (float) (y + heldTexture.getUpPadding()) / this.getPaddedHeight();
        return new Vector2f(u, v);
    }

    @Override
    public Vector2f getUV(Texture heldTexture, Vector2f localUv) {
        if (heldTexture == null)
            throw new NullPointerException("heldTexture");
        if (heldTexture != this)
            throw new IllegalArgumentException("Cannot get UV for texture " + Utils.getSimpleName(heldTexture) + ", because " + Utils.getSimpleName(this) + " doesn't support held textures.");

        double baseU = ((double) heldTexture.getLeftPadding()) / this.getPaddedWidth();
        double baseV = ((double) heldTexture.getUpPadding()) / this.getPaddedHeight();
        double uRatio = (double) getWidth() / getPaddedWidth();
        double vRatio = (double) getHeight() / getPaddedHeight();
        return new Vector2f((float) (baseU + (localUv.getX() * uRatio)), (float) (baseV + (localUv.getY() * vRatio)));
    }
}