package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.image.PsxAbrTransparency;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the CLUT format described <a href="http://www.psxdev.net/forum/viewtopic.php?t=109">here</a>.
 * Created by Kneesnap on 8/30/2018.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PSXClutColor implements IBinarySerializable {
    private byte red;
    private byte green;
    private byte blue;
    @Setter private boolean stp; // stp -> "Semi Transparent" Flag.

    private static final int BITS_PER_VALUE = 5;
    private static final int BIT_MASK = ((1 << BITS_PER_VALUE) - 1); // 0b11111
    private static final int RED_OFFSET = 0;
    private static final int GREEN_OFFSET = RED_OFFSET + BITS_PER_VALUE;
    private static final int BLUE_OFFSET = GREEN_OFFSET + BITS_PER_VALUE;
    private static final int TO_FULL_BYTE = Constants.BITS_PER_BYTE - BITS_PER_VALUE;
    private static final int STP_FLAG = Constants.BIT_FLAG_15; // "Semi-Transparent"
    public static final int SIZE_IN_BYTES = Constants.SHORT_SIZE;

    public static final int BIT_MASK_5BIT = 0b11111;
    public static final int ARGB8888_TO5BIT_COLOR_MASK = 0b11111000_11111000_11111000;

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

    public PSXClutColor(short value) {
        fromShort(value);
    }

    @Override
    public void load(DataReader reader) {
        fromShort(reader.readShort());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(toShort());
    }

    /**
     * Loads the color data from the provided short value
     * @param value the value to read from
     * @return this
     */
    public PSXClutColor fromShort(short value) {
        this.blue = getByte(value, BLUE_OFFSET);
        this.green = getByte(value, GREEN_OFFSET);
        this.red = getByte(value, RED_OFFSET);
        this.stp = (value & STP_FLAG) == STP_FLAG;
        return this;
    }

    private static byte getByte(short value, int byteOffset) {
        return (byte) (((value >>> byteOffset) & BIT_MASK) << TO_FULL_BYTE);
    }

    /**
     * Tests if the color is fully black. Used to control the behavior of the STP flag.
     */
    public boolean isFullBlack() {
        // Using getSmallRed() vs regular red is a good distinction, because the PC port also only cares about the final stripped color, not the unstripped color.
        // In other words, this is consistent with the reverse-engineered behavior of the PC port.
        // Also, this is consistent with the PSX version, by nature of it natively using 16-bit color.
        return getSmallRed() == 0 && getSmallGreen() == 0 && getSmallBlue() == 0;
    }

    /**
     * Gets the small (5-bit) red byte value
     */
    public byte getSmallRed() {
        return (byte) ((this.red >>> TO_FULL_BYTE) & BIT_MASK);
    }

    /**
     * Gets the small (5-bit) green byte value
     */
    public byte getSmallGreen() {
        return (byte) ((this.green >>> TO_FULL_BYTE) & BIT_MASK);
    }

    /**
     * Gets the small (5-bit) blue byte value
     */
    public byte getSmallBlue() {
        return (byte) ((this.blue >>> TO_FULL_BYTE) & BIT_MASK);
    }

    /**
     * Get this value as a short.
     * @return shortValue
     */
    public short toShort() {
        return (short) ((this.stp ? STP_FLAG : 0)
                | (getSmallBlue() << BLUE_OFFSET)
                | (getSmallGreen() << GREEN_OFFSET)
                | (getSmallRed() << RED_OFFSET));
    }

    /**
     * Gets this PSXClutColor as a CVector.
     */
    public CVector toCVector() {
        return new CVector(this.red, this.green, this.blue, (byte) (this.stp ? CVector.FLAG_SEMI_TRANSPARENT : 0));
    }

    @Override
    public PSXClutColor clone() {
        return new PSXClutColor(toShort());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PSXClutColor && ((PSXClutColor) other).toShort() == this.toShort();
    }

    @Override
    public int hashCode() {
        return toShort();
    }

    @Override
    public String toString() {
        return String.format("PSXClutColor{red=%02X,green=%02X,blue=%02X,stp=%b}",
                this.red & 0xFF, this.green & 0xFF, this.blue & 0xFF, this.stp);
    }

    /**
     * Loads this clut color from an ARGB8888 integer.
     * @param color the color to load from
     * @param enableTransparency whether PSX semi-transparency mode is expected to be enabled when using this color.
     * @return this
     */
    public PSXClutColor fromFullARGB(int color, boolean enableTransparency) {
        byte alpha = (byte) ((color >>> 24) & 0xFF);
        this.red = (byte) ((color >>> 16) & 0xFF);
        this.green = (byte) ((color >>> 8) & 0xFF);
        this.blue = (byte) (color & 0xFF);
        this.stp = getSTPBit(isFullBlack(), enableTransparency, alpha);
        return this;
    }

    /**
     * Loads this clut color from an RGB888 integer.
     * @param color the color to load from
     * @return this
     */
    public PSXClutColor fromRGB(int color, boolean stpBit) {
        this.red = (byte) ((color >>> 16) & 0xFF);
        this.green = (byte) ((color >>> 8) & 0xFF);
        this.blue = (byte) (color & 0xFF);
        this.stp = stpBit;
        return this;
    }

    /**
     * Gets this color as an ARGB8888 integer.
     * @param enableTransparency Whether the color obtained is mimicking PSX "transparency processing" enabled/disabled on the draw primitive using this color.
     * @return intValue
     */
    public int toARGB(boolean enableTransparency, PsxAbrTransparency abr) {
        boolean fullBlack = isFullBlack();
        return ColorUtils.toARGB(this.red, this.green, this.blue, getAlpha(fullBlack, this.stp, enableTransparency, abr));
    }

    /**
     * Turn an ARGB8888 color into a PSXClutColor.
     * @param value The argbColor to read from.
     * @return clutColor
     */
    public static PSXClutColor fromARGB(int value, boolean enableTransparency) {
        return new PSXClutColor().fromFullARGB(value, enableTransparency);
    }

    /**
     * Reads a PSXClutColor from a 16bit short into an RGBA int.
     * @param color              The short to read from.
     * @param enableTransparency If the PSX would have "transparency processing" enabled on the draw primitive.
     * @return rgbaColor
     */
    public static int readARGBColorFromShort(short color, boolean enableTransparency, PsxAbrTransparency abr) {
        byte red = getByte(color, RED_OFFSET);
        byte green = getByte(color, GREEN_OFFSET);
        byte blue = getByte(color, BLUE_OFFSET);

        boolean stp = (color & STP_FLAG) == STP_FLAG;
        boolean fullBlack = (red == 0) && (green == 0) && (blue == 0);
        return ColorUtils.toARGB(red, green, blue, getAlpha(fullBlack, stp, enableTransparency, abr));
    }

    /**
     * Calculates the alpha byte value for the given ABR.
     * @param fullBlack true iff the red, green, and blue short components are all zero
     * @param stpBit represents the stp bit of the clut color
     * @param enableTransparency true iff PSX HW transparency is enabled.
     * @param abr the ABR mode applied to the texture, or null if not known
     * @return pixelAlpha
     */
    public static byte getAlpha(boolean fullBlack, boolean stpBit, boolean enableTransparency, PsxAbrTransparency abr) {
        /*
        -- If Semi-Transparent is on --
        Full-black without STP bit = Transparent (alpha = 0)
        Full-black with STP bit = Semi-transparent black (alpha = 127/ABR-controlled value)
        Non full-black without STP bit = Solid color (alpha = 255)
        Non full-black with STP bit = Semi-transparent color (alpha = 127/ABR-controlled value)

        -- If Semi-Transparent mode is off --
        Full-black without STP bit = Transparent (alpha = 0)
        Full-black with STP bit = Opaque black (alpha = 255)
        Non full-black without STP bit = Solid color (alpha = 255)
        Non full-black with STP bit = Solid color (alpha = 255)
         */

        if (enableTransparency) {
            if (fullBlack) {
                return (byte) (stpBit ? (abr != null ? abr.getStandaloneAlpha() : 0x7F) : 0x00);
            } else {
                return (byte) (stpBit ? (abr != null ? abr.getStandaloneAlpha() : 0x7F) : 0xFF);
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
                    // return true; // Closest behavior. (Alpha 127)
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
            // Buut, there are situations such as Sony Cambridge VLOs which may have a texture be rendered in multiple places, sometimes with semiTransparency, sometimes without semiTransparency.
            // So, in order to support this situation, (and because this alpha range can only be reached with STP bit = true), we'll treat this as STP bit true.
            //if (!enableTransparency)
            //    throw new RuntimeException("Transparent colors (where Alpha is near 127) require PSX transparency to be enabled!");

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