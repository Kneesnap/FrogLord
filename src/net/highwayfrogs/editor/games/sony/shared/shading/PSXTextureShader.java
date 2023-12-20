package net.highwayfrogs.editor.games.sony.shared.shading;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * JavaFX does not give us anywhere near enough control over the 3D rendering pipeline to properly recreate the gouraud shading seen on the PlayStation.
 * This shading is very important to making the worlds render accurately too, since gouraud shading was used extensively to color Frogger maps.
 * While new FrogLord will be using a custom-built 3D engine capable of gouraud shading, a temporary solution is desired.
 * This solution is to create a new texture and apply gouraud shading to that texture. This is extremely resource inefficient, but it was pretty much the only solution before new FrogLord is ready.
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
    }

    /**
     * Gets the instance of the PSXTextureShader available to this thread.
     */
    private static PSXTextureShader getInstance() {
        return TEXTURE_SHADER_THREAD.get();
    }

    /**
     * Makes a gouraud shaded, untextured image (POLY_G3 / POLY_G4)
     * @param colors The colors to apply to the image. (8 bits, 0 - 255)
     * @return gouraudShadedImage
     */
    public static BufferedImage makeGouraudShadedImage(CVector[] colors) {
        return makeGouraudShadedImage(PSXShadeTextureDefinition.UNTEXTURED_IMAGE_GOURAUD_DIMENSIONS, PSXShadeTextureDefinition.UNTEXTURED_IMAGE_GOURAUD_DIMENSIONS, colors);
    }

    /**
     * Makes a gouraud shaded, untextured image (POLY_G3 / POLY_G4)
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param colors The colors to apply to the image. (8 bits, 0 - 255)
     * @return gouraudShadedImage
     */
    public static BufferedImage makeGouraudShadedImage(int width, int height, CVector[] colors) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int startX = PSXShadeTextureDefinition.UNTEXTURED_IMAGE_PADDING_DIMENSIONS;
        int startY = PSXShadeTextureDefinition.UNTEXTURED_IMAGE_PADDING_DIMENSIONS;
        int endX = width - PSXShadeTextureDefinition.UNTEXTURED_IMAGE_PADDING_DIMENSIONS - 1;
        int endY = height - PSXShadeTextureDefinition.UNTEXTURED_IMAGE_PADDING_DIMENSIONS - 1;

        PSXTextureShader instance = getInstance();
        TextureCoordinate[] coordinates = instance.getTriangleCoordinates();
        if (colors.length == 3) {
            coordinates[0].setXY(startX, startY);
            coordinates[1].setXY(startX, endY);
            coordinates[2].setXY(endX, startY);
            shadeTriangle(null, image, colors, coordinates, true, true);
        } else if (colors.length == 4) {
            CVector[] triangleColors = instance.getTriangleColors();

            // Left triangle.
            triangleColors[0].copyFrom(colors[0]);
            triangleColors[1].copyFrom(colors[1]);
            triangleColors[2].copyFrom(colors[2]);
            coordinates[0].setXY(startX, endY);
            coordinates[1].setXY(endX, endY);
            coordinates[2].setXY(startX, startY);
            shadeTriangle(null, image, triangleColors, coordinates, true, false);

            // Right triangle.
            triangleColors[0].copyFrom(colors[3]);
            triangleColors[1].copyFrom(colors[2]);
            triangleColors[2].copyFrom(colors[1]);
            coordinates[0].setXY(endX, startY);
            coordinates[1].setXY(startX, startY);
            coordinates[2].setXY(endX, endY);
            shadeTriangle(null, image, triangleColors, coordinates, false, true);
        } else {
            throw new RuntimeException("Can't create gouraud shaded image with " + colors.length + " colors.");
        }

        return image;
    }

    /**
     * Makes a gouraud shaded, textured image (POLY_GT3 / POLY_GT4)
     * @param originalImage The original image to create this one from.
     * @param colors        The colors to apply to the image. (8 bits, 0 - 255)
     * @param textureUvs    The texture uvs to use as the corner of the triangles.
     * @return gouraudShadedImage
     */
    public static BufferedImage makeTexturedGouraudShadedImage(BufferedImage originalImage, CVector[] colors, SCByteTextureUV[] textureUvs) {
        BufferedImage image = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int paddedWidth = originalImage.getWidth();
        int paddedHeight = originalImage.getHeight();

        int startX = PSXShadeTextureDefinition.UNTEXTURED_IMAGE_PADDING_DIMENSIONS;
        int startY = PSXShadeTextureDefinition.UNTEXTURED_IMAGE_PADDING_DIMENSIONS;
        int endX = paddedWidth - PSXShadeTextureDefinition.UNTEXTURED_IMAGE_PADDING_DIMENSIONS - 1;
        int endY = paddedHeight - PSXShadeTextureDefinition.UNTEXTURED_IMAGE_PADDING_DIMENSIONS - 1;

        PSXTextureShader instance = getInstance();
        TextureCoordinate[] coordinates = instance.getTriangleCoordinates();
        if (colors.length == 3) {
            // TODO: Make coordinates based on UVs.
            // TODO: Ensure out of bounds filling works good.
            coordinates[0].setXY(startX, startY);
            coordinates[1].setXY(startX, endY);
            coordinates[2].setXY(endX, startY);
            shadeTriangle(originalImage, image, colors, coordinates, true, true);
        } else if (colors.length == 4) {
            // TODO: Make coordinates based on UVs.
            // TODO: Ensure out of bounds filling works good.
            CVector[] triangleColors = instance.getTriangleColors();

            // Left triangle.
            triangleColors[0].copyFrom(colors[0]);
            triangleColors[1].copyFrom(colors[1]);
            triangleColors[2].copyFrom(colors[2]);
            coordinates[0].setXY(startX, endY);
            coordinates[1].setXY(endX, endY);
            coordinates[2].setXY(startX, startY);
            shadeTriangle(originalImage, image, triangleColors, coordinates, true, false);

            // Right triangle.
            triangleColors[0].copyFrom(colors[3]);
            triangleColors[1].copyFrom(colors[2]);
            triangleColors[2].copyFrom(colors[1]);
            coordinates[0].setXY(endX, startY);
            coordinates[1].setXY(startX, startY);
            coordinates[2].setXY(endX, endY);
            shadeTriangle(originalImage, image, triangleColors, coordinates, false, true);
        } else {
            throw new RuntimeException("Can't create gouraud shaded image with " + colors.length + " colors.");
        }

        return image;
    }

    /**
     * Applies gouraud shading to an image.
     * @param sourceImage          The texture to apply shading to, if one exists.
     * @param targetImage          The image to draw the shaded image onto.
     * @param colors               The colors of each triangle vertex.
     * @param vertices             The position of each triangle vertex.
     * @param fillOutOfBoundsLeft  If the area out of bounds to the left of the triangle should be shaded.
     * @param fillOutOfBoundsRight If the area out of bounds to the right of the triangle should be shaded.
     */
    public static void shadeTriangle(BufferedImage sourceImage, BufferedImage targetImage, CVector[] colors, TextureCoordinate[] vertices, boolean fillOutOfBoundsLeft, boolean fillOutOfBoundsRight) {
        if (sourceImage != null && (sourceImage.getWidth() != targetImage.getWidth() || sourceImage.getHeight() != targetImage.getHeight()))
            throw new RuntimeException("The source image had dimensions of " + sourceImage.getWidth() + "x" + sourceImage.getHeight() + ", but the target image was " + targetImage.getWidth() + "x" + targetImage.getHeight() + ".");

        // BufferedImage has an origin at the top left corner. (Ie: X = 0, Y = 0 is the top left corner)
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
        PSXTextureShader instance = getInstance();

        // Draw the scan-lines.
        for (int y = minTriangleY; y <= maxTriangleY; y++) {
            CVector leftLineColor;
            int leftLineX;
            if (y < leftPos.getY()) { // Interpolate between top vertex and left vertex.
                int yOffset = (y - minTriangleY);

                leftLineColor = interpolateCVector(colors[topIndex], colors[leftIndex], (float) yOffset / topToLeftHeight, instance.getTempColorVector1());
                leftLineX = topPos.getX() + Math.round(inverseLeftSlope * yOffset); // TODO: There's an inaccuracy causing it to try writing outside the size of the image. (It may or may not impact this line)
            } else if (y > leftPos.getY()) { // Interpolate between left vertex and right vertex.
                int yOffset = (y - leftPos.getY());
                leftLineColor = interpolateCVector(colors[leftIndex], colors[rightIndex], (float) yOffset / leftToRightHeight, instance.getTempColorVector1());
                leftLineX = leftPos.getX() + Math.round(inverseLeftRightSlope * yOffset); // TODO: There's an inaccuracy causing it to try writing outside the size of the image. (It may or may not impact this line)
            } else { // Use data directly from the vertex since it's on this scanline.
                leftLineColor = colors[leftIndex];
                leftLineX = leftPos.getX();
            }

            CVector rightLineColor;
            int rightLineX;
            if (y < rightPos.getY()) { // Interpolate between top vertex and right vertex.
                int yOffset = (y - minTriangleY);
                rightLineColor = interpolateCVector(colors[topIndex], colors[rightIndex], (float) yOffset / topToRightHeight, instance.getTempColorVector2());
                rightLineX = topPos.getX() + Math.round(inverseRightSlope * yOffset); // TODO: There's an inaccuracy causing it to try writing outside the size of the image. (It may or may not impact this line)
            } else if (y > rightPos.getY()) { // Interpolate between right vertex and left vertex.
                int yOffset = (y - rightPos.getY());
                rightLineColor = interpolateCVector(colors[rightIndex], colors[leftIndex], (float) yOffset / rightToLeftHeight, instance.getTempColorVector2());
                rightLineX = rightPos.getX() - Math.round(inverseLeftRightSlope * yOffset); // TODO: There's an inaccuracy causing it to try writing outside the size of the image.
            } else { // Use data directly from the vertex since it's on this scanline.
                rightLineColor = colors[rightIndex];
                rightLineX = rightPos.getX();
            }

            // TODO: Temporary fix to the out of bounds writing.
            if (leftLineX < 0)
                leftLineX = 0;
            if (rightLineX >= targetImage.getWidth())
                rightLineX = targetImage.getWidth() - 1;

            // Fill a scanline.
            for (int x = leftLineX; x <= rightLineX; x++) {
                float xLerpFactor = .5F;
                if (leftLineX != rightLineX)
                    xLerpFactor = Math.max(0, Math.min(1, ((float) (x - leftLineX)) / (rightLineX - leftLineX)));

                // Write texture.
                CVector pixelColor = interpolateCVector(leftLineColor, rightLineColor, xLerpFactor, instance.getTempColorVector3());
                shadePixel(sourceImage, targetImage, x, y, pixelColor);
            }

            // Fill out of bounds colors to the left.
            if (fillOutOfBoundsLeft)
                for (int x = 0; x < leftLineX; x++)
                    shadePixel(sourceImage, targetImage, x, y, leftLineColor);

            // Fill out of bounds colors to the right.
            if (fillOutOfBoundsRight)
                for (int x = rightLineX + 1; x < targetImage.getWidth(); x++)
                    shadePixel(sourceImage, targetImage, x, y, rightLineColor);
        }

        // Step 3) Fill in padding pixels.
        // Fill in any padding pixels by extending the pixels with color.
        if (fillOutOfBoundsLeft) {
            for (int x = 0; x < targetImage.getWidth(); x++) {
                for (int y = targetImage.getHeight() - 1; y >= 0; y--) {
                    int pixelColor = targetImage.getRGB(x, y);
                    if (pixelColor == 0)
                        continue; // Skip untouched pixels.

                    // Copy the pixels.
                    while (targetImage.getHeight() > ++y)
                        targetImage.setRGB(x, y, pixelColor);

                    break;
                }
            }
        }

        if (fillOutOfBoundsRight) {
            for (int x = 0; x < targetImage.getWidth(); x++) {
                for (int y = 0; y < targetImage.getHeight(); y++) {
                    int pixelColor = targetImage.getRGB(x, y);
                    if (pixelColor == 0)
                        continue; // Skip untouched pixels.

                    // Copy the pixels.
                    while (y-- > 0)
                        targetImage.setRGB(x, y, pixelColor);

                    break;
                }
            }
        }
    }

    private static void shadePixel(BufferedImage sourceImage, BufferedImage targetImage, int x, int y, CVector shadeColor) {
        if (sourceImage != null) {
            int textureColor = sourceImage.getRGB(x, y);
            byte alpha = Utils.getAlpha(textureColor);
            // If the value exceeds the max, clamp it to the max. Or at least that's what https://psx-spx.consoledev.net/graphicsprocessingunitgpu/ says.
            short red = (short) Math.min(255, (((double) shadeColor.getRedShort() / 127D) * Utils.getRedInt(textureColor)));
            short green = (short) Math.min(255, (((double) shadeColor.getGreenShort() / 127D) * Utils.getGreenInt(textureColor)));
            short blue = (short) Math.min(255, (((double) shadeColor.getBlueShort() / 127D) * Utils.getBlueInt(textureColor)));
            targetImage.setRGB(x, y, Utils.toARGB(Utils.unsignedShortToByte(red), Utils.unsignedShortToByte(green), Utils.unsignedShortToByte(blue), alpha));
        } else {
            targetImage.setRGB(x, y, shadeColor.toARGB());
        }
    }

    /**
     * Makes a flat shaded, untextured image (POLY_F3 / POLY_F4)
     * @param color The color to apply to the image. (8 bits, 0 - 255)
     * @return flatShadedImage
     */
    public static BufferedImage makeFlatShadedImage(CVector color) {
        return makeFlatShadedImage(PSXShadeTextureDefinition.UNTEXTURED_IMAGE_FLAT_DIMENSIONS, PSXShadeTextureDefinition.UNTEXTURED_IMAGE_FLAT_DIMENSIONS, color);
    }

    /**
     * Makes a flat shaded, untextured image (POLY_F3 / POLY_F4)
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param color  The color to apply to the image. (8 bits, 0 - 255)
     * @return flatShadedImage
     */
    public static BufferedImage makeFlatShadedImage(int width, int height, CVector color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color.toColor());
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        return image;
    }

    /**
     * Makes a textured flat shaded image (POLY_FT3 / POLY_FT4).
     * @param originalTexture The original texture to apply shading to.
     * @param color           The shading color to apply. (7 bit color range, 0 - 127, NOT 256)
     * @return flatTextureShadedImage
     */
    public static BufferedImage makeTexturedFlatShadedImage(BufferedImage originalTexture, CVector color) {
        BufferedImage newImage = new BufferedImage(originalTexture.getWidth(), originalTexture.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < newImage.getWidth(); x++) {
            for (int y = 0; y < newImage.getHeight(); y++) {
                int textureColor = originalTexture.getRGB(x, y);
                byte alpha = Utils.getAlpha(textureColor);
                // If the value exceeds the max, clamp it to the max. Or at least that's what https://psx-spx.consoledev.net/graphicsprocessingunitgpu/ says should happen for modulation.
                short red = (short) Math.min(255, (((double) color.getRedShort() / 127D) * Utils.getRedInt(textureColor)));
                short green = (short) Math.min(255, (((double) color.getGreenShort() / 127D) * Utils.getGreenInt(textureColor)));
                short blue = (short) Math.min(255, (((double) color.getBlueShort() / 127D) * Utils.getBlueInt(textureColor)));
                newImage.setRGB(x, y, Utils.toARGB(Utils.unsignedShortToByte(red), Utils.unsignedShortToByte(green), Utils.unsignedShortToByte(blue), alpha));
            }
        }

        return newImage;
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
        if (result == null)
            result = new CVector();

        result.setRed(Utils.unsignedShortToByte((short) ((a.getRedShort() * (1 - t)) + (b.getRedShort() * t))));
        result.setGreen(Utils.unsignedShortToByte((short) ((a.getGreenShort() * (1 - t)) + (b.getGreenShort() * t))));
        result.setBlue(Utils.unsignedShortToByte((short) ((a.getBlueShort() * (1 - t)) + (b.getBlueShort() * t))));
        return result;
    }
}