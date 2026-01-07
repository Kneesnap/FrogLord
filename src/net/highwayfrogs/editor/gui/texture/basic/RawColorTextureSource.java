package net.highwayfrogs.editor.gui.texture.basic;

import lombok.Getter;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * This is a texture source that builds a texture which is a solid color.
 * Created by Kneesnap on 1/3/2024.
 */
@Getter
public class RawColorTextureSource implements ITextureSource {
    private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    private final javafx.scene.paint.Color fxColor;
    private final int argbColor;

    public static final Vector2f TEXTURE_UV = new Vector2f(.5F, .5F);

    public RawColorTextureSource(javafx.scene.paint.Color fxColor) {
        this(fxColor != null ? ColorUtils.toARGB(fxColor) : 0);
    }

    public RawColorTextureSource(Color color) {
        this(color.getRGB());
    }

    public RawColorTextureSource(int argbColor) {
        this.argbColor = argbColor;
        this.fxColor = ColorUtils.fromARGB(argbColor);
    }

    /**
     * Gets the UV to use with this source.
     */
    public Vector2f getUv() {
        return TEXTURE_UV;
    }

    @Override
    public BufferedImage makeImage() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] writeBuffer = ImageUtils.getWritablePixelIntegerArray(image);
        Arrays.fill(writeBuffer, this.argbColor);

        return image;
    }

    @Override
    public int getWidth() {
        return 5;
    }

    @Override
    public int getHeight() {
        return 5;
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
