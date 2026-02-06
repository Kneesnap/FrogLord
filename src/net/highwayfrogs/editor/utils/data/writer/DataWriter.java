package net.highwayfrogs.editor.utils.data.writer;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;

/**
 * Allows writing data to a receiver.
 * TODO: Go over this and error when we write stuff outside of the expected values.
 * Created by Kneesnap on 8/10/2018.
 */
public class DataWriter {
    @Getter @Setter private ByteOrder endian = ByteOrder.LITTLE_ENDIAN;
    @Getter private final DataReceiver output;
    private final Stack<Integer> jumpStack = new Stack<>();
    private final Stack<Integer> anchorPoints = new Stack<>();
    private int currentAnchorPoint;

    private static final ByteBuffer FLOAT_BUFFER = ByteBuffer.allocate(Constants.INTEGER_SIZE);

    public DataWriter(DataReceiver output) {
        this.output = output;
    }

    /**
     * Write a given amount of null bytes.
     * @param amount The amount of null bytes to write.
     */
    public void writeNull(int amount) {
        writeByte(Constants.NULL_BYTE, amount);
    }

    /**
     * Write the given byte an arbitrary number of times.
     * @param value The byte value to write.
     * @param amount The number of times to write it.
     */
    public void writeByte(byte value, int amount) {
        int startIndex = getIndex();
        if (amount < 0)
            throw new RuntimeException("Cannot write a byte (" + DataUtils.toByteString(value) + ") " + amount + " times to " + NumberUtils.toHexString(startIndex) + ".");

        try {
            for (int i = 0; i < amount; i++)
                this.output.writeByte(value);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write byte (" + DataUtils.toByteString(value) + ") " + amount + " times to " + NumberUtils.toHexString(startIndex) + ".");
        }
    }

    /**
     * Skip bytes to align to the given byte boundary.
     * @param alignment The number of bytes the index should have an increment of.
     */
    public void align(int alignment) {
        align(alignment, Constants.NULL_BYTE);
    }

    /**
     * Skip bytes to align to the given byte boundary.
     * @param alignment The number of bytes the index should have an increment of.
     * @param padding   The padding byte
     */
    public void align(int alignment, byte padding) {
        int index = getIndex();
        int offsetAmount = (index % alignment);
        if (offsetAmount != 0)
            for (int i = 0; i < alignment - offsetAmount; i++)
                writeByte(padding); // Alignment.
    }
    /**
     * Write null bytes to a given address.
     * @param address The address to end at.
     */
    public void writeTo(int address) {
        writeTo(address, Constants.NULL_BYTE);
    }

    /**
     * Write null bytes to a given address.
     * @param address The address to end at.
     */
    public void writeTo(int address, byte writeByte) {
        int bytes = address - getIndex();
        if (bytes < 0)
            throw new RuntimeException("writeTo cannot write backwards! (" + bytes + ")");

        byte[] byteArray = new byte[bytes];
        if (writeByte != Constants.NULL_BYTE)
            Arrays.fill(byteArray, writeByte);

        writeBytes(byteArray);
    }

    /**
     * Temporarily jump to an offset. Use jumpReturn to return.
     * Jumps are recorded Last In First Out style.
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
     * Set the writer index.
     * @param newIndex The index to write data to.
     */
    public void setIndex(int newIndex) {
        try {
            this.output.setIndex(newIndex + this.currentAnchorPoint);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to set writer index.", ex);
        }
    }

    /**
     * Move a given number of bytes ahead.
     * @param amount The amount of bytes to move.
     */
    public void skipBytes(int amount) {
        setIndex(getIndex() + amount);
    }

    /**
     * Close the DataReceiver from receiving more data. In-case of streams, this safely closes the stream.
     */
    public void closeReceiver() {
        this.output.close();
    }

    /**
     * Gets the writer index.
     * @return index
     */
    public int getIndex() {
        try {
            return this.output.getIndex() - this.currentAnchorPoint;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to get writer index.", ex);
        }
    }

    /**
     * Write a single byte to the receiver.
     * @param value The value to write.
     */
    public void writeByte(byte value) {
        try {
            this.output.writeByte(value);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write a byte to the receiver.", ex);
        }
    }

    /**
     * Write a byte array to the receiver.
     * @param bytes The array of values to write.
     */
    public void writeBytes(byte... bytes) {
        try {
            this.output.writeBytes(bytes);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write a byte-array to the receiver.", ex);
        }
    }

    /**
     * Write a byte array to the receiver.
     * @param array The array of values to write.
     */
    public void writeBytes(byte[] array, int offset, int amount) {
        try {
            this.output.writeBytes(array, offset, amount);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write a byte-array to the receiver.", ex);
        }
    }

    /**
     * Writes an unsigned byte short as an unsigned byte.
     * @param value the short to write. TODO: Accept integer, and throw if outside of range. Throw if a negative value is provided too.
     */
    public void writeUnsignedByte(short value) {
        writeByte(DataUtils.unsignedShortToByte(value));
    }

    /**
     * Writes an unsigned int short as an unsigned short.
     * @param value The int to write.
     */
    public void writeUnsignedShort(int value) {
        writeShort(DataUtils.unsignedIntToShort(value));
    }

    /**
     * Writes an unsigned long int as an unsigned int.
     * @param value The long to write.
     */
    public void writeUnsignedInt(long value) {
        writeInt(DataUtils.unsignedLongToInt(value));
    }

    /**
     * Write a float value to the receiver.
     * @param value The integer to write.
     */
    public void writeFloat(float value) {
        FLOAT_BUFFER.clear();
        writeBytes(FLOAT_BUFFER.order(getEndian()).putFloat(value).array());
    }

    /**
     * Write an integer to the receiver.
     * @param value The integer to write.
     */
    public void writeInt(int value) {
        if (this.endian == ByteOrder.BIG_ENDIAN) {
            writeByte((byte) ((value >> 24) & 0xFF));
            writeByte((byte) ((value >> 16) & 0xFF));
            writeByte((byte) ((value >> 8) & 0xFF));
            writeByte((byte) value);
        } else {
            writeByte((byte) value);
            writeByte((byte) ((value >> 8) & 0xFF));
            writeByte((byte) ((value >> 16) & 0xFF));
            writeByte((byte) ((value >> 24) & 0xFF));
        }
    }

    /**
     * Write a pointer which we'll come back to later.
     * @return address
     */
    public int writeNullPointer() {
        int index = getIndex();
        writeInt(0);
        return index;
    }

    /**
     * Write a short to the receiver.
     * @param value The short to write.
     */
    public void writeShort(short value) {
        if (this.endian == ByteOrder.BIG_ENDIAN) {
            writeByte((byte) ((value >> 8) & 0xFF));
            writeByte((byte) value);
        } else {
            writeByte((byte) value);
            writeByte((byte) ((value >> 8) & 0xFF));
        }
    }

    /**
     * Write a number, taking up a wanted amount of bytes.
     * @param value The number value to write.
     * @param bytes The number of bytes.
     */
    public void writeNumber(Number value, int bytes) {
        if (bytes == Constants.BYTE_SIZE) {
            writeByte(value.byteValue());
        } else if (bytes == Constants.SHORT_SIZE) {
            writeShort(value.shortValue());
        } else if (bytes == Constants.INTEGER_SIZE) {
            writeInt(value.intValue());
        } else {
            throw new RuntimeException("Failed to write " + value + ". Don't know how to write " + bytes + " bytes.");
        }
    }

    /**
     * Write the bytes to a string, then return the amount of bytes written.
     * WARNING: This has no terminator or length, so only use this if you know what you're doing.
     * @param str The string to write.
     * @return byteCount
     */
    public int writeStringBytes(String str) {
        return writeStringBytes(str, StandardCharsets.US_ASCII);
    }

    /**
     * Write the bytes to a string, then return the amount of bytes written.
     * WARNING: This has no terminator or length, so only use this if you know what you're doing.
     * @param str The string to write.
     * @return byteCount
     */
    public int writeStringBytes(String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        writeBytes(bytes);
        return bytes.length;
    }

    /**
     * Writes a string to the receiver, terminated with a null byte.
     * @param str The string to write.
     */
    public void writeNullTerminatedString(String str) {
        writeTerminatorString(str, Constants.NULL_BYTE);
    }

    /**
     * Writes a string to the receiver, terminated with a null byte.
     * @param str The string to write.
     */
    public void writeNullTerminatedString(String str, Charset charset) {
        writeTerminatorString(str, Constants.NULL_BYTE, charset);
    }

    /**
     * Writes a string to the receiver, using a byte as the terminator.
     * @param str        The string to write.
     * @param terminator The terminator. Usually a null byte.
     */
    public void writeTerminatorString(String str, byte terminator) {
        writeTerminatorString(str, terminator, StandardCharsets.US_ASCII);
    }

    /**
     * Writes a string to the receiver, using a byte as the terminator.
     * @param str        The string to write.
     * @param terminator The terminator. Usually a null byte.
     */
    public void writeTerminatorString(String str, byte terminator, Charset charset) {
        if (charset == null)
            throw new NullPointerException("charset");

        if (str != null)
            writeBytes(str.getBytes(charset));
        writeByte(terminator); // Terminator Byte
    }

    /**
     * Write an address.
     * @param writeTo The old address to write the current address to.
     */
    public void writeAddressTo(int writeTo) {
        writeIntAtPos(writeTo, getIndex());
    }

    /**
     * Write an address.
     * @param addressLocation The old address to write the value to.
     * @param pointerValue    The value to write at that location.
     */
    public void writeIntAtPos(int addressLocation, int pointerValue) {
        jumpTemp(addressLocation);
        writeInt(pointerValue);
        jumpReturn();
    }

    /**
     * Writes a null-terminated string to a fixed-length buffer with US_ASCII encoding and null-bytes used as padding
     * @param stringToWrite The string to write
     * @param fixedLength The string buffer fixed size in bytes.
     */
    public void writeNullTerminatedFixedSizeString(String stringToWrite, int fixedLength) {
        writeNullTerminatedFixedSizeString(stringToWrite, StandardCharsets.US_ASCII, fixedLength, Constants.NULL_BYTE);
    }

    /**
     * Writes a null-terminated string to a fixed-length buffer.
     * @param stringToWrite The string to write
     * @param fixedLength The string buffer fixed size in bytes.
     * @param padding The padding byte used to pad empty string data. Generally 0x00.
     */
    public void writeNullTerminatedFixedSizeString(String stringToWrite, int fixedLength, byte padding) {
        writeNullTerminatedFixedSizeString(stringToWrite, StandardCharsets.US_ASCII, fixedLength, padding);
    }

    /**
     * Writes a null-terminated string to a fixed-length buffer.
     * @param stringToWrite The string to write
     * @param charset the charset to encode the string to a byte array with
     * @param fixedLength The string buffer fixed size in bytes.
     */
    public void writeNullTerminatedFixedSizeString(String stringToWrite, Charset charset, int fixedLength) {
        writeNullTerminatedFixedSizeString(stringToWrite, charset, fixedLength, Constants.NULL_BYTE);
    }

    /**
     * Writes a null-terminated string to a fixed-length buffer.
     * @param stringToWrite The string to write. If null is provided, this function will behave as if an empty string had been provided.
     * @param charset the charset to encode the string to a byte array with. This will pretty much always be US_ASCII, as there aren't many other encodings which are null-terminated.
     * @param fixedLength The string buffer fixed size in bytes.
     * @param padding The padding byte used to pad empty string data. Generally 0x00.
     */
    public void writeNullTerminatedFixedSizeString(String stringToWrite, Charset charset, int fixedLength, byte padding) {
        if (charset == null)
            throw new NullPointerException("charset");

        int pathEndIndex = (getIndex() + fixedLength);
        if (stringToWrite != null) {
            byte[] stringBytes = stringToWrite.getBytes(charset);
            if (stringBytes.length > fixedLength)
                throw new RuntimeException("The string '" + stringToWrite + "' is too large to be written. (Allowed: " + fixedLength + " bytes, Actual Size: " + stringBytes.length + " bytes)");

            writeBytes(stringBytes);
        }

        if (pathEndIndex != getIndex()) { // If the string reaches the end, it should be considered cut off because it was too long.
            writeByte(Constants.NULL_BYTE); // Terminator Byte
            writeTo(pathEndIndex, padding);
        }
    }

    /**
     * Push a new anchor point onto the stack.
     * An anchor point is a position relative to the parent anchor point, which when pushed, becomes the new origin (0x00000000) of the data.
     * This helps align data properly, and do pointer math properly.
     */
    public void pushAnchorPoint() {
        pushAnchorPoint(getIndex());
    }

    /**
     * Push a new anchor point onto the stack.
     * An anchor point is a position relative to the parent anchor point, which when pushed, becomes the new origin (0x00000000) of the data.
     * This helps align data properly, and do pointer math properly.
     * @param localIndex the index to push the anchor point for
     */
    public void pushAnchorPoint(int localIndex) {
        int absoluteIndex = this.currentAnchorPoint + localIndex;
        this.anchorPoints.push(this.currentAnchorPoint);
        this.currentAnchorPoint = absoluteIndex;
    }

    /**
     * Pops the most recent anchor point from the stack.
     * An anchor point is a position relative to the parent anchor point, which when pushed, becomes the new origin (0x00000000) of the data.
     * This helps align data properly, and do pointer math properly.
     */
    public void popAnchorPoint() {
        if (this.anchorPoints.isEmpty())
            throw new IllegalStateException("Cannot pop an anchor point when there are none on the stack!");

        this.currentAnchorPoint = this.anchorPoints.pop();
    }
}