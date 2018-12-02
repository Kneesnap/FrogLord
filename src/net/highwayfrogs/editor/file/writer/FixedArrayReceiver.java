package net.highwayfrogs.editor.file.writer;

import lombok.Getter;

/**
 * An in-memory data receiver.
 * Created by Kneesnap on 12/1/2018.
 */
@Getter
public class FixedArrayReceiver implements DataReceiver {
    private byte[] array;
    private int index;

    public FixedArrayReceiver(byte[] array) {
        this.array = array;
    }

    @Override
    public void writeByte(byte value) {
        this.array[this.index++] = value;
    }

    @Override
    public void writeBytes(byte[] values) {
        for (byte value : values)
            writeByte(value);
    }

    @Override
    public void setIndex(int newIndex) {
        this.index = newIndex;
    }
}
