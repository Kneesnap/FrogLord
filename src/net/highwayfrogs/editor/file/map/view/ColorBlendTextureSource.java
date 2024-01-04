package net.highwayfrogs.editor.file.map.view;

import lombok.Getter;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a texture which blends two colors together, which allows picking a position between the two colors.
 * Created by Kneesnap on 12/30/2023.
 */
public class ColorBlendTextureSource implements ITextureSource {
    @Getter private final int width;
    @Getter private final int height;
    @Getter private final Color color1;
    @Getter private final Color color2;
    @Getter private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    private BufferedImage cachedImage;

    private static final int DEFAULT_SIZE = 12;

    public ColorBlendTextureSource(Color color1, Color color2) {
        this(DEFAULT_SIZE, DEFAULT_SIZE, color1, color2);
    }

    public ColorBlendTextureSource(int width, int height, Color color1, Color color2) {
        this.width = width;
        this.height = height;
        this.color1 = color1;
        this.color2 = color2;
    }

    /**
     * Calculates a local UV to this texture which will give color 1 when used.
     * @param vector The vector to save the local uv output into. If null is provided, a new one will be created.
     * @return uvVector containing the local UV for this texture.
     */
    public Vector2f calculateUvForColor1(Vector2f vector) {
        return this.calculateUv(vector, 0F);
    }

    /**
     * Calculates a local UV to this texture which will give color 1 when used.
     * @param vector The vector to save the local uv output into. If null is provided, a new one will be created.
     * @return uvVector containing the local UV for this texture.
     */
    public Vector2f calculateUvForColor2(Vector2f vector) {
        return this.calculateUv(vector, 1F);
    }

    /**
     * Calculates a local UV to this texture which gives a color interpolated to the given t value.
     * @param vector The vector to save the local uv output into. If null is provided, a new one will be created.
     * @param t      Interpolation factor. 0 = fully color 1, 1 = fully color 2.
     * @return uvVector containing the local UV for this texture.
     */
    public Vector2f calculateUv(Vector2f vector, float t) {
        if (vector == null)
            vector = new Vector2f();
        if (t > 1F)
            t = 1F;
        if (t < 0F)
            t = 0F;

        vector.setX((getLeftPadding() + (t * getUnpaddedWidth())) / getWidth());
        vector.setY(.5F);
        return vector;
    }

    @Override
    public BufferedImage makeImage() {
        if (this.cachedImage != null)
            return this.cachedImage;

        BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < this.width; x++) {
            float t = (float) (x - getLeftPadding()) / getUnpaddedWidth();
            int interpolatedColor = Utils.calculateInterpolatedColourARGB(this.color1, this.color2, t);
            for (int y = 0; y < this.height; y++)
                image.setRGB(x, y, interpolatedColor);
        }

        // Cache the image.
        return this.cachedImage = image;
    }

    @Override
    public int getUpPadding() {
        return 2;
    }

    @Override
    public int getDownPadding() {
        return 2;
    }

    @Override
    public int getLeftPadding() {
        return 2;
    }

    @Override
    public int getRightPadding() {
        return 2;
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        fireChangeEvent0(newImage);
    }
}
