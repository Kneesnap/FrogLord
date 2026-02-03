package net.highwayfrogs.editor.games.sony.shared.utils;

import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Contains static utilities for working with images in Sony Cambridge games.
 * Created by Kneesnap on 01/01/2026.
 */
public class SCImageUtils {
    /**
     * Trim the padding off of this image.
     * @param gameImage The VloImage to get trimming data from.
     * @param image     The image to trim.
     * @return trimmedImage
     */
    public static BufferedImage trimEdges(VloImage gameImage, BufferedImage image) {
        BufferedImage trimImage = new BufferedImage(gameImage.getUnpaddedWidth(), gameImage.getUnpaddedHeight(), image.getType());
        Graphics2D graphics = trimImage.createGraphics();
        graphics.drawImage(image, -gameImage.getLeftPadding(), -gameImage.getUpPadding(), gameImage.getPaddedWidth(), gameImage.getPaddedHeight(), null);
        graphics.dispose();

        return trimImage;
    }

    /**
     * Resize an image to a new width / height.
     * @param image The image to scale.
     * @param sideLength The image size to rescale to.
     * @return scaledImage
     */
    public static BufferedImage scaleForDisplay(BufferedImage image, int sideLength) {
        if (sideLength == image.getWidth() || sideLength == image.getHeight())
            return image; // There would be no change.

        double scaleFactor;
        if (image.getWidth() > image.getHeight()) {
            scaleFactor = (double) sideLength / image.getWidth();
        } else {
            scaleFactor = (double) sideLength / image.getHeight();
        }

        int newWidth = (int) Math.round(scaleFactor * image.getWidth());
        int newHeight = (int) Math.round(scaleFactor * image.getHeight());
        return ImageUtils.resizeImage(image, newWidth, newHeight, true);
    }

    /**
     * Scale an image by its width.
     * @param image       The image to scale.
     * @param scaleFactor The factor to scale the image horizontally by.
     * @return scaledImage
     */
    public static BufferedImage scaleWidth(BufferedImage image, double scaleFactor) {
        int newWidth = (int) Math.round(image.getWidth() * scaleFactor);
        if (newWidth == image.getWidth())
            return image; // There would be no change.

        BufferedImage scaleImage = new BufferedImage(newWidth, image.getHeight(), image.getType());
        Graphics2D graphics = scaleImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(image, 0, 0, newWidth, image.getHeight(), null);
        graphics.dispose();
        return scaleImage;
    }
}
