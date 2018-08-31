package net.highwayfrogs.editor.file.standard.psx;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
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
    private static final int BLUE_OFFSET = 1;
    private static final int GREEN_OFFSET = BLUE_OFFSET + BITS_PER_VALUE;
    private static final int RED_OFFSET = GREEN_OFFSET + BITS_PER_VALUE;
    public static final int BYTE_SIZE = Constants.SHORT_SIZE;

    @Override
    public void load(DataReader reader) {
        short value = reader.readShort();
        this.red = getByte(value, RED_OFFSET);
        this.green = getByte(value, GREEN_OFFSET);
        this.blue = getByte(value, BLUE_OFFSET);
        this.stp = (value & 1) == 1;
    }

    private byte getByte(short value, int byteOffset) {
        value >>= byteOffset;
        for (int i = BITS_PER_VALUE; i < Constants.BITS_PER_BYTE; i++)
            value &= ~(1 << i); // Disable bits 5-7, as bits 0-4 are the values we care about for this number.

        return (byte) value;
    }

    @Override
    public void save(DataWriter writer) {
        short writeValue = (short) (this.stp ? 1 : 0);
        writeValue |= (getRed() << RED_OFFSET);
        writeValue |= (getGreen() << GREEN_OFFSET);
        writeValue |= (getBlue() << BLUE_OFFSET);
        writer.writeShort(writeValue);
    }

    /**
     * Checks if this image is drawn as transparent.
     * @return isTransparent.
     */
    public boolean isTransparent() {
        boolean isBlack = (getRed() == 0) && (getGreen() == 0) && (getBlue() == 0);
        return isStp() != isBlack; // RGB {0, 0, 0} is a special-case, where the outcome is flipped.
    }
}
