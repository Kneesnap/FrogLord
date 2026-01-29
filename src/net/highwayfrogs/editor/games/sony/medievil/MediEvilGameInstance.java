package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.psx.image.PsxVramBox;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.entity.MediEvilEntityTable;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.entity.MediEvilMapEntity;
import net.highwayfrogs.editor.games.sony.shared.ISCMWDHeaderGenerator;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVBFile;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVHFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.LazySCGameFileListGroup;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an instance of MediEvil game files.
 * TODO: GEN_SND.VB fails to load in retail.
 * TODO: Weird model cel set flags need investigation? (Also occurs in Frogger PSX Alpha)
 * TODO: No models seem to have collprims, is this just how the game works? Probably need to look at raw data to be sure. Perhaps they could be in the map data similar to beast wars instead?
 * TODO: Support multiple MOFs (& go over mof data again to ensure we're not missing anything).
 * TODO: Go over PS4 asset names again to see stuff we missed / might have gotten wrong. MOFFile - has unknown value MOFHolder flags -> are there values getting ignored?
 * TODO: Map Tex Anims? Map Sky Boxes?
 * TODO: Shading is done per-vertex. Right now, if you edit the shading of a polygon, it won't update other usages of the vertex.
 * Created by Kneesnap on 9/7/2023.
 */
@Getter
public class MediEvilGameInstance extends SCGameInstance implements ISCMWDHeaderGenerator {
    private final List<MediEvilLevelTableEntry> levelTable = new ArrayList<>();
    private final MediEvilEntityTable entityTable;

    public MediEvilGameInstance() {
        super(SCGameType.MEDIEVIL);
        this.entityTable = new MediEvilEntityTable(this);
    }

    private static final int FILE_TYPE_VLO = 1;
    private static final int FILE_TYPE_MOF = 2;
    private static final int FILE_TYPE_MAPMOF = 3;
    private static final int FILE_TYPE_MAP = 4;
    private static final int FILE_TYPE_QTR = 5;
    private static final int FILE_TYPE_PGD = 6;

    @Override
    public MediEvilConfig getVersionConfig() {
        return (MediEvilConfig) super.getVersionConfig();
    }

    @Override
    public SCGameFile<?> createFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        if (resourceEntry.getTypeId() == FILE_TYPE_MAP || resourceEntry.hasExtension("map"))
            return new MediEvilMapFile(this);
        if (resourceEntry.getTypeId() == 0 && (resourceEntry.hasExtension("bin") || resourceEntry.getDisplayName().startsWith("HELP")) && MediEvilHelpFile.isHelpFile(fileData))
            return new MediEvilHelpFile(this);

        // TODO FILE_TYPE_VLO
        return SCUtils.createSharedGameFile(resourceEntry, fileData);
    }

    @Override
    protected void resolveModelVloFiles() {
        super.resolveModelVloFiles();

        // Resolve based on maps after resolving for easier options.
        for (MediEvilMapFile mapFile : getMainArchive().getAllFiles(MediEvilMapFile.class)) {
            MediEvilLevelTableEntry levelTableEntry = mapFile.getLevelTableEntry();
            if (levelTableEntry == null)
                continue;

            VloFile mainVloFile = levelTableEntry.getVloFile();
            if (mainVloFile == null)
                continue;

            for (MediEvilMapEntity entity : mapFile.getEntitiesPacket().getEntities()) {
                MRModel model = entity.getModel();
                if (model != null && model.getVloFile() == null)
                    model.setVloFile(mainVloFile);
            }
        }
    }

    @Override
    protected VloFile resolveMainVlo(MRModel model) {
        // Set VLO archive to the map VLO if currently unset.
        WADFile wadFile = model.getParentWadFile();
        if (wadFile != null) {
            String wadFileName = wadFile.getFileDisplayName();
            String searchFileName = FileUtils.stripExtension(wadFileName) + ".VLO";
            if ("FIX_MOFS.WAD".equals(wadFileName)) {
                searchFileName = "FIXEDVRM.VLO";
            } else if (searchFileName.contains("_MEMDEBUG")) {
                searchFileName = searchFileName.replace("_MEMDEBUG", "_VRAM");
            } if (searchFileName.contains("_MEM3")) {
                searchFileName = searchFileName.replace("_MEM3", "_VRAM");
            } else if (searchFileName.contains("_MEM2")) {
                searchFileName = searchFileName.replace("_MEM2", "_VRAM");
            } else if (searchFileName.contains("_MEM")) {
                searchFileName = searchFileName.replace("_MEM", "_VRAM");
            }

            VloFile foundVlo = getMainArchive().getFileByName(searchFileName);
            if (foundVlo != null)
                return foundVlo;
        }

        return super.resolveMainVlo(model);
    }

    @Override
    protected void onConfigLoad(Config configObj) {
        super.onConfigLoad(configObj);
        readLevelTable(getExecutableReader());
        readEntityTable(getExecutableReader());
    }

    private void readLevelTable(DataReader reader) {
        this.levelTable.clear();
        if (this.getVersionConfig().getLevelTableAddress() < 0)
            return;

        reader.jumpTemp(this.getVersionConfig().getLevelTableAddress());
        for (int i = 0; i < this.getVersionConfig().getLevelTableSize(); i++) {
            MediEvilLevelTableEntry newEntry = new MediEvilLevelTableEntry(this);
            newEntry.load(reader);
            this.levelTable.add(newEntry);
        }
        reader.jumpReturn();
    }

    private void readEntityTable(DataReader reader) {
        this.entityTable.clear();

        if (this.getVersionConfig().getEntityTableAddress() > 0) {
            reader.jumpTemp(this.getVersionConfig().getEntityTableAddress());

            try {
                this.entityTable.load(reader);
            } catch (Throwable th) {
                getLogger().throwing("MediEvilGameInstance", "readEntityTable", th);
            } finally {
                reader.jumpReturn();
            }
        }
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MillenniumWadIndex wadIndex) {
        for (int i = 0; i < this.levelTable.size(); i++) {
            MediEvilLevelTableEntry entry = this.levelTable.get(i);
            if (entry.getTextureRemapPointer() < 0)
                continue;

            String mapCode = entry.getMapCode();
            if (mapCode != null) {
                mapCode = mapCode.toLowerCase();
            } else {
                mapCode = String.valueOf(i);
            }

            // Create new remap.
            VloFile vloFile = entry.getVloFile();
            TextureRemapArray remap = new TextureRemapArray(this, "txl_" + mapCode + "_data", entry.getTextureRemapPointer());
            if (vloFile != null)
                remap.setVloFileDefinition(vloFile.getIndexEntry());

            entry.setRemap(remap);
            addRemap(remap);
        }
    }

    @Override
    protected void onRemapRead(TextureRemapArray remap, DataReader reader) {
        super.onRemapRead(remap, reader);
        getLogger().info("Added remap %s at 0x%X with %d entries. (0x%X)", remap.getDebugName(), remap.getReaderIndex(), remap.getTextureIds().size(), reader.getIndex());
    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        fileListView.addGroup(new SCGameFileListTypeIdGroup("VLO Texture Bank", FILE_TYPE_VLO));
        fileListView.addGroup(new LazySCGameFileListGroup("TIM [PSX Image]", (file, index) -> file instanceof PSXTIMFile));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Models", FILE_TYPE_MOF));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Models", FILE_TYPE_MAPMOF));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Maps", FILE_TYPE_MAP));
        fileListView.addGroup(new LazySCGameFileListGroup("VAB Sound", (file, index) -> file instanceof SCSplitVBFile || file instanceof SCSplitVHFile));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("QTR [Quad Tree]", FILE_TYPE_QTR));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("PGD [Collision Grid]", FILE_TYPE_PGD));
    }

    @Override
    protected void setupFrameBuffers() {
        // Tested in ECTS Alpha, Build 0.28 PAL, Build 0.31, and Reviewable Version.
        this.primaryFrameBuffer = new PsxVramBox(0, 0, 512, getDefaultFrameBufferHeight());
        this.secondaryFrameBuffer = this.primaryFrameBuffer.cloneBelow();
    }

    /**
     * Returns the level table entry for the specified map resource ID, if one exists.
     * @param mapResourceId The resource ID of the map file.
     * @return levelTableEntry, or null
     */
    public MediEvilLevelTableEntry getLevelTableEntry(int mapResourceId) {
        for (int i = 0; i < this.levelTable.size(); i++) {
            MediEvilLevelTableEntry entry = this.levelTable.get(i);
            if (mapResourceId > entry.getWadResourceId()) {
                MediEvilMapFile levelTableMapFile = entry.getMapFile();
                if (levelTableMapFile != null && levelTableMapFile.getFileResourceId() == mapResourceId)
                    return entry;
            }
        }

        return null;
    }

    @Override
    public void generateMwdCHeader(@NonNull File file) {
        // Based on the data seen in the executables, and educated guesses.
        SCSourceFileGenerator.generateMwdCHeader(this, "S:\\\\", file, "MED", "STD", "VLO", "MOF", "FMOF", "MAP", "QTR", "PGD");
    }

    public void loadGame(String versionConfigName, net.highwayfrogs.editor.system.Config instanceConfig, File mwdFile, File exeFile, ProgressBarComponent progressBar) {
        super.loadGame(versionConfigName, instanceConfig, mwdFile, exeFile, progressBar);
        loadCreditsImages();
    }

    private void loadCreditsImages() {
        SCUtils.loadBsImagesByName(this, "CREDITS_MEM.WAD", 224, 192, true);
        SCUtils.loadBsImagesByName(this, "INTRO_MEM.WAD", 224, 192, false);
    }
}