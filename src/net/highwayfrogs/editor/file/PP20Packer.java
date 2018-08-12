package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Packs a byte array into PP20 compressed data.
 * I was unable to find a compression sub-routine (Even in C) which does this function.
 * So, this will/is being created from the documentation given below, and trying to reverse the unpacker.
 *
 * Work backwards.
 * Useful Links:
 *  - https://en.wikipedia.org/wiki/Lempel–Ziv–Welch
 *  - https://eblong.com/zarf/blorb/mod-spec.txt
 * Created by Kneesnap on 8/11/2018.
 */
public class PP20Packer {
    private static final String MARKER = "PP20";
    private static final byte[] COMPRESSION_SETTINGS = {0x09, 0x0A, 0x0C, 0x0D}; // PP20 compression settings.

    /**
     * Pack a byte array into PP20 compressed data.
     * @param data The data to compress.
     * @return packedData
     */
    public static byte[] packData(byte[] data) {
        // Take the compressed data, and pad it with the file structure. Then, we're done.
        byte[] compressedData = compressData(data);
        byte[] completeData = new byte[compressedData.length + 12];
        System.arraycopy(compressedData, 0, completeData, 8, compressedData.length); // Copy compressed data.

        for (int i = 0; i < MARKER.length(); i++)
            completeData[i] = (byte) MARKER.charAt(i);

        for (int i = 0; i < COMPRESSION_SETTINGS.length; i++)
            completeData[i + 4] = COMPRESSION_SETTINGS[i];

        //TODO: File length at the end.
        return completeData;
    }

    private static byte[] compressData(byte[] data) {
        BitWriter writer = new BitWriter();
        //TODO
        return writer.toArray();
    }

    public static class BitWriter {
        private List<Byte> bytes = new ArrayList<>();
        private int currentBit = Constants.BITS_PER_BYTE;
        private byte currentByte;

        public void writeBit(int bit) {
            Constants.verify(bit == 1 || bit == 0, "Invalid bit number %d.", bit);

            // Add the bit to the current byte.
            this.currentByte <<= 1; // Shift left. 00000001 -> 00000010
            this.currentByte |= bit;

            // If the current byte is complete, add it to the list of bytes.
            if (--this.currentBit == 0) {
                this.bytes.add(this.currentByte);
                this.currentByte = 0;
                this.currentBit = Constants.BITS_PER_BYTE;
            }
        }

        public void writeBits(int[] bits) {
            for (int i = 0; i < bits.length; i++) // Write them in reverse order.
                writeBit(bits[bits.length - 1 - i]);
        }

        public byte[] toArray() {
            while (this.currentBit != Constants.BITS_PER_BYTE)
                writeBit(0);

            byte[] arr = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++)
                arr[i] = bytes.get(i);
            return arr;
        }
    }
}