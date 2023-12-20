package net.highwayfrogs.editor.gui.texture;

import lombok.Getter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a buffered image that can be provided to the texturing system.
 * Created by Kneesnap on 9/24/2023.
 */
@Getter
public class BufferedImageWrapper implements ITextureSource {
    private final List<Consumer<BufferedImage>> imageChangeListeners;
    private BufferedImage image;

    public BufferedImageWrapper(BufferedImage image) {
        this.imageChangeListeners = new ArrayList<>();
        setImage(image);
    }

    /**
     * Sets the image stored by this.
     * @param newImage The new image.
     */
    public void setImage(BufferedImage newImage) {
        if (newImage == null)
            throw new NullPointerException("newImage");

        this.fireChangeEvent(newImage);
        this.image = newImage;
    }

    @Override
    public BufferedImage makeImage() {
        return this.image;
    }

    @Override
    public int getWidth() {
        return this.image != null ? this.image.getWidth() : 0;
    }

    @Override
    public int getHeight() {
        return this.image != null ? this.image.getHeight() : 0;
    }

    @Override
    public int getUpPadding() {
        return 0;
    }

    @Override
    public int getDownPadding() {
        return 0;
    }

    @Override
    public int getLeftPadding() {
        return 0;
    }

    @Override
    public int getRightPadding() {
        return 0;
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        fireChangeEvent0(newImage);
    }
}