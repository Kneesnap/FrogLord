package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Represents a dummy chunk of data.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class TGQDummyFileChunk extends kcCResource {
    private byte[] data;
    private String magic;

    public TGQDummyFileChunk(TGQChunkedFile parentFile, String magic) {
        super(parentFile, KCResourceID.getByMagic(magic));
        this.magic = magic;
    }

    @Override
    public void load(DataReader reader) {
        this.data = reader.readBytes(reader.getSize());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(this.data);
    }

    @Override
    public String getChunkMagic() {
        return this.magic;
    }
}
