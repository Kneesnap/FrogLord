package net.highwayfrogs.editor.file.vlo;

import net.highwayfrogs.editor.Constants;

import java.awt.*;
import java.awt.geom.AffineTransform;
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
     * Scale an image by its width.
     * @param image       The image to scale.
     * @param scaleFactor The factor to scale the image horizontally by.
     * @return scaledImage
     */
    public static BufferedImage scaleWidth(BufferedImage image, double scaleFactor) {
        int newWidth = (int) (image.getWidth() * scaleFactor);
        if (newWidth == image.getWidth())
            return image; // There would be no change.

        BufferedImage scaleImage = new BufferedImage(newWidth, image.getHeight(), image.getType());
        Graphics2D graphics = scaleImage.createGraphics();
        graphics.drawImage(image, 0, 0, newWidth, image.getHeight(), null);
        graphics.dispose();
        return scaleImage;
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

    /**
     * Creates a duplicate of an image.
     * @param source The image to create a duplicate of.
     * @return newImage
     */
    public static BufferedImage copyImage(BufferedImage source) {
        return copyImage(source, null);
    }

    /**
     * Copies the contents of one image to another.
     * @param source The image to transfer from.
     * @param target The target to transfer to.
     * @return targetImage
     */
    public static BufferedImage copyImage(BufferedImage source, BufferedImage target) {
        if (target == null || target.getWidth() != source.getWidth() || target.getHeight() != source.getHeight() || target.getType() != source.getType())
            target = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = target.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return target;
    }

    /**
     * Takes an image and creates a new rotated version.
     * Copied from https://stackoverflow.com/questions/37758061/rotate-a-buffered-image-in-java/37758533
     * @param img   The image to rotate.
     * @param angle The angle to rotate.
     * @return rotatedImage
     */
    public static BufferedImage rotateImage(BufferedImage img, double angle) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2D, (newHeight - h) / 2D);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        return rotated;
    }
}
