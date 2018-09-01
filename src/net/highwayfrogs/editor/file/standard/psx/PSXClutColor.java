package net.highwayfrogs.editor.file.standard.psx;

import lombok.Getter;
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
    private boolean stp;
    private byte red;
    private byte green;
    private byte blue;

    private static final int BITS_PER_VALUE = 5;
    private static final int RED_OFFSET = 0;
    private static final int GREEN_OFFSET = RED_OFFSET + BITS_PER_VALUE;
    private static final int BLUE_OFFSET = GREEN_OFFSET + BITS_PER_VALUE;
    private static final int TO_FULL_BYTE = Constants.BITS_PER_BYTE - BITS_PER_VALUE;
    private static final int STP_FLAG = (1 << 15);
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
        short writeValue = (short) (this.stp ? STP_FLAG : 0);
        writeValue |= (getBlue() << BLUE_OFFSET);
        writeValue |= (getGreen() << GREEN_OFFSET);
        writeValue |= (getRed() << RED_OFFSET);
        writer.writeShort(writeValue);
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
     * Checks if this image is drawn as transparent.
     * @return isTransparent.
     */
    public boolean isTransparent() {
        boolean isBlack = (getRed() == 0) && (getGreen() == 0) && (getBlue() == 0);
        // We're deviating from how I understand this to work on the website, because how it works on the website generates bad images it seems. This however may just be for Frogger, and not all PSX games.
        // Normally, I understand the check to be isBlack != isStp().
        return isBlack && !isStp(); // RGB {0, 0, 0} is a special-case, where the outcome is flipped.
    }
}
