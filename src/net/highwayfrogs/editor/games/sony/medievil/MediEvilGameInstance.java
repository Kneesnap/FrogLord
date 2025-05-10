package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.entity.MediEvilEntityTable;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVBFile;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVHFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.LazySCGameFileListGroup;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.utils.NumberUtils;

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
public class MediEvilGameInstance extends SCGameInstance {
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

        // TODO FILE_TYPE_VLO
        return SCUtils.createSharedGameFile(resourceEntry, fileData);
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

            // Create new remap.
            TextureRemapArray remap = new TextureRemapArray(this, "txl_map" + i, entry.getTextureRemapPointer());
            entry.setRemap(remap);
            addRemap(remap);
        }
    }

    @Override
    protected void onRemapRead(TextureRemapArray remap, DataReader reader) {
        super.onRemapRead(remap, reader);
        getLogger().info("Added remap " + remap.getDebugName() + " at " + NumberUtils.toHexString(remap.getReaderIndex()) + " with " + remap.getTextureIds().size() + " entries. (" + NumberUtils.toHexString(reader.getIndex()) + ")");
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
}