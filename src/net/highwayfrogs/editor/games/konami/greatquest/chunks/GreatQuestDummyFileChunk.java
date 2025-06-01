package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a dummy chunk of data.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class GreatQuestDummyFileChunk extends kcCResource {
    private final String identifier;
    private byte[] unhandledBytes;

    public GreatQuestDummyFileChunk(GreatQuestChunkedFile parentFile, String identifier) {
        super(parentFile, KCResourceID.getByMagic(identifier));
        this.identifier = identifier;
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
    public String getChunkIdentifier() {
        return this.identifier;
    }
}