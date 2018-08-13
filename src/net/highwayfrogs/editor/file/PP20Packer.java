package net.highwayfrogs.editor.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Packs a byte array into PP20 compressed data.
 * I was unable to find a compression sub-routine (Even in C) which does this function.
 * So, this will/is being created from the documentation given below, and trying to reverse the unpacker.
 * <p>
 * Work backwards.
 * Useful Links:
 * - https://en.wikipedia.org/wiki/Lempel–Ziv–Welch
 * - https://eblong.com/zarf/blorb/mod-spec.txt
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
    public static final int READ_FROM_INPUT_BIT = 0;

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

        System.arraycopy(COMPRESSION_SETTINGS, 0, completeData, 4, COMPRESSION_SETTINGS.length);
        byte[] array = ByteBuffer.allocate(Constants.INTEGER_SIZE).putInt(data.length - 11).array();
        System.arraycopy(array, 1, completeData, completeData.length - 5, array.length - 1);
        completeData[completeData.length - 1] += 7; //TODO: Include real bit skip info.
        return completeData;
    }

    private static byte[] compressData(byte[] data) {
        BitWriter writer = new BitWriter();
        writeInputData(writer, new ByteArrayWrapper(data));

        Map<ByteArrayWrapper, Integer> dictionary = new HashMap<>();

        /*List<ByteArrayWrapper> wrapperList = new ArrayList<>();
        ByteArrayWrapper sequence = new ByteArrayWrapper(new byte[0]);
        int index = 0;
        for (int i = 0; i < data.length; i++) {
            byte temp = data[i];

            ByteArrayWrapper checkMatch = sequence.appendNew(temp);
            if (dictionary.containsKey(checkMatch)) {
                 sequence = checkMatch;
            } else {
                wrapperList.add(sequence); //TODO: May not work for starting characters. (Is this why we check that the data isn't length 1?)
                if (!dictionary.containsKey(sequence))
                    dictionary.put(sequence, index);

                dictionary.put(checkMatch, index); //TODO: Verify this is right.
                sequence = new ByteArrayWrapper(temp);
                index = i;
            }
        }
        System.out.println("Dictionary Size: " + dictionary.size());
        System.out.println("Data Size: " + data.length);

        //Find the longest string with > 1 use. (If there is only one
        index = 0;
        for (ByteArrayWrapper wrapper : wrapperList) {
            int firstIndex = dictionary.get(wrapper);

            if (wrapper.getData().length == 1 || firstIndex != index) { //TODO
                writeInputData(writer, wrapper);
            } else {
                writeDataLink(writer, wrapper, dictionary, index);
            }
        }*/

        // Map<String, Integer> <Substr, Index>
        // Need to know: up will go decompressed.
        // str = String to add.

        //if (strLength == 1) or (str is the first occurance) or (Distance is past the compression setting)
        //  writeBit(0) -> copyFromInput
        //  write(strLength - 1) (0 -> writeBit(00). 1 -> writeBit(01). 2 -> writeBit(10). 3 -> writeBit(11) writeBit(00) 4 -> writeBit(11) writeBit(01). 5 -> writeBit(11) writeBit(10). 6 -> writeBit(11) writeBit(11) writeBit(00)
        //  writeBytes(strBytes)
        //else
        //  writeBit(1) -> skip copyFromInput
        //  int byteLength
        //  writeBits(..?) <Length,Written> -> <0, 00>, <1, 01>, <2, 10>, <3+, 11> This writes the compression level.
        //  wroteBit = Bit from last write. Compression setting?
        //  if (wroteBit == 3)
        //    writeBit(?) 0 = If the offset can fit in 7 bits. 1 = If the offset can not fit in 7 bits. If this is the case, it will use the compression level.
        //  writeBits(offset) (Make sure offset matches with offset length.)
        //  if (compressionSetting == 3) // wroteBit == 3
        //    writeBit(addToByteLength) addToByteLength = (byteLength - 3) (3 Bits) 0 -> 000. 1 -> 001. 2 -> 010. 3 -> 011. 4 -> 100. 5 -> 101. 6 -> 110. 7 -> 111 000 (Make sure each three bits are grouped together in a writeBits call. But, if you have one that needs more than three bits, do it in multiple writeBits calls.)
        //
        return writer.toArray();
    }

    private static void writeDataLink(BitWriter writer, ByteArrayWrapper wrapper, Map<ByteArrayWrapper, Integer> dictionary, int index) {
        int byteOffset = index - dictionary.get(wrapper);
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        Utils.verify(Math.pow(2, COMPRESSION_SETTINGS[maxCompressionIndex]) > byteOffset, "Past max distance: %d.", byteOffset); //TODO: Max distance: COMPRESSION_SETTINGS

        writer.writeBit(Utils.flipBit(READ_FROM_INPUT_BIT));

        int byteLength = wrapper.getData().length;
        int compressionLevel = Math.min(maxCompressionIndex, byteLength);
        writer.writeBits(Utils.getBits(compressionLevel, 2));

        int offsetSize = COMPRESSION_SETTINGS[compressionLevel];
        if (compressionLevel == maxCompressionIndex) {
            boolean useSetting = byteOffset >= Math.pow(2, DEFAULT_OFFSET_BITS);
            writer.writeBit(useSetting ? Constants.BIT_TRUE : Constants.BIT_FALSE);
            if (!useSetting)
                offsetSize = DEFAULT_OFFSET_BITS;
        }

        writer.writeBits(Utils.getBits(byteOffset, offsetSize));

        int writeLength = wrapper.getData().length - compressionLevel;
        int writtenNum;
        do { // Write the length of the data.
            writtenNum = Math.min(writeLength, PP20Packer.WRITE_LENGTH_CONTINUE_DECODE);
            writeLength -= writtenNum;
            writer.writeBits(Utils.getBits(writtenNum, PP20Packer.LENGTH_BIT_INTERVAL_DECODE));
        } while (writeLength > 0);

        if (writtenNum == PP20Packer.WRITE_LENGTH_CONTINUE_DECODE) // Write null terminator if the last value was the "continue" character.
            writer.writeBits(new int[LENGTH_BIT_INTERVAL_DECODE]);
    }

    private static void writeInputData(BitWriter writer, ByteArrayWrapper wrapper) {
        writer.writeBit(READ_FROM_INPUT_BIT); // Indicates this should readFromInput, not readFromAbove.

        int writeLength = wrapper.getData().length - 1;
        int writtenNum;

        System.out.println("Writing Length: " + writeLength);
        do { // Write the length of the data.
            writtenNum = Math.min(writeLength, PP20Packer.WRITE_LENGTH_CONTINUE);
            writeLength -= writtenNum;
            writer.writeBits(Utils.getBits(writtenNum, PP20Packer.LENGTH_BIT_INTERVAL));
        } while (writeLength > 0);

        if (writtenNum == PP20Packer.WRITE_LENGTH_CONTINUE) // Write null terminator if the last value was the "continue" character.
            writer.writeBits(new int[PP20Packer.LENGTH_BIT_INTERVAL]);

        for (byte toWrite : wrapper.getData()) // Writes the data.
            writer.writeByte(toWrite);
    }

    @Getter
    @AllArgsConstructor
    // byte[] can't be used as a key in a HashMap, because of equals and hashCode. This class wraps around byte[] allowing us to use it in its place.
    public static class ByteArrayWrapper {
        private final byte[] data;

        public ByteArrayWrapper(byte data) {
            this(new byte[]{data});
        }

        public ByteArrayWrapper appendNew(byte append) {
            byte[] newData = new byte[data.length + 1];
            System.arraycopy(this.data, 0, newData, 0, data.length);
            newData[newData.length - 1] = append;
            return new ByteArrayWrapper(newData);
        }

        @Override
        public boolean equals(Object other) {
            return Arrays.equals(this.data, ((ByteArrayWrapper) other).getData());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.data);
        }
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
            int count = 0; //TODO
            while (this.currentBit != Constants.BITS_PER_BYTE) {
                writeBit(0);
                count++;
            }
            System.out.println("Added " + count + " bits.");

            byte[] arr = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++)
                arr[i] = bytes.get(i);
            return arr;
        }
    }
}