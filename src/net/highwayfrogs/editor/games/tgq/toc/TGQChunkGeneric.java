package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Represents a generic Frogger TGQ game chunk.
 * Created by Kneesnap on 3/23/2020.
 */
@Getter
@Setter
public class TGQChunkGeneric extends kcCResource {
    private byte[] bytes;

    public TGQChunkGeneric(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.GENERIC);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.bytes = reader.readBytes(reader.getRemaining());
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        if (this.bytes != null)
            writer.writeBytes(this.bytes);
    }
}
