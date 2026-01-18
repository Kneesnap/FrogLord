package net.highwayfrogs.editor.games.sony.oldfrogger;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.psx.image.PsxVramBox;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.SCUtils.SCForcedLoadSoundFileType;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapVersion;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapForm;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapEntityMarkerPacket;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

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
    public static final int FILE_TYPE_AUDIO_HEADER = 3;
    public static final int FILE_TYPE_AUDIO_BODY = 4;
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
    public void loadGame(String versionConfigName, net.highwayfrogs.editor.system.Config instanceConfig, File mwdFile, File exeFile, ProgressBarComponent progressBar) {
        super.loadGame(versionConfigName, instanceConfig, mwdFile, exeFile, progressBar);
        OldFroggerMapEntityMarkerPacket.showFormGenerationOutput();
    }

    @Override
    public OldFroggerConfig getVersionConfig() {
        return (OldFroggerConfig) super.getVersionConfig();
    }

    @Override
    public SCGameFile<?> createFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        if (resourceEntry.getTypeId() == FILE_TYPE_MAP) {
            return new OldFroggerMapFile(this);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_LANGUAGE || resourceEntry.hasExtension("lan")) {
            return new OldFroggerLanguageFile(this);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_AUDIO_HEADER || resourceEntry.hasExtension("hed")) {
            return SCUtils.makeSound(resourceEntry, fileData, SCForcedLoadSoundFileType.HEADER);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_AUDIO_BODY || resourceEntry.hasExtension("bod")) {
            return SCUtils.makeSound(resourceEntry, fileData, SCForcedLoadSoundFileType.BODY);
        }

        return SCUtils.createSharedGameFile(resourceEntry, fileData);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MillenniumWadIndex wadIndex) {
        this.textureRemapsByLevelId.clear();
        if (this.getVersionConfig().getRemapTableCount() <= 0 || this.getVersionConfig().getRemapTableAddress() <= 0)
            return; // No remaps.

        // Find a list of all the maps.
        List<MWIResourceEntry> mapFileEntries = new ArrayList<>();
        for (int i = 0; i < wadIndex.getEntries().size(); i++) {
            MWIResourceEntry entry = wadIndex.getEntries().get(i);
            if (entry.getTypeId() == FILE_TYPE_MAP || entry.hasExtension("MAP"))
                mapFileEntries.add(entry);
        }

        // Find remaps.
        int unknownMapCount = 0;
        exeReader.setIndex(this.getVersionConfig().getRemapTableAddress());
        Map<Long, TextureRemapArray> remapsByPointer = new HashMap<>();
        for (int i = 0; i < this.getVersionConfig().getRemapTableCount(); i++) {
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
                remapNameSuffix = FileUtils.stripExtension(mapFileEntries.get(i).getDisplayName()).toLowerCase(Locale.ROOT);
            }

            // Create new remap.
            remap = new TextureRemapArray(this, "txl_" + remapNameSuffix, remapPointer);
            this.textureRemapsByLevelId.add(remap);
            remapsByPointer.put(remapPointer, remap);
            addRemap(remap);
        }
    }

    @Override
    protected void resolveModelVloFiles() {
        for (OldFroggerMapFile mapFile : getMainArchive().getAllFiles(OldFroggerMapFile.class)) {
            if (mapFile.getFormatVersion() != OldFroggerMapVersion.MILESTONE3)
                continue;

            OldFroggerLevelTableEntry levelTableEntry = mapFile.getLevelTableEntry();
            if (levelTableEntry == null)
                continue;

            VloFile mainVloArchive = levelTableEntry.getMainVloFile();
            if (mainVloArchive == null)
                continue;

            for (OldFroggerMapEntity entity : mapFile.getEntityMarkerPacket().getEntities()) {
                OldFroggerMapForm form = entity.getForm();
                MRModel model = form != null ? form.getModel() : null;
                if (model != null && model.getVloFile() == null)
                    model.setVloFile(mainVloArchive);
            }
        }

        super.resolveModelVloFiles();
    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        fileListView.addGroup(new SCGameFileListTypeIdGroup("MAP [Game Maps]", FILE_TYPE_MAP));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("LAN [Language]", FILE_TYPE_LANGUAGE));
    }

    @Override
    protected void setupFrameBuffers() {
        this.primaryFrameBuffer = new PsxVramBox(0, 0, 320, getDefaultFrameBufferHeight());
        this.secondaryFrameBuffer = this.primaryFrameBuffer.cloneBelow();
    }

    @Override
    public void onConfigLoad(Config config) {
        super.onConfigLoad(config);
        DataReader exeReader = getExecutableReader();
        OldFroggerConfig gameConfig = this.getVersionConfig();
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
        MWIResourceEntry mapMwiEntry = getArchiveIndex().getResourceEntryByID(mapResourceId);
        String mapFilePath = mapMwiEntry != null ? mapMwiEntry.getFullFilePath() : null;

        for (int i = 0; i < this.manuallyConfiguredLevelTableEntries.size(); i++) {
            OldFroggerLevelTableEntry entry = this.manuallyConfiguredLevelTableEntries.get(i);
            if (entry.getMapResourceId() == mapResourceId)
                return entry;

            // Resolves PC maps.
            if (mapFilePath != null && mapFilePath.equalsIgnoreCase(entry.getFilePath()))
                return entry;
        }

        for (int i = 0; i < this.levelTableEntries.size(); i++) {
            OldFroggerLevelTableEntry entry = this.levelTableEntries.get(i);
            if (entry.getMapResourceId() == mapResourceId)
                return entry;

            // Resolves PC maps.
            if (mapFilePath != null && mapFilePath.equalsIgnoreCase(entry.getFilePath()))
                return entry;
        }

        return null;
    }
}