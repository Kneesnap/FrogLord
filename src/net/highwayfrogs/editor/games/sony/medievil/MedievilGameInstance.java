package net.highwayfrogs.editor.games.sony.medievil;

import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.psx.PSXVBFile;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.gui.MainController.SCDisplayedFileType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an instance of MediEvil game files.
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
    protected void readTextureRemapData(DataReader exeReader, MWIFile mwiFile) {
        // TODO: IMPLEMENT.
    }

    @Override
    protected void writeTextureRemapData(DataWriter exeWriter) {
        // TODO: Implement.
    }

    @Override
    public void setupFileTypes(List<SCDisplayedFileType> fileTypes) {
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_VLO, "VLO Texture Bank"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_MOF, "Models"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_MAPMOF, "Models"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_MAP, "Maps"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_QTR, "QTR (Quad Tree)"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_PGD, "PGD (Collision Grid)"));
    }

    @Override
    public void setupUncategorizedFileTypes(Map<String, String> uncategorizedFileTypes) {
        uncategorizedFileTypes.clear();
        uncategorizedFileTypes.put("PSXTIMFile", "TIM");
        uncategorizedFileTypes.put("PSXVBFile", "VB/VH Sound Bank");
        uncategorizedFileTypes.put("PSXVHFile", "VB/VH Sound Bank");
    }
}