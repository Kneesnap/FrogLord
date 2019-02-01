package net.highwayfrogs.editor.file.vlo;

import net.highwayfrogs.editor.Constants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;

/**
 * Apply image filters.
 * Created by Kneesnap on 12/1/2018.
 */
public class ImageWorkHorse {

    /**
     * Trim the padding off of this image.
     * @param gameImage The GameImage to get trimming data from.
     * @param image     The image to trim.
     * @return trimmedImage
     */
    public static BufferedImage trimEdges(GameImage gameImage, BufferedImage image) {
        int xTrim = gameImage.getFullWidth() - gameImage.getIngameWidth();
        int yTrim = gameImage.getFullHeight() - gameImage.getIngameHeight();

        BufferedImage trimImage = new BufferedImage(gameImage.getIngameWidth(), gameImage.getIngameHeight(), image.getType());
        Graphics2D graphics = trimImage.createGraphics();
        graphics.drawImage(image, -xTrim / 2, -yTrim / 2, gameImage.getFullWidth(), gameImage.getFullHeight(), null);
        graphics.dispose();

        return trimImage;
    }

    /**
     * Flip an image vertically.
     * @param image The image to flip.
     * @return flippedImage
     */
    public static BufferedImage flipVertically(BufferedImage image) {
        BufferedImage trimImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D graphics = trimImage.createGraphics();
        graphics.drawImage(image, 0, image.getHeight(), image.getWidth(), -image.getHeight(), null);
        graphics.dispose();

        return trimImage;
    }

    /**
     * Apply a filter to an image.
     * @param image  The image to work on.
     * @param filter The filter to apply.
     * @return resultImage
     */
    public static BufferedImage applyFilter(BufferedImage image, ImageFilter filter) {
        Image write = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), filter)); // Make it transparent if marked.
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // Make transparent.
        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(write, 0, 0, image.getWidth(), image.getHeight(), null); // Draw the image not flipped.
        graphics.dispose();

        return newImage;
    }

    // Black -> Transparency
    public static class TransparencyFilter extends RGBImageFilter {
        @Override
        public int filterRGB(int x, int y, int rgb) {
            int colorWOAlpha = rgb & 0xFFFFFF;
            return colorWOAlpha == 0x000000 ? colorWOAlpha : rgb;
        }
    }

    // Transparency -> Black
    public static class BlackFilter extends RGBImageFilter {
        @Override
        public int filterRGB(int x, int y, int rgb) {
            int alpha = rgb >>> (3 * Constants.BITS_PER_BYTE);
            return alpha == 0 ? 0xFF000000 : rgb;
        }
    }
}
