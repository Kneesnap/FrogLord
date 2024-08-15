package net.highwayfrogs.editor.file.vlo;

import javafx.scene.image.PixelFormat;
import net.highwayfrogs.editor.Constants;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.nio.IntBuffer;

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
        BufferedImage trimImage = new BufferedImage(gameImage.getIngameWidth(), gameImage.getIngameHeight(), image.getType());
        Graphics2D graphics = trimImage.createGraphics();
        graphics.drawImage(image, -gameImage.getLeftPadding(), -gameImage.getUpPadding(), gameImage.getFullWidth(), gameImage.getFullHeight(), null);
        graphics.dispose();

        return trimImage;
    }

    /**
     * Trim the padding off of this image.
     * @param image     The image to trim.
     * @param upTrim    The amount of pixels to cut from the top.
     * @param downTrim  The amount of pixels to cut from the bottom.
     * @param leftTrim  The amount of pixels to cut from the left.
     * @param rightTrim The amount of pixels to cut from the right.
     * @return trimmedImage
     */
    public static BufferedImage trimEdges(BufferedImage image, int upTrim, int downTrim, int leftTrim, int rightTrim) {
        if (image == null)
            throw new NullPointerException("Cannot trim a null image!");
        if (upTrim < 0)
            throw new IllegalArgumentException("Cannot trim " + upTrim + " pixels from the top of the image.");
        if (downTrim < 0)
            throw new IllegalArgumentException("Cannot trim " + downTrim + " pixels from the bottom of the image.");
        if (leftTrim < 0)
            throw new IllegalArgumentException("Cannot trim " + leftTrim + " pixels from the left boundary of the image.");
        if (rightTrim < 0)
            throw new IllegalArgumentException("Cannot trim " + rightTrim + " pixels from the right boundary of the image.");

        // Don't need to trim.
        if (upTrim == 0 && downTrim == 0 && leftTrim == 0 && rightTrim == 0)
            return image;

        // Calculate new width / height.
        int newWidth = image.getWidth() - leftTrim - rightTrim;
        int newHeight = image.getHeight() - downTrim - upTrim;
        if (newWidth < 0)
            throw new IllegalArgumentException("The amount of pixels we tried to trim was more than the amount of pixels available to trim! (Left: " + leftTrim + ", Right: " + rightTrim + ", Image Width: " + image.getWidth() + ")");
        if (newHeight < 0)
            throw new IllegalArgumentException("The amount of pixels we tried to trim was more than the amount of pixels available to trim! (Up: " + upTrim + ", Down: " + downTrim + ", Image Height: " + image.getHeight() + ")");

        // Create new image.
        BufferedImage trimImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D graphics = trimImage.createGraphics();
        graphics.drawImage(image, -leftTrim, -upTrim, image.getWidth(), image.getHeight(), null);
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
     * Resize an image to a new width / height.
     * @param image           The image to scale.
     * @param newWidth        The new image width.
     * @param newHeight       The new image height.
     * @param nearestNeighbor Whether nearest neighbor interpolation should be used.
     * @return scaledImage
     */
    public static BufferedImage resizeImage(BufferedImage image, int newWidth, int newHeight, boolean nearestNeighbor) {
        if (newWidth == image.getWidth() && newHeight == image.getHeight())
            return image; // There would be no change.

        BufferedImage scaleImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D graphics = scaleImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, nearestNeighbor ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(image, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        return scaleImage;
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
        public int filterRGB(int x, int y, int argb) {
            int colorWOAlpha = argb & 0x00FFFFFF;
            return colorWOAlpha == 0x000000 ? colorWOAlpha : argb;
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
     * Draws one image onto another very quickly.
     * @param sourceImage The image to transfer from.
     * @param targetImage The image to transfer to.
     * @param targetGraphics The graphics to draw the image with if necessary.
     * @param x the x coordinate to place the texture in the target at.
     * @param y the y coordinate to place the texture in the target at.
     */
    public static void drawImageFast(BufferedImage sourceImage, BufferedImage targetImage, Graphics targetGraphics, int x, int y) {
        if (sourceImage.getType() != targetImage.getType()) {
            if (targetGraphics != null) {
                targetGraphics.drawImage(sourceImage, x, y, null);
            } else {
                Graphics graphics = targetImage.createGraphics();
                graphics.drawImage(sourceImage, x, y, null);
                graphics.dispose();
            }
        } else {
            int[] rawSourceImage = getPixelIntegerArray(sourceImage);
            int[] rawTargetImage = getPixelIntegerArray(targetImage);

            int sourceImageWidth = sourceImage.getWidth();
            int targetImageWidth = targetImage.getWidth();
            int copyWidth = Math.min(sourceImageWidth, targetImageWidth - x);
            for (int yOffset = 0; yOffset < sourceImage.getHeight(); yOffset++)
                System.arraycopy(rawSourceImage, (yOffset * sourceImageWidth), rawTargetImage, ((y + yOffset) * targetImageWidth) + x, copyWidth);
        }
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

    /**
     * Writes the buffered image to the FX image.
     * Adapted from <a href="https://stackoverflow.com/questions/30970005/bufferedimage-to-javafx-image"/>.
     * @param awtImage the BufferedImage to write to the FX image.
     * @param fxImage the FX image to be written to
     */
    public static void writeBufferedImageToFxImage(BufferedImage awtImage, javafx.scene.image.WritableImage fxImage, int x, int y) {
        if (awtImage == null)
            throw new NullPointerException("awtImage");
        if (fxImage == null)
            throw new NullPointerException("fxImage");
        if (x < 0 || y < 0 || x + awtImage.getWidth() > fxImage.getWidth() || y + awtImage.getHeight() > fxImage.getHeight())
            throw new IllegalArgumentException("Cannot paste image of dimensions " + awtImage.getWidth() + "x" + awtImage.getHeight() + " at position (" + x + ", " + y + ") for an FX image of dimensions " + fxImage.getWidth() + "x" + fxImage.getHeight() + ".");

        // Ensure the image is the appropriate format.
        awtImage = convertBufferedImageToFormat(awtImage, BufferedImage.TYPE_INT_ARGB);

        // Converting the BufferedImage to an IntBuffer.
        int[] intArgbBuffer = getPixelIntegerArray(awtImage);

        // Converting the IntBuffer to an Image.
        PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();
        fxImage.getPixelWriter().setPixels(x, y, awtImage.getWidth(), awtImage.getHeight(), pixelFormat, intArgbBuffer, 0, awtImage.getWidth());
    }

    /**
     * Gets the integer array containing raw pixel data for the image.
     * This can be edited directly, and yields the most performant editing results.
     * @param awtImage the image to edit directly
     * @return integerArray
     */
    public static int[] getPixelIntegerArray(BufferedImage awtImage) {
        return awtImage != null ? ((DataBufferInt) awtImage.getRaster().getDataBuffer()).getData() : null;
    }

    /**
     * Writes the buffered image to the desired format.
     * Performs no operations if the image is already the desired type.
     * @param oldAwtImage the BufferedImage to convert
     * @param imageType the image type to convert it to.
     */
    public static BufferedImage convertBufferedImageToFormat(BufferedImage oldAwtImage, int imageType) {
        if (oldAwtImage == null)
            throw new NullPointerException("oldAwtImage");

        if (oldAwtImage.getType() == imageType)
            return oldAwtImage;

        // Convert the image to the new format.
        BufferedImage newAwtImage = new BufferedImage(oldAwtImage.getWidth(), oldAwtImage.getHeight(), imageType);
        Graphics2D graphics = newAwtImage.createGraphics();
        try {
            graphics.drawImage(oldAwtImage, 0, 0, oldAwtImage.getWidth(), oldAwtImage.getHeight(), null);
        } finally {
            graphics.dispose();
        }

        return newAwtImage;
    }
}