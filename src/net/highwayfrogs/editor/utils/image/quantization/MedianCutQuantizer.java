package net.highwayfrogs.editor.utils.image.quantization;

import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * This sample code is made available as part of the book "Digital Image
 * Processing - An Algorithmic Introduction using Java" by Wilhelm Burger
 * and Mark J. Burge, Copyright (C) 2005-2008 Springer-Verlag Berlin,
 * Heidelberg, New York.
 * Note that this code comes with absolutely no warranty of any kind.
 * See <a href="http://www.imagingbook.com"/> for details and licensing conditions.
 * <p>
 * Date: 2007/11/10
 * Obtained from: <a href="https://github.com/biometrics/imagingbook/blob/master/src/color/MedianCutQuantizer.java"/> on 01/01/2026.
 */
public class MedianCutQuantizer {
    private final int maxColors;
    private final ColorNode[] quantColors;    // quantized colors
    private ColorNode[] imageColors;    // original (unique) image colors

    private MedianCutQuantizer(int[] pixels, int maxColors) {
        this.maxColors = maxColors;
        this.quantColors = findRepresentativeColors(pixels, maxColors);
    }

    ColorNode[] findRepresentativeColors(int[] pixels, int maxColors) {
        ColorHistogram colorHist = new ColorHistogram(pixels);
        int numberOfColors = colorHist.getNumberOfColors(); // Number of unique colors in source pixels.

        imageColors = new ColorNode[numberOfColors];
        for (int i = 0; i < numberOfColors; i++) {
            int argb = colorHist.getColor(i);
            int cnt = colorHist.getCount(i);
            imageColors[i] = new ColorNode(argb, cnt);
        }

        if (numberOfColors <= maxColors) // image has fewer colors than palette max, so we can just use the original.
            return imageColors;

        ColorBox initialBox = new ColorBox(0, numberOfColors - 1, 0);
        List<ColorBox> colorSet = new ArrayList<>();
        colorSet.add(initialBox);
        int k = 1;
        boolean done = false;
        while (k < maxColors && !done) {
            ColorBox nextBox = findBoxToSplit(colorSet);
            if (nextBox != null) {
                ColorBox newBox = nextBox.splitBox();
                colorSet.add(newBox);
                k = k + 1;
            } else {
                done = true;
            }
        }
        return averageColors(colorSet);
    }

    public int[] quantizeImage(int[] origPixels) {
        if (this.imageColors.length <= this.maxColors)
            return origPixels; // Image is already quantized.

        int[] quantPixels = origPixels.clone();
        for (int i = 0; i < origPixels.length; i++) {
            ColorNode color = findClosestColor(origPixels[i]);
            quantPixels[i] = color.argb;
        }
        return quantPixels;
    }

    public BufferedImage quantizeToByteIndexedImage(int width, int height, int[] originalPixels) {
        if (this.quantColors.length > 256)
            throw new Error("cannot index to more than 256 colors due to IndexedColorModel limitation");

        byte[] idxPixels = new byte[originalPixels.length];
        for (int i = 0; i < originalPixels.length; i++)
            idxPixels[i] = (byte) findClosestColorIndex(originalPixels[i]);

        IndexColorModel colorPalette = makeIndexColorModel();
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorPalette);
        newImage.getRaster().setDataElements(0, 0, width, height, idxPixels);

        return newImage;
    }

    IndexColorModel makeIndexColorModel() {
        int nColors = this.quantColors.length;
        byte[] rMap = new byte[nColors];
        byte[] gMap = new byte[nColors];
        byte[] bMap = new byte[nColors];
        byte[] aMap = new byte[nColors];
        for (int i = 0; i < nColors; i++) {
            rMap[i] = (byte) this.quantColors[i].red;
            gMap[i] = (byte) this.quantColors[i].grn;
            bMap[i] = (byte) this.quantColors[i].blu;
            aMap[i] = (byte) this.quantColors[i].alp;
        }

        return new IndexColorModel(8, nColors, rMap, gMap, bMap, aMap);
    }

    ColorNode findClosestColor(int rgb) {
        int idx = findClosestColorIndex(rgb);
        return this.quantColors[idx];
    }

    int findClosestColorIndex(int rgb) {
        int alp = ((rgb & 0xFF000000) >>> 24);
        int red = ((rgb & 0xFF0000) >> 16);
        int grn = ((rgb & 0xFF00) >> 8);
        int blu = (rgb & 0xFF);
        int minIdx = 0;
        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < this.quantColors.length; i++) {
            ColorNode color = this.quantColors[i];
            int d2 = color.distance2(alp, red, grn, blu);
            if (d2 < minDistance) {
                minDistance = d2;
                minIdx = i;
            }
        }
        return minIdx;
    }

    private ColorBox findBoxToSplit(List<ColorBox> colorBoxes) {
        ColorBox boxToSplit = null;
        // from the set of splittable color boxes
        // select the one with the minimum level
        int minLevel = Integer.MAX_VALUE;
        for (ColorBox box : colorBoxes) {
            if (box.colorCount() >= 2) {    // box can be split
                if (box.level < minLevel) {
                    boxToSplit = box;
                    minLevel = box.level;
                }
            }
        }
        return boxToSplit;
    }

    private ColorNode[] averageColors(List<ColorBox> colorBoxes) {
        int n = colorBoxes.size();
        ColorNode[] avgColors = new ColorNode[n];
        int i = 0;
        for (ColorBox box : colorBoxes) {
            avgColors[i] = box.getAverageColor();
            i = i + 1;
        }
        return avgColors;
    }

    // -------------- class ColorNode -------------------------------------------

    static class ColorNode {
        private final int argb;
        private final int alp, red, grn, blu;
        private final int cnt;

        ColorNode(int argb, int cnt) {
            this.argb = argb;
            this.alp = (argb & 0xFF000000) >>> 24;
            this.red = (argb & 0xFF0000) >> 16;
            this.grn = (argb & 0xFF00) >> 8;
            this.blu = (argb & 0xFF);
            this.cnt = cnt;
        }

        ColorNode(int alp, int red, int grn, int blu, int cnt) {
            this.argb = ((alp & 0xff) << 24) | ((red & 0xff) << 16) | ((grn & 0xff) << 8) | blu & 0xff;
            this.alp = alp;
            this.red = red;
            this.grn = grn;
            this.blu = blu;
            this.cnt = cnt;
        }

        int distance2(int alp, int red, int grn, int blu) {
            // returns the squared distance between (red, grn, blu)
            // and this color
            int da = this.alp - alp;
            int dr = this.red - red;
            int dg = this.grn - grn;
            int db = this.blu - blu;
            return da * da + dr * dr + dg * dg + db * db;
        }

        public String toString() {
            String s = this.getClass().getSimpleName();
            s = s + " alpha=" + alp + " red=" + red + " green=" + grn + " blue=" + blu + " count=" + cnt;
            return s;
        }
    }

    // -------------- class ColorBox -------------------------------------------

    private class ColorBox {
        int lower;    // lower index into 'imageColors'
        int upper; // upper index into 'imageColors'
        int level;        // split-level of this color box
        int count = 0;    // number of pixels represented by this color box
        int aMin, aMax;    // range of contained colors in red dimension
        int rMin, rMax;    // range of contained colors in red dimension
        int gMin, gMax;    // range of contained colors in green dimension
        int bMin, bMax;    // range of contained colors in blue dimension

        ColorBox(int lower, int upper, int level) {
            this.lower = lower;
            this.upper = upper;
            this.level = level;
            this.trim();
        }

        int colorCount() {
            return upper - lower;
        }

        void trim() {
            // recompute the boundaries of this color box
            aMin = 255;
            aMax = 0;
            rMin = 255;
            rMax = 0;
            gMin = 255;
            gMax = 0;
            bMin = 255;
            bMax = 0;
            count = 0;
            for (int i = lower; i <= upper; i++) {
                ColorNode color = imageColors[i];
                count = count + color.cnt;
                int a = color.alp;
                int r = color.red;
                int g = color.grn;
                int b = color.blu;
                if (a > aMax) aMax = a;
                if (a < aMin) aMin = a;
                if (r > rMax) rMax = r;
                if (r < rMin) rMin = r;
                if (g > gMax) gMax = g;
                if (g < gMin) gMin = g;
                if (b > bMax) bMax = b;
                if (b < bMin) bMin = b;
            }
        }

        // Split this color box at the median point along its
        // longest color dimension
        ColorBox splitBox() {
            if (this.colorCount() < 2)    // this box cannot be split
                return null;
            else {
                // find the longest dimension of this box:
                ColorDimension dim = getLongestColorDimension();

                // find median along dim
                int med = findMedian(dim);

                // now split this box at the median return the resulting new
                // box.
                int nextLevel = level + 1;
                ColorBox newBox = new ColorBox(med + 1, upper, nextLevel);
                this.upper = med;
                this.level = nextLevel;
                this.trim();
                return newBox;
            }
        }

        // Find the longest dimension of this color box (ALPHA, RED, GREEN, or BLUE)
        ColorDimension getLongestColorDimension() {
            int aLength = aMax - aMin;
            int rLength = rMax - rMin;
            int gLength = gMax - gMin;
            int bLength = bMax - bMin;
            if (bLength >= aLength && bLength >= rLength && bLength >= gLength)
                return ColorDimension.BLUE;
            else if (gLength >= aLength && gLength >= rLength/* && gLength >= bLength*/)
                return ColorDimension.GREEN;
            else if (rLength >= aLength/* && rLength >= gLength && rLength >= bLength*/)
                return ColorDimension.RED;
            else return ColorDimension.ALPHA;
        }

        // Find the position of the median in RGB space along
        // the red, green or blue dimension, respectively.
        int findMedian(ColorDimension dim) {
            // sort color in this box along dimension dim:
            Arrays.sort(imageColors, lower, upper + 1, dim.comparator);
            // find the median point:
            int half = count / 2;
            int nPixels, median;
            for (median = lower, nPixels = 0; median < upper; median++) {
                nPixels = nPixels + imageColors[median].cnt;
                if (nPixels >= half)
                    break;
            }
            return median;
        }

        ColorNode getAverageColor() {
            int aSum = 0;
            int rSum = 0;
            int gSum = 0;
            int bSum = 0;
            int n = 0;
            for (int i = lower; i <= upper; i++) {
                ColorNode ci = imageColors[i];
                int cnt = ci.cnt;
                aSum = aSum + cnt * ci.alp;
                rSum = rSum + cnt * ci.red;
                gSum = gSum + cnt * ci.grn;
                bSum = bSum + cnt * ci.blu;
                n = n + cnt;
            }
            double nd = n;
            int avgAlp = (int) (0.5 + aSum / nd);
            int avgRed = (int) (0.5 + rSum / nd);
            int avgGrn = (int) (0.5 + gSum / nd);
            int avgBlu = (int) (0.5 + bSum / nd);
            return new ColorNode(avgAlp, avgRed, avgGrn, avgBlu, n);
        }

        public String toString() {
            String s = this.getClass().getSimpleName();
            s = s + " lower=" + lower + " upper=" + upper;
            s = s + " count=" + count + " level=" + level;
            s = s + " aMin=" + aMin + " aMax=" + aMax;
            s = s + " rMin=" + rMin + " rMax=" + rMax;
            s = s + " gMin=" + gMin + " gMax=" + gMax;
            s = s + " bMin=" + bMin + " bMax=" + bMax;
            s = s + " bMin=" + bMin + " bMax=" + bMax;
            return s;
        }
    }

    //	 ---  color dimensions ------------------------

    // The main purpose of this enumeration class is associate
    // the color dimensions with the corresponding comparators.
    private enum ColorDimension {
        ALPHA(new alphaComparator()),
        RED(new redComparator()),
        GREEN(new grnComparator()),
        BLUE(new bluComparator());

        public final Comparator<ColorNode> comparator;

        ColorDimension(Comparator<ColorNode> cmp) {
            this.comparator = cmp;
        }
    }

    // --- color comparators used for sorting colors along different dimensions ---

    static class alphaComparator implements Comparator<ColorNode> {
        public int compare(ColorNode colA, ColorNode colB) {
            return colA.alp - colB.alp;
        }
    }

    static class redComparator implements Comparator<ColorNode> {
        public int compare(ColorNode colA, ColorNode colB) {
            return colA.red - colB.red;
        }
    }

    static class grnComparator implements Comparator<ColorNode> {
        public int compare(ColorNode colA, ColorNode colB) {
            return colA.grn - colB.grn;
        }
    }

    static class bluComparator implements Comparator<ColorNode> {
        public int compare(ColorNode colA, ColorNode colB) {
            return colA.blu - colB.blu;
        }
    }

    private static class ColorHistogram {
        int[] colorArray;
        int[] countArray;

        ColorHistogram(int[] pixelsOrig) {
            int[] pixelsCpy = pixelsOrig.clone();
            Arrays.sort(pixelsCpy);

            // count unique colors:
            int k = -1; // current color index
            int curColor = -1;
            for (int i = 0; i < pixelsCpy.length; i++) {
                if (pixelsCpy[i] != curColor) {
                    k++;
                    curColor = pixelsCpy[i];
                }
            }
            int nColors = k + 1;

            // tabulate and count unique colors:
            colorArray = new int[nColors];
            countArray = new int[nColors];
            k = -1;    // current color index
            curColor = -1;
            for (int i = 0; i < pixelsCpy.length; i++) {
                if (pixelsCpy[i] != curColor || i == 0) { // new color
                    k++;
                    curColor = pixelsCpy[i];
                    colorArray[k] = curColor;
                    countArray[k] = 1;
                } else {
                    countArray[k]++;
                }
            }
        }

        public int getNumberOfColors() {
            if (colorArray == null)
                return 0;
            else
                return colorArray.length;
        }

        public int getColor(int index) {
            return this.colorArray[index];
        }

        public int getCount(int index) {
            return this.countArray[index];
        }
    }

    /**
     * Quantize an image to the maximum number of colors given, and returns a new pixel buffer in ARGB8888 format.
     * NOTE: This ideally functions similarly to the following API call:
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/wincodec/nf-wincodec-iwicpalette-initializefrombitmap"/>
     * Most likely, this is what Vorg used for Frogger way back in the day. Instead of creating a palette themselves, why not just use the Windows API?
     * Paint.NET uses this API call, and Paint.NET's palette exports for the "Median Cut" algorithm look shockingly similar to the original game files.
     * Given Vorg ran on Windows, it doesn't seem very unreasonable to assume this was the same API call that Vorg used.
     * @param input the input image to quantize
     * @param maxColors the maximum number of colors in the resulting palette. Cannot exceed 256 due to Java's BufferedImage limitations.
     * @return quantizedImage
     */
    public static BufferedImage quantizeImageToARGB8888Image(BufferedImage input, int maxColors) {
        int[] pixelBuffer = quantizeImageToARGB8888Buffer(input, maxColors);
        if (input.getType() == BufferedImage.TYPE_INT_ARGB) {
            System.arraycopy(pixelBuffer, 0, ImageUtils.getWritablePixelIntegerArray(input), 0, pixelBuffer.length);
            return input;
        } else {
            return ImageUtils.createImageFromArray(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB, pixelBuffer);
        }
    }

    /**
     * Quantize an image to the maximum number of colors given, and returns a new pixel buffer in ARGB8888 format.
     * NOTE: This ideally functions similarly to the following API call:
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/wincodec/nf-wincodec-iwicpalette-initializefrombitmap"/>
     * Most likely, this is what Vorg used for Frogger way back in the day. Instead of creating a palette themselves, why not just use the Windows API?
     * Paint.NET uses this API call, and Paint.NET's palette exports for the "Median Cut" algorithm look shockingly similar to the original game files.
     * Given Vorg ran on Windows, it doesn't seem very unreasonable to assume this was the same API call that Vorg used.
     * @param input the input image to quantize
     * @param maxColors the maximum number of colors in the resulting palette. Cannot exceed 256 due to Java's BufferedImage limitations.
     * @return quantizedImage
     */
    public static int[] quantizeImageToARGB8888Buffer(BufferedImage input, int maxColors) {
        if (input == null)
            throw new NullPointerException("input");
        if (maxColors <= 0 || maxColors > 256) // Limit of 256 here since that's the limit of what BufferedImage can support.
            throw new IllegalArgumentException("Invalid maxColors: " + maxColors);

        // Ensure input image is in the accepted format.
        if (input.getType() != BufferedImage.TYPE_INT_ARGB && input.getType() != BufferedImage.TYPE_4BYTE_ABGR_PRE)
            input = ImageUtils.convertBufferedImageToFormat(input, BufferedImage.TYPE_INT_ARGB);

        int[] originalPixels = ImageUtils.getReadOnlyPixelIntegerArray(input);

        // Colors are made unique in the ColorHistogram constructor
        MedianCutQuantizer quantizer2 = new MedianCutQuantizer(originalPixels, maxColors);
        return quantizer2.quantizeImage(originalPixels);
    }

    /**
     * Quantize an image to the maximum number of colors given, and returns a new pixel buffer in ARGB8888 format.
     * NOTE: This version seems to be dithered for some reason.
     * NOTE: This ideally functions similarly to the following API call:
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/wincodec/nf-wincodec-iwicpalette-initializefrombitmap"/>
     * Most likely, this is what Vorg used for Frogger way back in the day. Instead of creating a palette themselves, why not just use the Windows API?
     * Paint.NET uses this API call, and Paint.NET's palette exports for the "Median Cut" algorithm look shockingly similar to the original game files.
     * Given Vorg ran on Windows, it doesn't seem very unreasonable to assume this was the same API call that Vorg used.
     * @param input the input image to quantize
     * @param maxColors the maximum number of colors in the resulting palette. Cannot exceed 256 due to Java's BufferedImage limitations.
     * @return quantizedImage
     */
    public static BufferedImage quantizeImageToByteIndexed(BufferedImage input, int maxColors) {
        if (input == null)
            throw new NullPointerException("input");
        if (maxColors <= 0 || maxColors > 256) // Limit of 256 here since that's the limit of what BufferedImage can support.
            throw new IllegalArgumentException("Invalid maxColors: " + maxColors);

        // Already quantized to the right amount, so return the original image.
        // Try converting without any quantization.
        // If it's already quantized (or is the right format), then return the image early.
        BufferedImage result = ImageUtils.tryConvertTo8BitIndexedBufferedImage(input, maxColors);
        if (result != null)
            return result;

        // Ensure input image is in the accepted format.
        if (input.getType() != BufferedImage.TYPE_INT_ARGB && input.getType() != BufferedImage.TYPE_4BYTE_ABGR_PRE && input.getType() != BufferedImage.TYPE_INT_RGB)
            input = ImageUtils.convertBufferedImageToFormat(input, BufferedImage.TYPE_INT_ARGB);

        int[] originalPixels = ImageUtils.getReadOnlyPixelIntegerArray(input);

        // Colors are made unique in the ColorHistogram constructor
        MedianCutQuantizer quantizer = new MedianCutQuantizer(originalPixels, maxColors);
        return quantizer.quantizeToByteIndexedImage(input.getWidth(), input.getHeight(), originalPixels);
    }
}