package net.highwayfrogs.editor.games.sony.medievil.entity;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.shared.overlay.SCOverlayTableEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the entity table.
 * Created by Kneesnap on 3/11/2024.
 */
@Getter
public class MediEvilEntityTable extends SCGameData<MediEvilGameInstance> {
    private final List<MediEvilEntityPointerTableEntry> entries = new ArrayList<>();
    private final List<MediEvilEntityDefinition> entityDefinitions = new ArrayList<>();

    public MediEvilEntityTable(MediEvilGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader exeReader) {
        clear();

        for (int i = 0; i < getConfig().getEntityTableSize(); i++) {
            MediEvilEntityPointerTableEntry entry = new MediEvilEntityPointerTableEntry(getGameInstance());
            entry.load(exeReader);
            this.entries.add(entry);

            // If the entity pointer is null, don't read an entity definition.
            if (entry.getEntityDataPointer() == 0) {
                this.entityDefinitions.add(null);
                continue;
            }

            // Find the overlay in the table based on the name we got.
            SCOverlayTableEntry overlayTableEntry = null;

            // TODO: Long-term, this should be changed to getConfig().isBuildAtOrBefore028() ? entry.getLevelId() : getLevelIDFromLevelBitFlags(entry.getOverlayLevelFlags()), since this check is otherwise impossible to understand the intent behind.
            int levelIndex = getConfig().getLevelTableEntryByteSize() <= 88 ? entry.getOverlayId() : getLevelIDFromLevelBitFlags(entry.getOverlayId());
            if (levelIndex != -1) {
                int overlayIndex = getGameInstance().getLevelTable().get(levelIndex).getOverlayId();
                overlayTableEntry = getGameInstance().getOverlayTable().getEntries().get(overlayIndex);
            }

            // Read the entity definition.
            long offset = overlayTableEntry != null ? overlayTableEntry.getOverlayStartPointer() : getGameInstance().getRamOffset();
            DataReader definitionReader = overlayTableEntry != null ? overlayTableEntry.getReader() : exeReader;
            MediEvilEntityDefinition definition = new MediEvilEntityDefinition(getGameInstance(), overlayTableEntry);
            this.entityDefinitions.add(definition);
            definitionReader.jumpTemp((int) (entry.getEntityDataPointer() - offset));

            try {
                definition.load(definitionReader);
            } catch (Throwable th) {
                getLogger().throwing("MediEvilEntityTable", "load", th);
            } finally {
                definitionReader.jumpReturn();
            }
        }
    }

    /**
     * Gets the level ID based on the level bit flags.
     * Defaults to choosing the smallest level ID if multiple bit flags are set.
     * @param levelBitFlags The bit flags to test.
     * @return levelID
     */
    public int getLevelIDFromLevelBitFlags(int levelBitFlags) {
        if (levelBitFlags == -1)
            return levelBitFlags;

        for (int i = 0; i < getGameInstance().getLevelTable().size(); i++) {
            if ((levelBitFlags >> i) == 1) {
                if (i == 24) // Hack for Stone Wolves who resolve to overlay PP.BIN which doesn't exist... TODO: Find better solution?
                    return 2;

                return i;
            }
        }

        return levelBitFlags;
    }

    @Override
    public void save(DataWriter writer) {
        // TODO: !
    }

    @Override
    public MediEvilConfig getConfig() {
        return (MediEvilConfig) super.getConfig();
    }

    /**
     * Clears the contents of the table.
     */
    public void clear() {
        this.entries.clear();
        this.entityDefinitions.clear();
    }

    /**
     * Gets the entity definition for the given index.
     * @param entityIndex The index to get the definition for.
     * @return entityDefinition, if one exists
     */
    public MediEvilEntityDefinition getDefinition(int entityIndex) {
        return entityIndex >= 0 && entityIndex < this.entityDefinitions.size() ? this.entityDefinitions.get(entityIndex) : null;
    }
}