package net.highwayfrogs.editor.file.writer;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * An in-memory data receiver.
 * Created by Kneesnap on 8/13/2018.
 */
@Getter
public class ArrayReceiver implements DataReceiver {
    private List<Byte> bytes = new ArrayList<>();

    @Override
    public void writeByte(byte value)  {
        bytes.add(value);
    }

    @Override
    public void writeBytes(byte[] values) {
        for (byte value : values)
            writeByte(value);
    }

    @Override
    public int getIndex() {
        return bytes.size();
    }

    /**
     * Get the array of bytes.
     * @return array
     */
    public byte[] toArray() {
        byte[] array = new byte[bytes.size()];
        for (int i = 0; i < array.length; i++)
            array[i] = bytes.get(i);
        return array;
    }
}
