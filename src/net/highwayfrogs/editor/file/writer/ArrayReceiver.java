package net.highwayfrogs.editor.file.writer;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;

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
        return Utils.toArray(bytes);
    }
}
