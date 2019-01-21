package net.highwayfrogs.editor.file.standard.psx;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the CLUT format described on http://www.psxdev.net/forum/viewtopic.php?t=109.
 * Created by Kneesnap on 8/30/2018.
 */
@Getter
public class PSXClutColor extends GameObject {
    @Setter private boolean stp; // stp -> "Semi Transparent" Flag.
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

    @Override
    public void load(DataReader reader) {
        short value = reader.readShort();
        this.blue = getByte(value, BLUE_OFFSET);
        this.green = getByte(value, GREEN_OFFSET);
        this.red = getByte(value, RED_OFFSET);
        this.stp = (value & STP_FLAG) == STP_FLAG;
    }

    private byte getByte(short value, int byteOffset) {
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
     * Set the byte value scaled from 0 - 255.
     * @param redByte The value to use.
     */
    public void setScaledRed(short redByte) {
        this.red = Utils.unsignedShortToByte((short) (redByte >> TO_FULL_BYTE));
    }

    /**
     * Set the byte value scaled from 0 - 255.
     * @param greenByte The value to use.
     */
    public void setScaledGreen(short greenByte) {
        this.green = Utils.unsignedShortToByte((short) (greenByte >> TO_FULL_BYTE));
    }

    /**
     * Set the byte value scaled from 0 - 255.
     * @param blueByte The value to use.
     */
    public void setScaledBlue(short blueByte) {
        this.blue = Utils.unsignedShortToByte((short) (blueByte >> TO_FULL_BYTE));
    }

    /**
     * Get the red byte value on a scale from 0 - 255.
     * @return redValue
     */
    public short getUnsignedScaledRed() {
        return (short) (Utils.byteToUnsignedShort(getRed()) << TO_FULL_BYTE);
    }

    /**
     * Get the green byte value on a scale from 0 - 255.
     * @return greenValue
     */
    public short getUnsignedScaledGreen() {
        return (short) (Utils.byteToUnsignedShort(getGreen()) << TO_FULL_BYTE);
    }

    /**
     * Get the blue byte value on a scale from 0 - 255.
     * @return blueValue
     */
    public short getUnsignedScaledBlue() {
        return (short) (Utils.byteToUnsignedShort(getBlue()) << TO_FULL_BYTE);
    }

    /**
     * Get this color's alpha value on a scale from 0 (Transparent) to 0xFF (Solid)
     * @param semiTransparentMode Whether or not this color is rendered with semi-transparent blending on the PSX. (This is image-specific, not color-specific.)
     * @return alphaColor
     */
    public byte getAlpha(boolean semiTransparentMode) {
        if (!isStp())
            return isBlack() ? (byte) 0x00 : (byte) 0xFF;

        return semiTransparentMode ? (byte) 127 : (byte) 0xFF;
    }

    /**
     * Is this image devoid of all RGB colors, and equal to RGB {0, 0, 0}?
     * @return isBlack
     */
    public boolean isBlack() {
        return (getRed() == 0) && (getGreen() == 0) && (getBlue() == 0);
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

        color.setScaledRed(Utils.byteToUnsignedShort(red));
        color.setScaledGreen(Utils.byteToUnsignedShort(green));
        color.setScaledBlue(Utils.byteToUnsignedShort(blue));

        short alphaShort = Utils.byteToUnsignedShort(alpha);
        color.setStp(alphaShort == 0xFF
                ? color.isBlack() // STP-Bit is true if alpha is 0xFF and the color is black.
                : alphaShort != 0); // If alpha is not zero, the stp bit is false. Otherwise, it's in semi-transparent mode, and the stp bit is true.

        return color;
    }

    /**
     * Turn an RGBA byte array into a PSXClutColor.
     * @param array The array to read RGBA bytes from.
     * @param index The index to read color data from.
     * @return clutColor
     */
    public static PSXClutColor fromRGBA(byte[] array, int index) {
        return fromRGBA(array[index], array[index + 1], array[index + 2], array[index + 3]);
    }
}
