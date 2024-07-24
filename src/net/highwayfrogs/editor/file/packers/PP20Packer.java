package net.highwayfrogs.editor.file.packers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.writer.BitWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;

/**
 * Packs a byte array into PP20 compressed data. PP20 is a LZSS variant.
 * This packer is now a recreation of the reverse engineered original packer.
 * <a href="https://github.com/lab313ru/powerpacker_src/blob/master/main.cpp"/> (Thanks lab313ru!)
 * This implementation should be accurate to the version used in Frogger: He's Back.
 *
 * Useful Links:
 * - <a href="https://en.wikipedia.org/wiki/LZ77_and_LZ78"/>
 * - <a href="https://en.wikipedia.org/wiki/Lempel%E2%80%93Ziv%E2%80%93Storer%E2%80%93Szymanski"/>
 * - <a href="https://eblong.com/zarf/blorb/mod-spec.txt"/>
 * - <a href="https://books.google.com/books?id=ujnQogzx_2EC&printsec=frontcover (Specifically, the section about how LzSS improves upon Lz77)
 * - <a href="https://www.programcreek.com/java-api-examples/index.php?source_dir=trie4j-master/trie4j/src/kitchensink/java/org/trie4j/lz/LZSS.java"/>
 * - <a href="http://michael.dipperstein.com/lzss/"/>
 * - <a href="https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm"/>
 * Created by Kneesnap on 8/11/2018.
 */
public class PP20Packer {
    public static final byte[] EXTREME_COMPRESSION_SETTINGS = {0x09, 0x0A, 0x0C, 0x0D};
    private static final int COMPRESSING_SETTING_SIZE = 4;
    public static final int OPTIONAL_BITS_SMALL_OFFSET = 7;
    public static final int INPUT_BIT_LENGTH = 2;
    public static final int INPUT_CONTINUE_WRITING_BITS = 3;
    public static final int OFFSET_BIT_LENGTH = 3;
    public static final int OFFSET_CONTINUE_WRITING_BITS = 7;
    public static final int HAS_RAW_DATA_BIT = Constants.BIT_FALSE;
    public static final int MINIMUM_DECODE_DATA_LENGTH = 2;
    public static final int COMPRESSION_LEVEL_BITS = 2;
    public static final String MARKER = "PP20";
    public static final byte[] MARKER_BYTES = MARKER.getBytes();
    public static final int MAX_UNCOMPRESSED_FILE_SIZE = Utils.power(2, 3 * Constants.BITS_PER_BYTE) - 1; // Has 3 bytes to store this info in.

    // To be perfectly honest, I'm not sure what this value is.
    // The "safety margin" is something of a miracle that we're able to calculate.
    // Originally, buildwad.exe (A program we do not have in any form) most likely implemented a version of PowerPacker.
    // But, Millennium Interactive wanted to be able to decompress data in-place due to the low amount of memory available.
    // So, they use this idea of a safety margin to ensure the decompression read pointer never crosses the decompressed data write pointer.
    // The MWI contains the safety margin values, but the original algorithm used to generate the values has largely been guessed.
    // I've managed to create an algorithm which seems to give the same output values as seen in the MWI for Frogger.
    // This constant is used within the algorithm. I didn't feel comfortable ascribing intent to what the constant is though, so it is named generically.
    // This method has been tested against Beast Wars PC/PSX, Frogger PC/PSX, MediEvil, MediEvil 2, Moon Warrior, and C-12 Final Resistance and outputs perfect safety margin matches for all of them.
    public static final int SAFETY_MARGIN_CONSTANT = 4;

    /**
     * Packs a byte array using extreme compression settings.
     * @param data The data to pack.
     * @return packedData
     */
    public static PackResult packData(byte[] data) {
        return packData(data, true, EXTREME_COMPRESSION_SETTINGS);
    }

    /**
     * Pack a byte array into PP20 compressed data.
     * @param data The data to compress.
     * @param oldVersion whether an older version should be used. Frogger seems to use this for all files.
     * @return packedData
     */
    public static PackResult packData(byte[] data, boolean oldVersion, byte[] compressionSettings) {
        if (data.length > MAX_UNCOMPRESSED_FILE_SIZE)
            throw new RuntimeException("packData tried to compress data larger than the maximum PP20 file size! (" + data.length + " > " + MAX_UNCOMPRESSED_FILE_SIZE + ")!");

        PackerDataInstance packerData = new PackerDataInstance(oldVersion, compressionSettings);

        // Take the compressed data, and pad it with the file structure. Then, we're done.
        byte[] compressedData = compressData(data, packerData);
        byte[] sizeBytes = Utils.reverseByteArray(Utils.toByteArray(data.length));
        System.arraycopy(MARKER_BYTES, 0, compressedData, 0, MARKER_BYTES.length);
        System.arraycopy(compressionSettings, 0, compressedData, 4, compressionSettings.length);
        System.arraycopy(sizeBytes, 1, compressedData, compressedData.length - 4, Constants.INTEGER_SIZE - 1);
        return new PackResult(compressedData, packerData.getByteMargin());
    }

    @Getter
    @RequiredArgsConstructor
    public static class PackResult {
        private final byte[] packedBytes;
        private final int minimumByteMargin;

        /**
         * Get the word offset to ensure the compressed reader never overlaps with the uncompressed writer when unpacking in-place.
         */
        public int getSafetyMarginWordCount() {
            return 2 + (this.minimumByteMargin / Constants.INTEGER_SIZE);
        }
    }

    private static int updateSpeedupLarge(byte[] curr, int curIndex, int next, int count, PackerDataInstance info) {
        for (int i = curIndex + info.getWindowMax(); i < curIndex + info.getWindowMax() + count; ++i) {
            if (i >= curr.length - 1)
                continue;

            int val = ((curr[i] & 0xFF) << 8) | (curr[i + 1] & 0xFF);
            int back = info.getAddrs()[val];
            if (back != Integer.MIN_VALUE) {
                int newVal = i - back;
                int diff = back - next;

                if (diff >= 0 && newVal < info.getWindowMax() / 2) {
                    if (diff >= info.getWindowLeft())
                        diff -= info.getWindowMax();
                    info.getWindowArray()[info.getWindowOffset() + diff] = Utils.unsignedIntToShort(newVal);
                    info.getWindowArray()[info.getWindowMax() + info.getWindowOffset() + diff] = Utils.unsignedIntToShort(newVal);
                }
            }

            info.getAddrs()[val] = i;
        }

        return curIndex + count;
    }

    private static void prepareDict(int repeats, PackerDataInstance info) {
        for (int i = 0; i < repeats; ++i) {
            info.getWindowArray()[info.getWindowOffset()] = 0;
            info.getWindowArray()[info.getWindowMax() + info.getWindowOffset()] = 0;
            info.setWindowOffset(info.getWindowOffset() + 1);
            info.setWindowLeft(info.getWindowLeft() - 1);
            if (info.getWindowLeft() == 0) {
                info.setWindowLeft(info.getWindowMax());
                info.setWindowOffset(0);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static byte[] compressData(byte[] data, PackerDataInstance info) {
        BitWriter writer = new BitWriter();
        writer.setReverseBits(true);

        int maxSize = Math.min(data.length, info.getWindowLeft());
        updateSpeedupLarge(data, -info.getWindowMax(), 0, maxSize, info);

        int srcCurrIdx = 0;
        int bits = 0;
        while (srcCurrIdx < data.length) {
            int srcMax = Math.min(data.length, srcCurrIdx + 0x7FFF);

            final int oldWindowOffset = info.getWindowOffset();
            int nextSrc = srcCurrIdx + 1;
            int cmpSrc = srcCurrIdx;
            int dataRefCompressionLevel = bits;
            int dataRefOffset = 0;
            int repeats = 1;
            while (true) {
                nextSrc += repeats - 1;
                cmpSrc += repeats - 1;
                boolean skip = false;

                int offset;
                while ((offset = info.getWindowArray()[info.getWindowOffset()]) != 0) {
                    nextSrc += offset;
                    info.setWindowOffset(info.getWindowOffset() + offset);

                    if (nextSrc < srcMax && data[nextSrc] == data[srcCurrIdx + repeats] && nextSrc >= cmpSrc) {
                        nextSrc += (1 - repeats);

                        cmpSrc = srcCurrIdx + 2;
                        int cmpFrom = nextSrc + 1;
                        while (cmpSrc < srcMax && data[cmpSrc++] == data[Math.min(data.length - 1, cmpFrom++)])
                            ; // I spent hours debugging how to fix this compared to the C++ version. I tried fixing the C++ version to no avail. Then, somehow... Math.min ended up fixing it. For literally every file I tried, and I tried the full game. I'm pretty suspicious that this is a proper fix, and not just lucky, but I'll take it.
                        cmpFrom--;

                        if (cmpFrom > srcMax) {
                            cmpSrc = cmpSrc - cmpFrom + srcMax;
                            cmpFrom = srcMax;
                        }

                        int currRepeats = (cmpSrc - srcCurrIdx - 1);
                        if (currRepeats > repeats) {
                            int shift = cmpFrom - srcCurrIdx - currRepeats;
                            int currBits = Math.min(info.getCompressionSettings().length - 1, currRepeats - 2);

                            if (info.getMaxCompressionOffsets()[currBits] >= shift) {
                                repeats = currRepeats;
                                // The token (for writing) is changed here. However, it gets restored before any writing is done.
                                dataRefOffset = (shift & 0xFFFF) - 1;
                                dataRefCompressionLevel = currBits;
                            }
                        }

                        skip = true;
                        break;
                    }
                }

                if (skip)
                    continue;

                // Restore old values.
                info.setWindowOffset(oldWindowOffset);

                if (repeats == 1) {
                    writer.writeByte(data[srcCurrIdx]);
                    bits++;
                    prepareDict(1, info);
                    srcCurrIdx = updateSpeedupLarge(data, srcCurrIdx, srcCurrIdx + 1, 1, info);
                    break;
                }

                if (info.getWindowMax() > repeats) {
                    prepareDict(repeats, info);
                    srcCurrIdx = updateSpeedupLarge(data, srcCurrIdx, srcCurrIdx + repeats, repeats, info);
                } else {
                    srcCurrIdx += repeats;

                    info.setWindowOffset(0);
                    info.setWindowLeft(info.getWindowMax());
                    for (int i = 0; i < info.getWindowMax(); i++) {
                        info.getWindowArray()[i] = 0;
                        info.getWindowArray()[info.getWindowMax() + i] = 0;
                    }

                    srcCurrIdx = updateSpeedupLarge(data, srcCurrIdx - info.getWindowLeft(), srcCurrIdx, info.getWindowLeft(), info);
                }

                // Write Control Code (And possibly raw data packer header.)
                if (bits == 0) {
                    writer.writeBit(Utils.flipBit(HAS_RAW_DATA_BIT)); // No Raw Data.
                } else {
                    info.updateByteMargin(writer, srcCurrIdx);
                    writeRawDataPackerHeader(writer, bits); // Yes Raw Data.
                    bits = 0;
                }

                // Write data reference.
                info.updateByteMargin(writer, srcCurrIdx);
                if (repeats > COMPRESSING_SETTING_SIZE) {
                    int repeatValue = (repeats - (PP20Packer.COMPRESSING_SETTING_SIZE + 1));

                    // Write data length.
                    writer.writeBits(repeatValue % PP20Packer.OFFSET_CONTINUE_WRITING_BITS, PP20Packer.OFFSET_BIT_LENGTH);
                    for (int i = 0; i < repeatValue / PP20Packer.OFFSET_CONTINUE_WRITING_BITS; ++i)
                        writer.writeBits(PP20Packer.OFFSET_CONTINUE_WRITING_BITS, PP20Packer.OFFSET_BIT_LENGTH);

                    boolean largeMode = (dataRefOffset >= 0x80); // Offset small mode vs not.
                    writer.writeBits(dataRefOffset, largeMode ? info.getCompressionSettings()[dataRefCompressionLevel] : PP20Packer.OPTIONAL_BITS_SMALL_OFFSET); // Write offset. (Length is deterministic)
                    writer.writeBit(Utils.getBit(largeMode)); // Write whether small offset mode is used.
                } else {
                    // Write offset. (Data length is deterministic based on the compression level, which is also written.)
                    writer.writeBits(dataRefOffset, info.getCompressionSettings()[dataRefCompressionLevel]);
                }

                // Write compression level.
                writer.writeBits(dataRefCompressionLevel, 2);
                break;
            }
        }
        info.updateByteMargin(writer, srcCurrIdx);
        writeRawDataPackerHeader(writer, bits);

        int skippedBits = writer.finishCurrentByte();
        int extraBytes = ((writer.getByteCount() % 4) > 0) ? 4 - (writer.getByteCount() % 4) : 0; // Align by 4 bytes, which is what the real PowerPacker does.
        byte[] byteArray = writer.toByteArray(PP20Packer.MARKER.length() + info.getCompressionSettings().length, 4 + extraBytes);
        byteArray[byteArray.length - 1] = (byte) (skippedBits + (Constants.BITS_PER_BYTE * extraBytes));
        return byteArray;
    }

    private static void writeRawDataPackerHeader(BitWriter writer, int byteLength) {
        int writeLength = byteLength - 1;
        writer.writeBits(writeLength % PP20Packer.INPUT_CONTINUE_WRITING_BITS, PP20Packer.INPUT_BIT_LENGTH);
        for (int i = 0; i < writeLength / PP20Packer.INPUT_CONTINUE_WRITING_BITS; ++i) // Writing number of bytes.
            writer.writeBits(PP20Packer.INPUT_CONTINUE_WRITING_BITS, PP20Packer.INPUT_BIT_LENGTH);

        writer.writeBit(PP20Packer.HAS_RAW_DATA_BIT); // Write raw data marker.
    }

    @Getter
    @Setter
    private static class PackerDataInstance {
        private final byte[] compressionSettings;
        private final short[] maxCompressionOffsets;
        private final short[] windowArray;
        private final int windowMax;
        private int byteMargin;
        private int windowOffset;
        private int windowLeft;
        private final int[] addrs;

        public PackerDataInstance(boolean oldVersion, byte[] compressionSettings) {
            if (compressionSettings == null || compressionSettings.length != 4)
                throw new RuntimeException("Compression Settings should have four entries. Had: " + (compressionSettings != null ? compressionSettings.length : -1));

            this.compressionSettings = Arrays.copyOf(compressionSettings, compressionSettings.length);
            this.maxCompressionOffsets = new short[4];
            for (int i = 0; i < this.maxCompressionOffsets.length; i++)
                this.maxCompressionOffsets[i] = (short) (1 << compressionSettings[i]);

            int multiply = (oldVersion ? 2 : 1);
            this.windowMax = (1 << compressionSettings[3]) * Constants.SHORT_SIZE * multiply;
            this.windowArray = new short[2 * multiply * this.windowMax];
            this.addrs = new int[65536]; // 0x10000. Sized at every possible combination of two chars.
            clear();
        }

        /**
         * Clears all instance data from the packer, readying it for use.
         */
        public void clear() {
            this.windowOffset = 0;
            this.windowLeft = this.windowMax;
            Arrays.fill(this.addrs, Integer.MIN_VALUE);
            Arrays.fill(this.windowArray, (short) 0);
        }

        /**
         * Updates the byte margin.
         * @param writer the writer
         */
        public void updateByteMargin(BitWriter writer, int srcCurrIdx) {
            // Check the documentation for SAFETY_MARGIN_CONSTANT to explain what's going on here.
            int currentByteMargin = (writer.getBytes().size() + 1) + SAFETY_MARGIN_CONSTANT - srcCurrIdx;
            if (currentByteMargin > this.byteMargin)
                this.byteMargin = currentByteMargin;
        }
    }
}