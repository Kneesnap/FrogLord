package net.highwayfrogs.editor.games.tgq.audio;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a .IDX streamed index file.
 * Created by Kneesnap on 8/17/2023.
 */
@Getter
public class IDXFile extends GameObject {
    private final List<kcStreamIndexEntry> indexEntries = new ArrayList<>();

    @Override
    public void load(DataReader reader) {
        this.indexEntries.clear();

        int entryCount = reader.readInt();
        for (int i = 0; i < entryCount; i++) {
            kcStreamIndexEntry entry = new kcStreamIndexEntry();
            entry.load(reader);
            this.indexEntries.add(entry);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.indexEntries.size());
        for (int i = 0; i < this.indexEntries.size(); i++)
            this.indexEntries.get(i).save(writer);
    }

    @Getter
    public static class kcStreamIndexEntry extends GameObject {
        private int sfxId;
        private int offset;

        @Override
        public void load(DataReader reader) {
            this.sfxId = reader.readInt();
            this.offset = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.sfxId);
            writer.writeInt(this.offset);
        }
    }
}
