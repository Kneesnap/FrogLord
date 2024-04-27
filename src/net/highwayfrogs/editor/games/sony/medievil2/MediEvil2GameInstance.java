package net.highwayfrogs.editor.games.sony.medievil2;

import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.LazySCGameFileListGroup;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;

/**
 * Represents an instance of MediEvil 2 game files.
 * Created by RampantSpirit on 9/14/2023. Based on MediEvilGameInstance.
 */
public class MediEvil2GameInstance extends SCGameInstance {
    public MediEvil2GameInstance() {
        super(SCGameType.MEDIEVIL2);
    }

    private static final int FILE_TYPE_VLO = 1;

    @Override
    public SCGameFile<?> createFile(MWIFile.FileEntry fileEntry, byte[] fileData) {
        // TODO FILE_TYPE_VLO
        return SCUtils.createSharedGameFile(fileEntry, fileData);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MWIFile mwiFile) {
        // TODO: IMPLEMENT.
    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        fileListView.addGroup(new SCGameFileListTypeIdGroup("VLO Texture Bank", FILE_TYPE_VLO));
        fileListView.addGroup(new LazySCGameFileListGroup("TIM [PSX Image]", (file, index) -> file instanceof PSXTIMFile));
    }
}