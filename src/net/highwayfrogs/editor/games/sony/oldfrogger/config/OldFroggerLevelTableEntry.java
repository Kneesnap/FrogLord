package net.highwayfrogs.editor.games.sony.oldfrogger.config;

import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;

/**
 * Represents an entry in the level table.
 * Created by Kneesnap on 12/10/2023.
 */
@Getter
public class OldFroggerLevelTableEntry extends SCGameData<OldFroggerGameInstance> {
    private static final int FILENAME_LENGTH = 20;

    private final int mapIndex;
    private int mapResourceId = -1;
    private int wadResourceId = -1;
    private int ulrResourceId = -1;
    private String fileName = "";

    public OldFroggerLevelTableEntry(OldFroggerGameInstance instance, int mapIndex) {
        super(instance);
        this.mapIndex = mapIndex;
    }

    private OldFroggerLevelTableEntry(OldFroggerGameInstance instance, int mapIndex, int mapResourceId, int wadResourceId, int ulrResourceId, String fileName) {
        this(instance, mapIndex);
        this.mapResourceId = mapResourceId;
        this.wadResourceId = wadResourceId;
        this.ulrResourceId = ulrResourceId;
        this.fileName = fileName;
    }

    @Override
    public void load(DataReader reader) {
        this.mapResourceId = reader.readInt();
        this.wadResourceId = reader.readInt();
        this.ulrResourceId = reader.readInt();
        this.fileName = reader.readTerminatedStringOfLength(FILENAME_LENGTH);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.mapResourceId);
        writer.writeInt(this.wadResourceId);
        writer.writeInt(this.ulrResourceId);
        writer.writeTerminatedStringOfLength(this.fileName, FILENAME_LENGTH);
    }

    /**
     * Gets the .MAP file containing data for this level.
     */
    public OldFroggerMapFile getMapFile() {
        return this.mapResourceId >= 0 ? getGameInstance().getGameFile(this.mapResourceId) : null;
    }

    /**
     * Gets the .WAD file used by this level.
     */
    public WADFile getWadFile() {
        return this.wadResourceId >= 0 ? getGameInstance().getGameFile(this.wadResourceId) : null;
    }

    /**
     * Gets the .ULR file (Secondary .WAD?) used by this level.
     */
    public WADFile getUlrFile() {
        return this.ulrResourceId >= 0 ? getGameInstance().getGameFile(this.ulrResourceId) : null;
    }

    /**
     * Get the VLO archive containing images for this level.
     * @return The main VLO Archive, if one exists.
     */
    public VLOArchive getMainVLOArchive() {
        for (WADEntry wadEntry : getUlrFile().getFiles())
            if (wadEntry.getFile() instanceof VLOArchive)
                return (VLOArchive) wadEntry.getFile();
        return null;
    }

    /**
     * Get the texture remap assigned to this level, if there is one.
     */
    public TextureRemapArray getTextureRemap() {
        return this.mapIndex >= 0 && this.mapIndex < getGameInstance().getTextureRemapsByLevelId().size()
                ? getGameInstance().getTextureRemapsByLevelId().get(this.mapIndex)
                : null;
    }

    /**
     * Parse a level table entry from the provided string. Used when configuring level table entries for levels that otherwise don't have them.
     * @param instance           The game instance to create the level table entry string for.
     * @param levelTableEntryStr The string to parse into a level table entry.
     * @return newLevelTableEntry
     */
    public static OldFroggerLevelTableEntry parseLevelTableEntry(OldFroggerGameInstance instance, String levelTableEntryStr) {
        if (levelTableEntryStr == null || levelTableEntryStr.isEmpty())
            return null;

        String[] split = levelTableEntryStr.split(",");
        if (split.length != 5) {
            System.out.println("Failed to parse level table entry from '" + levelTableEntryStr + "'.");
            return null;
        }

        int mapIndex;
        int mapResourceId;
        int wadResourceId;
        int ulrResourceId;
        String levelName = split[4];
        try {
            mapIndex = Integer.parseInt(split[0]);
            mapResourceId = Integer.parseInt(split[1]);
            wadResourceId = Integer.parseInt(split[2]);
            ulrResourceId = Integer.parseInt(split[3]);
        } catch (NumberFormatException ex) {
            System.out.println("Failed to parse number / resource ids from '" + levelTableEntryStr + "'.");
            return null;
        }

        return new OldFroggerLevelTableEntry(instance, mapIndex, mapResourceId, wadResourceId, ulrResourceId, levelName);
    }
}