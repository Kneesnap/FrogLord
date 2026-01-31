package net.highwayfrogs.editor.games.sony.medievil2;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.medievil2.map.MediEvil2Map;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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

    // Cached data. Use MWIResourceEntries so if these files are replaced, their new object instances will be returned.
    private MWIResourceEntry cachedWadFileEntry;
    private MWIResourceEntry cachedVloFileEntry;
    private MWIResourceEntry cachedTimFileEntry;

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
        getLogger().info("Read level definition %s:", (this.levelRelocName != null ? "'" + this.levelRelocName + "'" : ""));
        getLogger().info(" Resources: %d, %d, %d, %d, Sections: %d", this.vloResourceId, this.wadResourceId, this.timResourceId, this.levelNameId, sectionCount);
        for (MediEvil2LevelSectionDefinition sectionDefinition : this.levelSections)
            getLogger().info(" Section: %d, %d -> 0x%X", sectionDefinition.getMapResourceId(), sectionDefinition.getVloResourceId(), sectionDefinition.getTextureRemapPointer());
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

        SCGameFile<?> gameFile = getGameInstance().getGameFile(this.wadResourceId);
        if (gameFile instanceof MediEvil2Map) {
            return (MediEvil2Map) gameFile;
        } else {
            getLogger().warning("Don't know how to interpret %s/%s as a map file.", Utils.getSimpleName(gameFile), gameFile.getFileDisplayName());
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
    public VloFile getVloFile() {
        if (this.vloResourceId <= 0)
            return null;

        if (this.cachedVloFileEntry == null || this.cachedVloFileEntry.getResourceId() != this.vloResourceId)
            this.cachedVloFileEntry = getGameInstance().getResourceEntryByID(this.vloResourceId);

        return this.cachedVloFileEntry != null ? (VloFile) this.cachedVloFileEntry.getGameFile() : null;
    }

    /**
     * Gets the .TIM file associated with this entry, if it exists.
     */
    public PSXTIMFile getTimFile() {
        if (this.timResourceId <= 0)
            return null;

        if (this.cachedTimFileEntry == null || this.cachedTimFileEntry.getResourceId() != this.timResourceId)
            this.cachedTimFileEntry = getGameInstance().getResourceEntryByID(this.timResourceId);

        return this.cachedTimFileEntry != null ? (PSXTIMFile) this.cachedTimFileEntry.getGameFile() : null;
    }

    /**
     * Gets the .WAD file associated with this entry, if it exists.
     */
    public WADFile getWadFile() {
        if (this.wadResourceId <= 0)
            return null;

        if (this.cachedWadFileEntry == null || this.cachedWadFileEntry.getResourceId() != this.wadResourceId)
            this.cachedWadFileEntry = getGameInstance().getResourceEntryByID(this.wadResourceId);

        return this.cachedWadFileEntry != null ? (WADFile) this.cachedWadFileEntry.getGameFile() : null;
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
        private MWIResourceEntry cachedMapEntry;
        private MWIResourceEntry cachedVloEntry; // TODO: Should probably use MWI entries instead.

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
                String mapName = (mapFile != null ? FileUtils.stripExtension(mapFile.getFileDisplayName()).toLowerCase() : "map" + this.mapResourceId);
                if (this.textureRemapPointer > 0) {
                    this.textureRemap = new TextureRemapArray(getGameInstance(), "txl_" + mapName, this.textureRemapPointer);
                    if (this.vloResourceId > 0)
                        this.textureRemap.setVloFileDefinition(getGameInstance().getResourceEntryByID(this.vloResourceId));
                }
            }

            return this.textureRemap;
        }

        @Override
        public VloFile getVloFile() {
            if (this.vloResourceId <= 0)
                return null;

            if (this.cachedVloEntry == null || this.cachedVloEntry.getResourceId() != this.vloResourceId)
                this.cachedVloEntry = getGameInstance().getResourceEntryByID(this.vloResourceId);

            return this.cachedVloEntry != null ? (VloFile) this.cachedVloEntry.getGameFile() : null;
        }

        @Override
        public MediEvil2Map getMapFile() {
            if (this.cachedMapEntry != null && this.cachedMapEntry.getResourceId() == this.mapResourceId)
                return (MediEvil2Map) this.cachedMapEntry.getGameFile();

            MediEvil2Map mapFile = null;
            SCGameFile<?> gameFile =  this.mapResourceId > 0 ? getGameInstance().getGameFile(this.mapResourceId) : null;
            if (gameFile instanceof MediEvil2Map) {
                mapFile = (MediEvil2Map) gameFile;
            } else if (gameFile instanceof WADFile) {
                WADFile wadFile = (WADFile) gameFile;
                for (int i = wadFile.getFiles().size() - 1; i >= 0; i--) {
                    SCGameFile<?> wadEntryFile = wadFile.getFiles().get(i).getFile();
                    if (wadEntryFile instanceof MediEvil2Map)
                        mapFile = (MediEvil2Map) wadEntryFile;
                }

                // Didn't find it in the WAD file.
            } else if (gameFile == null) {
                return null; // No file.
            } else {
                throw new RuntimeException("Don't know how to interpret " + Utils.getSimpleName(gameFile) + " as a map file.");
            }

            this.cachedMapEntry = mapFile != null ? mapFile.getIndexEntry() : null;
            return mapFile;
        }
    }
}