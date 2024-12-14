package net.highwayfrogs.editor.file.reader;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

/**
 * Allows reading from a byte array.
 * Created by Kneesnap on 8/11/2018.
 */
@Getter
public class ArraySource implements DataSource {
    @Setter private int index;
    private byte[] data;

    public ArraySource(byte[] data) {
        this.data = data;
    }

    @Override
    public byte readByte() {
        return data[this.index++];
    }

    @Override
    public byte[] readBytes(int amount) {
        byte[] readBytes = new byte[amount];
        System.arraycopy(this.data, this.index, readBytes, 0, amount);
        this.index += amount;
        return readBytes;
    }

    @Override
    public int readBytes(byte[] output, int offset, int amount) throws IOException {
        amount = Math.max(0, Math.min(amount, this.data.length - this.index));
        if (amount == 0)
            return 0;

        System.arraycopy(this.data, this.index, output, offset, amount);
        this.index += amount;
        return amount;
    }

    @Override
    public void skip(int byteCount) {
        this.index += byteCount;
    }


    @Override
    public int getSize() {
        return data.length;
    }
}