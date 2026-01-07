package net.highwayfrogs.editor.games.sony.c12;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.psx.image.PsxVramScreenSize;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.SCUtils.SCForcedLoadSoundFileType;
import net.highwayfrogs.editor.games.sony.medievil2.MediEvil2Config;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSetFile;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.DummyFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.LazySCGameFileListGroup;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

/**
 * Represents an instance of C12 Final Resistance.
 * Created by Kneesnap on 9/22/2025.
 */
@Getter
public class C12GameInstance extends SCGameInstance {
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
            return new DummyFile(this, fileData != null ? fileData.length : 0);
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
        // Do nothing for now.
    }

    @Override
    public MediEvil2Config getVersionConfig() {
        return (MediEvil2Config) super.getVersionConfig();
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
        this.primaryFrameBuffer = new PsxVramScreenSize(0, 0, 512, getDefaultFrameBufferHeight());
        this.secondaryFrameBuffer = this.primaryFrameBuffer.add(0, 256); // Y: 256 regardless of height of parent.
    }
}
