package net.highwayfrogs.editor.games.sony.shared.map.section;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.c12.C12GameInstance;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a level section definition.
 * Created by Kneesnap on 4/17/2026.
 */
public class SCLevelSectionDefinition extends SCGameData<SCGameInstance> implements ISCLevelTableEntry {
    @Getter private final SCLevelDefinition levelDefinition;
    @Getter private short mapResourceId;
    @Getter private short vloResourceId;
    @Getter private long textureRemapPointer;
    @Getter private byte[] unknownData;

    // Non-local data.
    private TextureRemapArray textureRemap;

    // Cached data.
    private MWIResourceEntry cachedMapEntry;
    private MWIResourceEntry cachedVloEntry;

    private static final int MEDIEVIL2_SIZE_IN_BYTES = 0x44; // (68)
    private static final int C12_SIZE_IN_BYTES_MAY_PROTOTYPE = 0x48; // (72)
    private static final int C12_SIZE_IN_BYTES = 0x5C; // (92)

    public SCLevelSectionDefinition(SCLevelDefinition levelDefinition) {
        super(levelDefinition.getGameInstance());
        this.levelDefinition = levelDefinition;
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
        this.mapResourceId = reader.readShort();
        this.vloResourceId = reader.readShort();
        this.textureRemapPointer = reader.readUnsignedIntAsLong();
        this.unknownData = reader.readBytes(getExpectedSizeInBytes() - (reader.getIndex() - dataReadStartIndex)); // TODO: Figure this data out later.
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
            SCMapFile<?> mapFile = getMapFile();
            String mapName = (mapFile != null ? FileUtils.stripExtension(mapFile.getFileDisplayName()).toLowerCase() : "map" + this.mapResourceId);
            if (this.textureRemapPointer > 0) {
                this.textureRemap = new TextureRemapArray(getGameInstance(), "gaus_" + mapName, this.textureRemapPointer);
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
    public SCMapFile<?> getMapFile() {
        if (this.cachedMapEntry != null && this.cachedMapEntry.getResourceId() == this.mapResourceId)
            return (SCMapFile<?>) this.cachedMapEntry.getGameFile();

        SCMapFile<?> mapFile = null;
        SCGameFile<?> gameFile =  this.mapResourceId > 0 ? getGameInstance().getGameFile(this.mapResourceId) : null;
        if (gameFile instanceof SCMapFile<?>) {
            mapFile = (SCMapFile<?>) gameFile;
        } else if (gameFile instanceof WADFile) {
            // If it's a wad file, find the first entry in the wad file.
            WADFile wadFile = (WADFile) gameFile;
            for (int i = wadFile.getFiles().size() - 1; i >= 0; i--) {
                SCGameFile<?> wadEntryFile = wadFile.getFiles().get(i).getFile();
                if (wadEntryFile instanceof SCMapFile<?>)
                    mapFile = (SCMapFile<?>) wadEntryFile;
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
