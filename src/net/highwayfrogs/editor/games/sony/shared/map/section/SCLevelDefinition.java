package net.highwayfrogs.editor.games.sony.shared.map.section;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.c12.C12GameInstance;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entry in the game's level table.
 * Created by Kneesnap on 4/17/2026.
 */
public class SCLevelDefinition extends SCSharedGameData implements ISCLevelTableEntry {
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
    @Getter private final List<SCLevelSectionDefinition> levelSections = new ArrayList<>();

    // Cached data. Use MWIResourceEntries so if these files are replaced, their new object instances will be returned.
    private MWIResourceEntry cachedWadFileEntry;
    private MWIResourceEntry cachedVloFileEntry;
    private MWIResourceEntry cachedTimFileEntry;

    private static final int MEDIEVIL2_SIZE_IN_BYTES = 0x6C;
    private static final int C12_SIZE_IN_BYTES_MAY_PROTOTYPE = 0x60;
    private static final int C12_SIZE_IN_BYTES = 0x5C;

    public SCLevelDefinition(SCGameInstance instance) {
        super(instance);
    }

    private int getExpectedSizeInBytes() {
        if (getGameInstance().isC12()) {
            if (((C12GameInstance) getGameInstance()).getVersionConfig().isAtLeastBetaCandidate3()) {
                return C12_SIZE_IN_BYTES;
            } else {
                return C12_SIZE_IN_BYTES_MAY_PROTOTYPE;
            }
        } else if (getGameInstance().isMediEvil2()) {
            return MEDIEVIL2_SIZE_IN_BYTES;
        } else {
            throw new UnsupportedOperationException("Don't know expected size in bytes for this game type.");
        }
    }

    @Override
    public void load(DataReader reader) {
        int dataReadStartIndex = reader.getIndex();
        this.relocNamePointer = reader.readUnsignedIntAsLong();
        this.vloResourceId = reader.readShort();
        this.wadResourceId = reader.readShort();
        this.timResourceId = reader.readShort(); // TODO: Figure out what this is in C-12 Final Resistance.
        this.levelNameId = reader.readShort();
        this.unknown1 = reader.readShort();
        this.unknown2 = reader.readUnsignedByteAsShort();
        short sectionCount = reader.readUnsignedByteAsShort();
        this.sectionDefinitionsPointer = reader.readUnsignedIntAsLong();
        this.unknownData = reader.readBytes(getExpectedSizeInBytes() - (reader.getIndex() - dataReadStartIndex)); // TODO: Figure this data out later.

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
                // TODO: !
                SCLevelSectionDefinition newSectionDefinition = new SCLevelSectionDefinition(this);
                newSectionDefinition.load(reader);
                this.levelSections.add(newSectionDefinition);
            }
            reader.jumpReturn();
        }

        // Write section info.
        getLogger().info("Read level definition %s:", (this.levelRelocName != null ? "'" + this.levelRelocName + "'" : ""));
        getLogger().info(" Resources: %d, %d, %d, %d, Sections: %d", this.vloResourceId, this.wadResourceId, this.timResourceId, this.levelNameId, sectionCount);
        for (SCLevelSectionDefinition sectionDefinition : this.levelSections) // TODO: !
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
    public SCMapFile<?> getMapFile() {
        WADFile wadFile = getWadFile();
        if (wadFile != null) {
            for (int i = wadFile.getFiles().size() - 1; i >= 0; i--) {
                WADEntry wadEntry = wadFile.getFiles().get(i);
                if (wadEntry.getFile() instanceof SCMapFile<?>)
                    return (SCMapFile<?>) wadEntry.getFile();
            }
        }

        SCGameFile<?> gameFile = getGameInstance().getGameFile(this.wadResourceId);
        if (gameFile instanceof SCMapFile<?>) {
            return (SCMapFile<?>) gameFile;
        } else {
            getLogger().warning("Don't know how to interpret %s/%s as a map file.", Utils.getSimpleName(gameFile), gameFile.getFileDisplayName());
        }

        // Couldn't find.
        return null;
    }

    @Override
    public SCLevelDefinition getLevelDefinition() {
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
}
