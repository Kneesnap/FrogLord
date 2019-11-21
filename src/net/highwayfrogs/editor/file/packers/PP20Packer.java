package net.highwayfrogs.editor.file.packers;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.writer.BitWriter;
import net.highwayfrogs.editor.system.ByteArrayWrapper;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.Utils;

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
 * - http://michael.dipperstein.com/lzss/
 * - https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm
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
    private static final ThreadLocal<PackerDataInstance> dataPerThread = ThreadLocal.withInitial(PackerDataInstance::new);
    public static final int MAX_UNCOMPRESSED_FILE_SIZE = Utils.power(2, 3 * Constants.BYTE_SIZE) - 1; // Has 3 bytes to store this info in.

    /**
     * Pack a byte array into PP20 compressed data.
     * @param data The data to compress.
     * @return packedData
     */
    public static byte[] packData(byte[] data) {
        if (data.length > MAX_UNCOMPRESSED_FILE_SIZE)
            throw new RuntimeException("packData tried to compress data larger than the maximum PP20 file size! (" + data.length + " > " + MAX_UNCOMPRESSED_FILE_SIZE + ")!");

        Utils.reverseByteArray(data); // Does this cause problems?
        PackerDataInstance packerData = dataPerThread.get();

        // Take the compressed data, and pad it with the file structure. Then, we're done.
        byte[] compressedData = compressData(data, packerData);
        System.arraycopy(MARKER_BYTES, 0, compressedData, 0, MARKER_BYTES.length);
        System.arraycopy(COMPRESSION_SETTINGS, 0, compressedData, 4, COMPRESSION_SETTINGS.length);

        System.arraycopy(packerData.getIntBytes(data.length), 1, compressedData, compressedData.length - 4, Constants.INTEGER_SIZE - 1);
        Utils.reverseByteArray(data); // Makes sure the input array's contents have no net change when this method finishes.
        return compressedData;
    }

    private static int findLongest(PackerDataInstance packerData, byte[] data, int bufferEnd) {
        ByteArrayWrapper target = packerData.getSearchBuffer();

        target.clear();
        byte startByte = data[bufferEnd];
        target.add(startByte);

        IntList possibleResults = packerData.getDictionary()[hashCode(startByte)];
        if (possibleResults == null)
            return -1;

        int bestIndex = -1;
        for (int resultId = possibleResults.size() - 1; resultId >= 0; resultId--) {
            int testIndex = possibleResults.get(resultId);
            int targetSize = target.size();
            int minIndex = Math.max(0, bufferEnd - COMPRESSION_SETTING_MAX_OFFSETS[Math.min(COMPRESSION_SETTING_MAX_OFFSETS.length - 1, targetSize - 1)]);

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

    private static byte[] compressData(byte[] data, PackerDataInstance packerData) {
        if (COMPRESSION_SETTING_MAX_OFFSETS == null) { // Generate cached offset values. (This is a rather heavy operation, so we cache the stuff.)
            COMPRESSION_SETTING_MAX_OFFSETS = new int[COMPRESSION_SETTINGS.length + MINIMUM_DECODE_DATA_LENGTH];
            for (int i = 0; i < COMPRESSION_SETTING_MAX_OFFSETS.length; i++)
                COMPRESSION_SETTING_MAX_OFFSETS[i] = getMaximumOffset(i);
        }

        packerData.setup(data.length);
        BitWriter writer = new BitWriter();
        writer.setReverseBytes(true);

        ByteArrayWrapper searchBuffer = packerData.getSearchBuffer();
        ByteArrayWrapper noMatchQueue = packerData.getNoMatchQueue();

        for (int i = 0; i < data.length; i++) {
            int bestIndex = findLongest(packerData, data, i);

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
                    packerData.addToDictionary(searchBuffer.get(byteId), i++);

                i--;
            } else { // It's not large enough to be compressed.
                byte temp = data[i];
                noMatchQueue.add(temp);
                packerData.addToDictionary(temp, i); // Add current byte to the search dictionary.
            }
        }

        if (noMatchQueue.size() > 0) { // Add whatever remains at the end, if there is any.
            writer.writeBit(HAS_RAW_DATA_BIT);
            writeRawData(writer, noMatchQueue);
        }

        return writer.toByteArray(8, 4);
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

    @Getter
    private static class PackerDataInstance {
        private final IntList[] dictionary = new IntList[256];
        private final ByteBuffer intBuffer = ByteBuffer.allocate(Constants.INTEGER_SIZE);
        private final ByteArrayWrapper searchBuffer = new ByteArrayWrapper(0);
        private final ByteArrayWrapper noMatchQueue = new ByteArrayWrapper(0);

        public byte[] getIntBytes(int number) {
            intBuffer.clear();
            return intBuffer.putInt(number).array();
        }

        public void setup(int uncompressedLength) {
            // Clear dictionary.
            for (int i = 0; i < dictionary.length; i++)
                if (dictionary[i] != null)
                    dictionary[i].clear();

            noMatchQueue.clearExpand(uncompressedLength);
            searchBuffer.clearExpand(uncompressedLength);
        }

        public void addToDictionary(byte byteValue, int index) {
            int hashCode = PP20Packer.hashCode(byteValue);
            IntList list = dictionary[hashCode];
            if (list == null)
                dictionary[hashCode] = list = new IntList();

            list.add(index);
        }
    }
}