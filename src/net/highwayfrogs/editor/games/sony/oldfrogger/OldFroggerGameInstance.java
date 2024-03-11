package net.highwayfrogs.editor.games.sony.oldfrogger;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapEntityMarkerPacket;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.MainController.SCDisplayedFileType;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.*;

/**
 * Represents pre-recode frogger
 * TODO: Profile the editor doing various things after all the todos are taken care of.
 *  - Let's fix lag.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerGameInstance extends SCGameInstance {
    public static final int FILE_TYPE_LANGUAGE = 8;
    public static final int FILE_TYPE_MAP = 9;

    public static final int DIFFICULTY_LEVELS = 10; // There are 7 different difficulty levels.
    public static final int DIFFICULTY_FLAG_UNKNOWN_1 = 10;
    public static final int DIFFICULTY_FLAG_UNKNOWN_2 = 11;
    public static final int DIFFICULTY_FLAG_CHECKPOINT = 12;
    public static final int DIFFICULTY_FLAG_INACTIVE_DEFAULT = 13;
    public static final int DIFFICULTY_FLAG_UNKNOWN_3 = 14;
    public static final int DIFFICULTY_FLAG_STATIC_MODEL = 15;

    private final List<OldFroggerLevelTableEntry> levelTableEntries = new ArrayList<>();
    private final List<OldFroggerLevelTableEntry> manuallyConfiguredLevelTableEntries = new ArrayList<>();
    private final List<TextureRemapArray> textureRemapsByLevelId = new ArrayList<>();

    public OldFroggerGameInstance() {
        super(SCGameType.OLD_FROGGER);
    }

    @Override
    public void loadGame(String configName, Config config, File mwdFile, File exeFile) {
        super.loadGame(configName, config, mwdFile, exeFile);
        OldFroggerMapEntityMarkerPacket.showFormGenerationOutput();
    }

    @Override
    public OldFroggerConfig getConfig() {
        return (OldFroggerConfig) super.getConfig();
    }

    @Override
    protected SCGameConfig makeConfig(String internalName) {
        return new OldFroggerConfig(internalName);
    }

    @Override
    public SCGameFile<?> createFile(FileEntry fileEntry, byte[] fileData) {
        if (fileEntry.getTypeId() == FILE_TYPE_MAP)
            return new OldFroggerMapFile(this);

        return SCUtils.createSharedGameFile(fileEntry, fileData);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MWIFile mwiFile) {
        this.textureRemapsByLevelId.clear();
        if (getConfig().getRemapTableCount() <= 0 || getConfig().getRemapTableAddress() <= 0)
            return; // No remaps.

        // Find a list of all the maps.
        List<FileEntry> mapFileEntries = new ArrayList<>();
        for (int i = 0; i < mwiFile.getEntries().size(); i++) {
            FileEntry entry = mwiFile.getEntries().get(i);
            if (entry.getTypeId() == FILE_TYPE_MAP || entry.hasExtension("MAP"))
                mapFileEntries.add(entry);
        }

        // Find remaps.
        int unknownMapCount = 0;
        exeReader.setIndex(getConfig().getRemapTableAddress());
        Map<Long, TextureRemapArray> remapsByPointer = new HashMap<>();
        for (int i = 0; i < getConfig().getRemapTableCount(); i++) {
            long remapPointer = exeReader.readUnsignedIntAsLong();

            // Find remap which has already been read.
            TextureRemapArray remap = remapsByPointer.get(remapPointer);
            if (remap != null) {
                this.textureRemapsByLevelId.add(remap);
                continue;
            }

            String remapNameSuffix;
            if (i >= mapFileEntries.size()) {
                remapNameSuffix = "unknown" + (++unknownMapCount);
            } else {
                remapNameSuffix = Utils.stripExtension(mapFileEntries.get(i).getDisplayName()).toLowerCase(Locale.ROOT);
            }

            // Create new remap.
            remap = new TextureRemapArray(this, "txl_" + remapNameSuffix, (int) remapPointer);
            this.textureRemapsByLevelId.add(remap);
            remapsByPointer.put(remapPointer, remap);
            addRemap(remap);
        }
    }

    @Override
    public void setupFileTypes(List<SCDisplayedFileType> fileTypes) {
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_MAP, "MAP (Game Maps)"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_LANGUAGE, "LAN (Language)"));
    }

    @Override
    public void onConfigLoad(Config config) {
        super.onConfigLoad(config);
        DataReader exeReader = getExecutableReader();
        OldFroggerConfig gameConfig = getConfig();
        readLevelTable(exeReader, gameConfig);
    }

    private void readLevelTable(DataReader exeReader, OldFroggerConfig config) {
        this.levelTableEntries.clear();
        if (config.getLevelTableAddress() <= 0 || config.getLevelTableCount() <= 0)
            return;

        exeReader.jumpTemp(config.getLevelTableAddress());
        for (int i = 0; i < config.getLevelTableCount(); i++) {
            OldFroggerLevelTableEntry newEntry = new OldFroggerLevelTableEntry(this, i);
            newEntry.load(exeReader);
            this.levelTableEntries.add(newEntry);
        }

        exeReader.jumpReturn();

        // Manually configured level table entries.
        this.manuallyConfiguredLevelTableEntries.clear();
        for (String manualLevelTableEntryStr : config.getManualLevelTableEntryStrings()) {
            OldFroggerLevelTableEntry levelTableEntry = OldFroggerLevelTableEntry.parseLevelTableEntry(this, manualLevelTableEntryStr);
            if (levelTableEntry != null)
                this.manuallyConfiguredLevelTableEntries.add(levelTableEntry);
        }
    }

    /**
     * Returns the level table entry for the specified map resource ID, if one exists.
     * @param mapResourceId The resource ID of the map file.
     * @return levelTableEntry, or null
     */
    public OldFroggerLevelTableEntry getLevelTableEntry(int mapResourceId) {
        for (int i = 0; i < this.manuallyConfiguredLevelTableEntries.size(); i++) {
            OldFroggerLevelTableEntry entry = this.manuallyConfiguredLevelTableEntries.get(i);
            if (entry.getMapResourceId() == mapResourceId)
                return entry;
        }

        for (int i = 0; i < this.levelTableEntries.size(); i++) {
            OldFroggerLevelTableEntry entry = this.levelTableEntries.get(i);
            if (entry.getMapResourceId() == mapResourceId)
                return entry;
        }

        return null;
    }
}