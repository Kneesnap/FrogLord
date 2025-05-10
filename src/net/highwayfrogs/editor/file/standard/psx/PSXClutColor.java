package net.highwayfrogs.editor.file.standard.psx;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;

/**
 * Represents the CLUT format described on http://www.psxdev.net/forum/viewtopic.php?t=109.
 * TODO: It appears the colors stored in this file are flipped (red & blue are swapped), as evidenced by the toARGB() functions.
 * Created by Kneesnap on 8/30/2018.
 */
@Getter
public class PSXClutColor extends GameObject {
    @Setter private boolean stp; // stp -> "Semi Transparent" Flag. TODO: For new FrogLord, we should take the time to understand these things, instead of just saying "it looks like it works".
    private byte red;
    private byte green;
    private byte blue;

    private static final int BITS_PER_VALUE = 5;
    private static final int RED_OFFSET = 0;
    private static final int GREEN_OFFSET = RED_OFFSET + BITS_PER_VALUE;
    private static final int BLUE_OFFSET = GREEN_OFFSET + BITS_PER_VALUE;
    private static final int TO_FULL_BYTE = Constants.BITS_PER_BYTE - BITS_PER_VALUE;
    private static final int STP_FLAG = Constants.BIT_FLAG_15;
    public static final int BYTE_SIZE = Constants.SHORT_SIZE;

    // A note in the STP bit.
    // STP is "special transparency processing" and has "various different meanings".
    // Its behavior seems to only be relevant when "transparency processing" is enabled.
    // Reference: https://www.psxdev.net/forum/viewtopic.php?t=109
    // Reference: https://www.psxdev.net/forum/viewtopic.php?t=953
    /*

    The logic as to how the semi-transparency bit affects the pixel is as follows (full-black means all RGB components are 0):

    -- If Semi-Transparent mode is off --
    Full-black without STP bit = Transparent (alpha = 0)
    Full-black with STP bit = Opaque black (alpha = 255)
    Non full-black without STP bit = Solid color (alpha = 255)
    Non full-black with STP bit = Solid color (alpha = 255)

    -- If Semi-Transparent is on --
    Full-black without STP bit = Transparent (alpha = 0)
    Full-black with STP bit = Semi-transparent black (alpha = 127)
    Non full-black without STP bit = Solid color (alpha = 255)
    Non full-black with STP bit = Semi-transparent color (alpha = 127)
     */

    @Override
    public void load(DataReader reader) {
        short value = reader.readShort();
        this.blue = getByte(value, BLUE_OFFSET);
        this.green = getByte(value, GREEN_OFFSET);
        this.red = getByte(value, RED_OFFSET);
        this.stp = (value & STP_FLAG) == STP_FLAG;
    }

    private static byte getByte(short value, int byteOffset) {
        value >>= byteOffset;
        for (int i = BITS_PER_VALUE; i < Constants.BITS_PER_BYTE; i++)
            value &= ~(1 << i); // Disable bits 5-7, as bits 0-4 are the values we care about for this number.

        return (byte) value;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(toShort());
    }

    /**
     * Get this value as a short.
     * @return shortValue
     */
    public short toShort() {
        short writeValue = (short) (this.stp ? STP_FLAG : 0);
        writeValue |= (getBlue() << BLUE_OFFSET);
        writeValue |= (getGreen() << GREEN_OFFSET);
        writeValue |= (getRed() << RED_OFFSET);
        return writeValue;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PSXClutColor && ((PSXClutColor) other).toShort() == this.toShort();
    }

    @Override
    public int hashCode() {
        return toShort();
    }

    /**
     * Gets this color as an RGBA integer.
     * @return intValue
     */
    public int toRGBA() {
        byte[] arr = new byte[4];
        arr[0] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(getRed()) << TO_FULL_BYTE));
        arr[1] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(getGreen()) << TO_FULL_BYTE));
        arr[2] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(getBlue()) << TO_FULL_BYTE));
        arr[3] = (byte) (isStp() ? 0x01 : 0x00);
        return DataUtils.readNumberFromBytes(arr);
    }

    /**
     * Gets this color as an ARGB integer.
     * @param enableTransparency If the PSX would have "transparency processing" enabled on the draw primitive using this color.
     * @return intValue
     */
    public int toFullARGB(boolean enableTransparency) {
        byte rawRed = getByte(this.red, RED_OFFSET);
        byte rawGreen = getByte(this.green, GREEN_OFFSET);
        byte rawBlue = getByte(this.blue, BLUE_OFFSET);
        byte red = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawRed) << TO_FULL_BYTE));
        byte green = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawGreen) << TO_FULL_BYTE));
        byte blue = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawBlue) << TO_FULL_BYTE));

        boolean fullBlack = (red == 0) && (green == 0) && (blue == 0);
        return ColorUtils.toARGB(red, green, blue, getAlpha(fullBlack, this.stp, enableTransparency));
    }

    /**
     * Get this value as a BGRA integer.
     */
    public int toBGRA() {
        byte[] arr = new byte[4]; //BGRA
        arr[0] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(getBlue()) << TO_FULL_BYTE));
        arr[1] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(getGreen()) << TO_FULL_BYTE));
        arr[2] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(getRed()) << TO_FULL_BYTE));
        arr[3] = (byte) (isStp() ? 0x00 : 0xFF);
        return DataUtils.readNumberFromBytes(arr);
    }

    /**
     * Gets this color as an ABGR integer.
     * @param enableTransparency If the PSX would have "transparency processing" enabled on the draw primitive using this color.
     * @return intValue
     */
    public int toFullABGR(boolean enableTransparency) {
        byte rawRed = getByte(this.red, RED_OFFSET);
        byte rawGreen = getByte(this.green, GREEN_OFFSET);
        byte rawBlue = getByte(this.blue, BLUE_OFFSET);
        byte red = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawRed) << TO_FULL_BYTE));
        byte green = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawGreen) << TO_FULL_BYTE));
        byte blue = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawBlue) << TO_FULL_BYTE));

        boolean fullBlack = (red == 0) && (green == 0) && (blue == 0);
        return ColorUtils.toABGR(red, green, blue, getAlpha(fullBlack, this.stp, enableTransparency));
    }

    /**
     * Turn red, green, blue, alpha values (0 -> 255) into a PSX Clut color.
     * @param red   Red color value.
     * @param green Green color value.
     * @param blue  Blue color value.
     * @param alpha Alpha value.
     * @return clutColor
     */
    public static PSXClutColor fromRGBA(byte red, byte green, byte blue, byte alpha) {
        PSXClutColor color = new PSXClutColor();
        color.red = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(red) >> TO_FULL_BYTE));
        color.green = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(green) >> TO_FULL_BYTE));
        color.blue = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(blue) >> TO_FULL_BYTE));
        color.setStp(alpha != Constants.NULL_BYTE);
        return color;
    }

    /**
     * Turn an RGBA byte array into a PSXClutColor.
     * @param array The array to read RGBA bytes from.
     * @param index The index to read color data from.
     * @return clutColor
     */
    public static PSXClutColor fromRGBA(byte[] array, int index) {
        return fromRGBA(array[index + 3], array[index + 2], array[index + 1], array[index]);
    }

    /**
     * Turn an RGBA byte array into a PSXClutColor.
     * @param value The argbColor to read from.
     * @return clutColor
     */
    public static PSXClutColor fromARGB(int value, boolean enableTransparency) {
        PSXClutColor clutColor = new PSXClutColor();
        byte alpha = DataUtils.unsignedShortToByte((short) ((value >>> 27) & 0b11111));
        clutColor.red = DataUtils.unsignedShortToByte((short) ((value >>> 19) & 0b11111));
        clutColor.green = DataUtils.unsignedShortToByte((short) ((value >>> 11) & 0b11111));
        clutColor.blue = DataUtils.unsignedShortToByte((short) ((value >>> 3) & 0b11111));

        boolean fullBlack = (clutColor.red == 0) && (clutColor.green == 0) && (clutColor.blue == 0);
        clutColor.stp = getSTPBit(fullBlack, enableTransparency, alpha);
        return clutColor;
    }

    /**
     * Reads a PSXClutColor from a 16bit short into an RGBA int.
     * @param color The short to read from.
     * @return rgbaColor
     */
    public static int readBGRAColorFromShort(short color, boolean fullAlpha) {
        byte blue = getByte(color, BLUE_OFFSET);
        byte green = getByte(color, GREEN_OFFSET);
        byte red = getByte(color, RED_OFFSET);
        boolean stp = (color & STP_FLAG) == STP_FLAG;
        byte[] arr = new byte[4]; //RGBA
        arr[0] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(red) << TO_FULL_BYTE));
        arr[1] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(green) << TO_FULL_BYTE));
        arr[2] = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(blue) << TO_FULL_BYTE));
        arr[3] = (byte) (stp ? (fullAlpha ? 0xFF : 0x01) : 0x00);
        return DataUtils.readNumberFromBytes(arr);
    }

    /**
     * Reads a PSXClutColor from a 16bit short into an RGBA int.
     * @param color              The short to read from.
     * @param enableTransparency If the PSX would have "transparency processing" enabled on the draw primitive.
     * @return rgbaColor
     */
    public static int readARGBColorFromShort(short color, boolean enableTransparency) {
        byte rawRed = getByte(color, RED_OFFSET);
        byte rawGreen = getByte(color, GREEN_OFFSET);
        byte rawBlue = getByte(color, BLUE_OFFSET);
        byte red = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawRed) << TO_FULL_BYTE));
        byte green = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawGreen) << TO_FULL_BYTE));
        byte blue = DataUtils.unsignedShortToByte((short) (DataUtils.byteToUnsignedShort(rawBlue) << TO_FULL_BYTE));

        boolean stp = (color & STP_FLAG) == STP_FLAG;
        boolean fullBlack = (red == 0) && (green == 0) && (blue == 0);
        return ColorUtils.toARGB(red, green, blue, getAlpha(fullBlack, stp, enableTransparency));
    }

    private static byte getAlpha(boolean fullBlack, boolean stpBit, boolean enableTransparency) {
        /*
        -- If Semi-Transparent is on --
        Full-black without STP bit = Transparent (alpha = 0)
        Full-black with STP bit = Semi-transparent black (alpha = 127)
        Non full-black without STP bit = Solid color (alpha = 255)
        Non full-black with STP bit = Semi-transparent color (alpha = 127)

        -- If Semi-Transparent mode is off --
        Full-black without STP bit = Transparent (alpha = 0)
        Full-black with STP bit = Opaque black (alpha = 255)
        Non full-black without STP bit = Solid color (alpha = 255)
        Non full-black with STP bit = Solid color (alpha = 255)
         */

        if (enableTransparency) {
            if (fullBlack) {
                return (byte) (stpBit ? 0x7F : 0x00);
            } else {
                return (byte) (stpBit ? 0x7F : 0xFF);
            }
        } else {
            return (byte) ((fullBlack && !stpBit) ? 0x00 : 0xFF);
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean getSTPBit(boolean fullBlack, boolean enableTransparency, byte alpha) {
        /*
        -- If Semi-Transparent is on --
        Full-black without STP bit = Transparent (alpha = 0)
        Full-black with STP bit = Semi-transparent black (alpha = 127)
        Non full-black without STP bit = Solid color (alpha = 255)
        Non full-black with STP bit = Semi-transparent color (alpha = 127)

        -- If Semi-Transparent mode is off --
        Full-black without STP bit = Transparent (alpha = 0)
        Full-black with STP bit = Opaque black (alpha = 255)
        Non full-black without STP bit = Solid color (alpha = 255)
        Non full-black with STP bit = Solid color (alpha = 255)
         */

        // Ranges:
        // [170, 255] 0xFF: (enableTransparency && !fullBlack && !stpBit) || (!enableTransparency && (!fullBlack || stpBit))
        // [85, 169] 0x7F: (enableTransparency && stpBit) Works regardless of fullBlack.
        // [0, 84] 0x00: (fullBlack && !stpBit) Works regardless of enableTransparency.

        short uAlpha = DataUtils.byteToUnsignedShort(alpha);
        if (uAlpha >= 170 && uAlpha < 256) {
            if (enableTransparency) {
                if (fullBlack) {
                    throw new RuntimeException("Opaque colors (where Alpha >= 170) cannot have transparency enabled and be fully black.");
                } else {
                    return false;
                }
            } else if (fullBlack) { // if fullBlack is true, stpBit must be true, otherwise the alpha value would be 00.
                return true;
            } else {
                // If fullBlack is false, and enableTransparency is false...
                // stpBit could either be true or false, so we need to pick one.
                // This alpha range is the most opaque alpha range of the three.
                // If we were to compare though, importing an image with us returning true here while transparency was disabled, we'd get an image.
                // If we were to then enable transparency, it would cause the highest opacity pixels to suddenly get half transparency.
                // Because of this situation, defaulting to an STP bit of false is preferable.
                return false;
            }
        } else if (uAlpha >= 85 && uAlpha < 170) {
            // 0x7F should only be possible if stpBit is true and enableTransparency is true.
            if (!enableTransparency)
                throw new RuntimeException("Transparent colors (where Alpha is near 127) require PSX transparency to be enabled!");

            return true;
        } else if (uAlpha >= 0 && uAlpha < 85) {
            // 0x00 should only be possible if fullBlack is true and stpBit is false.
            // However, non full-black color cannot get an alpha of zero.
            if (!fullBlack)
                throw new RuntimeException("Transparent colors (where Alpha < 85) should have RGB components of zero! (RGB 0)");

            return false;
        }

        throw new RuntimeException("This exception should not be possible.");
    }
}