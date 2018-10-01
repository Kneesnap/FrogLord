package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Packs a byte array into PP20 compressed data.
 * I was unable to find any code or documentation on how PP20 compresses data.
 * So, this was created from research and attempts to reverse the unpacker.
 *
 * PP20 is a Lz77 (sliding window compression) variant.
 * It resembles LZSS, but not exactly.
 *
 * Useful Links:
 * - https://en.wikipedia.org/wiki/LZ77_and_LZ78
 * - https://eblong.com/zarf/blorb/mod-spec.txt
 *
 * Created by Kneesnap on 8/11/2018.
 */
public class PP20Packer {
    private static final byte[] COMPRESSION_SETTINGS = {0x07, 0x07, 0x07, 0x07}; // PP20 compression settings.
    public static final int OPTIONAL_BITS_SMALL_OFFSET = 7;
    public static final int INPUT_BIT_LENGTH = 2;
    public static final int INPUT_CONTINUE_WRITING_BITS = 3;
    public static final int OFFSET_BIT_LENGTH = 3;
    public static final int OFFSET_CONTINUE_WRITING_BITS = 7;
    public static final int READ_FROM_INPUT_BIT = Constants.BIT_FALSE;
    public static final int MINIMUM_DECODE_DATA_LENGTH = 2;
    public static final String MARKER = "PP20";

    /**
     * Pack a byte array into PP20 compressed data.
     * @param data The data to compress.
     * @return packedData
     */
    public static byte[] packData(byte[] data) {
        data = Utils.reverseCloneByteArray(data);

        // Take the compressed data, and pad it with the file structure. Then, we're done.
        byte[] compressedData = compressData(data);
        byte[] completeData = new byte[compressedData.length + 12];
        System.arraycopy(compressedData, 0, completeData, 8, compressedData.length); // Copy compressed data.

        for (int i = 0; i < MARKER.length(); i++)
            completeData[i] = (byte) MARKER.charAt(i);

        System.arraycopy(COMPRESSION_SETTINGS, 0, completeData, 4, COMPRESSION_SETTINGS.length);
        byte[] array = ByteBuffer.allocate(Constants.INTEGER_SIZE).putInt(data.length).array();
        System.arraycopy(array, 1, completeData, completeData.length - 4, array.length - 1);
        return completeData;
    }

    private static int search(byte[] data, int bufferEnd, List<Byte> target, Map<Byte, List<Integer>> dictionary) {
        int minIndex = Math.max(0, bufferEnd - getMaximumOffset(target.size())); // There's a certain point at which data will not be compressed. By calculating it here, it saves a lot of overheard, and prevents this from becoming O(n^2)
        byte test = target.get(0);

        List<Integer> possibleResults = dictionary.get(test);
        if (possibleResults == null)
            return -1; // No results found.

        for (int i = possibleResults.size() - 1; i >= 0; i--) {
            int testIndex = possibleResults.get(i);
            if (minIndex > testIndex)
                break; // We've gone too far.

            // Test this
            boolean pass = true;
            for (int j = 1; j < target.size(); j++) {
                if (target.get(j) != data[j + testIndex]) {
                    pass = false;
                    break; // Break from the j for loop.
                }
            }

            if (pass) // A match has been found. Return it.
                return testIndex;
        }

        return -1;
    }

    private static byte[] compressData(byte[] data) {
        BitWriter writer = new BitWriter();
        List<Byte> noMatchQueue = new ArrayList<>();
        List<Byte> searchList = new ArrayList<>();

        Map<Byte, List<Integer>> dictionary = new HashMap<>();
        for (int i = 0; i < data.length; i++) {
            byte temp = data[i];

            searchList.clear();
            searchList.add(temp);

            int tempIndex;
            int bestIndex = -1;
            int readIndex = i;

            while ((tempIndex = search(data, i, searchList, dictionary)) >= 0) { // Find the longest compressable bunch of characters.
                bestIndex = tempIndex;
                if (data.length - 1 == readIndex) { // If we've reached the end of the data, exit. Add a null byte to the end because the end of this list will be removed.
                    searchList.add(Constants.NULL_BYTE);
                    break;
                }

                searchList.add(data[++readIndex]);
            }

            searchList.remove(searchList.size() - 1); // Remove the byte that was not found.

            int byteOffset = i - bestIndex - 1;
            if (searchList.size() >= MINIMUM_DECODE_DATA_LENGTH) { // Large enough that it can be compressed.
                if (!noMatchQueue.isEmpty()) { // When a compressed one has been reached, write all the data in-between, if there is any.
                    writeInputData(writer, Utils.toArray(noMatchQueue));
                    noMatchQueue.clear();
                } else {
                    writer.writeBit(Utils.flipBit(READ_FROM_INPUT_BIT));
                }

                writeDataLink(writer, searchList.size(), byteOffset);

                for (int j = 0; j < searchList.size(); j++) {
                    int recordIndex = i + j;
                    byte recordByte = searchList.get(j);
                    if (!dictionary.containsKey(recordByte))
                        dictionary.put(recordByte, new ArrayList<>());
                    dictionary.get(recordByte).add(recordIndex);
                }

                i = readIndex - 1;
            } else { // It's not large enough to be compressed.
                noMatchQueue.add(temp);

                // Add current byte to the search dictionary.
                if (!dictionary.containsKey(temp))
                    dictionary.put(temp, new ArrayList<>());
                dictionary.get(temp).add(i);
            }
        }
        if (!noMatchQueue.isEmpty()) // Add whatever remains at the end, if there is any.
            writeInputData(writer, Utils.toArray(noMatchQueue));

        return writer.toArray();
    }

    private static int getMaximumOffset(int byteLength) {
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        int compressionLevel = Math.max(0, Math.min(maxCompressionIndex, byteLength - MINIMUM_DECODE_DATA_LENGTH));
        int offsetSize = COMPRESSION_SETTINGS[compressionLevel];
        return (int) Math.pow(2, offsetSize);
    }

    private static void writeDataLink(BitWriter writer, int byteLength, int byteOffset) {
        // Calculate compression level.
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        int compressionLevel = Math.min(maxCompressionIndex, byteLength - MINIMUM_DECODE_DATA_LENGTH);

        boolean maxCompression = (compressionLevel == maxCompressionIndex);
        boolean useSmallOffset = maxCompression && Math.pow(2, OPTIONAL_BITS_SMALL_OFFSET) > byteOffset;
        int offsetSize = useSmallOffset ? OPTIONAL_BITS_SMALL_OFFSET : COMPRESSION_SETTINGS[compressionLevel];

        int writeLength = byteLength - compressionLevel;
        writer.writeBits(Utils.getBits(compressionLevel, 2));

        if (maxCompression) {
            writer.writeBit(useSmallOffset ? Constants.BIT_FALSE : Constants.BIT_TRUE);
            writeLength -= MINIMUM_DECODE_DATA_LENGTH;
        }

        writer.writeBits(Utils.getBits(byteOffset, offsetSize));

        if (maxCompression) {
            int writtenNum;
            do { // Write the length of the data.
                writtenNum = Math.min(writeLength, PP20Packer.OFFSET_CONTINUE_WRITING_BITS);
                writeLength -= writtenNum;
                writer.writeBits(Utils.getBits(writtenNum, PP20Packer.OFFSET_BIT_LENGTH));
            } while (writeLength > 0);

            if (writtenNum == PP20Packer.OFFSET_CONTINUE_WRITING_BITS) // Write null terminator if the last value was the "continue" character.
                writer.writeBits(new int[OFFSET_BIT_LENGTH]);
        }
    }

    private static void writeInputData(BitWriter writer, byte[] data) {
        writer.writeBit(READ_FROM_INPUT_BIT); // Indicates this should readFromInput, not readFromAbove.

        int writeLength = data.length - 1;
        int writtenNum;

        do { // Write the length of the data.
            writtenNum = Math.min(writeLength, PP20Packer.INPUT_CONTINUE_WRITING_BITS);
            writeLength -= writtenNum;
            writer.writeBits(Utils.getBits(writtenNum, PP20Packer.INPUT_BIT_LENGTH));
        } while (writeLength > 0);

        if (writtenNum == PP20Packer.INPUT_CONTINUE_WRITING_BITS) // Write null terminator if the last value was the "continue" character.
            writer.writeBits(new int[PP20Packer.INPUT_BIT_LENGTH]);

        for (byte toWrite : data) // Writes the data.
            writer.writeByte(toWrite);
    }

    public static class BitWriter {
        private LinkedList<Byte> bytes = new LinkedList<>();
        private int currentBit = Constants.BITS_PER_BYTE;
        private byte currentByte;

        public void writeBit(int bit) {
            Utils.verify(bit == Constants.BIT_TRUE || bit == Constants.BIT_FALSE, "Invalid bit number %d.", bit);

            // Add the bit to the current byte.
            int shiftedBit = bit << (Constants.BITS_PER_BYTE - this.currentBit);
            this.currentByte |= shiftedBit;

            // If the current byte is complete, add it to the list of bytes.
            if (--this.currentBit == 0) {
                this.bytes.add(this.currentByte);
                this.currentByte = 0;
                this.currentBit = Constants.BITS_PER_BYTE;
            }
        }

        public void writeBits(int[] bits) {
            for (int bit : bits)
                writeBit(bit);
        }

        public void writeByte(byte value) {
            writeBits(Utils.getBits(value, Constants.BITS_PER_BYTE));
        }

        public byte[] toArray() {
            while (this.currentBit != Constants.BITS_PER_BYTE)
                writeBit(0);

            // Write in backwards order, because PP20 does that.
            byte[] arr = new byte[this.bytes.size()];
            int i = arr.length - 1;
            for (Byte aByte : this.bytes)
                arr[i--] = aByte;

            return arr;
        }
    }
}