package net.highwayfrogs.editor.file;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.nio.ByteBuffer;

/**
 * A dummy file that represents a file which has not been implemented yet.
 * Created by Kneesnap on 8/11/2018.
 */
@Getter
public class DummyFile extends GameObject {
    private FileEntry entry;
    private ByteBuffer buffer;
    private int size;

    public DummyFile(FileEntry entry) {
        this.entry = entry;
        this.size = entry.isCompressed() ? entry.getPackedSize() : entry.getUnpackedSize();
        this.buffer = ByteBuffer.allocate(size);
    }

    @Override
    public void load(DataReader reader) {
        this.buffer.put(reader.readBytes(size));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(buffer.array());
    }
}
