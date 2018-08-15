package net.highwayfrogs.editor.file;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.nio.ByteBuffer;

/**
 * A dummy file that represents a file which has not been implemented yet.
 * Created by Kneesnap on 8/11/2018.
 */
@Getter
public class DummyFile extends GameFile {
    private int length;
    private ByteBuffer buffer;

    public DummyFile(int length) {
        this.length = length;
        this.buffer = ByteBuffer.allocate(length);
    }

    @Override
    public void load(DataReader reader) {
        this.buffer.put(reader.readBytes(length));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(buffer.array());
    }

    /**
     * Get a dummy file from a byte array.
     * @param source The byte array to read from.
     * @return dummyFile
     */
    public static DummyFile read(byte[] source) {
        DummyFile file = new DummyFile(source.length);
        file.load(new DataReader(new ArraySource(source)));
        return file;
    }
}
