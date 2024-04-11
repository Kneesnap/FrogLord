package net.highwayfrogs.editor.games.greatquest.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.TGQChunkedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kneesnap on 4/1/2020.
 */
@Getter
public class TOCChunk extends kcCResource {
    private final List<Integer> hashes = new ArrayList<>();

    public TOCChunk(TGQChunkedFile chunkedFile) {
        super(chunkedFile, KCResourceID.TOC);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        while (reader.hasMore())
            this.hashes.add(reader.readInt());
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int hash : getHashes())
            writer.writeInt(hash);
    }
}