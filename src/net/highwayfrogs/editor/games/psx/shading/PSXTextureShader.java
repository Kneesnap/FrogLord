package net.highwayfrogs.editor.games.psx.shading;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.IndexBitArray;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArray;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * JavaFX does not give us anywhere near enough control over the 3D rendering pipeline to properly recreate the gouraud shading seen on the PlayStation.
 * This shading is very important to making the worlds render accurately too, since gouraud shading was used extensively to color Frogger maps.
 * While new FrogLord will be using a custom-built 3D engine capable of gouraud shading, a temporary solution is desired.
 * This solution is to create a new texture and apply gouraud shading to that texture. This is extremely resource inefficient, but it was pretty much the only solution before new FrogLord is ready.
 * The images generated by this class place the vertices at the following positions, unless UVs are included:
 * 1---2
 * |  /|
 * | / |
 * |/  |
 * 3---4
 * This order matches how the PSX GPU processes a quad, first using vertices 1-2-3, then 2-3-4, according to <a href="https://psx-spx.consoledev.net/graphicsprocessingunitgpu/">this link</a>.
 * Created by Kneesnap on 12/19/2023.
 */
@Getter
public class PSXTextureShader {
    private static final ThreadLocal<PSXTextureShader> TEXTURE_SHADER_THREAD = ThreadLocal.withInitial(PSXTextureShader::new);
    @SuppressWarnings("MismatchedReadAndWriteOfArray") // It is actually read.
    private final TextureCoordinate[] triangleCoordinates = new TextureCoordinate[3];
    @SuppressWarnings("MismatchedReadAndWriteOfArray") // It is actually read.
    private final CVector[] triangleColors = new CVector[3];
    private final CVector tempColorVector1 = new CVector();
    private final CVector tempColorVector2 = new CVector();
    private final CVector tempColorVector3 = new CVector();
    private final IndexBitArray pixelPosSeen = new IndexBitArray();
    private final IndexBitArray firstLayerPixelShadePositions = new IndexBitArray();
    private final FXIntArray pixelPosBuffer = new FXIntArray();
    private static final int DEFAULT_SHADING_EXPANSION_LAYERS = 4; // 2 seems to be the minimum which fully covers the polygons. There are a few diagonal polygons in VOL2.MAP that seem like they need some help, but I don't think the issue is with this constant.
    public static final int UNSHADED_COLOR_ARGB = 0x80808080;
    public static final CVector UNSHADED_COLOR = CVector.makeColorFromRGB(UNSHADED_COLOR_ARGB);

    private PSXTextureShader() {
        for (int i = 0; i < this.triangleCoordinates.length; i++)
            this.triangleCoordinates[i] = new TextureCoordinate();
        for (int i = 0; i < this.triangleColors.length; i++)
            this.triangleColors[i] = new CVector();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextureCoordinate {
        private int x;
        private int y;

        /**
         * Set both the X and Y values.
         * @param x The x value.
         * @param y The y value.
         */
        public void setXY(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Load the texture coordinates from a texture UV.
         * @param textureSource The texture source
         * @param uv The uv to load coordinates from.
         * @param scaleX The horizontal texture scaling factor. (Default = 1, which indicates no scaling)
         * @param scaleY The vertical texture scaling factor. (Default = 1, which indicates no scaling)
         */
        public void loadUV(ITextureSource textureSource, SCByteTextureUV uv, int scaleX, int scaleY) {
            this.x = (scaleX * textureSource.getLeftPadding()) + (int) (uv.getFloatU() * ((scaleX * textureSource.getUnpaddedWidth()) - 1));
            this.y = (scaleY * textureSource.getUpPadding()) + (int) (uv.getFloatV() * ((scaleY * textureSource.getUnpaddedHeight()) - 1));
        }
    }

    /**
     * Gets the instance of the PSXTextureShader available to this thread.
     */
    private static PSXTextureShader getInstance() {
        return TEXTURE_SHADER_THREAD.get();
    }

    /**
     * Makes a gouraud shaded, untextured image (POLY_G3 / POLY_G4)
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param colors The colors to apply to the image. (8 bits, 0 - 255)
     * @return gouraudShadedImage
     */
    public static BufferedImage makeGouraudShadedImage(int width, int height, CVector[] colors) {
        return makeGouraudShadedImage(null, width, height, colors);
    }

    /**
     * Makes a gouraud shaded, untextured image (POLY_G3 / POLY_G4)
     * @param targetImage The image to write the shaded data to. If null, a new one will be created.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param colors The colors to apply to the image. (8 bits, 0 - 255)
     * @return gouraudShadedImage
     */
    public static BufferedImage makeGouraudShadedImage(BufferedImage targetImage, int width, int height, CVector[] colors) {
        if (targetImage == null)
            targetImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int startX = PSXShadeTextureDefinition.UNTEXTURED_PADDING_SIZE;
        int startY = PSXShadeTextureDefinition.UNTEXTURED_PADDING_SIZE;
        int endX = width - PSXShadeTextureDefinition.UNTEXTURED_PADDING_SIZE - 1;
        int endY = height - PSXShadeTextureDefinition.UNTEXTURED_PADDING_SIZE - 1;

        PSXTextureShader instance = getInstance();
        instance.getFirstLayerPixelShadePositions().clear();
        instance.getPixelPosSeen().clear();
        instance.getPixelPosBuffer().clear();
        TextureCoordinate[] coordinates = instance.getTriangleCoordinates();
        if (colors.length == 3) {
            coordinates[0].setXY(startX, startY);
            coordinates[1].setXY(endX, startY);
            coordinates[2].setXY(startX, endY);
            shadeTriangle(instance, null, targetImage, colors, coordinates);
        } else if (colors.length == 4) {
            CVector[] triangleColors = instance.getTriangleColors();

            // Left triangle. (0, 0 is the top-left corner)
            triangleColors[0].copyFrom(colors[0]);
            triangleColors[1].copyFrom(colors[1]);
            triangleColors[2].copyFrom(colors[2]);
            coordinates[0].setXY(startX, startY);
            coordinates[1].setXY(endX, startY);
            coordinates[2].setXY(startX, endY);
            shadeTriangle(instance, null, targetImage, triangleColors, coordinates);

            // Right triangle. (width, height is the bottom-right corner)
            triangleColors[0].copyFrom(colors[3]);
            triangleColors[1].copyFrom(colors[2]);
            triangleColors[2].copyFrom(colors[1]);
            coordinates[0].setXY(endX, endY);
            coordinates[1].setXY(startX, endY);
            coordinates[2].setXY(endX, startY);
            shadeTriangle(instance, null, targetImage, triangleColors, coordinates);
        } else {
            throw new RuntimeException("Can't create gouraud shaded image with " + colors.length + " colors.");
        }

        expandShading(targetImage, instance, DEFAULT_SHADING_EXPANSION_LAYERS);
        return targetImage;
    }

    /**
     * Makes a gouraud shaded, textured image (POLY_GT3 / POLY_GT4)
     * @param originalImage The original image to create this one from.
     * @param textureSource The texture source which the texture came from.
     * @param colors The colors to apply to the image.
     * @param textureUvs The texture uvs to use as the corner of the triangles.
     * @param textureScaleX The horizontal texture scaling factor. (Default = 1, which indicates no scaling)
     * @param textureScaleY The vertical texture scaling factor. (Default = 1, which indicates no scaling)
     * @param highlightCorners If true, the corners of the polygon will be colored using the baked lighting polygon vertex colors. (Order: Yellow, Green, Red, Blue)
     * @return gouraudShadedImage
     */
    public static BufferedImage makeTexturedGouraudShadedImage(BufferedImage originalImage, ITextureSource textureSource, CVector[] colors, SCByteTextureUV[] textureUvs, int textureScaleX, int textureScaleY, boolean highlightCorners) {
        return makeTexturedGouraudShadedImage(originalImage, null, textureSource, colors, textureUvs, textureScaleX, textureScaleY, highlightCorners);
    }

    /**
     * Makes a gouraud shaded, textured image (POLY_GT3 / POLY_GT4)
     * @param originalImage The original image to create this one from.
     * @param targetImage The image to write the shaded data to. If null, a new one will be created.
     * @param textureSource The texture source which the texture came from.
     * @param colors The colors to apply to the image.
     * @param textureUvs The texture uvs to use as the corner of the triangles.
     * @param textureScaleX The horizontal texture scaling factor. (Default = 1, which indicates no scaling)
     * @param textureScaleY The vertical texture scaling factor. (Default = 1, which indicates no scaling)
     * @param highlightCorners If true, the corners of the polygon will be colored using the baked lighting polygon vertex colors. (Order: Yellow, Green, Red, Blue)
     * @return gouraudShadedImage
     */
    public static BufferedImage makeTexturedGouraudShadedImage(BufferedImage originalImage, BufferedImage targetImage, ITextureSource textureSource, CVector[] colors, SCByteTextureUV[] textureUvs, int textureScaleX, int textureScaleY, boolean highlightCorners) {
        if (targetImage == null)
            targetImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        PSXTextureShader instance = getInstance();
        TextureCoordinate[] coordinates = instance.getTriangleCoordinates();
        instance.getFirstLayerPixelShadePositions().clear();
        instance.getPixelPosSeen().clear();
        instance.getPixelPosBuffer().clear();
        if (colors.length == 3) {
            coordinates[0].loadUV(textureSource, textureUvs[0], textureScaleX, textureScaleY);
            coordinates[1].loadUV(textureSource, textureUvs[1], textureScaleX, textureScaleY);
            coordinates[2].loadUV(textureSource, textureUvs[2], textureScaleX, textureScaleY);
            shadeTriangle(instance, originalImage, targetImage, colors, coordinates);

            // Expand shading, then apply shading to the source image.
            expandShading(targetImage, instance, DEFAULT_SHADING_EXPANSION_LAYERS);
            applyShadingToSourceImage(originalImage, targetImage);
            if (highlightCorners) {
                targetImage.setRGB(coordinates[0].getX(), coordinates[0].getY(), Color.YELLOW.getRGB());
                targetImage.setRGB(coordinates[1].getX(), coordinates[1].getY(), Color.GREEN.getRGB());
                targetImage.setRGB(coordinates[2].getX(), coordinates[2].getY(), Color.RED.getRGB());
            }

        } else if (colors.length == 4) {
            CVector[] triangleColors = instance.getTriangleColors();

            // Left triangle.
            triangleColors[0].copyFrom(colors[0]);
            triangleColors[1].copyFrom(colors[1]);
            triangleColors[2].copyFrom(colors[2]);
            coordinates[0].loadUV(textureSource, textureUvs[0], textureScaleX, textureScaleY);
            coordinates[1].loadUV(textureSource, textureUvs[1], textureScaleX, textureScaleY);
            coordinates[2].loadUV(textureSource, textureUvs[2], textureScaleX, textureScaleY);
            shadeTriangle(instance, originalImage, targetImage, triangleColors, coordinates);

            // Right triangle.
            triangleColors[0].copyFrom(colors[3]);
            triangleColors[1].copyFrom(colors[2]);
            triangleColors[2].copyFrom(colors[1]);
            coordinates[0].loadUV(textureSource, textureUvs[3], textureScaleX, textureScaleY);
            coordinates[1].loadUV(textureSource, textureUvs[2], textureScaleX, textureScaleY);
            coordinates[2].loadUV(textureSource, textureUvs[1], textureScaleX, textureScaleY);
            shadeTriangle(instance, originalImage, targetImage, triangleColors, coordinates);

            // Expand shading, then apply shading to the source image.
            expandShading(targetImage, instance, DEFAULT_SHADING_EXPANSION_LAYERS);
            applyShadingToSourceImage(originalImage, targetImage);
            if (highlightCorners) {
                targetImage.setRGB(coordinates[0].getX(), coordinates[0].getY(), Color.BLUE.getRGB()); // 3
                targetImage.setRGB(coordinates[1].getX(), coordinates[1].getY(), Color.RED.getRGB()); // 2
                targetImage.setRGB(coordinates[2].getX(), coordinates[2].getY(), Color.GREEN.getRGB()); // 1
                coordinates[0].loadUV(textureSource, textureUvs[0], textureScaleX, textureScaleY);
                targetImage.setRGB(coordinates[0].getX(), coordinates[0].getY(), Color.YELLOW.getRGB()); // 0
            }
        } else {
            throw new RuntimeException("Can't create gouraud shaded image with " + colors.length + " colors.");
        }

        return targetImage;
    }

    /**
     * Draws a gouraud shading triangle onto an image.
     * @param instance The cached data we can use for the shading.
     * @param sourceImage The texture to validate against.
     * @param targetImage The image to draw the shaded image onto.
     * @param colors The colors of each triangle vertex.
     * @param vertices The position of each triangle vertex.
     */
    private static void shadeTriangle(PSXTextureShader instance, BufferedImage sourceImage, BufferedImage targetImage, CVector[] colors, TextureCoordinate[] vertices) {
        if (sourceImage != null && (sourceImage.getWidth() != targetImage.getWidth() || sourceImage.getHeight() != targetImage.getHeight()))
            throw new RuntimeException("The source image had dimensions of " + sourceImage.getWidth() + "x" + sourceImage.getHeight() + ", but the target image was " + targetImage.getWidth() + "x" + targetImage.getHeight() + ".");

        int imageWidth = targetImage.getWidth();

        // BufferedImage's origin is the top left corner. (Ie: X = 0, Y = 0 is the top left corner)
        // Step 1) Find the scanline bounds.
        int topIndex = -1;
        int leftIndex = -1;
        int rightIndex = -1;

        // Find top index (The vertex which scanlines start from, connecting to left & right index)
        for (int i = 0; i < vertices.length; i++)
            if (topIndex == -1 || vertices[i].getY() < vertices[topIndex].getY())
                topIndex = i;

        // Find left index (This is the left-most scanline index which is not the top index or the right index.)
        for (int i = 0; i < vertices.length; i++) {
            if (i == topIndex)
                continue; // Skip top index, since it can't be both the top and the left/right point.

            if (leftIndex == -1 || vertices[i].getX() < vertices[leftIndex].getX())
                leftIndex = i;
        }

        // Find right index (This is the right-most scanline index which is not the top index or the left index.)
        for (int i = 0; i < vertices.length; i++) {
            if (i != topIndex && i != leftIndex)
                rightIndex = i;
        }

        // Step 2) Scanline
        TextureCoordinate topPos = vertices[topIndex];
        TextureCoordinate leftPos = vertices[leftIndex];
        TextureCoordinate rightPos = vertices[rightIndex];
        int minTriangleY = topPos.getY();
        int maxTriangleY = Math.max(leftPos.getY(), rightPos.getY());
        int topToLeftHeight = leftPos.getY() - topPos.getY();
        int topToRightHeight = rightPos.getY() - topPos.getY();
        int leftToRightHeight = rightPos.getY() - leftPos.getY();
        int rightToLeftHeight = leftPos.getY() - rightPos.getY();
        float inverseLeftSlope = (topToLeftHeight != 0) ? (((float) (leftPos.getX() - topPos.getX())) / topToLeftHeight) : Float.NaN;
        float inverseRightSlope = (topToRightHeight != 0) ? (((float) (rightPos.getX() - topPos.getX())) / topToRightHeight) : Float.NaN;
        float inverseLeftRightSlope = (leftToRightHeight != 0) ? (((float) (rightPos.getX() - leftPos.getX())) / leftToRightHeight) : Float.NaN;

        // Draw the scan-lines.
        int lastLeftLineX = -1, lastRightLineX = -1;
        IndexBitArray seenPixelPos = instance.getPixelPosSeen();
        IndexBitArray firstLayerPixelShadePositions = instance.getFirstLayerPixelShadePositions();
        FXIntArray pixelPosBuffer = instance.getPixelPosBuffer();
        int[] rawTargetImage = ImageWorkHorse.getPixelIntegerArray(targetImage);
        int maxRenderedTriangleY = Math.min(maxTriangleY, targetImage.getHeight() - 1);
        for (int y = Math.max(0, minTriangleY); y <= maxRenderedTriangleY; y++) {
            // Calculate the left scanline boundary.
            CVector leftLineColor;
            int leftLineX;
            if (y < leftPos.getY()) { // Interpolate between top vertex and left vertex.
                int yOffset = (y - minTriangleY);
                leftLineColor = interpolateCVector(colors[topIndex], colors[leftIndex], (float) yOffset / topToLeftHeight, instance.getTempColorVector1());
                leftLineX = topPos.getX() + (int) (inverseLeftSlope * yOffset);
            } else if (y > leftPos.getY()) { // Interpolate between left vertex and right vertex.
                int yOffset = (y - leftPos.getY());
                leftLineColor = interpolateCVector(colors[leftIndex], colors[rightIndex], (float) yOffset / leftToRightHeight, instance.getTempColorVector1());
                leftLineX = leftPos.getX() + (int) (inverseLeftRightSlope * yOffset);
            } else { // Use data directly from the vertex since it's on this scanline.
                leftLineColor = colors[leftIndex];
                leftLineX = leftPos.getX();
            }

            // Calculate the right scanline boundary.
            CVector rightLineColor;
            int rightLineX;
            if (y < rightPos.getY()) { // Interpolate between top vertex and right vertex.
                int yOffset = (y - minTriangleY);
                rightLineColor = interpolateCVector(colors[topIndex], colors[rightIndex], (float) yOffset / topToRightHeight, instance.getTempColorVector2());
                rightLineX = topPos.getX() + (int) (inverseRightSlope * yOffset);
            } else if (y > rightPos.getY()) { // Interpolate between right vertex and left vertex.
                int yOffset = (y - rightPos.getY());
                rightLineColor = interpolateCVector(colors[rightIndex], colors[leftIndex], (float) yOffset / rightToLeftHeight, instance.getTempColorVector2());
                rightLineX = rightPos.getX() + (int) (inverseLeftRightSlope * yOffset);
            } else { // Use data directly from the vertex since it's on this scanline.
                rightLineColor = colors[rightIndex];
                rightLineX = rightPos.getX();
            }

            // Ensures the line is always drawn. (MediEvil has situations where the topIndex is somewhat ambiguous, and this ensures the image still draws properly)
            if (leftLineX > rightLineX) {
                int temp = leftLineX;
                leftLineX = rightLineX;
                rightLineX = temp;
                CVector tempColor = leftLineColor;
                leftLineColor = rightLineColor;
                rightLineColor = tempColor;
            }

            // Fill a scanline with interpolated pixel shading colors, and mark other areas as having shading.
            CVector tempInterpolatedColor = instance.getTempColorVector3();
            int minX = Math.max(0, lastLeftLineX >= 0 ? Math.min(lastLeftLineX, leftLineX) : leftLineX);
            int maxX = Math.max(lastRightLineX, rightLineX);
            for (int x = minX; x <= maxX; x++) {
                int pixelIndex = (y * imageWidth) + x;

                // The pixel is part of the current scanline.
                if (x >= leftLineX && x <= rightLineX) {
                    // Calculate (& write to the image) the interpolated pixel shading color.
                    int pixelColor = calculatePixelColor(tempInterpolatedColor, leftLineColor, rightLineColor, leftLineX, rightLineX, x);
                    rawTargetImage[pixelIndex] = pixelColor;
                    seenPixelPos.setBit(pixelIndex, true);

                    // Test if the pixel is part of the current scanline, but not the previous one. (It needs expansion shading!)
                    if (y > 0) {
                        int lastLinePixelIndex = ((y - 1) * imageWidth) + x;
                        if (!seenPixelPos.getBit(lastLinePixelIndex) && firstLayerPixelShadePositions.setBit(lastLinePixelIndex, true)) {
                            pixelPosBuffer.add(lastLinePixelIndex);
                            pixelPosBuffer.add(1); // Different IDs are used for debugging / troubleshooting.
                        }
                    }
                } else {
                    // The pixel was part of the previous scanline, but not the current one. (It needs expansion shading!)
                    if (!seenPixelPos.getBit(pixelIndex) && firstLayerPixelShadePositions.setBit(pixelIndex, true)) {
                        pixelPosBuffer.add(pixelIndex);
                        pixelPosBuffer.add(2);
                    }
                }
            }

            // Adds the pixel to the left & right of the line to the shading expansion buffer.
            if (rightLineX >= leftLineX) {
                if (leftLineX > 0) {
                    int pixelIndex = (y * imageWidth) + (leftLineX - 1);
                    if (!seenPixelPos.getBit(pixelIndex) && firstLayerPixelShadePositions.setBit(pixelIndex, true)) {
                        pixelPosBuffer.add(pixelIndex);
                        pixelPosBuffer.add(3);
                    }
                }
                if (rightLineX < targetImage.getWidth() - 1) {
                    int pixelIndex = (y * imageWidth) + (rightLineX + 1);
                    if (!seenPixelPos.getBit(pixelIndex) && firstLayerPixelShadePositions.setBit(pixelIndex, true)) {
                        pixelPosBuffer.add(pixelIndex);
                        pixelPosBuffer.add(4);
                    }
                }
            }

            lastLeftLineX = leftLineX;
            lastRightLineX = rightLineX;
        }

        // Mark the line under the final scanline for expansion shading.
        if (targetImage.getHeight() > maxRenderedTriangleY + 1) {
            for (int x = Math.max(0, lastLeftLineX); x <= lastRightLineX; x++) {
                int pixelIndex = ((maxRenderedTriangleY + 1) * imageWidth) + x;
                if (!seenPixelPos.getBit(pixelIndex) && firstLayerPixelShadePositions.setBit(pixelIndex, true)) {
                    pixelPosBuffer.add(pixelIndex);
                    pixelPosBuffer.add(5);
                }
            }
        }
    }

    private static int calculatePixelColor(CVector temp, CVector leftLineColor, CVector rightLineColor, int leftLineX, int rightLineX, int x) {
        // Calculate interpolation factor.
        float xLerpFactor = .5F;
        if (leftLineX != rightLineX)
            xLerpFactor = ((float) (x - leftLineX)) / (rightLineX - leftLineX);

        // Apply shading to pixel in scanline.
        return interpolateCVector(leftLineColor, rightLineColor, xLerpFactor, temp).toARGB();
    }

    /**
     * Expands the gouraud shading to other parts of the image using flood fill.
     * This assumes the pixel position buffer has been filled with the edges of the polygon as part of the shadeTriangle() function.
     * @param targetImage The image to draw the shaded image onto.
     * @param instance The shader instance to get the data from.
     * @param maxLayers the maximum number of pixels to write
     */
    private static void expandShading(BufferedImage targetImage, PSXTextureShader instance, int maxLayers) {
        // 1) Clear the pixel seen flags from the expansion pixels, since them being set will cause the shading to load from unset pixels.
        FXIntArray pixelPosBuffer = instance.getPixelPosBuffer();
        IndexBitArray seenPixelPos = instance.getPixelPosSeen();

        // 2) Calculate the colors for the pixel positions in the buffer.
        int[] rawTargetImage = ImageWorkHorse.getPixelIntegerArray(targetImage);
        for (int i = 0; i < pixelPosBuffer.size(); i += 2) {
            int pixelPos = pixelPosBuffer.get(i);
            if (seenPixelPos.getBit(pixelPos)) {
                pixelPosBuffer.set(i, -1); // Skip this, the image actually did have a pixel get placed here.
                continue;
            }

            CVector pixelShadingColor = tryLoadColor(targetImage, rawTargetImage, instance, pixelPos);
            pixelPosBuffer.set(i + 1, pixelShadingColor != null ? pixelShadingColor.toARGB() : UNSHADED_COLOR_ARGB);
        }

        // 3) Flood-fill the shading layers.
        int colorStartIndex = 0;
        for (int i = 0; i < maxLayers; i++)
            colorStartIndex = floodFillLayer(targetImage, rawTargetImage, instance, colorStartIndex, i >= maxLayers - 1);
    }

    private static int floodFillLayer(BufferedImage targetImage, int[] rawTargetImage, PSXTextureShader instance, int colorStartIndex, boolean lastLayer) {
        FXIntArray pixelPosBuffer = instance.getPixelPosBuffer();
        IndexBitArray seenPixelPos = instance.getPixelPosSeen();
        int imageWidth = targetImage.getWidth();
        int imageHeight = targetImage.getHeight();

        // 1) Fill the colors for the current layer.
        int nextColorStartIndex = pixelPosBuffer.size();
        for (int i = colorStartIndex; i < nextColorStartIndex; i += 2) {
            int currentPixelPos = pixelPosBuffer.get(i);
            int currentColor = pixelPosBuffer.get(i + 1);
            if (currentPixelPos >= 0)
                rawTargetImage[currentPixelPos] = currentColor;
        }

        // 2) Fill the queue for the next layer.
        if (!lastLayer) {
            for (int i = colorStartIndex; i < nextColorStartIndex; i += 2) {
                int currentPixelPos = pixelPosBuffer.get(i);
                if (currentPixelPos < 0)
                    continue;

                int x = currentPixelPos % imageWidth;
                int y = currentPixelPos / imageWidth;

                // Attempt to load color from the left pixel.
                if (x > 0) {
                    int leftPixel = (y * imageWidth) + (x - 1);

                    if (!seenPixelPos.getBit(leftPixel)) { // The nearby pixel needs shading color data.
                        CVector color = tryLoadColor(targetImage, rawTargetImage, instance, leftPixel);
                        if (color != null) {
                            seenPixelPos.setBit(leftPixel, true);
                            pixelPosBuffer.add(leftPixel);
                            pixelPosBuffer.add(color.toARGB());
                        }
                    }
                }

                // Attempt to load color from the right pixel.
                if (x < imageWidth - 1) {
                    int rightPixel = (y * imageWidth) + (x + 1);

                    if (!seenPixelPos.getBit(rightPixel)) { // The nearby pixel needs shading color data.
                        CVector color = tryLoadColor(targetImage, rawTargetImage, instance, rightPixel);
                        if (color != null) {
                            seenPixelPos.setBit(rightPixel, true);
                            pixelPosBuffer.add(rightPixel);
                            pixelPosBuffer.add(color.toARGB());
                        }
                    }
                }

                // Attempt to load color from the upper pixel.
                if (y > 0) {
                    int upperPixel = ((y - 1) * imageWidth) + x;

                    if (!seenPixelPos.getBit(upperPixel)) { // The nearby pixel needs shading color data.
                        CVector color = tryLoadColor(targetImage, rawTargetImage, instance, upperPixel);
                        if (color != null) {
                            seenPixelPos.setBit(upperPixel, true);
                            pixelPosBuffer.add(upperPixel);
                            pixelPosBuffer.add(color.toARGB());
                        }
                    }
                }

                // Attempt to load color from the lower pixel.
                if (y < imageHeight - 1) {
                    int lowerPixel = ((y + 1) * imageWidth) + x;

                    if (!seenPixelPos.getBit(lowerPixel)) { // The nearby pixel needs shading color data.
                        CVector color = tryLoadColor(targetImage, rawTargetImage, instance, lowerPixel);
                        if (color != null) {
                            seenPixelPos.setBit(lowerPixel, true);
                            pixelPosBuffer.add(lowerPixel);
                            pixelPosBuffer.add(color.toARGB());
                        }
                    }
                }
            }
        }

        // Done.
        return nextColorStartIndex;
    }

    private static CVector tryLoadColor(BufferedImage targetImage, int[] rawTargetImage, PSXTextureShader instance, int currentPixelPos) {
        IndexBitArray seenPixelPos = instance.getPixelPosSeen();
        CVector color1 = instance.getTempColorVector1();
        CVector color2 = instance.getTempColorVector2();
        int imageWidth = targetImage.getWidth();
        int x = currentPixelPos % imageWidth;
        int y = currentPixelPos / imageWidth;
        boolean loadedAnyColorsYet = false;

        // Attempt to load color from the left pixel.
        if (x > 0) {
            int leftPixel = (y * imageWidth) + (x - 1);

            if (seenPixelPos.getBit(leftPixel)) { // The nearby pixel has shading color data.
                int leftPixelColor = rawTargetImage[leftPixel];
                if (leftPixelColor != 0) {
                    color1.fromARGB(leftPixelColor);
                    loadedAnyColorsYet = true;
                }
            }
        }

        // Attempt to load color from the right pixel.
        if (x < imageWidth - 1) {
            int rightPixel = (y * imageWidth) + (x + 1);

            if (seenPixelPos.getBit(rightPixel)) { // The nearby pixel has shading color data.
                int rightPixelColor = rawTargetImage[rightPixel];
                if (rightPixelColor != 0) {
                    if (loadedAnyColorsYet) {
                        color2.fromARGB(rightPixelColor);
                        color1 = interpolateCVector(color1, color2, .5F, color1);
                    } else {
                        color1.fromARGB(rightPixelColor);
                        loadedAnyColorsYet = true;
                    }
                }
            }
        }

        // Attempt to load color from the above pixel.
        if (y > 0) {
            int upperPixel = ((y - 1) * imageWidth) + x;

            if (seenPixelPos.getBit(upperPixel)) { // The nearby pixel has shading color data.
                int upperPixelColor = rawTargetImage[upperPixel];
                if (upperPixelColor != 0) {
                    if (loadedAnyColorsYet) {
                        color2.fromARGB(upperPixelColor);
                        color1 = interpolateCVector(color1, color2, .5F, color1);
                    } else {
                        color1.fromARGB(upperPixelColor);
                        loadedAnyColorsYet = true;
                    }
                }
            }
        }

        // Attempt to load color from the lower pixel.
        if (y < targetImage.getHeight() - 1) {
            int lowerPixel = ((y + 1) * imageWidth) + x;

            if (seenPixelPos.getBit(lowerPixel)) { // The nearby pixel has shading color data.
                int lowerPixelColor = rawTargetImage[lowerPixel];
                if (lowerPixelColor != 0) {
                    if (loadedAnyColorsYet) {
                        color2.fromARGB(lowerPixelColor);
                        color1 = interpolateCVector(color1, color2, .5F, color1);
                    } else {
                        color1.fromARGB(lowerPixelColor);
                        loadedAnyColorsYet = true;
                    }
                }
            }
        }

        return loadedAnyColorsYet ? color1 : null;
    }

    private static void shadeRawPixel(int[] sourceImage, int[] targetImage, int pixelIndex, int shadeColor) {
        // This function is optimized for performance, since this is performance critical.
        // The big performance killer here was .setRGB(), with the runner up being the color functions in the Utils class.
        // Putting the bit manipulation here seemed to make a huge difference, which is why it was implemented here.
        if (sourceImage != null) {
            int textureColor = sourceImage[pixelIndex];
            byte alpha = (byte) ((textureColor >> 24) & 0xFF);
            int oldRed = ((textureColor >> 16) & 0xFF);
            int oldGreen = ((textureColor >> 8) & 0xFF);
            int oldBlue = (textureColor & 0xFF);

            // If the value exceeds the max, clamp it to the max.
            // It's not explicitly mentioned what happens if it goes above 255, but I think clamping it works.
            // I think "the results can't exceed the maximum brightness, i.e. the 5bit values written to the frame-buffer are saturated to max 1F" means it's clamped, but I'm not sure.
            // Reference: https://psx-spx.consoledev.net/graphicsprocessingunitgpu/
            int shadeRed = (shadeColor >> 16) & 0xFF;
            int shadeGreen = (shadeColor >> 8) & 0xFF;
            int shadeBlue = shadeColor & 0xFF;
            byte newRed = (byte) Math.min(255, (shadeRed / 128D) * oldRed);
            byte newGreen = (byte) Math.min(255, (shadeGreen / 128D) * oldGreen);
            byte newBlue = (byte) Math.min(255, (shadeBlue / 128D) * oldBlue);
            targetImage[pixelIndex] = Utils.toARGB(newRed, newGreen, newBlue, alpha);
        } else {
            targetImage[pixelIndex] = shadeColor;
        }
    }

    private static void applyShadingToSourceImage(BufferedImage sourceImage, BufferedImage targetImage) {
        if (sourceImage == null)
            return;
        if (targetImage == null)
            throw new NullPointerException("targetImage");

        // This has been optimized for performance, since it has been deemed performance critical code.
        int[] rawSourceImage = ImageWorkHorse.getPixelIntegerArray(sourceImage);
        int[] rawTargetImage = ImageWorkHorse.getPixelIntegerArray(targetImage);
        int pixelCount = targetImage.getWidth() * targetImage.getHeight();
        for (int i = 0; i < pixelCount; i++)
            shadeRawPixel(rawSourceImage, rawTargetImage, i, rawTargetImage[i]);
    }

    /**
     * Makes a flat shaded, untextured image (POLY_F3 / POLY_F4)
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param color  The color to apply to the image. (8 bits, 0 - 255)
     * @return flatShadedImage
     */
    public static BufferedImage makeFlatShadedImage(int width, int height, CVector color) {
        return makeFlatShadedImage(null, width, height, color);
    }

    /**
     * Makes a flat shaded, untextured image (POLY_F3 / POLY_F4)
     * @param targetImage The image to write the shaded data to. If null, a new one will be created.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param color  The color to apply to the image. (8 bits, 0 - 255)
     * @return flatShadedImage
     */
    public static BufferedImage makeFlatShadedImage(BufferedImage targetImage, int width, int height, CVector color) {
        if (targetImage == null)
            targetImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = targetImage.createGraphics();
        graphics.setColor(color.toColor());
        graphics.fillRect(0, 0, targetImage.getWidth(), targetImage.getHeight());
        graphics.dispose();

        return targetImage;
    }
    /**
     * Makes a textured flat shaded image (POLY_FT3 / POLY_FT4).
     * @param originalTexture The original texture to apply shading to.
     * @param color           The shading color to apply.
     * @return flatTextureShadedImage
     */
    public static BufferedImage makeTexturedFlatShadedImage(BufferedImage originalTexture, CVector color) {
        return makeTexturedFlatShadedImage(originalTexture, null, color);
    }

    /**
     * Makes a textured flat shaded image (POLY_FT3 / POLY_FT4).
     * @param originalTexture The original texture to apply shading to.
     * @param targetImage The image to write the shaded data to. If null, a new one will be created.
     * @param color           The shading color to apply.
     * @return flatTextureShadedImage
     */
    public static BufferedImage makeTexturedFlatShadedImage(BufferedImage originalTexture, BufferedImage targetImage, CVector color) {
        int colorArgb = color.toARGB();
        if (colorArgb == UNSHADED_COLOR_ARGB)
            return originalTexture;

        if (targetImage == null)
            targetImage = new BufferedImage(originalTexture.getWidth(), originalTexture.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] rawSourceImage = ImageWorkHorse.getPixelIntegerArray(originalTexture);
        int[] rawTargetImage = ImageWorkHorse.getPixelIntegerArray(targetImage);
        int pixelCount = targetImage.getWidth() * targetImage.getHeight();
        for (int i = 0; i < pixelCount; i++)
            shadeRawPixel(rawSourceImage, rawTargetImage, i, colorArgb);

        return targetImage;
    }

    /**
     * Interpolate two color vectors.
     * @param a      The first color vector.
     * @param b      The second color vector.
     * @param t      A value between 0 and 1 representing how much of each vector to include.
     * @param result The resulting vector.
     * @return interpolatedColorVector
     */
    public static CVector interpolateCVector(CVector a, CVector b, float t, CVector result) {
        // Clamp the value.
        if (t < 0)
            t = 0;
        if (t > 1)
            t = 1;

        if (result == null)
            result = new CVector();

        result.setRed((byte) ((((a.getRed() & 0xFF) * (1 - t)) + ((b.getRed() & 0xFF) * t))));
        result.setGreen((byte) ((((a.getGreen() & 0xFF) * (1 - t)) + ((b.getGreen() & 0xFF) * t))));
        result.setBlue((byte) ((((a.getBlue() & 0xFF) * (1 - t)) + ((b.getBlue() & 0xFF) * t))));
        return result;
    }
}