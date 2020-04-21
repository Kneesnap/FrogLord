package net.highwayfrogs.editor.file.writer;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Stack;

/**
 * Allows writing data to a receiver.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class DataWriter {
    @Setter private ByteOrder endian = ByteOrder.LITTLE_ENDIAN;
    private DataReceiver output;
    private Stack<Integer> jumpStack = new Stack<>();

    private static final ByteBuffer INT_BUFFER = ByteBuffer.allocate(Constants.INTEGER_SIZE);
    private static final ByteBuffer SHORT_BUFFER = ByteBuffer.allocate(Constants.SHORT_SIZE);

    public DataWriter(DataReceiver output) {
        this.output = output;
    }

    /**
     * Write a given amount of null bytes.
     * @param amount The amount of null bytes to write.
     */
    public void writeNull(int amount) {
        writeBytes(new byte[amount]);
    }

    /**
     * Jump to a given write offset, leaving null-bytes in between.
     * @param address The address to jump to.
     */
    public void jumpTo(int address) {
        int index = getIndex();
        Utils.verify(address >= index, "Tried to jump to %s, which is before the current writer address (%s).", Integer.toHexString(address), Integer.toHexString(index));
        writeNull(address - index);
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
            for (int i = 0; i < byteArray.length; i++)
                byteArray[i] = writeByte;

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
            output.setIndex(newIndex);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to set writer index.", ex);
        }
    }

    /**
     * Close the DataReceiver from receiving more data. In-case of streams, this safely closes the stream.
     */
    public void closeReceiver() {
        output.close();
    }

    /**
     * Gets the writer index.
     * @return index
     */
    public int getIndex() {
        try {
            return output.getIndex();
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
            output.writeByte(value);
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
            output.writeBytes(bytes);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write a byte-array to the receiver.", ex);
        }
    }

    /**
     * Writes an unsigned byte short as an unsigned byte.
     * @param value the short to write.
     */
    public void writeUnsignedByte(short value) {
        writeByte(Utils.unsignedShortToByte(value));
    }

    /**
     * Writes an unsigned int short as an unsigned short.
     * @param value The int to write.
     */
    public void writeUnsignedShort(int value) {
        writeShort(Utils.unsignedIntToShort(value));
    }

    /**
     * Writes an unsigned long int as an unsigned int.
     * @param value The long to write.
     */
    public void writeUnsignedInt(long value) {
        writeInt(Utils.unsignedLongToInt(value));
    }

    /**
     * Write a float value to the receiver.
     * @param value The integer to write.
     */
    public void writeFloat(float value) {
        INT_BUFFER.clear();
        writeBytes(INT_BUFFER.order(getEndian()).putFloat(value).array());
    }

    /**
     * Write an integer to the receiver.
     * @param value The integer to write.
     */
    public void writeInt(int value) {
        INT_BUFFER.clear();
        writeBytes(INT_BUFFER.order(getEndian()).putInt(value).array());
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
        SHORT_BUFFER.clear();
        writeBytes(SHORT_BUFFER.order(getEndian()).putShort(value).array());
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
        byte[] bytes = str.getBytes();
        writeBytes(bytes);
        return bytes.length;
    }

    /**
     * Writes a string to the receiver, terminated with a null byte.
     * @param str The string to write.
     */
    public void writeTerminatorString(String str) {
        writeTerminatorString(str, Constants.NULL_BYTE);
    }

    /**
     * Writes a string to the receiver, using a byte as the terminator.
     * @param str        The string to write.
     * @param terminator The terminator. Usually a null byte.
     */
    public void writeTerminatorString(String str, byte terminator) {
        writeBytes(str.getBytes());
        writeByte(terminator);
    }

    /**
     * Write an address.
     * @param writeTo The old address to write the current address to.
     */
    public void writeAddressTo(int writeTo) {
        writeAddressAt(writeTo, getIndex());
    }

    /**
     * Write an address.
     * @param addressLocation The old address to write the value to.
     * @param pointerValue    The value to write at that location.
     */
    public void writeAddressAt(int addressLocation, int pointerValue) {
        jumpTemp(addressLocation);
        writeInt(pointerValue);
        jumpReturn();
    }

    /**
     * Writes a terminated string of a given length.
     * @param stringToWrite The string to write
     * @param byteSize      The string fixed size.
     */
    public void writeTerminatedStringOfLength(String stringToWrite, int byteSize) {
        writeTerminatedStringOfLength(stringToWrite, byteSize, Constants.NULL_BYTE);
    }

    /**
     * Writes a terminated string of a given length.
     * @param stringToWrite The string to write
     * @param byteSize      The string fixed size.
     */
    public void writeTerminatedStringOfLength(String stringToWrite, int byteSize, byte terminator) {
        int pathEndIndex = (getIndex() + byteSize);
        writeTerminatorString(stringToWrite); // Include the null byte after the string data before using 0xCD.
        writeTo(pathEndIndex, terminator);
    }
}
