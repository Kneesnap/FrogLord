package net.highwayfrogs.editor.utils.image.quantization.octree;

import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.image.BufferedImage;

public class OctreeQuantizer {
    /**
     * Quantize the image to use only up to the given number of colors.
     * This is a good algorithm to use if the image has alpha.
     * @param argbPixels the input pixels to quantize. This array will be modified directly.
     * @param maxColors the maximum number of images to
     */
    public static void quantizeImage(int[] argbPixels, int maxColors) {
        if (argbPixels == null)
            throw new NullPointerException("argbPixels");
        if (maxColors <= 0)
            throw new IllegalArgumentException("Invalid maxColors: " + maxColors);

        Octree ot = new Octree();

        // Insert all pixel colors to Octree.
        for (int i = 0; i < argbPixels.length; i++) {
            int argb = argbPixels[i];
            int a = (argb >>> 24) & 0xff;
            int r = (argb >> 16) & 0xff;
            int g = (argb >> 8) & 0xff;
            int b = argb & 0xff;
            ot.insert(a, r, g, b);
        }

        // If there are less than the max number of colors found, then we can just return the image as-is, it's already quantized to the given specification.
        if (ot.allLeaves.size() <= maxColors)
            return;

        // Reduce down to the desired color maximum.
        ot.reduce(maxColors);

        // Apply quantized colors to the image.
        for (int i = 0; i < argbPixels.length; i++) {
            int argb = argbPixels[i];
            int a = (argb >>> 24) & 0xff;
            int r = (argb >> 16) & 0xff;
            int g = (argb >> 8) & 0xff;
            int b = argb & 0xff;

            OTNode node = ot.find(a, r, g, b);
            if (node == null)
                throw new RuntimeException(String.format("Octree failed to find a replacement color for ARGB: %08X.", argb));

            // Apply new quantized pixel.
            argbPixels[i] = node.getColor();
        }
    }

    /**
     * Quantize the image to use only up to the given number of colors.
     * This is a good algorithm to use if the image has alpha.
     * @param input the input image to quantize. Sometimes, this image will be modified directly.
     * @param maxColors the maximum number of images to
     * @return quantizedImage in ARGB8888 format.
     */
    public static BufferedImage quantizeImage(BufferedImage input, int maxColors) {
        if (input == null)
            throw new NullPointerException("input");
        if (maxColors <= 0)
            throw new IllegalArgumentException("Invalid maxColors: " + maxColors);

        BufferedImage output = ImageUtils.convertBufferedImageToFormat(input, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ImageUtils.getWritablePixelIntegerArray(output);
        quantizeImage(pixels, maxColors);
        return output;
    }
}
