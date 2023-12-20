package net.highwayfrogs.editor.gui.texture;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents something which provides a single texture's data.
 * Back-ported from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
public interface ITextureSource {
    /**
     * Gets a list of listeners listening for an image change that impacts rendering.
     */
    public List<Consumer<BufferedImage>> getImageChangeListeners();

    /**
     * Makes the image which this source provides.
     * @return The image this source provides.
     */
    public BufferedImage makeImage();

    /**
     * Gets the up to date width of the image.
     */
    public int getWidth();

    /**
     * Gets the up to date height of the image.
     */
    public int getHeight();

    /**
     * The number of pixels at the top of the image which should be considered padding.
     */
    public int getUpPadding();

    /**
     * The number of pixels at the bottom of the image which should be considered padding.
     */
    public int getDownPadding();

    /**
     * The number of pixels at the left side of the image which should be considered padding.
     */
    public int getLeftPadding();

    /**
     * The number of pixels at the right side of the image which should be considered padding.
     */
    public int getRightPadding();

    /**
     * Fires the change event.
     */
    public default void fireChangeEvent() {
        fireChangeEvent(null);
    }

    /**
     * Fires the change event.
     * @param newImage An optional value of the new image.
     */
    public void fireChangeEvent(BufferedImage newImage);

    /**
     * Default logic for running the change event.
     * @param newImage The image to fire with.
     */
    public default void fireChangeEvent0(BufferedImage newImage) {
        if (newImage == null)
            newImage = this.makeImage();

        List<Consumer<BufferedImage>> imageChangeListeners = getImageChangeListeners();
        for (int i = 0; i < imageChangeListeners.size(); i++)
            imageChangeListeners.get(i).accept(newImage);
    }
}