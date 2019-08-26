package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQTOCFile;

/**
 * Represents a dummy chunk of data.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class TOCDummyChunk extends TOCChunk {
    private byte[] data;
    private String magic;

    public TOCDummyChunk(TGQTOCFile parentFile, String magic) {
        super(parentFile, TOCChunkType.getByMagic(magic));
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
    public String getSignature() {
        return this.magic;
    }
}
