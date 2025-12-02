package net.highwayfrogs.editor.games.sony.frogger;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.DemoFile;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.config.TargetPlatform;
import net.highwayfrogs.editor.file.config.data.FroggerMapWorldID;
import net.highwayfrogs.editor.file.config.data.MusicTrack;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.PickupData;
import net.highwayfrogs.editor.file.config.exe.PickupData.PickupAnimationFrame;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.config.exe.pc.PCThemeBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXThemeBook;
import net.highwayfrogs.editor.file.config.script.FroggerScript;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerDemoTable;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerDemoTableEntry;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerPCDemoTableEntry;
import net.highwayfrogs.editor.games.sony.frogger.file.FroggerPaletteFile;
import net.highwayfrogs.editor.games.sony.frogger.file.FroggerSkyLand;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGeneral;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketHeader;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerUtils;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.games.sony.shared.ISCMWDHeaderGenerator;
import net.highwayfrogs.editor.games.sony.shared.ISCTextureUser;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.LazySCGameFileListGroup;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent.SCGameFileListTypeIdGroup;
import net.highwayfrogs.editor.games.sony.shared.utils.SCAnalysisUtils.SCTextureUsage;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.File;
import java.util.*;

/**
 * Represents an instance of the game files for Frogger (1997).
 * Created by Kneesnap on 9/7/2023.
 */
@Getter
public class FroggerGameInstance extends SCGameInstance implements ISCTextureUser, ISCMWDHeaderGenerator {
    private final List<MapBook> mapLibrary = new ArrayList<>();
    private final List<MusicTrack> musicTracks = new ArrayList<>();
    private final List<LevelInfo> arcadeLevelInfo = new ArrayList<>();
    private final List<LevelInfo> raceLevelInfo = new ArrayList<>();
    private final List<LevelInfo> allLevelInfo = new ArrayList<>();
    private final Map<FroggerMapLevelID, LevelInfo> levelInfoMap = new HashMap<>();
    private final List<FormEntry> fullFormBook = new ArrayList<>();
    private final List<FroggerScript> scripts = new ArrayList<>();
    private final Map<FroggerMapLevelID, Image> levelImageMap = new HashMap<>();
    private final Map<FroggerMapTheme, FormEntry[]> allowedForms = new HashMap<>();
    private final ThemeBook[] themeLibrary = new ThemeBook[FroggerMapTheme.values().length];
    private final PickupData[] pickupData = new PickupData[FroggerFlyScoreType.values().length];
    private final TextureRemapArray skyLandTextureRemap;
    private final FroggerDemoTable demoTable;

    private static final String CHILD_RESTORE_MAP_BOOK = "MapBookRestore";
    private static final String CHILD_RESTORE_THEME_BOOK = "ThemeBookRestore";
    private static final int FILE_TYPE_ANY = 0;
    private static final int FILE_TYPE_VLO = 1;
    private static final int FILE_TYPE_SOUND = 2;
    private static final int FILE_TYPE_MOF = 3;
    private static final int FILE_TYPE_MAPMOF = 4;
    private static final int FILE_TYPE_DEMO_DATA = 6;
    public static final int FILE_TYPE_PAL = 7;

    public FroggerGameInstance() {
        super(SCGameType.FROGGER);
        this.skyLandTextureRemap = new TextureRemapArray(this, "txl_sky_land");
        this.demoTable = new FroggerDemoTable(this);
    }

    @Override
    public void loadGame(String versionConfigName, net.highwayfrogs.editor.system.Config instanceConfig, File mwdFile, File exeFile, ProgressBarComponent progressBar) {
        super.loadGame(versionConfigName, instanceConfig, mwdFile, exeFile, progressBar);

        // Setup version comparison.
        FroggerVersionComparison.setup(getMainGameFolder());
        FroggerVersionComparison.addNewVersionToConfig(this);
    }

    @Override
    public FroggerConfig getVersionConfig() {
        return (FroggerConfig) super.getVersionConfig();
    }

    @Override
    public SCGameFile<?> createFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        if (resourceEntry.getTypeId() == FILE_TYPE_ANY && resourceEntry.getDisplayName().startsWith(Constants.SKY_LAND_PREFIX)) {
            return new FroggerSkyLand(this);
        } else if ((resourceEntry.getTypeId() == FILE_TYPE_ANY && resourceEntry.hasExtension("map")) || DataUtils.testSignature(fileData, FroggerMapFilePacketHeader.IDENTIFIER)) {
            return new FroggerMapFile(this, resourceEntry);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_PAL || resourceEntry.hasExtension("pal")) {
            return new FroggerPaletteFile(this);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_DEMO_DATA || resourceEntry.hasExtension("dat")) {
            return new DemoFile(this);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_SOUND) {
            return SCUtils.makeSound(resourceEntry, fileData, null);
        } else if (resourceEntry.getTypeId() == FILE_TYPE_MOF || resourceEntry.getTypeId() == FILE_TYPE_MAPMOF) {
            return SCUtils.makeModel(resourceEntry);
        } else {
            return SCUtils.createSharedGameFile(resourceEntry, fileData);
        }
    }

    @Override
    protected VLOArchive resolveMainVlo(MRModel model) {
        WADFile wadFile = model.getParentWadFile();
        if (wadFile != null) {
            FroggerMapTheme theme = FroggerUtils.getFroggerMapTheme(wadFile);
            ThemeBook themeBook = getThemeBook(theme);
            if (themeBook != null) {
                VLOArchive themeVlo = themeBook.getVLO(FroggerUtils.isMultiplayerFile(wadFile, theme), FroggerUtils.isLowPolyMode(wadFile));
                if (themeVlo != null)
                    return themeVlo;
            }

            // Attempt to search by WAD name. (Failsafe)
            String wadFileName = wadFile.getFileDisplayName();
            String searchFileName = FileUtils.stripExtension(wadFileName) + ".VLO";
            if (searchFileName.startsWith("THEME_")) {
                searchFileName = searchFileName.substring("THEME_".length());
            } else if ("OPTIONSL.WAD".equals(wadFileName) || "OPTIONSH.WAD".equals(wadFileName) || "OPTIONS.WAD".equals(wadFileName)) {
                searchFileName = "OPT_VRAM.VLO";
            }

            VLOArchive foundVlo = getMainArchive().getFileByName(searchFileName);
            if (foundVlo != null)
                return foundVlo;
        }

        return super.resolveMainVlo(model);
    }

    @Override
    protected void setupTextureRemaps(DataReader exeReader, MillenniumWadIndex wadIndex) {
        // Add sky land.
        if (this.getVersionConfig().getSkyLandTextureAddress() > 0) {
            this.skyLandTextureRemap.setFileOffset(this.getVersionConfig().getSkyLandTextureAddress());
            addRemap(this.skyLandTextureRemap);
        }

        // ISLAND.MAP is read by piggybacking on the location of ARN1.MAP (for builds which includes the txl_island remap)
        // txl_for3 is read in a similar manner as well.
        // This occurs when those remaps are read.

        // Add remaps for individual levels.
        this.mapLibrary.forEach(book -> book.addTextureRemaps(this));
    }

    @Override
    protected void onRemapRead(TextureRemapArray remap, DataReader reader) {
        super.onRemapRead(remap, reader);

        // Hack to read island remap. Build 20 is the last build with the remap present (it's also the last build with the unique textures present.)
        if (remap instanceof FroggerTextureRemap) {
            MWIResourceEntry resourceEntry = ((FroggerTextureRemap) remap).getResourceEntry();

            if ((!this.getVersionConfig().isBeforeBuild1() && this.getVersionConfig().isAtOrBeforeBuild20()) && resourceEntry.getDisplayName().equals("ARN1.MAP")) {
                MWIResourceEntry islandMap = getResourceEntryByName("ISLAND.MAP");

                // Register island remap
                if (islandMap != null) {
                    FroggerTextureRemap islandRemap = new FroggerTextureRemap(this, islandMap, "txl_island");
                    islandRemap.setFileOffset(reader.getIndex());
                    addRemap(islandRemap);
                }
            }

            // Hack to read txl_for3 remap.
            if (!this.getVersionConfig().isAtLeastRetailWindows() && !this.getVersionConfig().isAtOrBeforeBuild23() && resourceEntry.getDisplayName().equals("FOR2.MAP")) {
                TextureRemapArray forestRemap = new TextureRemapArray(this, "txl_for3");
                forestRemap.setFileOffset(reader.getIndex());
                addRemap(forestRemap);
            }
        }
    }

    @Override
    protected boolean isEndOfRemap(TextureRemapArray current, TextureRemapArray next, DataReader reader, short value) {
        // Look for the data which comes after the remap table.
        if (next == null && isPSX() && value == 0x80) {
            reader.jumpTemp(reader.getIndex());
            long nextData = reader.readUnsignedIntAsLong();
            reader.jumpReturn();

            if (nextData == 0x00100020L)
                return true; // 'FRInput_default_map' comes after texture remaps, which seems to start with this. This happens even as far back as the April 97 build.
        }

        // The Sony Presentation Build (April '97) has extremely low texture IDs, low enough to be mistaken for a pointer.
        // The simplest solution for now is just to use the above check to end the remap, which is confirmed to work.
        return !this.getVersionConfig().isSonyPresentation() && super.isEndOfRemap(current, next, reader, value);
    }

    @Override
    public void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView) {
        fileListView.addGroup(new LazySCGameFileListGroup("MAP [Playable Maps]",
                (file, index) -> index.getTypeId() == FILE_TYPE_ANY && (index.hasExtension("map") || file instanceof FroggerMapFile)));

        fileListView.addGroup(new SCGameFileListTypeIdGroup("VLO Texture Bank", FILE_TYPE_VLO));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Models", FILE_TYPE_MOF));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Models", FILE_TYPE_MAPMOF));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("VB/VH Sound Bank", FILE_TYPE_SOUND));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("DAT [Recorded Demo]", FILE_TYPE_DEMO_DATA));
        fileListView.addGroup(new SCGameFileListTypeIdGroup("Unused Palettes (.PAL)", FILE_TYPE_PAL));
    }

    @Override
    protected void setupScriptEngine(NoodleScriptEngine engine) {
        super.setupScriptEngine(engine);
        engine.addWrapperTemplates(FroggerGameInstance.class, FroggerConfig.class, FroggerTextureRemap.class, FroggerMapFile.class,
                LevelInfo.class, FroggerMapLevelID.class, FroggerMapWorldID.class, PCMapBook.class, FroggerMapTheme.class,
                PSXMapBook.class, MapBook.class, ThemeBook.class, PCThemeBook.class, PSXThemeBook.class,
                FroggerUtils.class, FroggerMapFilePacketEntity.class, MusicTrack.class,
                FroggerDemoTable.class, FroggerPCDemoTableEntry.class, FroggerDemoTableEntry.class,
                FroggerMapFilePacketGeneral.class);
    }

    @Override
    public List<Short> getUsedTextureIds() {
        return ISCTextureUser.getTextureIdsFromUsages(getTextureUsages());
    }

    @Override
    public String getTextureUserName() {
        return null;
    }

    @Override
    public Set<SCTextureUsage> getTextureUsages() {
        Set<SCTextureUsage> textures = new HashSet<>();

        final String levelSelectName = "Level Select";
        for (int i = 0; i < this.allLevelInfo.size(); i++) {
            LevelInfo levelInfo = this.allLevelInfo.get(i);
            tryAddTexture(textures, levelInfo.getWorldLevelStackColoredImage(), levelSelectName);
            tryAddTexture(textures, levelInfo.getUnusedWorldVisitedImage(), levelSelectName);
            tryAddTexture(textures, levelInfo.getWorldNotTriedImage(), levelSelectName);
            tryAddTexture(textures, levelInfo.getLevelPreviewScreenshotImage(), levelSelectName);
            tryAddTexture(textures, levelInfo.getLevelNameImage(), levelSelectName);
            tryAddTexture(textures, levelInfo.getIngameLevelNameImage(), levelSelectName);
        }

        // Add pickup data.
        for (int i = 0; i < this.pickupData.length; i++) {
            PickupData pickupData = this.pickupData[i];
            if (pickupData == null)
                continue;

            for (int j = 0; j < pickupData.getFrames().size(); j++) {
                PickupAnimationFrame pickupFrame = pickupData.getFrames().get(j);
                GameImage image = pickupFrame.getImage();
                if (image != null)
                    textures.add(new SCTextureUsage(this, image.getTextureId(), "PickupData"));
            }
        }

        // Add sky land remap.
        if (this.skyLandTextureRemap != null)
            addTextureIdUsages(textures, this.skyLandTextureRemap.getTextureIds(), this.skyLandTextureRemap.getName());
        return textures;
    }

    private void tryAddTexture(Set<SCTextureUsage> textures, GameImage image, String name) {
        if (image == null)
            return;

        short textureId = image.getTextureId();
        if (textureId >= 0)
            textures.add(new SCTextureUsage(this, textureId, name));
    }

    /**
     * Gets this MWD's FroggerSkyLand file.
     * @return skyLand
     */
    public FroggerSkyLand getSkyLand() {
        for (SCGameFile<?> file : getMainArchive().getFiles())
            if (file instanceof FroggerSkyLand)
                return (FroggerSkyLand) file;
        throw new RuntimeException("Sky Land was not found.");
    }

    /**
     * Get a theme book by a theme.
     * @param theme The theme to get the book for.
     * @return themeBook
     */
    public ThemeBook getThemeBook(FroggerMapTheme theme) {
        return theme != null ? this.themeLibrary[theme.ordinal()] : null;
    }

    /**
     * Get a map book by a FroggerMapLevelID.
     * @param level The level to get the book for.
     * @return mapBook
     */
    public MapBook getMapBook(FroggerMapLevelID level) {
        return this.mapLibrary.size() > 0 && this.mapLibrary.size() > level.ordinal() ? this.mapLibrary.get(level.ordinal()) : null;
    }

    /**
     * Export code to a folder.
     */
    @SneakyThrows
    public void exportCode(File folder) {
        if (isPC()) {
            getLogger().warning("Cannot generate headers for PC builds yet.");
            return;
        }

        // Save MWI.
        DataWriter writer = new DataWriter(new FileReceiver(new File(folder, "FROGPSX.MWI")));
        getArchiveIndex().save(writer);
        writer.closeReceiver();

        generateMwdCHeader(new File(folder, "frogpsx.h"));
        generateVloSourceFiles(this, new File(folder, "frogvram.h"));

        getLogger().info("Generated source files.");
    }

    @Override
    public void generateMwdCHeader(@NonNull File file) {
        // Based on the data seen in FROGPSX.H in the E3 Frogger prototype.
        SCSourceFileGenerator.generateMwdCHeader(this, "L:\\\\FROGGER\\\\", file, "FR", "STD", "VLO", "SPU", "MOF", "MAPMOF", "MAPANIMMOF", "DEMODATA");
    }

    /**
     * Get the form book for the given formBookId and MapTheme.
     * @param mapTheme   The map theme the form belongs to.
     * @param formBookId The form id in question. Normally passed to ENTITY_GET_FORM_BOOK as en_form_book_id.
     * @return formBook
     */
    public FormEntry getMapFormEntry(FroggerMapTheme mapTheme, int formBookId) {
        if ((formBookId & FormEntry.FLAG_GENERAL) == FormEntry.FLAG_GENERAL)
            mapTheme = FroggerMapTheme.GENERAL;

        ThemeBook themeBook = getThemeBook(mapTheme);
        if (themeBook == null)
            return null;

        int formIndex = formBookId & (FormEntry.FLAG_GENERAL - 1);
        List<FormEntry> formBook = themeBook.getFormBook();
        return formBook != null && formBook.size() > formIndex ? formBook.get(formIndex) : null;
    }

    /**
     * Get the music track for the particular level.
     * @param level The level to get the track for.
     * @return music.
     */
    public MusicTrack getMusic(FroggerMapLevelID level) {
        return this.musicTracks.size() > level.ordinal() ? this.musicTracks.get(level.ordinal()) : null;
    }

    /**
     * Gets the LevelInfo for a given MapLevel. Returns null if not found.
     * @param level The level to get the info for.
     * @return levelInfo
     */
    public LevelInfo getLevel(FroggerMapLevelID level) {
        return this.levelInfoMap.get(level);
    }

    /**
     * Gets allowed forms for a given theme.
     * @param theme The theme to get forms for.
     * @return allowedForms
     */
    public FormEntry[] getAllowedForms(FroggerMapTheme theme) {
        return allowedForms.computeIfAbsent(theme, safeTheme -> {
            ThemeBook themeBook = this.themeLibrary[FroggerMapTheme.GENERAL.ordinal()];
            if (themeBook == null)
                return new FormEntry[0];

            List<FormEntry> formType = new ArrayList<>(themeBook.getFormBook());
            if (safeTheme != null && safeTheme != FroggerMapTheme.GENERAL) {
                ThemeBook mapBook = this.themeLibrary[safeTheme.ordinal()];
                if (mapBook != null)
                    formType.addAll(mapBook.getFormBook());
            }

            return formType.toArray(new FormEntry[0]);
        });
    }

    /**
     * Gets the pickup data for the given fly score type.
     * @param flyScoreType the fly score type to get the pickup data for
     * @return pickupData, or null if it couldn't be found
     */
    public PickupData getPickupData(FroggerFlyScoreType flyScoreType) {
        return flyScoreType != null ? this.pickupData[flyScoreType.ordinal()] : null;
    }

    @Override
    protected FroggerMainMenuUIController makeMainMenuController() {
        return new FroggerMainMenuUIController(this);
    }

    @Override
    public void onConfigLoad(Config config) {
        super.onConfigLoad(config);
        DataReader exeReader = getExecutableReader();
        readThemeLibrary(exeReader, config);
        readFormLibrary();
        readMapLibrary(exeReader, config);
        readScripts(exeReader);
        readMusicData(exeReader);
        readLevelData(exeReader);
        readPickupData(exeReader);
    }

    @Override
    protected void readExecutableData(DataReader reader, net.highwayfrogs.editor.system.Config executableConfig) {
        super.readExecutableData(reader, executableConfig);
        readDemoTable(getLogger(), reader, executableConfig);
    }

    @Override
    public net.highwayfrogs.editor.system.Config createExecutableConfig() {
        net.highwayfrogs.editor.system.Config config = super.createExecutableConfig();
        this.demoTable.toConfig(config);
        return config;
    }

    @Override
    public void writeExecutableData(DataWriter writer, MillenniumWadIndex wadIndex) {
        super.writeExecutableData(writer, wadIndex);
        writeThemeLibrary(writer);
        writeMapLibrary(writer);
        writeScripts(writer);
        this.demoTable.save(writer);
        writeMusicData(writer);
        writeLevelData(writer);
    }

    private void readPickupData(DataReader reader) {
        if (this.getVersionConfig().getPickupDataAddress() <= 0)
            return;

        reader.setIndex(this.getVersionConfig().getPickupDataAddress());
        for (int i = 0; i < this.pickupData.length; i++) {
            long tempPointer = reader.readUnsignedIntAsLong();
            reader.jumpTemp((int) (tempPointer - getRamOffset()));
            PickupData pickupData = new PickupData(this, FroggerFlyScoreType.values()[i]);
            pickupData.load(reader);
            this.pickupData[i] = pickupData;
            reader.jumpReturn();
        }
    }

    private void readThemeLibrary(DataReader reader, Config config) {
        if (this.getVersionConfig().getThemeBookAddress() <= 0)
            return;

        reader.setIndex(this.getVersionConfig().getThemeBookAddress());
        for (int i = 0; i < this.themeLibrary.length; i++) {
            ThemeBook book = TargetPlatform.makeNewThemeBook(this);
            book.setTheme(FroggerMapTheme.values()[i]);
            book.load(reader);
            this.themeLibrary[i] = book;
            Constants.logExeInfo(book);
        }

        if (config.hasChild(CHILD_RESTORE_THEME_BOOK)) {
            Config themeBookRestore = config.getChild(CHILD_RESTORE_THEME_BOOK);

            for (String key : themeBookRestore.keySet()) {
                FroggerMapTheme theme = FroggerMapTheme.getTheme(key);
                Utils.verify(theme != null, "Unknown theme: '%s'", key);
                getThemeBook(theme).handleCorrection(themeBookRestore.getString(key));
            }
        }
    }

    private void writeThemeLibrary(DataWriter exeWriter) {
        if (this.getVersionConfig().getThemeBookAddress() <= 0)
            return;

        exeWriter.setIndex(this.getVersionConfig().getThemeBookAddress());
        for (ThemeBook book : this.themeLibrary)
            book.save(exeWriter);
    }

    private void readFormLibrary() {
        FroggerMapTheme lastTheme = FroggerMapTheme.values()[0];
        for (int i = 1; i < FroggerMapTheme.values().length; i++) {
            FroggerMapTheme currentTheme = FroggerMapTheme.values()[i];

            ThemeBook lastBook = getThemeBook(lastTheme);
            ThemeBook currentBook = getThemeBook(currentTheme);
            if (currentBook == null || currentBook.getFormLibraryPointer() == 0 || lastBook == null || lastBook.getFormLibraryPointer() == 0)
                continue;

            // Determine number of form entries and compare with name bank.
            NameBank formBank = this.getVersionConfig().getFormBank().getChildBank(lastTheme.name());
            int nameCount = formBank != null ? formBank.size() : 0;
            int byteSize = this.getVersionConfig().isAtOrBeforeBuild4() ? FormEntry.OLD_BYTE_SIZE : FormEntry.BYTE_SIZE;
            int entryCount = (int) (currentBook.getFormLibraryPointer() - lastBook.getFormLibraryPointer()) / byteSize;
            if (entryCount != nameCount)
                getLogger().warning("%s has %d configured form names but %d calculated form entries in the form library.", lastTheme, nameCount, entryCount);

            // Load form library.
            lastBook.loadFormLibrary(this, nameCount);
            lastTheme = currentTheme;
        }

        // Load the last theme, which we use the number of names for to determine size.
        ThemeBook lastBook = getThemeBook(lastTheme);
        if (lastBook != null && lastBook.getFormLibraryPointer() != 0) {
            int nameCount = this.getVersionConfig().getFormBank().getChildBank(lastTheme.name()).size();
            lastBook.loadFormLibrary(this, nameCount);
        }
    }

    private void readMapLibrary(DataReader reader, Config config) {
        if (this.getVersionConfig().getMapBookAddress() <= 0)
            return;

        reader.setIndex(this.getVersionConfig().getMapBookAddress());
        int themeAddress = this.getVersionConfig().isSonyPresentation() ? Integer.MAX_VALUE : this.getVersionConfig().getThemeBookAddress();
        if (themeAddress >= 0) {
            while (themeAddress > reader.getIndex()) {
                MapBook book = TargetPlatform.makeNewMapBook(this);
                book.load(reader);
                this.mapLibrary.add(book);
                Constants.logExeInfo(book);

                if (this.getVersionConfig().isSonyPresentation())
                    break; // There's only one map.
            }
        }

        if (!config.hasChild(CHILD_RESTORE_MAP_BOOK))
            return;

        Config mapBookRestore = config.getChild(CHILD_RESTORE_MAP_BOOK);
        for (String key : mapBookRestore.keySet()) {
            FroggerMapLevelID level = FroggerMapLevelID.getByName(key);
            Utils.verify(level != null, "Unknown level: '%s'", key);
            MapBook mapBook = getMapBook(level);
            Utils.verify(mapBook != null, "Cannot modify %s, its level doesn't exist.", key);
            mapBook.handleCorrection(mapBookRestore.getString(key));
        }
    }

    private void writeMapLibrary(DataWriter exeWriter) {
        if (this.getVersionConfig().getMapBookAddress() <= 0)
            return;

        exeWriter.setIndex(this.getVersionConfig().getMapBookAddress());
        this.mapLibrary.forEach(book -> book.save(exeWriter));
    }

    private void readDemoTable(ILogger logger, DataReader reader, net.highwayfrogs.editor.system.Config config) {
        try {
            if (!this.demoTable.resolveFromConfig(config))
                this.demoTable.resolveDemoTableAddress(logger);

            this.demoTable.load(reader);
        } catch (Throwable th) {
            Utils.handleError(logger, th, false, "Failed to read demo table data.");
        }
    }

    private void readScripts(DataReader reader) {
        if (this.getVersionConfig().getScriptArrayAddress() <= 0)
            return; // Wasn't specified.

        reader.setIndex(this.getVersionConfig().getScriptArrayAddress());
        for (int i = 0; i < this.getVersionConfig().getScriptBank().size(); i++) {
            long address = reader.readUnsignedIntAsLong();
            if (address == 0) { // Default / null.
                this.scripts.add(FroggerScript.EMPTY_SCRIPT);
                continue;
            }

            reader.jumpTemp((int) (address - getRamOffset()));
            FroggerScript newScript = new FroggerScript(this);
            this.scripts.add(newScript); // Adds before loading so getName() can be accessed.
            try {
                newScript.load(reader);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Failed to load script '%s'.", this.getVersionConfig().getScriptBank().getName(i));
            } finally {
                reader.jumpReturn();
            }
        }
    }

    private void writeScripts(DataWriter exeWriter) {
        if (this.getVersionConfig().getScriptArrayAddress() <= 0)
            return; // Wasn't specified.

        DataReader reader = getExecutableReader();
        reader.setIndex(this.getVersionConfig().getScriptArrayAddress());
        for (int i = 0; i < this.scripts.size(); i++) {
            long address = reader.readUnsignedIntAsLong();
            if (address == 0) // Default / null.
                continue;

            FroggerScript script = this.scripts.get(i);
            if (script.isTooLarge())
                getLogger().warning("Saving %s, which is larger than what is considered safe!", script.getName());

            exeWriter.setIndex((int) (address - getRamOffset()));
            script.save(exeWriter);
        }
    }

    private void readMusicData(DataReader reader) {
        if (this.getVersionConfig().getMusicAddress() <= 0)
            return;

        byte readByte;
        reader.setIndex(this.getVersionConfig().getMusicAddress());
        while ((readByte = reader.readByte()) != MusicTrack.TERMINATOR)
            this.musicTracks.add(MusicTrack.getTrackById(this, readByte));
    }

    private void writeMusicData(DataWriter exeWriter) {
        if (this.getVersionConfig().getMusicAddress() <= 0)
            return;

        exeWriter.setIndex(this.getVersionConfig().getMusicAddress());
        this.musicTracks.forEach(track -> exeWriter.writeByte(track.getTrack(this)));
        exeWriter.writeByte(MusicTrack.TERMINATOR);
    }

    private void readLevelData(DataReader reader) {
        if (this.getVersionConfig().getArcadeLevelAddress() <= 0)
            return; // No level select is present.

        reader.setIndex(this.getVersionConfig().getArcadeLevelAddress());
        LevelInfo level = null;
        while (level == null || !level.isTerminator()) {
            level = new LevelInfo(this);
            level.load(reader);
            this.arcadeLevelInfo.add(level);
            Constants.logExeInfo(level);
        }

        level = null;
        while (level == null || !level.isTerminator()) {
            level = new LevelInfo(this);
            level.load(reader);
            this.raceLevelInfo.add(level);
            Constants.logExeInfo(level);
        }

        this.allLevelInfo.addAll(this.arcadeLevelInfo);
        this.allLevelInfo.addAll(this.raceLevelInfo);
        for (LevelInfo info : this.allLevelInfo)
            this.levelInfoMap.put(info.getLevel(), info);
    }

    private void writeLevelData(DataWriter exeWriter) {
        if (this.getVersionConfig().getArcadeLevelAddress() <= 0)
            return; // No level select is present.

        exeWriter.setIndex(this.getVersionConfig().getArcadeLevelAddress());
        this.arcadeLevelInfo.forEach(level -> level.save(exeWriter));
        this.raceLevelInfo.forEach(level -> level.save(exeWriter));
    }
}