package net.highwayfrogs.editor.games.sony.beastwars;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a loaded instance of the Beast Wars: Transformers game files.
 * TODO: Many animated mofs have broken animations.
 * TODO: .DAT
 * TODO: Collprims need finished preview in MOF editor + MAP Editor.
 * TODO: Light UI manager.
 * Created by Kneesnap on 9/8/2023.
 */
@Getter
public class BeastWarsInstance extends SCGameInstance {
    private final List<Integer> modelRemaps = new ArrayList<>();

    public static final int FILE_TYPE_STD = 0;
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
    public BeastWarsConfig getVersionConfig() {
        return (BeastWarsConfig) super.getVersionConfig();
    }

    @Override
    public SCGameFile<?> createFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        if (resourceEntry.getTypeId() == FILE_TYPE_MAP || DataUtils.testSignature(fileData, BeastWarsMapFile.FILE_SIGNATURE))
            return new BeastWarsMapFile(this);
        if (resourceEntry.getTypeId() == FILE_TYPE_TEX || DataUtils.testSignature(fileData, BeastWarsTexFile.SIGNATURE))
            return new BeastWarsTexFile(this);
        if (resourceEntry.getTypeId() == FILE_TYPE_PLT || resourceEntry.hasExtension("plt"))
            return new PLTFile(this);
        if (resourceEntry.getTypeId() == FILE_TYPE_STD && resourceEntry.hasExtension("bpp"))
            return new BeastWarsBPPImageFile(this);

        return SCUtils.createSharedGameFile(resourceEntry, fileData);
    }

    @Override
    protected void onConfigLoad(Config configObj) {
        super.onConfigLoad(configObj);

        DataReader exeReader = getExecutableReader();
        readModelRemaps(exeReader);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MillenniumWadIndex wadIndex) {
        // This game does not appear to contain texture remap data.
    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        fileListView.addGroup(new SCGameFileListTypeIdGroup("VLO Texture Bank", FILE_TYPE_VLO));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Models", FILE_TYPE_MOF));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("MAP [Playable Map]", FILE_TYPE_MAP));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Map Texture", FILE_TYPE_TEX));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("TIM [PSX Image]", FILE_TYPE_TIM));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("PLT [Palette]", FILE_TYPE_PLT));
    }

    private void readModelRemaps(DataReader reader) {
        this.modelRemaps.clear();
        if (getVersionConfig().getModelRemapTablePointer() <= 0 || getVersionConfig().getModelRemapTableLength() <= 0)
            return;

        reader.setIndex(getVersionConfig().getModelRemapTablePointer());
        for (int i = 0; i < getVersionConfig().getModelRemapTableLength(); i++)
            this.modelRemaps.add(reader.readInt());
    }
}