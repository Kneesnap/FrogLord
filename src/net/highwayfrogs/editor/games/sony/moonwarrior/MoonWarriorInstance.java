package net.highwayfrogs.editor.games.sony.moonwarrior;

import net.highwayfrogs.editor.games.psx.image.PsxVramBox;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.LazySCGameFileListGroup;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

/**
 * Implements basic
 * Created by Kneesnap on 5/7/2024.
 */
public class MoonWarriorInstance extends SCGameInstance {
    public static final int FILE_TYPE_MAP_VLO = 1;
    public static final int FILE_TYPE_MAP_VH = 2;
    public static final int FILE_TYPE_MAP_VB = 3;
    public static final int FILE_TYPE_MAP = 9;
    public static final int FILE_TYPE_TXT = 10;
    public MoonWarriorInstance() {
        super(SCGameType.MOONWARRIOR);
    }

    @Override
    public SCGameFile<?> createFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        if (resourceEntry.getTypeId() == FILE_TYPE_MAP)
            return new MoonWarriorMap(this);

        return SCUtils.createSharedGameFile(resourceEntry, fileData);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MillenniumWadIndex wadIndex) {
        // It doesn't appear Moon Warrior has any texture remaps, unless you consider the VLO itself a texture remap.
    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Map Image Banks", FILE_TYPE_MAP_VLO));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Map", FILE_TYPE_MAP));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Txt", FILE_TYPE_TXT));
        fileListView.addGroup(new LazySCGameFileListGroup("Sound", (file, index) -> index.getTypeId() == FILE_TYPE_MAP_VB || index.getTypeId() == FILE_TYPE_MAP_VH));
    }

    @Override
    protected void setupFrameBuffers() {
        // Tested in ECTS Alpha, Build 0.05a
        this.primaryFrameBuffer = new PsxVramBox(0, 0, 512, getDefaultFrameBufferHeight());
        this.secondaryFrameBuffer = this.primaryFrameBuffer.add(0, 256); // Regardless of the actual screen height, the second framebuffer seems to be placed at y=256.

    }
}