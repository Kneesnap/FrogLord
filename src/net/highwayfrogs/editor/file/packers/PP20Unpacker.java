package net.highwayfrogs.editor.file.packers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.writer.BitReader;
import net.highwayfrogs.editor.utils.DataUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * PP20 Unpacker: Unpacks PowerPacker compressed data.
 * Source:
 * Original Author: Josef Jelinek
 * URL: <a href="https://github.com/josef-jelinek/tiny-mod-player/blob/master/lib.gamod/src/gamod/unpack/PowerPacker.java"/>
 * Copied on August 11, 2018. There is no license attached to the repository, however the author has explicitly granted written permission to use this code.
 */
public class PP20Unpacker {
    private static final int OFFSET_BIT_OPTIONS = 4;

    /**
     * Is a given byte array PP20 compressed data?
     * @param a The bytes to test.
     * @return isCompressed
     */
    public static boolean isCompressed(byte[] a) {
        return a.length > 11 && DataUtils.testSignature(a, PP20Packer.MARKER_BYTES);
    }

    /**
     * Unpacks PP20 compressed data.
     * @param data The data to unpack.
     * @return unpackedData
     */
    public static UnpackResult unpackData(byte[] data) {
        int[] offsetBitLengths = getOffsetBitLengths(data);
        int skip = data[data.length - 1] & 0xFF; // Last byte contains the amount of bits to trash.
        byte[] out = new byte[getDecodedDataSize(data)];
        int outPos = out.length;
        BitReader in = new BitReader(data, 4);
        in.setReverseBytes(true);
        in.readBits(skip); // skipped bits

        AtomicInteger byteMargin = new AtomicInteger();
        while (outPos > 0)
            outPos = decodeSegment(in, out, outPos, offsetBitLengths, byteMargin);
        return new UnpackResult(out, byteMargin.get());
    }

    @Getter
    @RequiredArgsConstructor
    public static class UnpackResult {
        private final byte[] unpackedBytes;
        private final int minimumByteMargin;

        /**
         * Get the word offset to ensure the compressed reader never overlaps with the uncompressed writer when unpacking in-place.
         */
        public int getSafetyMarginWordCount() {
            return 2 + (this.minimumByteMargin / Constants.INTEGER_SIZE);
        }
    }

    private static int[] getOffsetBitLengths(byte[] data) {
        int[] a = new int[OFFSET_BIT_OPTIONS];
        for (int i = 0; i < OFFSET_BIT_OPTIONS; i++)
            a[i] = data[i + OFFSET_BIT_OPTIONS];
        return a;
    }

    private static int getDecodedDataSize(byte[] data) {
        int i = data.length - 2;
        return (data[i - 2] & 0xFF) << 16 | (data[i - 1] & 0xFF) << 8 | data[i] & 0xFF;
    }

    private static int decodeSegment(BitReader in, byte[] out, int outPos, int[] offsetBitLengths, AtomicInteger byteMargin) {
        if (in.readBit() == PP20Packer.HAS_RAW_DATA_BIT)
            outPos = copyFromInput(in, out, outPos, byteMargin);
        if (outPos > 0)
            outPos = copyFromDecoded(in, out, outPos, offsetBitLengths, byteMargin);
        return outPos;
    }

    // Appears to put it into the table.
    private static int copyFromInput(BitReader reader, byte[] out, int bytePos, AtomicInteger byteMargin) {
        int count = 1, countInc;
        while ((countInc = reader.readBits(PP20Packer.INPUT_BIT_LENGTH)) == PP20Packer.INPUT_CONTINUE_WRITING_BITS) // Read the string size. If it == 3, that means the length might be longer.
            count += PP20Packer.INPUT_CONTINUE_WRITING_BITS;

        updateByteMargin(reader, out, bytePos, byteMargin);
        for (count += countInc; count > 0; count--) // Register the string in the table.
            out[--bytePos] = (byte) reader.readBits(Constants.BITS_PER_BYTE);

        return bytePos;
    }

    private static int copyFromDecoded(BitReader in, byte[] out, int bytePos, int[] offsetBitLengths, AtomicInteger byteMargin) {
        int compressionLevel = in.readBits(PP20Packer.COMPRESSION_LEVEL_BITS); // always at least 2 bytes (2 bytes ~ 0, 3 ~ 1, 4 ~ 2, 5+ ~ 3)
        boolean extraLengthData = (compressionLevel == PP20Packer.INPUT_CONTINUE_WRITING_BITS);
        int offBits = extraLengthData && in.readBit() == Constants.BIT_FALSE ? PP20Packer.OPTIONAL_BITS_SMALL_OFFSET : offsetBitLengths[compressionLevel];
        int off = in.readBits(offBits);

        int copyLength = compressionLevel + PP20Packer.MINIMUM_DECODE_DATA_LENGTH;
        if (extraLengthData) { // The length might be extended further.
            int lastLengthBits;
            do { // Keep adding until the three read bits are not '111', meaning the length has stopped.
                lastLengthBits = in.readBits(PP20Packer.OFFSET_BIT_LENGTH);
                copyLength += lastLengthBits;
            } while (lastLengthBits == PP20Packer.OFFSET_CONTINUE_WRITING_BITS);
        }

        updateByteMargin(in, out, bytePos, byteMargin);
        for (int i = 0; i < copyLength; i++, bytePos--)
            out[bytePos - 1] = out[bytePos + off];

        return bytePos;
    }

    private static void updateByteMargin(BitReader reader, byte[] out, int bytePos, AtomicInteger byteMargin) {
        // Check the documentation for PP20Packer.SAFETY_MARGIN_CONSTANT to explain what's going on here.
        // This method has been tested against Beast Wars PC/PSX, Frogger PC/PSX, MediEvil, MediEvil 2, Moon Warrior, and C-12 Final Resistance and outputs perfect safety margin matches for all of them.
        int writerPosFromOutputBufferEnd = out.length - bytePos;
        int readerPosFromOutputBufferEnd = out.length - (reader.getData().length - PP20Packer.SAFETY_MARGIN_CONSTANT - 1) + reader.getBytePos();
        int currentByteMargin = writerPosFromOutputBufferEnd - readerPosFromOutputBufferEnd;
        if (currentByteMargin > byteMargin.get())
            byteMargin.set(currentByteMargin);
    }
}
