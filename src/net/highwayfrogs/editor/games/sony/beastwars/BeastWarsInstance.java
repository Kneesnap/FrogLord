package net.highwayfrogs.editor.games.sony.beastwars;

import net.highwayfrogs.editor.PLTFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.gui.MainController.SCMainMenuFileGroup;
import net.highwayfrogs.editor.gui.MainController.SCMainMenuFileGroupFileID;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Represents a loaded instance of the Beast Wars: Transformers game files.
 * TODO: Many animated mofs have broken animations.
 * TODO: SFX on PC plays very poorly. PSX is fine.
 * TODO: .BPP, .DAT
 * TODO: PSX .TEX files need some work.
 * TODO: May be a level select in the PC version, also there are potentially names for the unknown map file names.
 * TODO: .PLT files cannot be viewed.
 * TODO: Collprims need finished preview in MOF editor + MAP Editor.
 * Created by Kneesnap on 9/8/2023.
 */
public class BeastWarsInstance extends SCGameInstance {
    public static final int FILE_TYPE_VLO = 1;
    public static final int FILE_TYPE_MOF = 3;
    public static final int FILE_TYPE_MAP = 4;
    public static final int FILE_TYPE_TEX = 5;
    public static final int FILE_TYPE_TIM = 8;
    public static final int FILE_TYPE_PLT = 9;

    public BeastWarsInstance() {
        super(SCGameType.BEAST_WARS);
    }

    @Override
    protected SCGameConfig makeConfig(String internalName) {
        return new BeastWarsConfig(internalName);
    }

    @Override
    public SCGameFile<?> createFile(FileEntry fileEntry, byte[] fileData) {
        if (fileEntry.getTypeId() == FILE_TYPE_MAP || Utils.testSignature(fileData, BeastWarsMapFile.FILE_SIGNATURE))
            return new BeastWarsMapFile(this);
        if (fileEntry.getTypeId() == FILE_TYPE_TEX || Utils.testSignature(fileData, BeastWarsTexFile.SIGNATURE))
            return new BeastWarsTexFile(this);
        if (fileEntry.getTypeId() == FILE_TYPE_PLT || fileEntry.hasExtension("plt"))
            return new PLTFile(this);


        return SCUtils.createSharedGameFile(fileEntry, fileData);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MWIFile mwiFile) {
        // This game does not appear to contain texture remap data.
    }

    @Override
    public void setupFileGroups(List<SCMainMenuFileGroup> fileGroups) {
        fileGroups.add(new SCMainMenuFileGroupFileID("VLO Texture Bank", FILE_TYPE_VLO));
        fileGroups.add(new SCMainMenuFileGroupFileID("Models", FILE_TYPE_MOF));
        fileGroups.add(new SCMainMenuFileGroupFileID("MAP [Playable Map]", FILE_TYPE_MAP));
        fileGroups.add(new SCMainMenuFileGroupFileID("Map Texture", FILE_TYPE_TEX));
        fileGroups.add(new SCMainMenuFileGroupFileID("TIM [PSX Image]", FILE_TYPE_TIM));
        fileGroups.add(new SCMainMenuFileGroupFileID("PLT [Palette]", FILE_TYPE_PLT));
    }
}