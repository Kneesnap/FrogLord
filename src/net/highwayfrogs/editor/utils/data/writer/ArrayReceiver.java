package net.highwayfrogs.editor.utils.data.writer;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.system.ByteArrayWrapper;

import java.io.IOException;

/**
 * An in-memory data receiver.
 * Created by Kneesnap on 8/13/2018.
 */
@Getter
public class ArrayReceiver implements DataReceiver {
    private final ByteArrayWrapper byteBuffer;
    private int index;

    public ArrayReceiver() {
        this.byteBuffer = new ByteArrayWrapper();
    }

    public ArrayReceiver(int startingSize) {
        this.byteBuffer = new ByteArrayWrapper(startingSize, true);
    }

    @Override
    public void writeByte(byte value)  {
        while (this.index > this.byteBuffer.size()) // Add data up to the index.
            this.byteBuffer.add(Constants.NULL_BYTE);

        if (this.byteBuffer.size() > this.index) { // Writing over existing bytes.
            this.byteBuffer.set(this.index, value);
        } else { // Index matches exactly. Append at end.
            this.byteBuffer.add(value);
        }

        this.index++; // Increment index.
    }

    @Override
    public void writeBytes(byte[] values) throws IOException {
        writeBytes(values, 0, values.length);
    }

    @Override
    public void writeBytes(byte[] values, int offset, int amount) throws IOException {
        this.byteBuffer.set(this.index, values, offset, amount);
        this.index += amount;
    }

    @Override
    public void setIndex(int newIndex) {
        this.index = newIndex;
    }

    /**
     * Get the array of bytes.
     * @return array
     */
    public byte[] toArray() {
        return this.byteBuffer.toNewArray();
    }
}