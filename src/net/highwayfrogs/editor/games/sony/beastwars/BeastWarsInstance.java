package net.highwayfrogs.editor.games.sony.beastwars;

import net.highwayfrogs.editor.PLTFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.gui.MainController.SCDisplayedFileType;

import java.util.List;

/**
 * Represents a loaded instance of the Beast Wars: Transformers game files.
 * Created by Kneesnap on 9/8/2023.
 */
public class BeastWarsInstance extends SCGameInstance {
    // MAP is four4
    // TEX (REMAP???) is 5
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
        if (fileEntry.getTypeId() == FILE_TYPE_PLT || fileEntry.hasExtension("plt"))
            return new PLTFile(this);

        return SCUtils.createSharedGameFile(fileEntry, fileData);
    }

    @Override
    protected void readTextureRemapData(DataReader exeReader, MWIFile mwiFile) {
        // TODO: Implement later.
    }

    @Override
    protected void writeTextureRemapData(DataWriter exeWriter) {
        // TODO: Implement later.
    }

    @Override
    public void setupFileTypes(List<SCDisplayedFileType> fileTypes) {
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_TIM, "TIM"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_PLT, "PLT"));
    }
}