package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A recreation of the 'kcCResourceNamedHash' class in Frogger PS2 PAL.
 * This is a list of hash table entries allowing the lookup of a hash by a string.
 * It is used to get action sequence chunks from a string.
 * An example of how this is used is that the idle animations are in different files called different things.
 * But, the code wants a clean way to make any entity enter the idle animation.
 * So, in many named hash chunks, there is an entry for "NrmIdle01" so the code can easily find the idle animation regardless of entity.
 * An actor chooses one of these chunks for its "mpSeqMap" field, in methods such as kcCActorBase::SetSequence or kcCActorBase::IsSequence
 * While in theory any resource could be kept in this hash table, it seems the code only ever uses this for action sequences, so we can treat it as if that's all it supports.
 * TODO: Implement a new hashing system which can do it. (Frick, how are we going to avoid collisions with the name itself? Just create a new random hash and apply it after loading/resolution?)
 *  -> How will we handle kcProxyTriMeshDesc
 * Created by Kneesnap on 8/26/2019.
 */
@Getter
public class kcCResourceNamedHash extends kcCResource implements IMultiLineInfoWriter {
    private final List<HashTableEntry> entries = new ArrayList<>();

    public static final String NAME_SUFFIX = "{seqs}";

    public kcCResourceNamedHash(GreatQuestChunkedFile parentFile) {
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
            getLogger().warning("Unexpected non-zero value in named hash chunk! [" + version + ", " + reserved1 + ", " + reserved2 + "]");

        // Read entries.
        this.entries.clear();
        for (int i = 0; i < entryCount; i++) {
            HashTableEntry newEntry = new HashTableEntry(this);
            newEntry.load(reader);
            this.entries.add(newEntry);
        }

        // Check nothing remains.
        if (reader.hasMore())
            getLogger().warning("There are " + reader.getRemaining() + " unread bytes in " + getParentFile().getDebugName());
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

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        for (HashTableEntry hashTableEntry : this.entries) {
            builder.append(padding).append(" - '").append(hashTableEntry.getKeyName()).append("'/")
                    .append(Utils.to0PrefixedHexString(hashTableEntry.getKeyHash())).append(" -> ")
                    .append(hashTableEntry.getValueRef())
                    .append(Constants.NEWLINE);
        }
    }

    @Getter
    public static class HashTableEntry extends GameData<GreatQuestInstance> {
        private final kcCResourceNamedHash parentHashTable;
        private final GreatQuestHash<kcCActionSequence> valueRef; // This is a hash of another file.
        private String keyName;

        private static final int NAME_SIZE = 32;

        public HashTableEntry(kcCResourceNamedHash parentHashTable) {
            super(parentHashTable.getGameInstance());
            this.parentHashTable = parentHashTable;
            this.valueRef = new GreatQuestHash<>();
        }

        @Override
        public void load(DataReader reader) {
            int originalKeyHash = reader.readInt();
            int valueHash = reader.readInt();
            this.keyName = reader.readNullTerminatedFixedSizeString(NAME_SIZE, Constants.NULL_BYTE);

            // Validate key.
            int ourHash = getKeyHash();
            if (ourHash != originalKeyHash)
                throw new IllegalArgumentException("The kcCResourceNamedHash read an entry for key '" + this.keyName + "' with hash " + Utils.to0PrefixedHexString(originalKeyHash) + ". However, the key actually hashes to " + Utils.to0PrefixedHexString(ourHash) + ".");

            // Resolve value.
            GreatQuestUtils.resolveResourceHash(kcCActionSequence.class, this.parentHashTable, this.valueRef, valueHash, false);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(getKeyHash());
            writer.writeInt(this.valueRef.getHashNumber());
            writer.writeNullTerminatedFixedSizeString(this.keyName, NAME_SIZE, Constants.NULL_BYTE);
        }

        /**
         * Gets the hash of the key.
         * @return keyHash
         */
        public int getKeyHash() {
            // If name = 'SlpIdle01', this is hash("SlpIdle01", ignoreCase: true)
            return GreatQuestUtils.hash(this.keyName);
        }
    }
}