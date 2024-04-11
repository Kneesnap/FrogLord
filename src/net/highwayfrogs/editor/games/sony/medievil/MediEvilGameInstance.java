package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VBAudioBody;
import net.highwayfrogs.editor.file.sound.VHAudioHeader;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.entity.MediEvilEntityTable;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.MainController.LazySCMainMenuFileGroup;
import net.highwayfrogs.editor.gui.MainController.SCMainMenuFileGroup;
import net.highwayfrogs.editor.gui.MainController.SCMainMenuFileGroupFileID;
import net.highwayfrogs.editor.utils.Utils;

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
    public MediEvilConfig getConfig() {
        return (MediEvilConfig) super.getConfig();
    }

    @Override
    protected MediEvilConfig makeConfig(String internalName) {
        return new MediEvilConfig(internalName);
    }

    @Override
    public SCGameFile<?> createFile(FileEntry fileEntry, byte[] fileData) {
        if (fileEntry.getTypeId() == FILE_TYPE_MAP || fileEntry.hasExtension("map"))
            return new MediEvilMapFile(this);

        // TODO FILE_TYPE_VLO
        return SCUtils.createSharedGameFile(fileEntry, fileData);
    }

    @Override
    protected void onConfigLoad(Config configObj) {
        super.onConfigLoad(configObj);
        readLevelTable(getExecutableReader());
        readEntityTable(getExecutableReader());
    }

    private void readLevelTable(DataReader reader) {
        this.levelTable.clear();
        if (getConfig().getLevelTableAddress() < 0)
            return;

        reader.jumpTemp(getConfig().getLevelTableAddress());
        for (int i = 0; i < getConfig().getLevelTableSize(); i++) {
            MediEvilLevelTableEntry newEntry = new MediEvilLevelTableEntry(this);
            newEntry.load(reader);
            this.levelTable.add(newEntry);
        }
        reader.jumpReturn();
    }

    private void readEntityTable(DataReader reader) {
        this.entityTable.clear();

        if (getConfig().getEntityTableAddress() > 0) {
            reader.jumpTemp(getConfig().getEntityTableAddress());

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
    protected void setupTextureRemaps(DataReader exeReader, MWIFile mwiFile) {
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
        getLogger().info("Added remap " + remap.getDebugName() + " at " + Utils.toHexString(remap.getReaderIndex()) + " with " + remap.getTextureIds().size() + " entries. (" + Utils.toHexString(reader.getIndex()) + ")");
    }

    @Override
    public void setupFileGroups(List<SCMainMenuFileGroup> fileGroups) {
        fileGroups.add(new SCMainMenuFileGroupFileID("VLO Texture Bank", FILE_TYPE_VLO));
        fileGroups.add(new LazySCMainMenuFileGroup("TIM [PSX Image]", (file, index) -> file instanceof PSXTIMFile));
        fileGroups.add(new SCMainMenuFileGroupFileID("Models", FILE_TYPE_MOF));
        fileGroups.add(new SCMainMenuFileGroupFileID("Models", FILE_TYPE_MAPMOF));
        fileGroups.add(new SCMainMenuFileGroupFileID("Maps", FILE_TYPE_MAP));
        fileGroups.add(new LazySCMainMenuFileGroup("VAB Sound", (file, index) -> file instanceof VBAudioBody<?> || file instanceof VHAudioHeader));
        fileGroups.add(new SCMainMenuFileGroupFileID("QTR [Quad Tree]", FILE_TYPE_QTR));
        fileGroups.add(new SCMainMenuFileGroupFileID("PGD [Collision Grid]", FILE_TYPE_PGD));
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
                if (levelTableMapFile != null && levelTableMapFile.getIndexEntry().getResourceId() == mapResourceId)
                    return entry;
            }
        }

        return null;
    }
}