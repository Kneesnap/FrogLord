package net.highwayfrogs.editor.file.packers;

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
 * This packer is not thread-safe, but it wouldn't take much effort to make it thread-safe.
 *
 * PP20 is a LZSS variant.
 *
 * Useful Links:
 * - https://en.wikipedia.org/wiki/LZ77_and_LZ78
 * - https://en.wikipedia.org/wiki/Lempel%E2%80%93Ziv%E2%80%93Storer%E2%80%93Szymanski
 * - https://eblong.com/zarf/blorb/mod-spec.txt
 * - https://books.google.com/books?id=ujnQogzx_2EC&printsec=frontcover (Specifically, the section about how LzSS improves upon Lz77)
 * - https://www.programcreek.com/java-api-examples/index.php?source_dir=trie4j-master/trie4j/src/kitchensink/java/org/trie4j/lz/LZSS.java
 * Created by Kneesnap on 8/11/2018.
 */
public class PP20Packer {
    private static final byte[] COMPRESSION_SETTINGS = {0x07, 0x07, 0x07, 0x07}; // PP20 compression settings. Extreme: 0x09, 0x0A, 0x0C, 0x0D
    private static final int MAX_COMPRESSION_INDEX = COMPRESSION_SETTINGS.length - 1;
    private static int[] COMPRESSION_SETTING_MAX_OFFSETS;
    public static final int OPTIONAL_BITS_SMALL_OFFSET = 7;
    public static final int INPUT_BIT_LENGTH = 2;
    public static final int INPUT_CONTINUE_WRITING_BITS = 3;
    public static final int OFFSET_BIT_LENGTH = 3;
    public static final int OFFSET_CONTINUE_WRITING_BITS = 7;
    public static final int HAS_RAW_DATA_BIT = Constants.BIT_FALSE;
    public static final int MINIMUM_DECODE_DATA_LENGTH = 2;
    public static final int COMPRESSION_LEVEL_BITS = 2;
    public static final int OPTIONAL_BITS_SMALL_SIZE_MAX_OFFSET = Utils.power(2, OPTIONAL_BITS_SMALL_OFFSET);
    public static final String MARKER = "PP20";
    public static final byte[] MARKER_BYTES = MARKER.getBytes();

    private static final IntList[] DICTIONARY = new IntList[256];
    private static final ByteBuffer INT_BUFFER = ByteBuffer.allocate(Constants.INTEGER_SIZE);
    private static final ByteArrayWrapper searchBuffer = new ByteArrayWrapper(0);
    private static final ByteArrayWrapper noMatchQueue = new ByteArrayWrapper(0);

    /**
     * Pack a byte array into PP20 compressed data.
     * @param data The data to compress.
     * @return packedData
     */
    public static byte[] packData(byte[] data) {
        Utils.reverseByteArray(data); // Does this cause problems?

        // Take the compressed data, and pad it with the file structure. Then, we're done.
        byte[] compressedData = compressData(data);
        System.arraycopy(MARKER_BYTES, 0, compressedData, 0, MARKER_BYTES.length);
        System.arraycopy(COMPRESSION_SETTINGS, 0, compressedData, 4, COMPRESSION_SETTINGS.length);

        INT_BUFFER.clear();
        System.arraycopy(INT_BUFFER.putInt(data.length).array(), 1, compressedData, compressedData.length - 4, Constants.INTEGER_SIZE - 1);
        Utils.reverseByteArray(data); // Makes sure the input array's contents have no net change when this method finishes.
        return compressedData;
    }

    private static int findLongest(byte[] data, int bufferEnd, ByteArrayWrapper target) {
        target.clear();
        byte startByte = data[bufferEnd];
        target.add(startByte);

        IntList possibleResults = DICTIONARY[hashCode(startByte)];
        if (possibleResults == null)
            return -1;

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
            if (newSize > targetSize) // Prefer the ones closest to bufferEnd (Lower offsets = Smaller file). (Ie: They're read first, therefore if we found a duplicate example, we want to rely on the one we've already read.
                bestIndex = testIndex;
        }

        return target.size() >= MINIMUM_DECODE_DATA_LENGTH ? bestIndex : -1;
    }

    private static byte[] compressData(byte[] data) {
        if (COMPRESSION_SETTING_MAX_OFFSETS == null) { // Generate cached offset values. (This is a rather heavy operation, so we cache the stuff.)
            COMPRESSION_SETTING_MAX_OFFSETS = new int[COMPRESSION_SETTINGS.length + MINIMUM_DECODE_DATA_LENGTH];
            for (int i = 0; i < COMPRESSION_SETTING_MAX_OFFSETS.length; i++)
                COMPRESSION_SETTING_MAX_OFFSETS[i] = getMaximumOffset(i);
        }

        // Clear dictionary.
        for (IntList list : DICTIONARY)
            if (list != null)
                list.clear();

        BitWriter writer = new BitWriter();
        writer.setReverseBytes(true);

        noMatchQueue.clearExpand(data.length);
        searchBuffer.clearExpand(data.length);

        for (int i = 0; i < data.length; i++) {
            int bestIndex = findLongest(data, i, searchBuffer);

            if (bestIndex >= 0) { // Verify the compression index was found.
                boolean hasRawData = (noMatchQueue.size() > 0);

                // Write Input Data.
                writer.writeBit(Utils.getBit(!hasRawData)); // Marks if there is raw data present or not.
                if (hasRawData) { // When a compressed one has been reached, write all the data in-between, if there is any.
                    writeRawData(writer, noMatchQueue);
                    noMatchQueue.clear();
                }

                writeDataReference(writer, searchBuffer.size(), i - bestIndex - 1);
                for (int byteId = 0; byteId < searchBuffer.size(); byteId++) // Add to dictionary.
                    addToDictionary(searchBuffer.get(byteId), i++);

                i--;
            } else { // It's not large enough to be compressed.
                byte temp = data[i];
                noMatchQueue.add(temp);
                addToDictionary(temp, i); // Add current byte to the search dictionary.
            }
        }

        if (noMatchQueue.size() > 0) { // Add whatever remains at the end, if there is any.
            writer.writeBit(HAS_RAW_DATA_BIT);
            writeRawData(writer, noMatchQueue);
        }

        return writer.toByteArray(8, 4);
    }

    private static void addToDictionary(byte byteValue, int index) {
        int hashCode = hashCode(byteValue);
        IntList list = DICTIONARY[hashCode];
        if (list == null)
            DICTIONARY[hashCode] = list = new IntList();

        list.add(index);
    }

    private static int hashCode(byte byteVal) {
        return Utils.getUnsignedByte(byteVal);
    }

    private static int getMaximumOffset(int byteLength) {
        return Utils.power(2, COMPRESSION_SETTINGS[getCompressionLevel(byteLength)]);
    }

    private static int getCompressionLevel(int byteLength) {
        return Math.max(0, Math.min(MAX_COMPRESSION_INDEX, byteLength - MINIMUM_DECODE_DATA_LENGTH));
    }

    private static void writeDataReference(BitWriter writer, int byteLength, int byteOffset) {
        // Calculate compression level.
        int compressionLevel = getCompressionLevel(byteLength);

        boolean maxCompression = (compressionLevel == MAX_COMPRESSION_INDEX);
        boolean useSmallOffset = maxCompression && OPTIONAL_BITS_SMALL_SIZE_MAX_OFFSET > byteOffset;
        int offsetSize = useSmallOffset ? OPTIONAL_BITS_SMALL_OFFSET : COMPRESSION_SETTINGS[compressionLevel];

        writer.writeBits(compressionLevel, COMPRESSION_LEVEL_BITS);
        if (maxCompression)
            writer.writeBit(Utils.getBit(!useSmallOffset));

        writer.writeBits(byteOffset, offsetSize);

        if (maxCompression) {
            int writeLength = byteLength - compressionLevel - MINIMUM_DECODE_DATA_LENGTH;
            int writtenNum;
            do { // Write the length of the data.
                writtenNum = Math.min(writeLength, PP20Packer.OFFSET_CONTINUE_WRITING_BITS);
                writeLength -= writtenNum;
                writer.writeBits(writtenNum, PP20Packer.OFFSET_BIT_LENGTH);
            } while (writeLength > 0);

            if (writtenNum == PP20Packer.OFFSET_CONTINUE_WRITING_BITS) // Write null terminator if the last value was the "continue" character.
                writer.writeFalseBits(OFFSET_BIT_LENGTH);
        }
    }

    private static void writeRawData(BitWriter writer, ByteArrayWrapper bytes) {
        int writeLength = bytes.size() - 1;
        int writtenNum;

        do { // Write the length of the data.
            writtenNum = Math.min(writeLength, PP20Packer.INPUT_CONTINUE_WRITING_BITS);
            writeLength -= writtenNum;
            writer.writeBits(writtenNum, PP20Packer.INPUT_BIT_LENGTH);
        } while (writeLength > 0);

        if (writtenNum == PP20Packer.INPUT_CONTINUE_WRITING_BITS) // Write null terminator if the last value was the "continue" character.
            writer.writeFalseBits(PP20Packer.INPUT_BIT_LENGTH);

        for (int i = 0; i < bytes.size(); i++) // Writes the data.
            writer.writeByte(bytes.get(i));
    }
}