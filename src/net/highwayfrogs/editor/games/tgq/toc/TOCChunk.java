package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kneesnap on 4/1/2020.
 */
@Getter
public class TOCChunk extends kcCResource {
    private String name;
    private List<Integer> hashes = new ArrayList<>();

    private static final int NAME_SIZE = 32;

    public TOCChunk(TGQChunkedFile chunkedFile) {
        super(chunkedFile, KCResourceID.TOC);
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
        while (reader.hasMore())
            this.hashes.add(reader.readInt());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeTerminatedStringOfLength(this.name, NAME_SIZE);
        for (int hash : getHashes())
            writer.writeInt(hash);
    }
}
