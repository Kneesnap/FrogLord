package net.highwayfrogs.editor.file.writer;

import java.io.IOException;

/**
 * Represents a consumer of data where information can be saved.
 * Created by Kneesnap on 8/10/2018.
 */
public interface DataReceiver {

    /**
     * Write a single byte to this receiver.
     * @param value The value to write.
     */
    public void writeByte(byte value) throws IOException;

    /**
     * Write an array of bytes to this receiver.
     * @param values The bytes to write.
     */
    public void writeBytes(byte[] values) throws IOException;

    /**
     * Write an array of bytes to this receiver.
     * @param values The bytes to write.
     * @param offset the offset into the array.
     * @param amount the amount of bytes to write.
     */
    public void writeBytes(byte[] values, int offset, int amount) throws IOException;

    /**
     * Set the current write index.
     * @param newIndex The new index to write data at.
     */
    public void setIndex(int newIndex) throws IOException;

    /**
     * Get the current write index.
     * @return writeIndex
     */
    public int getIndex() throws IOException;

    /**
     * Close this receiver. Should be called when all data has been written.
     */
    default void close() {

    }
}