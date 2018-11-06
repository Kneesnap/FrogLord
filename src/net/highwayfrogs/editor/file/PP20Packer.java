package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.writer.BitWriter;
import net.highwayfrogs.editor.system.ByteArrayWrapper;
import net.highwayfrogs.editor.system.IntList;

import java.nio.ByteBuffer;

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
    private static final byte[] COMPRESSION_SETTINGS = {0x07, 0x07, 0x07, 0x07}; // PP20 compression settings. Extreme: 0x09, 0x0A, 0x0C, 0x0D
    private static int[] COMPRESSION_SETTING_MAX_OFFSETS;
    public static final int OPTIONAL_BITS_SMALL_OFFSET = 7;
    public static final int INPUT_BIT_LENGTH = 2;
    public static final int INPUT_CONTINUE_WRITING_BITS = 3;
    public static final int OFFSET_BIT_LENGTH = 3;
    public static final int OFFSET_CONTINUE_WRITING_BITS = 7;
    public static final int READ_FROM_INPUT_BIT = Constants.BIT_FALSE;
    public static final int MINIMUM_DECODE_DATA_LENGTH = 2;
    public static final int OPTIONAL_BITS_SMALL_SIZE_MAX_OFFSET = (int) Math.pow(2, OPTIONAL_BITS_SMALL_OFFSET);
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

    private static int findLongest(byte[] data, int bufferEnd, ByteArrayWrapper target, IntList[] dictionary) {
        target.clear();
        byte startByte = data[bufferEnd];
        target.add(startByte);

        IntList possibleResults = dictionary[hashCode(startByte)];
        if (possibleResults == null)
            return -1;

        if (COMPRESSION_SETTING_MAX_OFFSETS == null) { // Generate cached offset values. (This is a rather heavy operation, so we cache the stuff.)
            COMPRESSION_SETTING_MAX_OFFSETS = new int[COMPRESSION_SETTINGS.length + MINIMUM_DECODE_DATA_LENGTH];
            for (int i = 0; i < COMPRESSION_SETTING_MAX_OFFSETS.length; i++)
                COMPRESSION_SETTING_MAX_OFFSETS[i] = getMaximumOffset(i);
        }

        int bestIndex = -1;
        int minIndex = 0;

        for (int resultId = possibleResults.size() - 1; resultId >= 0; resultId--) {
            int testIndex = possibleResults.get(resultId);
            int targetSize = target.size();

            if (COMPRESSION_SETTING_MAX_OFFSETS.length > targetSize) // We'd rather cache this variable, as it's rather expensive to calculate.
                minIndex = Math.max(0, bufferEnd - COMPRESSION_SETTING_MAX_OFFSETS[targetSize]);

            if (minIndex > testIndex)
                break; // We've gone too far.

            boolean existingPass = true;
            for (int i = 1; i < targetSize; i++) {
                if (data[i + bufferEnd] != data[i + testIndex]) {
                    existingPass = false;
                    break;
                }
            }

            if (!existingPass)
                continue; // It didn't match existing data read up to here, it can't be the longest.

            // Grow the target for as long as it matches.
            byte temp;
            int j = targetSize;
            while (data.length > bufferEnd + j && (temp = data[j + bufferEnd]) == data[j + testIndex]) { // Break on reaching end of data, or when the data stops matching.
                target.add(temp);
                j++;
            }

            int newSize = target.size();
            if (newSize >= MINIMUM_DECODE_DATA_LENGTH // Verify large enough that it can be compressed.
                    && newSize != targetSize) // Prefer the ones closest to bufferEnd (Lower offsets = Smaller file). (Ie: They're read first, therefore if we found a duplicate example, we want to rely on the one we've already read.
                bestIndex = testIndex;
        }

        return bestIndex;
    }

    private static byte[] compressData(byte[] data) {
        BitWriter writer = new BitWriter();
        writer.setReverseBytes(true);

        ByteArrayWrapper noMatchQueue = new ByteArrayWrapper(data.length);
        ByteArrayWrapper searchBuffer = new ByteArrayWrapper(data.length);

        IntList[] dictionary = new IntList[256];
        for (int i = 0; i < data.length; i++) {
            byte temp = data[i];

            int bestIndex = findLongest(data, i, searchBuffer, dictionary);
            int byteOffset = i - bestIndex - 1;

            if (bestIndex >= 0) { // Verify the compression index was found.
                if (noMatchQueue.size() > 0) { // When a compressed one has been reached, write all the data in-between, if there is any.
                    writeRawData(writer, noMatchQueue);
                    noMatchQueue.clear();
                } else {
                    writer.writeBit(Utils.flipBit(READ_FROM_INPUT_BIT));
                }

                writeDataReference(writer, searchBuffer.size(), byteOffset);

                for (int byteId = 0; byteId < searchBuffer.size(); byteId++) {
                    int hashCode = hashCode(searchBuffer.get(byteId));
                    IntList list = dictionary[hashCode];
                    if (list == null)
                        dictionary[hashCode] = list = new IntList();

                    list.add(i++);
                }

                i--;
            } else { // It's not large enough to be compressed.
                noMatchQueue.add(temp);

                // Add current byte to the search dictionary.
                int hashCode = hashCode(temp);
                IntList list = dictionary[hashCode];
                if (list == null)
                    dictionary[hashCode] = list = new IntList();

                list.add(i);
            }
        }
        if (noMatchQueue.size() > 0) // Add whatever remains at the end, if there is any.
            writeRawData(writer, noMatchQueue);

        return writer.toByteArray();
    }

    private static int hashCode(byte byteVal) {
        return byteVal >= 0 ? byteVal : (int) Byte.MAX_VALUE - byteVal;
    }

    private static int getMaximumOffset(int byteLength) {
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        int compressionLevel = Math.max(0, Math.min(maxCompressionIndex, byteLength - MINIMUM_DECODE_DATA_LENGTH));
        int offsetSize = COMPRESSION_SETTINGS[compressionLevel];
        return (int) Math.pow(2, offsetSize);
    }

    private static void writeDataReference(BitWriter writer, int byteLength, int byteOffset) {
        // Calculate compression level.
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        int compressionLevel = Math.min(maxCompressionIndex, byteLength - MINIMUM_DECODE_DATA_LENGTH); //TODO: Find a better way of determining this value.

        boolean maxCompression = (compressionLevel == maxCompressionIndex);
        boolean useSmallOffset = maxCompression && OPTIONAL_BITS_SMALL_SIZE_MAX_OFFSET > byteOffset;
        int offsetSize = useSmallOffset ? OPTIONAL_BITS_SMALL_OFFSET : COMPRESSION_SETTINGS[compressionLevel];

        int writeLength = byteLength - compressionLevel;
        writer.writeBits(compressionLevel, 2);

        if (maxCompression) {
            writer.writeBit(useSmallOffset ? Constants.BIT_FALSE : Constants.BIT_TRUE);
            writeLength -= MINIMUM_DECODE_DATA_LENGTH;
        }

        writer.writeBits(byteOffset, offsetSize);

        if (maxCompression) {
            int writtenNum;
            do { // Write the length of the data.
                writtenNum = Math.min(writeLength, PP20Packer.OFFSET_CONTINUE_WRITING_BITS);
                writeLength -= writtenNum;
                writer.writeBits(writtenNum, PP20Packer.OFFSET_BIT_LENGTH);
            } while (writeLength > 0);

            if (writtenNum == PP20Packer.OFFSET_CONTINUE_WRITING_BITS) // Write null terminator if the last value was the "continue" character.
                writer.writeBits(new int[OFFSET_BIT_LENGTH]);
        }
    }

    private static void writeRawData(BitWriter writer, ByteArrayWrapper bytes) {
        writer.writeBit(READ_FROM_INPUT_BIT); // Indicates this should readFromInput, not readFromAbove.

        int writeLength = bytes.size() - 1;
        int writtenNum;

        do { // Write the length of the data.
            writtenNum = Math.min(writeLength, PP20Packer.INPUT_CONTINUE_WRITING_BITS);
            writeLength -= writtenNum;
            writer.writeBits(writtenNum, PP20Packer.INPUT_BIT_LENGTH);
        } while (writeLength > 0);

        if (writtenNum == PP20Packer.INPUT_CONTINUE_WRITING_BITS) // Write null terminator if the last value was the "continue" character.
            writer.writeBits(new int[PP20Packer.INPUT_BIT_LENGTH]);

        for (int i = 0; i < bytes.size(); i++) // Writes the data.
            writer.writeByte(bytes.get(i));
    }
}