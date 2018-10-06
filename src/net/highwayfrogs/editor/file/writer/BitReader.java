package net.highwayfrogs.editor.file.writer;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;

/**
 * Reads bits from a ByteArray.
 * Created by Kneesnap on 10/5/2018.
 */
@Getter
public class BitReader {
    private final byte[] data;
    private int bytePos;
    private int bitPos = 0;
    @Setter private boolean reverseBits;
    @Setter private boolean reverseBytes;

    private static final int MAX_BIT = Constants.BITS_PER_BYTE - 1;

    public BitReader(final byte[] data, final int pos) {
        this.data = data;
        this.bytePos = pos;
    }

    /**
     * Read the next bit.
     * @return bitValue
     */
    public int readBit() {
        int readBitPos = isReverseBits() ? (MAX_BIT - this.bitPos) : this.bitPos;
        int readBytePos = isReverseBytes() ? (this.data.length - 1 - this.bytePos) : this.bytePos;

        final int bit = (this.data[readBytePos] >> readBitPos) & 1; // Get the bit at the next position.

        if (bitPos == MAX_BIT) { // Reached the end of the bit.
            this.bitPos = 0;
            this.bytePos--;
        } else {
            this.bitPos++;
        }
        return bit;
    }

    /**
     * Read a variable number of bits into an integer.
     * @param amount The number of bits to read.
     * @return readValue
     */
    public int readBits(int amount) {
        int num = 0;
        for (int i = 0; i < amount; i++)
            num = num << 1 | readBit(); // Shift the existing read bits left, and add the next available bit. If you read four bits, it will read it like an integer.
        return num;
    }
}
