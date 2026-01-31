package net.highwayfrogs.editor.games.sony.beastwars;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.psx.image.PsxVramBox;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern WAD_PATTERN_RESCUE_LEVEL = Pattern.compile("RM_LEV_(\\d)");
    private static final Pattern WAD_PATTERN_RESCUE_SHIP = Pattern.compile("RM_SHIP([12])");
    private static final Pattern WAD_PATTERN_TESTMAP = Pattern.compile("TESTMAP(\\d)");
    private static final Pattern WAD_PATTERN_MULTIPLAYER_MAP = Pattern.compile("MP(\\d)_MAP");
    private static final Pattern WAD_PATTERN_MAP_TERRAIN = Pattern.compile("MP(\\d)_([PM])_(01|02|03|IB|OB)");
    private static final Pattern WAD_PATTERN_TEST_MAP_GAME_MODEL = Pattern.compile("TESTMAP(\\d)_GM");
    private static final Pattern WAD_PATTERN_MAP_SHARE_GAME_MODEL = Pattern.compile("MS(\\d)_([PM])_GM");
    private static final Pattern WAD_PATTERN_TEST_MAP_DETAIL = Pattern.compile("MD_TESTMAP(\\d)");
    private static final Pattern WAD_PATTERN_MPLAYER = Pattern.compile("MPLAYER(\\d)");
    private static final Pattern WAD_PATTERN_MAP_DETAIL = Pattern.compile("MD(\\d)_([PM])_(01|02|03|IB|OB)");

    @Override
    protected VloFile resolveMainVlo(MRModel model) {
        WADFile wadFile = model.getParentWadFile();
        if (wadFile != null) {
            String wadFileName = FileUtils.stripExtension(wadFile.getFileDisplayName());

            Matcher matcher;
            String searchFileName = null;
            if (WAD_PATTERN_RESCUE_LEVEL.matcher(wadFileName).matches()) {
                searchFileName = wadFileName;
            } else if ((matcher = WAD_PATTERN_RESCUE_SHIP.matcher(wadFileName)).matches()) {
                if ("1".equals(matcher.group(1))) {
                    searchFileName = "RM_PREDSHIP";
                } else {
                    searchFileName = "RM_MAXSHIP";
                }
            } else if ((matcher = WAD_PATTERN_MULTIPLAYER_MAP.matcher(wadFileName)).matches()
                    || (matcher = WAD_PATTERN_MPLAYER.matcher(wadFileName)).matches()) {
                searchFileName = "MPLAYER" + matcher.group(1);
            } else if ((matcher = WAD_PATTERN_MAP_TERRAIN.matcher(wadFileName)).matches() || (matcher = WAD_PATTERN_MAP_DETAIL.matcher(wadFileName)).matches()) {
                searchFileName = "MS" + matcher.group(1) + "_" + matcher.group(2) + "_" + matcher.group(3);
            } else if ((matcher = WAD_PATTERN_TEST_MAP_GAME_MODEL.matcher(wadFileName)).matches()
                    || (matcher = WAD_PATTERN_TEST_MAP_DETAIL.matcher(wadFileName)).matches()
                    || (matcher = WAD_PATTERN_TESTMAP.matcher(wadFileName)).matches()) {
                searchFileName = "TESTMAP" + matcher.group(1);
            } else if ((matcher = WAD_PATTERN_MAP_SHARE_GAME_MODEL.matcher(wadFileName)).matches()) {
                searchFileName = "MS" + matcher.group(1) + "_" + matcher.group(2) + "_GM";
            }

            if (searchFileName != null) {
                VloFile foundVlo = getMainArchive().getFileByName(searchFileName + ".VLO");
                if (foundVlo != null)
                    return foundVlo;
            }
        }

        return super.resolveMainVlo(model);
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

    @Override
    protected void setupFrameBuffers() {
        // Tested: NTSC Prototype, Retail NTSC, Retail PAL
        this.primaryFrameBuffer = new PsxVramBox(0, 0, 320, getDefaultFrameBufferHeight());
        this.secondaryFrameBuffer = this.primaryFrameBuffer.cloneBelow();
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