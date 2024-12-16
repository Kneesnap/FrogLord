package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A recreation of the 'kcCResourceNamedHash' class in Frogger PS2 PAL.
 * This is a list of hash table entries allowing the lookup of a hash by a string.
 * It is used to get action sequence chunks from a string.
 * An example of how this is used is that the idle animations are in different files called different things.
 * But, the code wants a clean way to make any entity enter the idle animation.
 * So, in many named hash chunks, there is an entry for "NrmIdle01" so the code can easily find the idle animation regardless of entity.
 * An actor chooses one of these chunks for its "mpSeqMap" field, in methods such as kcCActorBase::SetSequence or kcCActorBase::IsSequence
 * While in theory any resource could be kept in this hash table, it seems the code only ever uses this for action sequences, so we can treat it as if that's all it supports.
 * Created by Kneesnap on 8/26/2019.
 */
@Getter
public class kcCResourceNamedHash extends kcCResource implements IMultiLineInfoWriter {
    private final List<HashTableEntry> entries = new ArrayList<>(); // Searched by kcCActorBase::IsSequence()

    public static final String NAME_SUFFIX = "{seqs}";

    private static final String FILE_EXTENSION = "seq";
    private static final String PATH_KEY = "actionSequenceFilePath";
    private static final BrowserFileType SEQUENCE_FILE_TYPE = new BrowserFileType("Great Quest Action Sequence", FILE_EXTENSION);
    private static final SavedFilePath SEQUENCE_EXPORT_PATH = new SavedFilePath(PATH_KEY, "Select the file to save the sequence as...", SEQUENCE_FILE_TYPE);
    private static final SavedFilePath SEQUENCE_IMPORT_PATH = new SavedFilePath(PATH_KEY, "Select the sequence to import...", SEQUENCE_FILE_TYPE);

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
                    .append(NumberUtils.to0PrefixedHexString(hashTableEntry.getKeyHash())).append(" -> ")
                    .append(hashTableEntry.getValueRef())
                    .append(Constants.NEWLINE);
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Entries", this.entries.size());
        for (int i = 0; i < this.entries.size(); i++) {
            HashTableEntry entry = this.entries.get(i);
            propertyList.add(entry.getKeyName() + " (" + NumberUtils.to0PrefixedHexString(entry.getKeyHash()) + ")", entry.getValueRef().getDisplayString(false));
        }

        return propertyList;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportSequencesItem = new MenuItem("Export Sequence List");
        contextMenu.getItems().add(exportSequencesItem);
        exportSequencesItem.setOnAction(event -> {
            File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), SEQUENCE_EXPORT_PATH, getBaseName() + "." + FILE_EXTENSION, true);
            if (outputFile == null)
                return;

            Config config = toConfigNode(getParentFile().createScriptDisplaySettings());
            config.saveTextFile(outputFile);
            getLogger().info("Saved " + config.getChildConfigNodes().size() + " sequence definitions to the file '" + outputFile.getName() + "'.");
        });

        MenuItem clearSequencesItem = new MenuItem("Clear Sequence List");
        contextMenu.getItems().add(clearSequencesItem);
        clearSequencesItem.setOnAction(event -> {
            if (FXUtils.makePopUpYesNo("Are you sure you'd like to clear all the kcCActionSequence definitions in the file?")) {
                this.entries.clear();
                getLogger().info("Cleared the sequence list.");
            }
        });

        MenuItem importSequencesItem = new MenuItem("Import Sequence List");
        contextMenu.getItems().add(importSequencesItem);
        importSequencesItem.setOnAction(event -> {
            File inputFile = FileUtils.askUserToOpenFile(getGameInstance(), SEQUENCE_IMPORT_PATH);
            if (inputFile == null)
                return;

            Config scriptCfg = Config.loadConfigFromTextFile(inputFile, false);
            addSequencesFromConfigNode(scriptCfg);
            getLogger().info("Imported " + scriptCfg.getChildConfigNodes().size() + " kcCActionSequence definitions from '" + inputFile.getName() + "'.");
        });
    }

    /**
     * Gets an entry by its sequence name.
     * The provided name can be either the full sequence name, or the short sequence name.
     * @param name the name to evaluate.
     * @return entryUsingName, if found
     */
    public HashTableEntry getEntryByName(String name) {
        if (name == null)
            throw new NullPointerException("name");

        for (int i = 0; i < this.entries.size(); i++) {
            HashTableEntry entry = this.entries.get(i);
            if (name.equalsIgnoreCase(entry.getKeyName()) || name.equalsIgnoreCase(entry.makeSequenceName()))
                return entry;

            // Test against the real sequence name, since it has been observed to differ due to typos.
            kcCActionSequence sequence = entry.getSequence();
            if (sequence != null && (name.equalsIgnoreCase(sequence.getName()) || name.equalsIgnoreCase(sequence.getSequenceName())))
                return entry;
        }

        return null;
    }

    /**
     * Gets the name of this resource with the name suffix removed.
     * @return baseName
     */
    public String getBaseName() {
        String baseName = getName();
        if (baseName.endsWith(NAME_SUFFIX)) {
            return baseName.substring(0, baseName.length() - NAME_SUFFIX.length());
        } else {
            return baseName;
        }
    }

    /**
     * Saves this sequence list to a Config node.
     * @param settings The settings to save the script with
     * @return configNode
     */
    public Config toConfigNode(kcScriptDisplaySettings settings) {
        Config result = new Config(getBaseName());
        for (int i = 0; i < this.entries.size(); i++) {
            HashTableEntry entry = this.entries.get(i);
            Config newSequence = kcCActionSequence.toConfigNode(entry, settings);
            result.addChildConfig(newSequence);
        }

        return result;
    }

    /**
     * Gets the action sequences in the table as a list.
     */
    public List<kcCActionSequence> getSequences() {
        List<kcCActionSequence> sequences = new ArrayList<>();

        for (int i = 0; i < this.entries.size(); i++) {
            HashTableEntry entry = this.entries.get(i);
            kcCActionSequence sequence = entry.getSequence();
            if (sequence != null)
                sequences.add(sequence);
        }

        return sequences;
    }

    /**
     * Loads sequences from a Config node.
     * The provided config node will have its child config nodes read, with their names used as the sequence names.
     * Existing sequences will have their actions replaced/read if present in the provided config node.
     * New sequences will be created as configured.
     * @param config The config to load the script from
     */
    public void addSequencesFromConfigNode(Config config) {
        if (config == null)
            throw new NullPointerException("config");

        // Create a map to quickly lookup entries by their names.
        Map<Integer, HashTableEntry> entryLookupByValueHash = new HashMap<>();
        for (int i = 0; i < this.entries.size(); i++) {
            HashTableEntry entry = this.entries.get(i);
            entryLookupByValueHash.put(entry.getValueRef().getHashNumber(), entry);
        }

        for (Config sequenceCfg : config.getChildConfigNodes()) {
            int sequenceHash = sequenceCfg.getKeyValueNodeOrError(kcCActionSequence.HASH_CONFIG_FIELD).getAsInteger();
            String sequenceName = sequenceCfg.getSectionName();

            // Lookup by hash lookup and NOT by name lookup.
            // This is to ensure typos (such as seen in Frog[ThroatFloatLan] vs ThroatFloatLand) do not cause any issues.
            // Hashes are how the game looks up chunked resources too, so this enforces consistent behavior.
            HashTableEntry entry = entryLookupByValueHash.get(sequenceHash);
            if (entry == null) {
                entry = new HashTableEntry(this, sequenceName);
                entry.getValueRef().setHash(sequenceHash);
                entryLookupByValueHash.put(sequenceHash, entry);
                this.entries.add(entry);
            }

            // Replace (or creates) the existing sequence.
            boolean newlyCreatedSequence = false;
            kcCActionSequence sequence = entry.getSequence();
            if (sequence == null) {
                newlyCreatedSequence = true;
                sequence = new kcCActionSequence(getParentFile());
                sequence.setName(entry.makeSequenceName(), false);
                sequence.getSelfHash().setHash(sequenceHash);
            }

            try {
                sequence.loadFromConfigNode(sequenceCfg);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Could not load the sequenced named '%s' as part of '%s'.", sequenceName, getName());
                continue; // Don't register anything that loaded via error.
            }

            // Registers the sequence if it is new, and actually has actions.
            if (newlyCreatedSequence) {
                getParentFile().addResource(sequence);
                entry.getValueRef().setResource(sequence, true);
            }
        }
    }

    @Getter
    public static class HashTableEntry extends GameData<GreatQuestInstance> {
        private final kcCResourceNamedHash parentHashTable;
        private final GreatQuestHash<kcCActionSequence> valueRef; // This is a hash of another file.
        private String keyName;

        private static final int NAME_SIZE = 32;

        public HashTableEntry(kcCResourceNamedHash parentHashTable) {
            this(parentHashTable, null);
        }

        public HashTableEntry(kcCResourceNamedHash parentHashTable, String keyName) {
            super(parentHashTable.getGameInstance());
            this.parentHashTable = parentHashTable;
            this.valueRef = new GreatQuestHash<>();
            this.keyName = keyName;
        }

        @Override
        public void load(DataReader reader) {
            int originalKeyHash = reader.readInt();
            int valueHash = reader.readInt();
            this.keyName = reader.readNullTerminatedFixedSizeString(NAME_SIZE, Constants.NULL_BYTE);

            // Validate key.
            int ourHash = getKeyHash();
            if (ourHash != originalKeyHash)
                throw new IllegalArgumentException("The kcCResourceNamedHash read an entry for key '" + this.keyName + "' with hash " + NumberUtils.to0PrefixedHexString(originalKeyHash) + ". However, the key actually hashes to " + NumberUtils.to0PrefixedHexString(ourHash) + ".");

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

        /**
         * Makes/calculates the expected sequence name.
         */
        public String makeSequenceName() {
            return this.parentHashTable.getBaseName() + "[" + this.keyName + "]";
        }

        /**
         * Gets the linked action sequence value, if it was found.
         */
        public kcCActionSequence getSequence() {
            return this.valueRef.getResource();
        }
    }
}