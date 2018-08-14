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

    private static byte[] compressData(byte[] data) {
        BitWriter writer = new BitWriter();

        // Wrote
        writeDataLink(writer, new ByteArrayWrapper(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0e}), new HashMap<>(), 0);

        //writeInputData(writer, new ByteArrayWrapper(data));

        // Search buffer is the left-side of the string. [Symbols we've already seen and already processed.]
        // The Look Ahead buffer is the right side of the string. [Contains symbols we haven't seen yet. 10s of symbols long.]

        // Encoder reads a symbol from the LA buffer, and attempt to find a match in the search buffer.
        // If it's found, read more from the Look Ahead buffer, and search backwards in the search buffer until it finds the longest match.
        //   When that longest match is found, it is now the token. <Offset, Length, Next Byte after Symbol>
        //   Shift the window (Buffer seperator) to right after the token we just compressed. (Not the original token)
        // Else, if the backwards search has no match, or we're seeing the search for the first time.
        //   Until one is found, compound all the missing ones together, and write with writeInput.

        //TODO: Will this work with the decode requirement?

        /*Map<ByteArrayWrapper, Integer> dictionary = new HashMap<>();

        List<ByteArrayWrapper> wrapperList = new ArrayList<>();
        ByteArrayWrapper sequence = new ByteArrayWrapper(new byte[0]);
        int index = 0; //TODO: If data is one character, add a second one and try to build another dictionary input.
        for (int i = 0; i < data.length; i++) { //TODO: Each iteration is a starting point.
            byte temp = data[i];

            ByteArrayWrapper checkMatch = sequence.appendNew(temp);
            if (dictionary.containsKey(checkMatch)) {
                 sequence = checkMatch;
            } else {
                if (sequence.getData().length > 0)
                    wrapperList.add(sequence); //TODO: May not work for starting characters.
                dictionary.putIfAbsent(sequence, index);
                dictionary.putIfAbsent(checkMatch, index); //TODO: Verify this is right.

                sequence = new ByteArrayWrapper(temp);
                index = i;
            }
        }

        index = 0;
        for (ByteArrayWrapper wrapper : wrapperList) {
            int firstIndex = dictionary.get(wrapper);

            if (wrapper.getData().length == 1 || firstIndex != index) { //TODO: Distance compare check.
                writeInputData(writer, wrapper);
                writeBlankLink(writer);
            } else {
                writeDataLink(writer, wrapper, dictionary, index);
            }

            index++;
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

    //TODO: Verify this works fully.
    private static void writeDataLink(BitWriter writer, ByteArrayWrapper wrapper, Map<ByteArrayWrapper, Integer> dictionary, int index) {
        int byteOffset = 128; // index - dictionary.get(wrapper); // TODO UNDO
        int byteLength = wrapper.getData().length;

        // Calculate compression level.
        int maxCompressionIndex = COMPRESSION_SETTINGS.length - 1;
        int compressionLevel = Math.min(maxCompressionIndex, byteLength - MINIMUM_DECODE_DATA_LENGTH);

        boolean maxCompression = (compressionLevel == maxCompressionIndex);
        boolean useSmallOffset = maxCompression && Math.pow(2, DEFAULT_OFFSET_BITS) > byteOffset;
        int offsetSize = useSmallOffset ? DEFAULT_OFFSET_BITS : COMPRESSION_SETTINGS[compressionLevel];

        Utils.verify(Math.pow(2, offsetSize) > byteOffset, "Past max distance: %d.", byteOffset);

        int writeLength = byteLength - compressionLevel;

        writer.writeBit(Utils.flipBit(READ_FROM_INPUT_BIT));
        writer.writeBits(Utils.getBits(compressionLevel, 2));
        System.out.println("Wrote Compression Level: " + compressionLevel);


        if (maxCompression) {
            writer.writeBit(useSmallOffset ? Constants.BIT_FALSE : Constants.BIT_TRUE);
            writeLength -= MINIMUM_DECODE_DATA_LENGTH;
        }

        System.out.println("Wrote Byte Offset " + byteOffset + " of bit-size: " + offsetSize + " bits.");
        writer.writeBits(Utils.getBits(byteOffset, offsetSize));

        int writtenNum;
        System.out.println("Writing Length: " + byteLength);
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
            return other instanceof ByteArrayWrapper && Arrays.equals(this.data, ((ByteArrayWrapper) other).getData());
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
            while (this.currentBit != Constants.BITS_PER_BYTE)
                writeBit(0);

            byte[] arr = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++)
                arr[i] = bytes.get(i);
            return arr;
        }
    }
}