package net.highwayfrogs.editor.games.sony.c12;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.psx.image.PsxVramBox;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.SCUtils.SCForcedLoadSoundFileType;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2Config;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.section.SCLevelDefinition;
import net.highwayfrogs.editor.games.sony.shared.map.section.SCLevelSectionDefinition;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSetFile;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.LazySCGameFileListGroup;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an instance of C12 Final Resistance.
 * Created by Kneesnap on 9/22/2025.
 */
@Getter
public class C12GameInstance extends SCGameInstance {
    private final List<SCLevelDefinition> levelTable = new ArrayList<>();

    public C12GameInstance() {
        super(SCGameType.C12);
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
    public SCGameFile<?> createFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        if (resourceEntry.getTypeId() == FILE_TYPE_STAT) {
            return new PTStaticFile(this);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_SKEL) {
            return new PTSkeletonFile(this);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_ANIM) {
            return new PTActionSetFile(this);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_MAP || resourceEntry.getTypeId() == FILE_TYPE_MAP_ALTERNATE) {
            return new C12MapFile(this);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_VB || resourceEntry.getTypeId() == FILE_TYPE_VB_ALTERNATE) {
            return SCUtils.makeSound(resourceEntry, fileData, SCForcedLoadSoundFileType.BODY);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_VH) {
            return SCUtils.makeSound(resourceEntry, fileData, SCForcedLoadSoundFileType.HEADER);
        } else {
            return SCUtils.createSharedGameFile(resourceEntry, fileData);
        }
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MillenniumWadIndex wadIndex) {
        for (int i = 0; i < this.levelTable.size(); i++) {
            SCLevelDefinition levelDefinition = this.levelTable.get(i);
            for (int j = 0; j < levelDefinition.getLevelSections().size(); j++) {
                SCLevelSectionDefinition levelSection = levelDefinition.getLevelSections().get(j);
                TextureRemapArray sectionRemap = levelSection.getRemap();
                if (sectionRemap != null)
                    addRemap(sectionRemap);
            }
        }
    }

    @Override
    public MediEvil2Config getVersionConfig() {
        return (MediEvil2Config) super.getVersionConfig();
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
        fileListView.addGroup(new LazySCGameFileListGroup("SFX Banks", (file, index) -> index.getTypeId() == FILE_TYPE_VH || index.getTypeId() == FILE_TYPE_VB || index.getTypeId() == FILE_TYPE_VB_ALTERNATE));
    }

    @Override
    protected void setupFrameBuffers() {
        // NTSC is probably 512x236, not 512x240. But w/e, it's fine for now.
        this.primaryFrameBuffer = new PsxVramBox(0, 0, 512, getDefaultFrameBufferHeight());
        this.secondaryFrameBuffer = this.primaryFrameBuffer.add(0, 256); // Y: 256 regardless of height of parent.
    }

    private void readLevelTable(DataReader reader) {
        if (this.getVersionConfig().getLevelTableAddress() <= 0 || this.getVersionConfig().getLevelTableEntryCount() <= 0)
            return;

        // Read the level table.
        this.levelTable.clear();
        reader.setIndex(this.getVersionConfig().getLevelTableAddress());
        for (int i = 0; i < this.getVersionConfig().getLevelTableEntryCount(); i++) {
            SCLevelDefinition levelDefinition = new SCLevelDefinition(this);
            levelDefinition.load(reader);
            this.levelTable.add(levelDefinition);
        }
    }
}
