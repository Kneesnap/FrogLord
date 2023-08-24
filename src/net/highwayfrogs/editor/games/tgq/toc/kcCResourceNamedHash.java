package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * A recreation of the 'kcCResourceNamedHash' class in Frogger PS2 PAL.
 * This is a list of hash table entries allowing the lookup of a hash by a string.
 * It is used to get action sequence chunks from a string.
 * An example of how this is used is that the idle animations are in different files called different things.
 * But, the code wants a clean way to make any entity enter the idle animation.
 * So, in many named hash chunks, there is an entry for "NrmIdle01" so the code can easily find the idle animation regardless of entity.
 * An actor chooses one of these chunks for its "mpSeqMap" field.
 * Created by Kneesnap on 8/26/2019.
 */
@Getter
public class kcCResourceNamedHash extends kcCResource {
    private final List<HashTableEntry> entries = new ArrayList<>();

    public kcCResourceNamedHash(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.NAMEDHASH);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int version = reader.readInt();
        int entryCount = reader.readInt();
        int reserved1 = reader.readInt();
        int reserved2 = reader.readInt();

        if (version != 0 || reserved1 != 0 || reserved2 != 0)
            System.out.println("Unexpected non-zero value in named hash chunk! [" + version + ", " + reserved1 + ", " + reserved2 + "]");

        // Read entries.
        this.entries.clear();
        for (int i = 0; i < entryCount; i++) {
            HashTableEntry newEntry = new HashTableEntry();
            newEntry.load(reader);
            this.entries.add(newEntry);
        }

        // Check nothing remains.
        if (reader.hasMore())
            System.out.println("There are " + reader.getRemaining() + " unread bytes in " + getParentFile().getDebugName());
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(0);
        writer.writeInt(this.entries.size());
        writer.writeInt(0);
        writer.writeInt(0);

        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).save(writer);
    }

    @Getter
    public static class HashTableEntry extends GameObject {
        private static final int NAME_SIZE = 32;
        private int keyHash; // If name = 'SlpIdle01', this is hash("SlpIdle01", ignoreCase: true)
        private int valueHash; // This is a hash of another file.
        private String keyName;

        @Override
        public void load(DataReader reader) {
            this.keyHash = reader.readInt();
            this.valueHash = reader.readInt();
            this.keyName = reader.readTerminatedStringOfLength(NAME_SIZE);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.keyHash);
            writer.writeInt(this.valueHash);
            writer.writeTerminatedStringOfLength(this.keyName, NAME_SIZE);
        }
    }
}