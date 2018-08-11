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
}
