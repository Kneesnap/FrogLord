package net.highwayfrogs.editor.utils.data.reader;

import java.io.IOException;

/**
 * Represents a source of data where information can be read.
 * Created by Kneesnap on 8/10/2018.
 */
public interface DataSource {

    /**
     * Read the next byte from the current index.
     * @return nextByte
     */
    public byte readByte() throws IOException;

    /**
     * Read a given amount of bytes into an array.
     * @param amount The amount of bytes to read.
     * @return byteArray
     */
    public byte[] readBytes(int amount) throws IOException;

    /**
     * Read a given amount of bytes into an array.
     * @param output the output byte array to read bytes into.
     * @param offset The offset into the array to place bytes at.
     * @param amount The amount of bytes to read.
     * @return amount of bytes actually read.
     */
    public int readBytes(byte[] output, int offset, int amount) throws IOException;

    /**
     * Skip a given number of bytes, from the current index.
     * @param byteCount The bytes to skip.
     */
    public void skip(int byteCount) throws IOException;

    /**
     * Set the current read index to a value.
     * @param newIndex The new index to read from.
     */
    public void setIndex(int newIndex) throws IOException;

    /**
     * Gets the current read index.
     * @return readIndex
     */
    public int getIndex() throws IOException;

    /**
     * Gets the total size of readable data.
     * @return size
     */
    public int getSize() throws IOException;
}