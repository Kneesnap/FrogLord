package net.highwayfrogs.editor.file.reader;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

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
        return DataUtils.byteToUnsignedShort(readByte());
    }

    /**
     * Reads a single short as an unsigned short, and returns it as a int. Since the java byte is -65534 to 65535, this allows us to read a short as the appropriate value.
     * @return unsignedShortInt.
     */
    public int readUnsignedShortAsInt() {
        return DataUtils.shortToUnsignedInt(readShort());
    }

    /**
     * Reads a single integer as an unsigned integer, returning it as a long.
     * @return unsignedIntLong
     */
    public long readUnsignedIntAsLong() {
        return DataUtils.intToUnsignedLong(readInt());
    }

    /**
     * Read the next bytes as a float32.
     * Reads four bytes.
     * @return floatValue
     */
    public float readFloat() {
        return DataUtils.readFloatFromBytes(readBytes(Constants.FLOAT_SIZE));
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
            value += (short) ((readByte() & 0xFF) << (Constants.BITS_PER_BYTE * i));
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
            value += (readByte() & 0xFF) << (Constants.BITS_PER_BYTE * i);
        return value;
    }

    /**
     * Read a string of a pre-specified length.
     * @param length The length of the string.
     * @return readStr
     */
    public String readTerminatedString(int length) {
        return readTerminatedString(length, StandardCharsets.US_ASCII);
    }

    /**
     * Read a string of a pre-specified length.
     * @param length The length of the string.
     * @return readStr
     */
    public String readTerminatedString(int length, Charset charset) {
        return new String(readBytes(length), charset);
    }

    /**
     * Verify the next few bytes match a string.
     * @param verify The string to verify.
     */
    public void verifyString(String verify) {
        String str = readTerminatedString(verify.getBytes().length);
        Utils.verify(str.equals(verify), "String verify failure! \"%s\" does not match \"%s\".", str, verify);
    }

    /**
     * Requires the reader to be at a given index.
     * @param logger the logger to warn if not at the index.
     * @param desiredIndex the index to require.
     * @param messagePrefix The message to print for a warning.
     */
    public void requireIndex(ILogger logger, int desiredIndex, String messagePrefix) {
        if (getIndex() != desiredIndex) {
            logger.warning(messagePrefix + " at " + NumberUtils.toHexString(getIndex()) + ", but it actually started at " + NumberUtils.toHexString(desiredIndex) + ".");
            setIndex(desiredIndex);
        }
    }

    /**
     * Read a string until a given byte is found.
     * @param terminator The byte which terminates the string. Usually 0.
     * @return strValue
     */
    public String readTerminatedString(byte terminator, Charset charset) {
        if (charset == null)
            throw new NullPointerException("charset");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            byte[] temp = new byte[1];
            while ((temp[0] = readByte()) != terminator)
                out.write(temp);
            out.close();
            return out.toString(charset.name());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read terminating string.", ex);
        }
    }

    /**
     * Reads a null-terminated string from a fixed-length buffer with US_ASCII encoding and null-bytes used as padding.
     * @param fixedLength The string buffer fixed size in bytes.
     */
    public String readNullTerminatedFixedSizeString(int fixedLength) {
        return readNullTerminatedFixedSizeString(StandardCharsets.US_ASCII, fixedLength);
    }

    /**
     * Reads a null-terminated string from a fixed-length buffer.
     * @param fixedLength The string buffer fixed size in bytes.
     * @param padding The padding byte used expected to fill the empty string data. Generally 0x00.
     */
    public String readNullTerminatedFixedSizeString(int fixedLength, byte padding) {
        return readNullTerminatedFixedSizeString(StandardCharsets.US_ASCII, fixedLength, padding);
    }

    /**
     * Reads a null-terminated string from a fixed-length buffer.
     * This will not validate the padding bytes.
     * @param charset the charset to decode the string from a byte array with. This will pretty much always be US_ASCII, as there aren't many other encodings which are null-terminated.
     * @param fixedLength The string buffer fixed size in bytes.
     */
    public String readNullTerminatedFixedSizeString(Charset charset, int fixedLength) {
        if (charset == null)
            throw new NullPointerException("charset");
        if (fixedLength < 0)
            throw new IllegalArgumentException("Cannot read a string with a fixed buffer size of " + fixedLength + ".");

        int startIndex = getIndex();
        int expectedEndIndex = startIndex + fixedLength;
        String result = readTerminatedString(Constants.NULL_BYTE, charset);
        if (getIndex() > expectedEndIndex) {
            result = result.substring(0, fixedLength);
            setIndex(expectedEndIndex);
        } else {
            skipBytes(expectedEndIndex - getIndex());
        }

        return result;
    }

    /**
     * Reads a null-terminated string from a fixed-length buffer.
     * @param charset the charset to decode the string from a byte array with. This will pretty much always be US_ASCII, as there aren't many other encodings which are null-terminated.
     * @param fixedLength The string buffer fixed size in bytes.
     * @param padding The padding byte used to pad empty string data. An exception will be thrown if the subsequent data does not match this byte.
     */
    public String readNullTerminatedFixedSizeString(Charset charset, int fixedLength, byte padding) {
        if (charset == null)
            throw new NullPointerException("charset");
        if (fixedLength < 0)
            throw new IllegalArgumentException("Cannot read a string with a fixed buffer size of " + fixedLength + ".");

        int startIndex = getIndex();
        int expectedEndIndex = startIndex + fixedLength;
        String result = readTerminatedString(Constants.NULL_BYTE, charset);
        if (getIndex() > expectedEndIndex) {
            result = result.substring(0, fixedLength);
            setIndex(expectedEndIndex);
        } else {
            skipBytesRequire(padding, expectedEndIndex - getIndex());
        }

        return result;
    }

    /**
     * Read a string which is terminated by a null byte.
     * @return strValue
     */
    public String readNullTerminatedString() {
        return readTerminatedString(Constants.NULL_BYTE, StandardCharsets.US_ASCII);
    }

    /**
     * Read a string which is terminated by a null byte.
     * @return strValue
     */
    public String readNullTerminatedString(Charset charset) {
        return readTerminatedString(Constants.NULL_BYTE, charset);
    }

    /**
     * Read bytes from the source, respecting endian.
     * @return readBytes
     */
    public byte[] readBytes(byte[] destination, int offset, int amount) {
        try {
            int bytesRead = 0;
            int lastReadAmount;
            while (amount > bytesRead && (lastReadAmount = this.source.readBytes(destination, offset + bytesRead, amount - bytesRead)) > 0)
                bytesRead += lastReadAmount;

            if (bytesRead != amount)
                throw new RuntimeException("Failed to read " + amount + " bytes, as only " + bytesRead + " bytes were actually available to read.");
        } catch (Exception ex) {
            int remainingBytes = getRemaining();
            if (remainingBytes > 0) {
                throw new RuntimeException("Error while reading " + amount + " bytes. (It seems there is an issue with " + Utils.getSimpleName(this.source) + ", because it still reports having " + remainingBytes + " bytes available)", ex);
            } else {
                throw new RuntimeException("Error while reading " + amount + " bytes. (Bytes Remaining: " + remainingBytes + ")", ex);
            }
        }

        return destination;
    }

    /**
     * Read bytes from the source, respecting endian.
     * @return readBytes
     */
    public byte[] readBytes(byte[] destination) {
        return readBytes(destination, 0, destination.length);
    }

    /**
     * Read bytes from the source, respecting endian.
     * @param amount The amount of bytes to read.
     * @return readBytes
     */
    public byte[] readBytes(int amount) {
        try {
            return this.source.readBytes(amount);
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
            this.source.skip(amount);
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
                throw new RuntimeException("Reader wanted to skip " + amount + " bytes to reach " + NumberUtils.toHexString(index + amount) + ", but got 0x" + DataUtils.toByteString(nextByte) + " at " + NumberUtils.toHexString(index + i) + " when 0x" + DataUtils.toByteString(expected) + " was desired.");
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