package net.highwayfrogs.editor.file.standard.psx;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

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
        byte[] arr = new byte[4]; //RGBA
        arr[0] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(getRed()) << TO_FULL_BYTE));
        arr[1] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(getGreen()) << TO_FULL_BYTE));
        arr[2] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(getBlue()) << TO_FULL_BYTE));
        arr[3] = (byte) (isStp() ? 0x01 : 0x00);
        return Utils.readNumberFromBytes(arr);
    }

    /**
     * Get this value as a BGRA integer.
     */
    public int toBGRA() {
        byte[] arr = new byte[4]; //BGRA
        arr[0] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(getBlue()) << TO_FULL_BYTE));
        arr[1] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(getGreen()) << TO_FULL_BYTE));
        arr[2] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(getRed()) << TO_FULL_BYTE));
        arr[3] = (byte) (isStp() ? 0x00 : 0xFF);
        return Utils.readNumberFromBytes(arr);
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
        color.red = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(red) >> TO_FULL_BYTE));
        color.green = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(green) >> TO_FULL_BYTE));
        color.blue = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(blue) >> TO_FULL_BYTE));
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
     * Reads a PSXClutColor from a 16bit short into an RGBA int.
     * @param color The short to read from.
     * @return rgbaColor
     */
    public static int readColorFromShort(short color) {
        byte blue = getByte(color, BLUE_OFFSET);
        byte green = getByte(color, GREEN_OFFSET);
        byte red = getByte(color, RED_OFFSET);
        boolean stp = (color & STP_FLAG) == STP_FLAG;
        byte[] arr = new byte[4]; //RGBA
        arr[0] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(red) << TO_FULL_BYTE));
        arr[1] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(green) << TO_FULL_BYTE));
        arr[2] = Utils.unsignedShortToByte((short) (Utils.byteToUnsignedShort(blue) << TO_FULL_BYTE));
        arr[3] = (byte) (stp ? 0x01 : 0x00);
        return Utils.readNumberFromBytes(arr);
    }
}
