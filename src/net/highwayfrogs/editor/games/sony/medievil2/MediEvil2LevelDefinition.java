package net.highwayfrogs.editor.games.sony.medievil2;

import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.medievil2.map.MediEvil2Map;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entry in the MediEvil2 level table.
 * Created by Kneesnap on 5/12/2024.
 */
public class MediEvil2LevelDefinition extends SCGameData<MediEvil2GameInstance> implements IMediEvil2LevelTableEntry {
    @Getter private long relocNamePointer;
    @Getter private short vloResourceId;
    @Getter private short wadResourceId;
    @Getter private short timResourceId;
    @Getter private short levelNameId;
    @Getter private short unknown1;
    @Getter private short unknown2;
    @Getter private long sectionDefinitionsPointer;
    @Getter private byte[] unknownData;

    // Non-local data:
    @Getter private String levelRelocName;
    @Getter private final List<MediEvil2LevelSectionDefinition> levelSections = new ArrayList<>();

    // Cached data.
    private WADFile cachedWadFile;
    private VLOArchive cachedVloFile;
    private PSXTIMFile cachedTimFile;

    private static final int SIZE_IN_BYTES = 0x6C;

    public MediEvil2LevelDefinition(MediEvil2GameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int dataReadStartIndex = reader.getIndex();
        this.relocNamePointer = reader.readUnsignedIntAsLong();
        this.vloResourceId = reader.readShort();
        this.wadResourceId = reader.readShort();
        this.timResourceId = reader.readShort();
        this.levelNameId = reader.readShort();
        this.unknown1 = reader.readShort();
        this.unknown2 = reader.readUnsignedByteAsShort();
        short sectionCount = reader.readUnsignedByteAsShort();
        this.sectionDefinitionsPointer = reader.readUnsignedIntAsLong();
        this.unknownData = reader.readBytes(SIZE_IN_BYTES - (reader.getIndex() - dataReadStartIndex)); // TODO: Figure this data out later.

        // Read level name.
        this.levelRelocName = null;
        if (this.relocNamePointer > getGameInstance().getRamOffset()) {
            reader.jumpTemp((int) (this.relocNamePointer - getGameInstance().getRamOffset()));
            this.levelRelocName = reader.readNullTerminatedString();
            reader.jumpReturn();
        }

        // Read section definitions.
        this.levelSections.clear();
        if (sectionCount > 0 && this.sectionDefinitionsPointer > getGameInstance().getRamOffset()) {
            reader.jumpTemp((int) (this.sectionDefinitionsPointer - getGameInstance().getRamOffset()));
            for (int i = 0; i < sectionCount; i++) {
                MediEvil2LevelSectionDefinition newSectionDefinition = new MediEvil2LevelSectionDefinition(this);
                newSectionDefinition.load(reader);
                this.levelSections.add(newSectionDefinition);
            }
            reader.jumpReturn();
        }

        // Write section info.
        getLogger().info("Read level definition:");
        getLogger().info(" Resources: " + this.vloResourceId + ", " + this.wadResourceId + ", " + this.timResourceId + ", " + this.levelNameId + ", Sections: " + sectionCount);
        for (MediEvil2LevelSectionDefinition sectionDefinition : this.levelSections)
            getLogger().info(" Section: " + sectionDefinition.getMapResourceId() + ", " + sectionDefinition.getVloResourceId() + " -> " + Utils.toHexString(sectionDefinition.getTextureRemapPointer()));
        getLogger().info("");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.relocNamePointer);
        writer.writeShort(this.vloResourceId);
        writer.writeShort(this.wadResourceId);
        writer.writeShort(this.timResourceId);
        writer.writeShort(this.levelNameId);
        writer.writeShort(this.unknown1);
        writer.writeUnsignedByte(this.unknown2);
        writer.writeUnsignedByte((short) this.levelSections.size());
        writer.writeUnsignedInt(this.sectionDefinitionsPointer);
        if (this.unknownData != null)
            writer.writeBytes(this.unknownData);

        // The reloc name is not saved to avoid overflowing the amount of characters available.

        // Write section definitions if the pointer looks good.
        if (this.sectionDefinitionsPointer > getGameInstance().getRamOffset()) {
            writer.jumpTemp((int) (this.sectionDefinitionsPointer - getGameInstance().getRamOffset()));
            for (int i = 0; i < this.levelSections.size(); i++)
                this.levelSections.get(i).save(writer);
            writer.jumpReturn();
        }
    }

    @Override
    public TextureRemapArray getRemap() {
        return null; // No remap here.
    }

    @Override
    public MediEvil2Map getMapFile() {
        WADFile wadFile = getWadFile();
        if (wadFile != null) {
            for (int i = wadFile.getFiles().size() - 1; i >= 0; i--) {
                WADEntry wadEntry = wadFile.getFiles().get(i);
                if (wadEntry.getFile() instanceof MediEvil2Map)
                    return (MediEvil2Map) wadEntry.getFile();
            }
        }

        // Couldn't find.
        return null;
    }

    @Override
    public MediEvil2LevelDefinition getLevelDefinition() {
        return this;
    }

    /**
     * Gets the .VLO file associated with this entry, if it exists.
     */
    public VLOArchive getVloFile() {
        if (this.cachedVloFile != null && this.cachedVloFile.getIndexEntry().getResourceId() == this.vloResourceId)
            return this.cachedVloFile;

        return this.vloResourceId > 0 ? this.cachedVloFile = getGameInstance().getGameFile(this.vloResourceId) : null;
    }

    /**
     * Gets the .TIM file associated with this entry, if it exists.
     */
    public PSXTIMFile getTimFile() {
        if (this.cachedTimFile != null && this.cachedTimFile.getIndexEntry().getResourceId() == this.timResourceId)
            return this.cachedTimFile;

        return this.timResourceId > 0 ? this.cachedTimFile = getGameInstance().getGameFile(this.timResourceId) : null;
    }

    /**
     * Gets the .WAD file associated with this entry, if it exists.
     */
    public WADFile getWadFile() {
        if (this.cachedWadFile != null && this.cachedWadFile.getIndexEntry().getResourceId() == this.wadResourceId)
            return this.cachedWadFile;

        return this.wadResourceId > 0 ? this.cachedWadFile = getGameInstance().getGameFile(this.wadResourceId) : null;
    }

    /**
     * Represents a level section definition.
     */
    public static class MediEvil2LevelSectionDefinition extends SCGameData<MediEvil2GameInstance> implements IMediEvil2LevelTableEntry {
        @Getter private final MediEvil2LevelDefinition levelDefinition;
        @Getter private short mapResourceId;
        @Getter private short vloResourceId;
        @Getter private long textureRemapPointer;
        @Getter private byte[] unknownData;

        // Non-local data.
        private TextureRemapArray textureRemap;

        // Cached data.
        private MediEvil2Map cachedMapFile;
        private VLOArchive cachedVloFile;

        private static final int SIZE_IN_BYTES = 0x44; // (68)

        public MediEvil2LevelSectionDefinition(MediEvil2LevelDefinition levelDefinition) {
            super(levelDefinition.getGameInstance());
            this.levelDefinition = levelDefinition;
        }

        @Override
        public void load(DataReader reader) {
            int dataReadStartIndex = reader.getIndex();
            this.mapResourceId = reader.readShort();
            this.vloResourceId = reader.readShort();
            this.textureRemapPointer = reader.readUnsignedIntAsLong();
            this.unknownData = reader.readBytes(SIZE_IN_BYTES - (reader.getIndex() - dataReadStartIndex)); // TODO: Figure this data out later.
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.mapResourceId);
            writer.writeShort(this.vloResourceId);
            writer.writeUnsignedInt(this.textureRemapPointer);
            if (this.unknownData != null)
                writer.writeBytes(this.unknownData);
        }

        @Override
        public TextureRemapArray getRemap() {
            if (this.textureRemap == null) {
                MediEvil2Map mapFile = getMapFile();
                String mapName = (mapFile != null ? Utils.stripExtension(mapFile.getIndexEntry().getDisplayName()).toLowerCase() : "map" + this.mapResourceId);
                if (this.textureRemapPointer > 0)
                    this.textureRemap = new TextureRemapArray(getGameInstance(), "txl_" + mapName, this.textureRemapPointer);
            }

            return this.textureRemap;
        }

        @Override
        public VLOArchive getVloFile() {
            if (this.cachedVloFile != null && this.cachedVloFile.getIndexEntry().getResourceId() == this.vloResourceId)
                return this.cachedVloFile;

            return this.vloResourceId > 0 ? this.cachedVloFile = getGameInstance().getGameFile(this.vloResourceId) : null;
        }

        @Override
        public MediEvil2Map getMapFile() {
            if (this.cachedMapFile != null && this.cachedMapFile.getIndexEntry().getResourceId() == this.mapResourceId)
                return this.cachedMapFile;

            SCGameFile<?> gameFile =  this.mapResourceId > 0 ? getGameInstance().getGameFile(this.mapResourceId) : null;
            if (gameFile instanceof MediEvil2Map) {
                return this.cachedMapFile = (MediEvil2Map) gameFile;
            } else if (gameFile instanceof WADFile) {
                WADFile wadFile = (WADFile) gameFile;
                for (int i = wadFile.getFiles().size() - 1; i >= 0; i--) {
                    SCGameFile<?> wadEntryFile = wadFile.getFiles().get(i).getFile();
                    if (wadEntryFile instanceof MediEvil2Map)
                        return this.cachedMapFile = (MediEvil2Map) wadEntryFile;
                }

                // Didn't find it in the WAD file.
                return null;
            } else if (gameFile == null) {
                return null; // No file.
            } else {
                throw new RuntimeException("Don't know how to interpret " + Utils.getSimpleName(gameFile) + " as a map file.");
            }
        }
    }
}