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
public class DummyFile extends GameFile {
    private FileEntry entry;
    private ByteBuffer buffer;

    public DummyFile(FileEntry entry) {
        this.entry = entry;
        this.buffer = ByteBuffer.allocate(entry.getArchiveSize());
    }

    @Override
    public void load(DataReader reader) {
        this.buffer.put(reader.readBytes(entry.getArchiveSize()));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(buffer.array());
    }
}
