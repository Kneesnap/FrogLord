package net.highwayfrogs.editor.file.reader;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * A tool for reading information from a data source.
 * Created by Kneesnap on 8/10/2018.
 */
public class DataReader {
    private final DataSource source;
    private final Stack<Integer> jumpStack = new Stack<>();

    public DataReader(DataSource source) {
        this.source = source;
    }

    /**
     * Read the next byte.
     * @return byteValue
     */
    public byte readByte() {
        try {
            return source.readByte();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read byte.", ex);
        }
    }

    /**
     * Set the reader index.
     * @param newIndex The index to read data from.
     */
    public void setIndex(int newIndex) {
        try {
            source.setIndex(newIndex);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to set reader index.", ex);
        }
    }

    /**
     * Gets the current reader index.
     * @return readerIndex
     */
    public int getIndex() {
        try {
            return source.getIndex();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to get reader index.", ex);
        }
    }

    /**
     * Get the amount of readable bytes.
     * @return size
     */
    public int getSize() {
        try {
            return source.getSize();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to get source size.", ex);
        }
    }

    /**
     * Checks if there are more readable bytes.
     * @return moreBytes
     */
    public boolean hasMore() {
        return getSize() > getIndex();
    }

    /**
     * Get the amount of remaining bytes.
     * @return remainingCount
     */
    public int getRemaining() {
        return getSize() - getIndex();
    }

    /**
     * Temporarily jump to an offset. Use jumpReturn to return.
     * Jumps are recorded Last in First Out style.
     * @param newIndex The offset to jump to.
     */
    public void jumpTemp(int newIndex) {
        this.jumpStack.add(getIndex());
        setIndex(newIndex);
    }

    /**
     * Return to the offset before jumpTemp was called.
     */
    public void jumpReturn() {
        setIndex(this.jumpStack.pop());
    }

    /**
     * Reads a single byte as an unsigned int.
     * @return unsignedByte
     */
    public int readUnsignedByte() {
        return Byte.toUnsignedInt(readByte());
    }

    /**
     * Reads a single byte as an unsigned byte, and returns it as a short. Since the java byte is -127 to 128, this allows us to read a byte as the appropriate value.
     * @return unsignedByteShort.
     */
    public short readUnsignedByteAsShort() {
        return Utils.byteToUnsignedShort(readByte());
    }

    /**
     * Reads a single short as an unsigned short, and returns it as a int. Since the java byte is -65534 to 65535, this allows us to read a short as the appropriate value.
     * @return unsignedShortInt.
     */
    public int readUnsignedShortAsInt() {
        return Utils.shortToUnsignedInt(readShort());
    }

    /**
     * Reads a single integer as an unsigned integer, returning it as a long.
     * @return unsignedIntLong
     */
    public long readUnsignedIntAsLong() {
        return Utils.intToUnsignedLong(readInt());
    }

    /**
     * Read the next bytes as a float32.
     * Reads four bytes.
     * @return floatValue
     */
    public float readFloat() {
        return Utils.readFloatFromBytes(readBytes(Constants.FLOAT_SIZE));
    }

    /**
     * Read the next bytes as an integer.
     * Reads four bytes.
     * @return intValue
     */
    public int readInt() {
        return readInt(Constants.INTEGER_SIZE);
    }

    /**
     * Read the next bytes as a short.
     * Reads two bytes.
     * @return shortValue
     */
    public short readShort() {
        short value = 0;
        for (int i = 0; i < Constants.SHORT_SIZE; i++)
            value += ((long) readByte() & 0xFFL) << (Constants.BITS_PER_BYTE * i);
        return value;
    }

    /**
     * Read a variable number of bytes into an integer.
     * @param bytes The number of bytes to read.
     * @return intValue
     */
    public int readInt(int bytes) {
        int value = 0;
        for (int i = 0; i < bytes; i++)
            value += ((long) readByte() & 0xFFL) << (Constants.BITS_PER_BYTE * i);
        return value;
    }

    /**
     * Read a string of a pre-specified length.
     * @param length The length of the string.
     * @return readStr
     */
    public String readString(int length) {
        return readString(length, StandardCharsets.US_ASCII);
    }

    /**
     * Read a string of a pre-specified length.
     * @param length The length of the string.
     * @return readStr
     */
    public String readString(int length, Charset charset) {
        return new String(readBytes(length), charset);
    }


    /**
     * Verify the next few bytes match a string.
     * @param verify The string to verify.
     */
    public void verifyString(String verify) {
        String str = readString(verify.getBytes().length);
        Utils.verify(str.equals(verify), "String verify failure! \"%s\" does not match \"%s\".", str, verify);
    }

    /**
     * Requires the reader to be at a given index.
     * @param logger the logger to warn if not at the index.
     * @param desiredIndex the index to require.
     * @param messagePrefix The message to print for a warning.
     */
    public void requireIndex(Logger logger, int desiredIndex, String messagePrefix) {
        if (getIndex() != desiredIndex) {
            logger.warning(messagePrefix + " at " + Utils.toHexString(getIndex()) + ", but it actually started at " + Utils.toHexString(desiredIndex) + ".");
            setIndex(desiredIndex);
        }
    }

    /**
     * Read a string until a given byte is found.
     * @param terminator The byte which terminates the string. Usually 0.
     * @return strValue
     */
    public String readString(byte terminator) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            byte[] temp = new byte[1];
            while ((temp[0] = readByte()) != terminator)
                out.write(temp);
            out.close();
            return out.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read terminating string.", ex);
        }
    }

    /**
     * Read a string of a fixed size that has a terminator byte if it's smaller than that size.
     * @param length The fixed string size.
     * @return readString
     */
    public String readTerminatedStringOfLength(int length) {
        return readTerminatedStringOfLength(length, Constants.NULL_BYTE);
    }

    /**
     * Read a string of a fixed size that has a terminator byte if it's smaller than that size.
     * @param length     The fixed string size.
     * @param terminator The terminator byte.
     * @return readString
     */
    public String readTerminatedStringOfLength(int length, byte terminator) {
        jumpTemp(getIndex());
        String result = readString(terminator);
        jumpReturn();
        skipBytes(length);

        if (result.length() > length)
            result = result.substring(0, length);
        return result;
    }

    /**
     * Read a string which is terminated by a null byte.
     * @return strValue
     */
    public String readNullTerminatedString() {
        return readString(Constants.NULL_BYTE);
    }

    /**
     * Read bytes from the source, respecting endian.
     * @return readBytes
     */
    public byte[] readBytes(byte[] destination) {
        for (int i = 0; i < destination.length; i++)
            destination[i] = readByte();
        return destination;
    }

    /**
     * Read bytes from the source, respecting endian.
     * @param amount The amount of bytes to read.
     * @return readBytes
     */
    public byte[] readBytes(int amount) {
        try {
            return source.readBytes(amount);
        } catch (Exception ex) {
            throw new RuntimeException("Error while reading " + amount + " bytes. (Remaining: " + getRemaining() + ")", ex);
        }
    }

    /**
     * Skips bytes from the source.
     * @param amount The amount of bytes to skip.
     */
    public void skipBytes(int amount) {
        try {
            source.skip(amount);
        } catch (Exception ex) {
            throw new RuntimeException("Error while skipping " + amount + " bytes. (Remaining: " + getRemaining() + ")", ex);
        }
    }

    /**
     * Skip bytes, requiring the bytes skipped be 0.
     * @param amount The number of bytes to skip.
     */
    public void skipBytesRequireEmpty(int amount) {
        skipBytesRequire(Constants.NULL_BYTE, amount);
    }

    /**
     * Skip bytes, requiring the bytes skipped be 0.
     * @param amount The number of bytes to skip.
     */
    public void skipBytesRequire(byte expected, int amount) {
        int index = getIndex();
        if (amount == 0)
            return;

        if (amount < 0)
            throw new RuntimeException("Tried to skip " + amount + " bytes.");

        // Skip bytes.
        for (int i = 0; i < amount; i++) {
            byte nextByte = readByte();
            if (nextByte != expected)
                throw new RuntimeException("Reader wanted to skip " + amount + " bytes to reach " + Utils.toHexString(index + amount) + ", but got 0x" + Utils.toByteString(nextByte) + " at " + Utils.toHexString(index + i) + " when 0x" + Utils.toByteString(expected) + " was desired.");
        }
    }

    /**
     * Skip bytes to align to the given byte boundary.
     * @param alignment The number of bytes the index should have an increment of.
     */
    public void align(int alignment) {
        int index = getIndex();
        int offsetAmount = (index % alignment);
        if (offsetAmount != 0)
            skipBytes(alignment - offsetAmount); // Alignment.
    }

    /**
     * Skip bytes to align to the given byte boundary, requiring the bytes skipped be 0.
     * @param alignment The number of bytes the index should have an increment of.
     */
    public void alignRequireEmpty(int alignment) {
        alignRequireByte(Constants.NULL_BYTE, alignment);
    }

    /**
     * Skip bytes to align to the given byte boundary, requiring the bytes skipped be 0.
     * @param alignment The number of bytes the index should have an increment of.
     */
    public void alignRequireByte(byte value, int alignment) {
        int index = getIndex();
        int offsetAmount = (index % alignment);
        if (offsetAmount != 0)
            skipBytesRequire(value, alignment - offsetAmount);
    }

    /**
     * Skip the amount of bytes an pointer takes up.
     */
    public void skipPointer() {
        skipBytes(Constants.POINTER_SIZE);
    }

    /**
     * Skip the amount of bytes an integer takes up.
     */
    public void skipInt() {
        skipBytes(Constants.INTEGER_SIZE);
    }

    /**
     * Skip the amount of bytes a short takes up.
     */
    public void skipShort() {
        skipBytes(Constants.SHORT_SIZE);
    }

    /**
     * Skip the amount of bytes a single byte takes up.
     */
    public void skipByte() {
        skipBytes(Constants.BYTE_SIZE);
    }

    /**
     * Create a sub-reader.
     * @param startOffset The offset to start reading from.
     * @param length      The length to read. -1 = Get remaining.
     * @return newReader
     */
    public DataReader newReader(int startOffset, int length) {
        jumpTemp(startOffset);
        byte[] bytes = readBytes(length >= 0 ? length : getRemaining());
        jumpReturn();

        return new DataReader(new ArraySource(bytes));
    }
}