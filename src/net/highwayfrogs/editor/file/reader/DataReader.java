package net.highwayfrogs.editor.file.reader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A tool for reading information from a data source.
 * Created by Kneesnap on 8/10/2018.
 */
public class DataReader {
    private DataSource source;
    private int oldAddress = -1;

    public DataReader(DataSource source) {
        this.source = source;
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
     * Temporarily jump to an offset. Use jumpReturn to return.
     * @param newIndex The offset to jump to.
     */
    public void jumpTemp(int newIndex) {
        try {
            this.oldAddress = getIndex();
            source.setIndex(newIndex);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to temporarily jump to an offset.", ex);
        }
    }

    /**
     * Return to the offset before jumpTemp was called.
     */
    public void jumpReturn() {
        if (oldAddress < 0)
            return;

        try {
            source.setIndex(this.oldAddress);
            this.oldAddress = -1;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to return to previous address.", ex);
        }
    }


    /**
     * Read the next bytes as an integer.
     * Reads four bytes.
     * @return intValue
     */
    public int readInt() {
        byte[] data = readBytes(4);
        int value = 0;
        for (int i = 0; i < data.length; i++)
            value += ((long) data[i] & 0xFFL) << (8 * i);
        return value;
    }
    /**
     * Read a string of a pre-specified length.
     * @param length The length of the string.
     * @return readStr
     */
    public String readString(int length) {
        return new String(readBytes(length));
    }

    /**
     * Read a string until a given byte is found.
     * @param terminator The byte which terminates the string. Usually 0.
     * @return readStr
     */
    public String readString(byte terminator) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            byte[] temp = new byte[1];
            while ((temp[0] = source.readByte()) != terminator)
                out.write(temp);
            out.close();
            return out.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read terminating string.", ex);
        }
    }

    /**
     * Read bytes from the source, respecting endian.
     * @param amount The amount of bytes to read.
     * @return readBytes
     */
    public byte[] readBytes(int amount) {

        byte[] array;
        try {
            array = source.readBytes(amount);
        } catch (IOException ex) {
            throw new RuntimeException("Error while reading bytes.", ex);
        }

        return array;
    }
}
