package net.highwayfrogs.editor.file.writer;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
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
    private LinkedList<Byte> bytes = new LinkedList<>();
    private List<Integer> bits = new ArrayList<>(); //TODO: Avoid auto-boxing plz.
    private int currentBit = Constants.BITS_PER_BYTE;
    private byte currentByte;
    @Setter private boolean reverseBytes;
    @Setter private boolean reverseBits;

    /**
     * Write a singular bit.
     * @param bit The bit to write. 1 or 0.
     */
    public void writeBit(int bit) {
        Utils.verify(bit == Constants.BIT_TRUE || bit == Constants.BIT_FALSE, "Invalid bit number %d.", bit);
        this.bits.add(bit);

        // Add the bit to the current byte.
        int bitShift = isReverseBits() ? this.currentBit : getCurrentReverseBitID();
        int shiftedBit = bit << bitShift;
        this.currentByte |= shiftedBit;

        // If the current byte is complete, add it to the list of bytes.
        if (--this.currentBit == 0) {
            this.bytes.add(this.currentByte);
            this.currentByte = 0;
            this.currentBit = Constants.BITS_PER_BYTE;
        }
    }

    private int getCurrentReverseBitID() {
        return Constants.BITS_PER_BYTE - this.currentBit;
    }

    /**
     * Write an array of bits.
     * @param bits The bits to write.
     */
    public void writeBits(int[] bits) {
        for (int bit : bits)
            writeBit(bit);
    }

    /**
     * Write all the bits in a byte.
     * @param value The byte to write bits from.
     */
    public void writeByte(byte value) {
        writeBits(Utils.getBits(value, Constants.BITS_PER_BYTE));
    }

    /**
     * Export all written data as a bit array.
     * Array order is the order the bits were entered.
     * Gets the bits in the order they were entered?
     * Gets the bits from the bytes?
     * @return bitArray
     */
    public int[] toChronologicalBitArray() {
        int[] bits = new int[this.bits.size()];
        for (int i = 0; i < this.bits.size(); i++)
            bits[i] = this.bits.get(i);
        return bits;
    }

    /**
     * Export all written data as a byte array.
     * WARNING: This operation will add bits if there is an incomplete byte, meaning it will change the state of this writer.
     * @return byteArray
     */
    public byte[] toByteArray() {
        while (this.currentBit != Constants.BITS_PER_BYTE)
            writeBit(0); // Finish the current bit.

        // Write in backwards order, because PP20 does that.
        byte[] arr = new byte[this.bytes.size()];

        int i = isReverseBytes() ? arr.length - 1 : 0;
        for (Byte aByte : this.bytes)
            arr[isReverseBytes() ? i-- : i++] = aByte;

        return arr;
    }
}
