package net.highwayfrogs.editor.games.sony.medievil;

import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VBAudioBody;
import net.highwayfrogs.editor.file.sound.VHAudioHeader;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.gui.MainController.LazySCMainMenuFileGroup;
import net.highwayfrogs.editor.gui.MainController.SCMainMenuFileGroup;
import net.highwayfrogs.editor.gui.MainController.SCMainMenuFileGroupFileID;

import java.util.List;

/**
 * Represents an instance of MediEvil game files.
 * TODO: GEN_SND.VB fails to load in retail.
 * TODO: Weird model cel set flags need investigation? (Also occurs in Frogger PSX Alpha)
 * TODO: No models seem to have collprims, is this just how the game works? Probably need to look at raw data to be sure. Perhaps they could be in the map data similar to beast wars instead?
 * TODO: Support multiple MOFs (& go over mof data again to ensure we're not missing anything).
 * TODO: Go over PS4 asset names again to see stuff we missed / might have gotten wrong. MOFFile - has unknown value MOFHolder flags -> are there values getting ignored?
 * TODO: When adding map collprims (if I'm remembering right and they do exist), make sure to review flag data.
 * Created by Kneesnap on 9/7/2023.
 */
public class MedievilGameInstance extends SCGameInstance {
    public MedievilGameInstance() {
        super(SCGameType.MEDIEVIL);
    }

    private static final int FILE_TYPE_VLO = 1;
    private static final int FILE_TYPE_MOF = 2;
    private static final int FILE_TYPE_MAPMOF = 3;
    private static final int FILE_TYPE_MAP = 4;
    private static final int FILE_TYPE_QTR = 5;
    private static final int FILE_TYPE_PGD = 6;

    @Override
    protected SCGameConfig makeConfig(String internalName) {
        return new SCGameConfig(internalName);
    }

    @Override
    public SCGameFile<?> createFile(FileEntry fileEntry, byte[] fileData) {
        // TODO FILE_TYPE_VLO
        return SCUtils.createSharedGameFile(fileEntry, fileData);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MWIFile mwiFile) {
        // TODO: IMPLEMENT.
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
}