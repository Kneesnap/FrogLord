package net.highwayfrogs.editor.games.sony.medievil2;

import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.gui.MainController;

import java.util.List;

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
    protected SCGameConfig makeConfig(String internalName) {
        return new SCGameConfig(internalName);
    }

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
    public void setupFileTypes(List<MainController.SCDisplayedFileType> fileTypes) {
        fileTypes.add(new MainController.SCDisplayedFileType(FILE_TYPE_VLO, "VLO Texture Bank"));
        // TODO: Create category for .TIM file.
    }
}