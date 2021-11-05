package net.highwayfrogs.editor.file.writer;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Write bits into a buffer.
 * Default Behavior:
 * - First byte will end up at byte 0, instead of at the end.
 * - First bit will end up as the right-most byte, while the eight will be the left-most. Ie: writeBit(0); writeBit(1); -> 00000010
 * Created by Kneesnap on 10/5/2018.
 */
@Getter
public class BitWriter {
    private List<Byte> bytes = new ArrayList<>();
    private int currentBit = Constants.BITS_PER_BYTE;
    private byte currentByte;
    @Setter private boolean reverseBytes;
    @Setter private boolean reverseBits;

    /**
     * Gets the current number of bytes this takes up.
     * Even if only 1 bit is used in a byte, that counts.
     * @return byteCount
     */
    public int getByteCount() {
        return this.bytes.size() + (this.currentBit != Constants.BITS_PER_BYTE ? 1 : 0);
    }

    /**
     * Write a singular bit.
     * @param bit The bit to write. 1 or 0.
     */
    public void writeBit(int bit) {
        if (bit != Constants.BIT_TRUE && bit != Constants.BIT_FALSE) // Since this is a high call function, we avoid Utils.verify.
            throw new RuntimeException("Invalid bit number " + bit + ".");

        // Add the bit to the current byte.
        if (bit == Constants.BIT_TRUE)
            this.currentByte |= (bit << getCurrentBitID());

        // If the current byte is complete, add it to the list of bytes.
        if (--this.currentBit == 0) {
            this.bytes.add(this.currentByte);
            this.currentByte = 0;
            this.currentBit = Constants.BITS_PER_BYTE;
        }
    }

    private int getCurrentBitID() {
        return isReverseBits() ? this.currentBit - 1 : Constants.BITS_PER_BYTE - this.currentBit;
    }

    /**
     * Write a number of false bits.
     * @param count The number of bits to write.
     */
    public void writeFalseBits(int count) {
        for (int i = 0; i < count; i++)
            writeBit(0);
    }

    /**
     * Write bits from an integer.
     * @param number   The integer to write bits from.
     * @param bitCount the number of bits.
     */
    public void writeBits(int number, int bitCount) {
        if (this.reverseBits) {
            for (int i = 0; i < bitCount; i++)
                writeBit(((number & (Constants.BIT_TRUE << i)) != 0) ? Constants.BIT_TRUE : Constants.BIT_FALSE);
        } else {
            for (int i = bitCount - 1; i >= 0; i--)
                writeBit((number >> i) & Constants.BIT_TRUE);
        }
    }

    /**
     * Write all the bits in a byte.
     * @param value The byte to write bits from.
     */
    public void writeByte(byte value) {
        if (this.reverseBits) {
            for (int i = 0; i < Constants.BITS_PER_BYTE; i++)
                writeBit(((value & (Constants.BIT_TRUE << i)) != 0) ? Constants.BIT_TRUE : Constants.BIT_FALSE);
        } else {
            for (int i = Constants.BITS_PER_BYTE - 1; i >= 0; i--)
                writeBit((value >> i) & Constants.BIT_TRUE);
        }
    }

    /**
     * Export all written data as a byte array.
     * WARNING: This operation will add bits if there is an incomplete byte, meaning it will change the state of this writer.
     * @return byteArray
     */
    public byte[] toByteArray() {
        return toByteArray(0, 0);
    }

    /**
     * Export all written data as a byte array.
     * WARNING: This operation will add bits if there is an incomplete byte, meaning it will change the state of this writer.
     * @return byteArray
     */
    public byte[] toByteArray(int extraBytesBefore, int extraBytesAfter) {
        finishCurrentByte();

        // Write in backwards order, because PP20 does that.
        byte[] arr = new byte[extraBytesBefore + this.bytes.size() + extraBytesAfter];
        int i = isReverseBytes() ? arr.length - 1 - extraBytesAfter : extraBytesBefore;
        for (Byte aByte : this.bytes)
            arr[isReverseBytes() ? i-- : i++] = aByte;

        return arr;
    }

    /**
     * Finish the current byte being written.
     */
    public int finishCurrentByte() {
        int writtenBits = 0;
        while (this.currentBit != Constants.BITS_PER_BYTE) {
            writeBit(0); // Finish the current byte.
            writtenBits++;
        }

        return writtenBits;
    }
}
