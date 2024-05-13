package net.highwayfrogs.editor.games.sony.medievil2;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2LevelDefinition.MediEvil2LevelSectionDefinition;
import net.highwayfrogs.editor.games.sony.medievil2.map.MediEvil2Map;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.LazySCGameFileListGroup;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an instance of MediEvil 2 game files.
 *
 * TODO:
 *  - Audio files are using a slightly different format now.
 *  - Allow viewing maps as a whole.
 *  - Support more sections of a map file.
 *  - New model format support
 *  - Txt File Support?
 *  - Recreate file list as best as possible. I think because there are so many unknown, we should prefix guesses which are uncertain with ??
 * Created by RampantSpirit on 9/14/2023. Based on MediEvilGameInstance.
 */
@Getter
public class MediEvil2GameInstance extends SCGameInstance {
    private final List<MediEvil2LevelDefinition> levelTable = new ArrayList<>();

    public MediEvil2GameInstance() {
        super(SCGameType.MEDIEVIL2);
    }

    private static final int FILE_TYPE_VLO = 1;
    private static final int FILE_TYPE_STAT = 3;
    private static final int FILE_TYPE_SKEL = 4;
    private static final int FILE_TYPE_ANIM = 5;
    private static final int FILE_TYPE_MAP = 6;
    private static final int FILE_TYPE_MAP_ALTERNATE = 7;
    private static final int FILE_TYPE_VH = 8;
    private static final int FILE_TYPE_VB = 9;
    private static final int FILE_TYPE_VB_ALTERNATE = 10;

    @Override
    public SCGameFile<?> createFile(MWIFile.FileEntry fileEntry, byte[] fileData) {
        // TODO: SKEL, ANIM, STAT.

        if (fileEntry.getTypeId() == FILE_TYPE_MAP || fileEntry.getTypeId() == FILE_TYPE_MAP_ALTERNATE)
            return new MediEvil2Map(this);

        return SCUtils.createSharedGameFile(fileEntry, fileData, isSoundFile(fileEntry.getTypeId()));
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MWIFile mwiFile) {
        for (int i = 0; i < this.levelTable.size(); i++) {
            MediEvil2LevelDefinition levelDefinition = this.levelTable.get(i);
            for (int j = 0; j < levelDefinition.getLevelSections().size(); j++) {
                MediEvil2LevelSectionDefinition levelSection = levelDefinition.getLevelSections().get(j);
                TextureRemapArray sectionRemap = levelSection.getRemap();
                if (sectionRemap != null)
                    addRemap(sectionRemap);
            }
        }
    }

    @Override
    public MediEvil2Config getConfig() {
        return (MediEvil2Config) super.getConfig();
    }

    @Override
    public void onConfigLoad(Config config) {
        super.onConfigLoad(config);
        DataReader exeReader = getExecutableReader();
        readLevelTable(exeReader);
    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        fileListView.addGroup(new SCGameFileListTypeIdGroup("VLO Texture Banks", FILE_TYPE_VLO));
        fileListView.addGroup(new LazySCGameFileListGroup("Map Data", (file, index) -> file instanceof SCMapFile<?>));
        fileListView.addGroup(new LazySCGameFileListGroup("Model Files", (file, index) -> index.getTypeId() == FILE_TYPE_ANIM || index.getTypeId() == FILE_TYPE_SKEL || index.getTypeId() == FILE_TYPE_STAT));
        fileListView.addGroup(new LazySCGameFileListGroup("PSX TIM Images", (file, index) -> file instanceof PSXTIMFile));
        fileListView.addGroup(new LazySCGameFileListGroup("SFX Banks", (file, index) -> isSoundFile(index.getTypeId())));
    }

    private void readLevelTable(DataReader reader) {
        if (getConfig().getLevelTableAddress() <= 0 || getConfig().getLevelTableEntryCount() <= 0)
            return;

        // Read the level table.
        this.levelTable.clear();
        reader.setIndex(getConfig().getLevelTableAddress());
        for (int i = 0; i < getConfig().getLevelTableEntryCount(); i++) {
            MediEvil2LevelDefinition levelDefinition = new MediEvil2LevelDefinition(this);
            levelDefinition.load(reader);
            this.levelTable.add(levelDefinition);
        }
    }

    private static boolean isSoundFile(int typeId) {
        return typeId == FILE_TYPE_VH || typeId == FILE_TYPE_VB || typeId == FILE_TYPE_VB_ALTERNATE;
    }
}