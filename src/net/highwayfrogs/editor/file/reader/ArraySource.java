package net.highwayfrogs.editor.file.reader;

import lombok.Getter;
import lombok.Setter;

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
    public void skip(int byteCount) {
        this.index += byteCount;
    }


    @Override
    public int getSize() {
        return data.length;
    }
}
