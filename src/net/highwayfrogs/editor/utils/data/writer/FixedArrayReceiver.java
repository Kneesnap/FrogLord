package net.highwayfrogs.editor.utils.data.writer;

import lombok.Getter;

import java.io.IOException;

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
    public void writeBytes(byte[] values, int offset, int amount) throws IOException {
        amount = Math.max(0, Math.min(amount, values.length - offset - 1));
        if (amount == 0)
            return;

        for (int i = 0; i < amount; i++)
            writeByte(values[offset + i]);
    }

    @Override
    public void setIndex(int newIndex) {
        this.index = newIndex;
    }
}