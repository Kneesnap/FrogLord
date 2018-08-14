package net.highwayfrogs.editor.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Packs a byte array into PP20 compressed data.
 * I was unable to find a compression sub-routine (Even in C) which encodes data.
 * So, this was created from research and attempts to reverse the unpacke.
 *
 * Appears to be loosely based on LZ77.
 *
 * Useful Links:
 * - https://en.wikipedia.org/wiki/Lempel–Ziv–Welch
 * - https://eblong.com/zarf/blorb/mod-spec.txt
 *
 * TODO: Compression.
 * TODO: Cleanup
 * Created by Kneesnap on 8/11/2018.
 */
public class PP20Packer {
    private static final String MARKER = "PP20";
    private static final byte[] COMPRESSION_SETTINGS = {0x09, 0x0A, 0x0C, 0x0D}; // PP20 compression settings.
    private static final int DEFAULT_OFFSET_BITS = 7;
    public static final int LENGTH_BIT_INTERVAL = 2;
    public static final int LENGTH_BIT_INTERVAL_DECODE = 3;
    public static final int WRITE_LENGTH_CONTINUE = 3;
    public static final int WRITE_LENGTH_CONTINUE_DECODE = 7;
    public static final int READ_FROM_INPUT_BIT = Constants.BIT_FALSE;
    public static final int MINIMUM_DECODE_DATA_LENGTH = 2;

    /**
     * Pack a byte array into PP20 compressed data.
     * @param data The data to compress.
     * @return packedData
     */
    public static byte[] packData(byte[] data) {
        for (int i = 0; i < data.length / 2; i++) { // Reverse the byte order.
            byte temp = data[i];
            data[i] = data[data.length - 1 - i];
            data[data.length - 1 - i] = temp;
        }

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

    private static int search(byte[] data, int bufferEnd, List<Byte> target) { //TODO: Test.
        for (int search = bufferEnd - target.size(); search >= 0; search--) { // Search for anywhere in the buffer.
            boolean pass = true;
            for (int i = 0; i < target.size(); i++)
                if (target.get(i) != data[search + i])
                    pass = false;

            if (pass) {
                return search;
            }
        }

        return -1;
    }

    private static byte[] compressData(byte[] data) {
        BitWriter writer = new BitWriter();

        // Search buffer is the left-side of the string. [Symbols we've already seen and already processed.]
        // The Look Ahead buffer is the right side of the string. [Contains symbols we haven't seen yet. 10s of symbols long.]

        // Encoder reads a symbol from the LA buffer, and attempt to find a match in the search buffer.
        // If it's found, read more from the Look Ahead buffer, and search backwards in the search buffer until it finds the longest match.
        //   When that longest match is found, it is now the token. <Offset, Length>
        //   Shift the window (Buffer seperator) to right after the token we just compressed. (Not the original token)
        // Else, if the backwards search has no match, or we're seeing the search for the first time.
        //   Until one is found, compound all the missing ones together, and write with writeInput.

        List<Byte> noMatchQueue = new ArrayList<>();
        List<Byte> searchList = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            byte temp = data[i];

            searchList.clear();
            searchList.add(temp);

            int tempIndex;
            int bestIndex = -1;
            int readIndex = i;

            while ((tempIndex = search(data, i, searchList)) >= 0) { // Find the longest compressable bunch of characters.
                bestIndex = tempIndex;
                if (data.length - 1 == readIndex) { // If we've reached the end of the data, exit. Add a null byte to the end because the end of this list will be removed.
                    searchList.add(Constants.NULL_BYTE);
                    break;
                }

                searchList.add(data[++readIndex]);
            }

            searchList.remove(searchList.size() - 1); // Remove the byte that was not found.

            if (searchList.size() >= MINIMUM_DECODE_DATA_LENGTH) { // Large enough that it can be compressed.
                boolean writeQueue = !noMatchQueue.isEmpty();
                if (writeQueue) { // When a compressed one has been reached, write all the data in-between, if there is any.
                    writeInputData(writer, Utils.toArray(noMatchQueue));
                    noMatchQueue.clear();
                }

                if (writeDataLink(writer, Utils.toArray(searchList), i - bestIndex - 1, !writeQueue)) { // Data was written,
                    i = readIndex - 1;
                } else {
                    noMatchQueue.add(temp);
                }
            } else { // It's not large enough to be compressed.
                noMatchQueue.add(temp);
            }
        }
        if (!noMatchQueue.isEmpty()) // Add whatever remains at the end, if there is any.
            writeInputData(writer, Utils.toArray(noMatchQueue));

        System.out.println("INFO STUFF: " + writer.currentByte + ", " + writer.currentBit);
        return writer.toArray();
    }

    //TODO: Make this accept the length, instead of the data itself, to save on memory. (After debugging.)
    private static boolean writeDataLink(BitWriter writer, byte[] data, int byteOffset, boolean writeBit) {
        int byteLength = data.length;

        // Calculate compression level.
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        int compressionLevel = Math.min(maxCompressionIndex, byteLength - MINIMUM_DECODE_DATA_LENGTH);

        boolean maxCompression = (compressionLevel == maxCompressionIndex);
        boolean useSmallOffset = maxCompression && Math.pow(2, DEFAULT_OFFSET_BITS) > byteOffset;
        int offsetSize = useSmallOffset ? DEFAULT_OFFSET_BITS : COMPRESSION_SETTINGS[compressionLevel];
        if (byteOffset >= Math.pow(2, offsetSize)) // Past the max distance. Instead, add the bytes to the queue.
            return false;

        if (writeBit) // Should this write that there was no new data?
            writer.writeBit(Utils.flipBit(READ_FROM_INPUT_BIT));

        int writeLength = byteLength - compressionLevel;
        writer.writeBits(Utils.getBits(compressionLevel, 2));

        if (maxCompression) {
            writer.writeBit(useSmallOffset ? Constants.BIT_FALSE : Constants.BIT_TRUE);
            writeLength -= MINIMUM_DECODE_DATA_LENGTH;
        }

        writer.writeBits(Utils.getBits(byteOffset, offsetSize));

        System.out.println("Writing Compressed: " + byteLength + " Real String: " + new String(data));

        if (maxCompression) {
            int writtenNum;
            do { // Write the length of the data.
                writtenNum = Math.min(writeLength, PP20Packer.WRITE_LENGTH_CONTINUE_DECODE);
                writeLength -= writtenNum;
                writer.writeBits(Utils.getBits(writtenNum, PP20Packer.LENGTH_BIT_INTERVAL_DECODE));
            } while (writeLength > 0);

            if (writtenNum == PP20Packer.WRITE_LENGTH_CONTINUE_DECODE) // Write null terminator if the last value was the "continue" character.
                writer.writeBits(new int[LENGTH_BIT_INTERVAL_DECODE]);
        }
        return true;
    }

    private static void writeInputData(BitWriter writer, byte[] data) {
        writer.writeBit(READ_FROM_INPUT_BIT); // Indicates this should readFromInput, not readFromAbove.

        int writeLength = data.length - 1;
        int writtenNum;

        do { // Write the length of the data.
            writtenNum = Math.min(writeLength, PP20Packer.WRITE_LENGTH_CONTINUE);
            writeLength -= writtenNum;
            writer.writeBits(Utils.getBits(writtenNum, PP20Packer.LENGTH_BIT_INTERVAL));
        } while (writeLength > 0);

        if (writtenNum == PP20Packer.WRITE_LENGTH_CONTINUE) // Write null terminator if the last value was the "continue" character.
            writer.writeBits(new int[PP20Packer.LENGTH_BIT_INTERVAL]);

        System.out.println("Writing Input Data: " + new String(data));
        for (byte toWrite : data) // Writes the data.
            writer.writeByte(toWrite);
    }

    public static class BitWriter {
        private List<Byte> bytes = new ArrayList<>();
        private int currentBit = Constants.BITS_PER_BYTE;
        private byte currentByte;

        public void writeBit(int bit) {
            Utils.verify(bit == 1 || bit == 0, "Invalid bit number %d.", bit);

            // Add the bit to the current byte.
            int shiftedBit = bit << (Constants.BITS_PER_BYTE - this.currentBit);
            this.currentByte |= shiftedBit;

            // If the current byte is complete, add it to the list of bytes.
            if (--this.currentBit == 0) {
                this.bytes.add(0, this.currentByte);
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

            byte[] arr = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++)
                arr[i] = bytes.get(i);
            return arr;
        }
    }
}