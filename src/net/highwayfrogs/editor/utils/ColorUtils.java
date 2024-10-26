package net.highwayfrogs.editor.utils;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides utilities relating to color.
 * Created by Kneesnap on 10/25/2024.
 */
public class ColorUtils {
    private static final Map<Color, Image> colorImageCacheMap = new HashMap<>();

    /**
     * Get the alpha value from an ARGB8888 value.
     * @param argb The int value to get the color from.
     * @return colorByte
     */
    public static byte getAlpha(int argb) {
        return (byte) ((argb >> 24) & 0xFF);
    }

    /**
     * Get the alpha value from an ARGB8888 value.
     * @param argb The int value to get the color from.
     * @return colorValue
     */
    public static int getAlphaInt(int argb) {
        return ((argb >> 24) & 0xFF);
    }

    /**
     * Get the red value from an ARGB8888 value.
     * @param rgb The int value to get the color from.
     * @return colorByte
     */
    public static byte getRed(int rgb) {
        return (byte) ((rgb >> 16) & 0xFF);
    }

    /**
     * Get the red value from an ARGB8888 value.
     * @param rgb The int value to get the color from.
     * @return colorValue
     */
    public static int getRedInt(int rgb) {
        return ((rgb >> 16) & 0xFF);
    }

    /**
     * Get the green value from an ARGB8888 value.
     * @param rgb The int value to get the color from.
     * @return colorByte
     */
    public static byte getGreen(int rgb) {
        return (byte) ((rgb >> 8) & 0xFF);
    }

    /**
     * Get the green value from an ARGB8888 value.
     * @param rgb The int value to get the color from.
     * @return colorValue
     */
    public static int getGreenInt(int rgb) {
        return ((rgb >> 8) & 0xFF);
    }

    /**
     * Get the blue value from an ARGB8888 value.
     * @param rgb The int value to get the color from.
     * @return colorByte
     */
    public static byte getBlue(int rgb) {
        return (byte) (rgb & 0xFF);
    }

    /**
     * Get the blue value from an ARGB8888 value.
     * @param rgb The int value to get the color from.
     * @return colorValue
     */
    public static int getBlueInt(int rgb) {
        return (rgb & 0xFF);
    }

    /**
     * Get a Color object from an ARGB8888 value.
     * @param rgb The integer to get the color from.
     * @return color
     */
    public static Color fromRGB(int rgb) {
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Get a Color object from an ARGB8888 value.
     * @param rgb The integer to get the color from.
     * @return color
     */
    public static Color fromRGB(int rgb, double alpha) {
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
    }

    /**
     * Get a Color object from an ARGB8888 value.
     * @param rgb The integer to get the color from.
     * @return color
     */
    public static Color fromBGR(int rgb) {
        return Color.rgb(rgb & 0xFF, (rgb >> 8) & 0xFF, (rgb >> 16) & 0xFF);
    }

    /**
     * Get an ARGB8888 integer from a color object.
     * @param color The color to turn into rgb.
     * @return rgbInt
     */
    public static int toRGB(Color color) {
        int result = (int) (color.getRed() * 0xFF);
        result = (result << 8) + (int) (color.getGreen() * 0xFF);
        result = (result << 8) + (int) (color.getBlue() * 0xFF);
        return result;
    }

    /**
     * Swaps the red/blue value in a 32-bit ARGB8888/ABGR8888 integer.
     * @param color The color to swap components for.
     * @return swappedColor
     */
    public static int swapRedBlue(int color) {
        int oldBlue = (color & 0xFF);
        int oldRed = ((color >> 16) & 0xFF);

        int result = color & 0xFF00FF00;
        result |= oldRed;
        result |= ((oldBlue << 16) & 0xFF0000);
        return result;
    }

    /**
     * Get an ARGB8888 integer from a color object.
     * @param color The color to turn into rgb.
     * @return rgbInt
     */
    public static int toARGB(Color color) {
        return ((int) (color.getOpacity() * 255) << 24) | toRGB(color);
    }

    /**
     * Get an integer from color bytes.
     * @return rgbInt
     */
    public static int toRGB(byte red, byte green, byte blue) {
        return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Get an integer from color bytes.
     * @return rgbInt
     */
    public static int toARGB(byte red, byte green, byte blue, byte alpha) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Get an integer from a color object.
     * @param color The color to turn into bgr.
     * @return rgbInt
     */
    public static int toBGR(Color color) {
        int result = (int) (color.getBlue() * 0xFF);
        result = (result << 8) + (int) (color.getGreen() * 0xFF);
        result = (result << 8) + (int) (color.getRed() * 0xFF);
        return result;
    }

    /**
     * Get an integer from color bytes.
     * @return rgbInt
     */
    public static int toABGR(byte red, byte green, byte blue, byte alpha) {
        return ((alpha & 0xFF) << 24) | ((blue & 0xFF) << 16) | ((green & 0xFF) << 8) | (red & 0xFF);
    }

    /**
     * Creates an image of a solid color.
     * @param color The color to make the image of.
     * @return colorImage
     */
    public static Image makeColorImage(Color color) {
        return colorImageCacheMap.computeIfAbsent(color, key -> {
            BufferedImage colorImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = colorImage.createGraphics();
            graphics.setColor(toAWTColor(key));
            graphics.fillRect(0, 0, colorImage.getWidth(), colorImage.getHeight());
            graphics.dispose();
            return FXUtils.toFXImage(colorImage, true);
        });
    }

    /**
     * Creates an image of a solid color.
     * @param color The color to make the image of.
     * @return colorImage
     */
    public static Image makeColorImageNoCache(Color color, int width, int height) {
        BufferedImage colorImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = colorImage.createGraphics();
        graphics.setColor(toAWTColor(color));
        graphics.fillRect(0, 0, colorImage.getWidth(), colorImage.getHeight());
        graphics.dispose();
        return FXUtils.toFXImage(colorImage, false);
    }

    /**
     * Convert a JavaFX color to an AWT color.
     * @param fxColor The fx color to convert.
     * @return awtColor
     */
    public static java.awt.Color toAWTColor(Color fxColor) {
        return toAWTColor(fxColor, (byte) (int) (fxColor.getOpacity() * 255));
    }

    /**
     * Convert a JavaFX color to an AWT color.
     * @param fxColor The fx color to convert.
     * @return awtColor
     */
    public static java.awt.Color toAWTColor(Color fxColor, byte alpha) {
        return new java.awt.Color((toRGB(fxColor) & 0xFFFFFF) | ((alpha & 0xFF) << 24), true);
    }

    /**
     * Calculate interpolated color value based on "t" between source and target colours.
     * @param colorSrc The source color.
     * @param colorTgt The target color.
     * @param t        The desired delta (from 0.0 to 1.0 inclusive).
     * @return Color
     */
    public static int calculateInterpolatedColourARGB(final java.awt.Color colorSrc, final java.awt.Color colorTgt, float t) {
        t = Math.min(t, 1.0f);
        t = Math.max(t, 0.0f);

        short red = (short) Math.min(255, colorSrc.getRed() + (int) ((colorTgt.getRed() - colorSrc.getRed()) * t));
        short green = (short) Math.min(255, colorSrc.getGreen() + (int) ((colorTgt.getGreen() - colorSrc.getGreen()) * t));
        short blue = (short) Math.min(255, colorSrc.getBlue() + (int) ((colorTgt.getBlue() - colorSrc.getBlue()) * t));
        short alpha = (short) Math.min(255, colorSrc.getAlpha() + (int) ((colorTgt.getAlpha() - colorSrc.getAlpha()) * t));

        return toARGB(DataUtils.unsignedShortToByte(red), DataUtils.unsignedShortToByte(green), DataUtils.unsignedShortToByte(blue), DataUtils.unsignedShortToByte(alpha));
    }
}
