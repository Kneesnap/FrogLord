package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.TGQChunkedFile;

/**
 * Represents a dummy chunk of data.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class TGQDummyFileChunk extends kcCResource {
    private final String magic;
    private byte[] unhandledBytes;

    public TGQDummyFileChunk(TGQChunkedFile parentFile, String magic) {
        super(parentFile, KCResourceID.getByMagic(magic));
        this.magic = magic;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.unhandledBytes = reader.readBytes(reader.getRemaining());
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        if (this.unhandledBytes != null && this.unhandledBytes.length > 0)
            writer.writeBytes(this.unhandledBytes);
    }

    @Override
    public String getChunkMagic() {
        return this.magic;
    }
}