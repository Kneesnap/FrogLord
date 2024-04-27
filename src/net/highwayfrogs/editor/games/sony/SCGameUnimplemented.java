package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;

/**
 * Represents a game which support hasn't been added for yet.
 * Created by Kneesnap on 9/9/2023.
 */
public class SCGameUnimplemented extends SCGameInstance {
    public SCGameUnimplemented(SCGameType gameType) {
        super(gameType);
    }

    @Override
    public SCGameFile<?> createFile(FileEntry fileEntry, byte[] fileData) {
        return SCUtils.createSharedGameFile(fileEntry, fileData);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MWIFile mwiFile) {

    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        // Nothing.
    }
}