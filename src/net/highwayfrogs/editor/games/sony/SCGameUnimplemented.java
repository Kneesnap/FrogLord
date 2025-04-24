package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
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
    public SCGameFile<?> createFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        return SCUtils.createSharedGameFile(resourceEntry, fileData);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MillenniumWadIndex wadIndex) {

    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        // Nothing.
    }
}