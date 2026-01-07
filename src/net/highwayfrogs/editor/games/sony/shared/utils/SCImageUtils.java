package net.highwayfrogs.editor.games.sony.shared.utils;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RGBImageFilter;

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
     * @param image           The image to scale.
     * @param sideLength      The image size to rescale to.
     * @param nearestNeighbor Whether nearest neighbor interpolation should be used.
     * @return scaledImage
     */
    public static BufferedImage scaleForDisplay(BufferedImage image, int sideLength, boolean nearestNeighbor) {
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

        BufferedImage scaleImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D graphics = scaleImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, nearestNeighbor ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(image, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        return scaleImage;
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

    // Black -> Transparency TODO: Delete these later.
    public static class TransparencyFilter extends RGBImageFilter {
        public static TransparencyFilter INSTANCE = new TransparencyFilter();

        @Override
        public int filterRGB(int x, int y, int argb) {
            int colorWOAlpha = argb & 0x00FFFFFF;
            return colorWOAlpha == 0x000000 ? colorWOAlpha : argb;
        }
    }

    // Transparency -> Black
    public static class BlackFilter extends RGBImageFilter {
        public static BlackFilter INSTANCE = new BlackFilter();

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int alpha = rgb >>> (3 * Constants.BITS_PER_BYTE);
            return alpha == 0 ? 0xFF000000 : rgb;
        }
    }
}
