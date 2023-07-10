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
 * Created by Kneesnap on 8/26/2019.
 */
@Getter
public class kcCResourceNamedHash extends kcCResource {
    private final List<HashEntry> entries = new ArrayList<>();

    public kcCResourceNamedHash(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.NAMEDHASH);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int zero1 = reader.readInt();
        int entryCount = reader.readInt();
        int zero2 = reader.readInt();
        int zero3 = reader.readInt();

        if (zero1 != 0 || zero2 != 0 || zero3 != 0)
            System.out.println("Unexpected non-zero value in named hash chunk! [" + zero1 + ", " + zero2 + ", " + zero3 + "]");

        // Read entries.
        this.entries.clear();
        for (int i = 0; i < entryCount; i++) {
            HashEntry newEntry = new HashEntry();
            newEntry.load(reader);
            this.entries.add(newEntry);
        }

        // Check nothing remains.
        if (reader.hasMore())
            System.out.println("There are " + reader.getRemaining() + " unread bytes in " + getName() + "/" + getParentFile().getFilePath());
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
    public static class HashEntry extends GameObject {
        private static final int ANIM_NAME_SIZE = 32;
        private int rawHash; // If name = 'SlpIdle01', this is hash("SlpIdle01", ignoreCase: true)
        private int unknown; // TODO: This seems to be a hash to a resource found in the table of contents.
        private String name;

        @Override
        public void load(DataReader reader) {
            this.rawHash = reader.readInt();
            this.unknown = reader.readInt();
            this.name = reader.readTerminatedStringOfLength(ANIM_NAME_SIZE);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.rawHash);
            writer.writeInt(this.unknown);
            writer.writeTerminatedStringOfLength(this.name, ANIM_NAME_SIZE);
        }
    }
}
