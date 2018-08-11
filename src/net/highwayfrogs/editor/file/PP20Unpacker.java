package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;

/**
 * PP20 Unpacker: Unpacks PowerPacker compressed data.
 *
 * Source:
 *  Author: Josef Jelinek
 *  URL: https://github.com/josef-jelinek/tiny-mod-player/blob/master/lib.gamod/src/gamod/unpack/PowerPacker.java
 *  Copied on August 11, 2018. There is no license attached to the repository, however the author has explicitly granted permission to use this code.
 */
public class PP20Unpacker {

    /**
     * Is a given byte array PP20 compressed data?
     * @param a The bytes to test.
     * @return isCompressed
     */
    public static boolean isCompressed(byte[] a) {
        return a.length > 11 && a[0] == 'P' && a[1] == 'P' && a[2] == '2' && a[3] == '0';
    }

    /**
     * Unpacks PP20 compressed data.
     * @param data The data to unpack.
     * @return unpackedData
     */
    public static byte[] unpackData(byte[] data) {
        Constants.verify(isCompressed(data), "Not PowerPacker (PP20) compressed data!");
        int[] offsetBitLengths = getOffsetBitLengths(data);
        int skip = data[data.length - 1] & 255;
        byte[] out = new byte[getDecodedDataSize(data)];
        int outPos = out.length;
        BitReader in = new BitReader(data, data.length - 5, true);
        in.readBits(skip); // skipped bits
        while (outPos > 0)
            outPos = decodeSegment(in, out, outPos, offsetBitLengths);
        return out;
    }

    private static int[] getOffsetBitLengths(byte[] data) {
        int[] a = new int[4];
        for (int i = 0; i < 4; i++)
            a[i] = data[i + 4];
        return a;
    }

    private static int getDecodedDataSize(byte[] data) {
        int i = data.length - 2;
        return (data[i - 2] & 255) << 16 | (data[i - 1] & 255) << 8 | data[i] & 255;
    }

    private static int decodeSegment(BitReader in, byte[] out, int outPos, int[] offsetBitLengths) {
        if (in.readBit() == 0)
            outPos = copyFromInput(in, out, outPos);
        if (outPos > 0)
            outPos = copyFromDecoded(in, out, outPos, offsetBitLengths);
        return outPos;
    }

    private static int copyFromInput(BitReader in, byte[] out, int pos) {
        int count = 1, countInc;
        while ((countInc = in.readBits(2)) == 3) // '11's + 1 + the last non '11'
            count += 3;
        for (count += countInc; count > 0; count--)
            out[--pos] = (byte) in.readBits(8);
        return pos;
    }

    private static int copyFromDecoded(BitReader in, byte[] out, int pos, int[] offsetBitLengths) {
        int run = in.readBits(2); // always at least 2 bytes (2 bytes ~ 0, 3 ~ 1, 4 ~ 2, 5+ ~ 3)
        int offBits = run == 3 && in.readBit() == 0 ? 7 : offsetBitLengths[run];
        int off = in.readBits(offBits);
        int runInc = 0;
        if (run == 3)
            while ((runInc = in.readBits(3)) == 7) // '111's + 2 + the last non '111'
                run += 7;
        for (run += 2 + runInc; run > 0; run--, pos--)
            out[pos - 1] = out[pos + off];
        return pos;
    }

    public static final class BitReader {
        private final byte[] data;
        private int pos;
        private int bitPos = 7;
        private int dir = 1;

        public BitReader(final byte[] data, final int pos, final boolean reverseByBits) {
            this.data = data;
            this.pos = pos;
            if (reverseByBits) {
                bitPos = 0;
                dir = -1;
            }
        }

        public int readByte() {
            final int x = data[pos] & 255;
            pos += dir;
            bitPos = dir < 0 ? 0 : 7;
            return x;
        }

        public int readBit() {
            final int x = data[pos] >> bitPos & 1;
            if (bitPos == (dir < 0 ? 7 : 0)) {
                bitPos = dir < 0 ? 0 : 7;
                pos += dir;
            } else
                bitPos -= dir;
            return x;
        }

        public int readBits(final int n) {
            int x = 0;
            for (int i = 0; i < n; i++)
                x = x << 1 | readBit();
            return x;
        }

        public int readSignBits(final int n) {
            return readBits(n) << 32 - n >> 32 - n;
        }
    }
}
