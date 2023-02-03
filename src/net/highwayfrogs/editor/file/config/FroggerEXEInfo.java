package net.highwayfrogs.editor.file.config;

import javafx.scene.image.Image;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.data.MusicTrack;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.PickupData;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.general.DemoTableEntry;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.config.script.FroggerScript;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.file.writer.FixedArrayReceiver;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Information about a specific frogger.exe file.
 * Created by Kneesnap on 8/18/2018.
 */
@Getter
public class FroggerEXEInfo extends Config {
    private MWDFile MWD;
    private MWIFile MWI;
    private ThemeBook[] themeLibrary;
    private final List<MapBook> mapLibrary = new ArrayList<>();
    private final Map<FileEntry, List<Short>> remapTable = new HashMap<>();
    private final List<MusicTrack> musicTracks = new ArrayList<>();
    private final List<LevelInfo> arcadeLevelInfo = new ArrayList<>();
    private final List<LevelInfo> raceLevelInfo = new ArrayList<>();
    private final List<LevelInfo> allLevelInfo = new ArrayList<>();
    private final Map<MAPLevel, LevelInfo> levelInfoMap = new HashMap<>();
    private final List<DemoTableEntry> demoTableEntries = new ArrayList<>();
    private final List<Long> bmpTexturePointers = new ArrayList<>();
    private final List<FormEntry> fullFormBook = new ArrayList<>();
    private final List<FroggerScript> scripts = new ArrayList<>();
    private final short[] cosEntries = new short[ACOSTABLE_ENTRIES];
    private final short[] sinEntries = new short[ACOSTABLE_ENTRIES];
    private PickupData[] pickupData;
    private final String internalName;
    private boolean hasConfigIdentifier;
    private final Map<MAPLevel, Image> levelImageMap = new HashMap<>();
    private final Map<MAPTheme, FormEntry[]> allowedForms = new HashMap<>();
    private final List<Short> islandRemap = new ArrayList<>();


    private String name;
    private int build;
    private long ramPointerOffset;
    private int MWIOffset;
    private int MWILength;
    private int mapBookAddress;
    private int themeBookAddress;
    private int arcadeLevelAddress;
    private int bmpPointerAddress;
    private int musicAddress;
    private int demoTableAddress;
    private int pickupDataAddress;
    private int scriptArrayAddress;
    private int skyLandTextureAddress;
    private boolean prototype;
    private boolean demo;
    private TargetPlatform platform;
    private NameBank soundBank;
    private NameBank animationBank;
    private NameBank formBank;
    private NameBank entityBank;
    private NameBank scriptBank; // Name of scripts.
    private NameBank scriptCallbackBank; // Name of scripts.
    private final Map<String, FroggerMapConfig> mapConfigs = new HashMap<>();
    private final FroggerMapConfig defaultMapConfig = new FroggerMapConfig();
    private final Map<String, int[]> hiddenPartIds = new HashMap<>();
    private final Map<String, String> mofRenderOverrides = new HashMap<>();

    private DataReader reader;
    private byte[] exeBytes;
    private final File folder;
    private final File inputFile;
    private List<String> fallbackFileNames;

    private static final int ACOSTABLE_ENTRIES = 4096;

    public static final String FIELD_NAME = "name";
    private static final String FIELD_FILE_NAMES = "Files";

    private static final String CHILD_RESTORE_MAP_BOOK = "MapBookRestore";
    private static final String CHILD_RESTORE_THEME_BOOK = "ThemeBookRestore";
    public static final String CHILD_IMAGE_NAMES = "ImageNames";
    private static final String CHILD_MOF_FORCE_VLO = "ForceVLO";

    public FroggerEXEInfo(File inputExe, InputStream inputStream, String internalName, boolean hasConfigIdentifier) {
        super(inputStream);
        this.inputFile = inputExe;
        this.folder = inputExe.getParentFile();
        this.internalName = internalName;
        this.hasConfigIdentifier = hasConfigIdentifier;
    }

    /**
     * Get a list of hardcoded file names from this config.
     * @return fileNames
     */
    public List<String> getFileNames() {
        if (this.fallbackFileNames != null)
            return this.fallbackFileNames;

        this.fallbackFileNames = new ArrayList<>();
        if (hasChild(FIELD_FILE_NAMES))
            this.fallbackFileNames.addAll(getChild(FIELD_FILE_NAMES).getText());
        return this.fallbackFileNames;
    }

    /**
     * Loads the remap table from the Frogger EXE.
     * @param mapEntry the map file entry.
     * @return remapTable
     */
    public List<Short> getRemapTable(FileEntry mapEntry) {
        return getRemapTable().get(mapEntry);
    }

    /**
     * Read data from the EXE which needs reading.
     */
    public void setup() {
        loadBanks();
        readConfig();
        readMapConfigs();
        readMWI();
        readCosTable();
        readThemeLibrary();
        readDemoTable();
        readMapLibrary();
        readScripts();
        readRemapData();
        readMusicData();
        readLevelData();
        readBmpPointerData();
        readPickupData();
        readHiddenParts();
        readMofOverrides();
        this.MWD = new MWDFile(getMWI());
    }

    private void readConfig() {
        this.name = getString(FIELD_NAME);
        this.build = getInt("build", -1);
        this.demo = getBoolean("demo");
        this.prototype = getBoolean("prototype");
        this.platform = getEnum("platform", TargetPlatform.class);
        this.MWIOffset = getInt("mwiOffset");
        this.MWILength = getInt("mwiLength");
        this.themeBookAddress = getInt("themeBook", -1);
        this.mapBookAddress = getInt("mapBook", -1);
        this.demoTableAddress = getInt("demoTable", -1);
        this.ramPointerOffset = getLong("ramOffset"); // If I have an offset in a file, adding this number will give its pointer.
        this.arcadeLevelAddress = getInt("arcadeLevelAddress", 0);
        this.musicAddress = getInt("musicAddress", -1); // Music is generally always the same data, so you can find it with a search.
        this.bmpPointerAddress = getInt("bmpPointerAddress", 0); // Gives output to assist in finding.
        this.pickupDataAddress = getInt("pickupData", 0); // Pointer to Pickup_data[] in ent_gen. If this is not set, bugs will not have textures in the viewer. On PSX, search for 63 63 63 00 then after this entries image pointers, there's Pickup_data.
        this.scriptArrayAddress = getInt("scripts", 0); // Get this by searching for "07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 1e 00 00 00 03 00 00 00 01 00 00 00 07 00 00 00". Search for the pointer that points to this (Don't forget to include ramOffset)
        this.skyLandTextureAddress = getInt("txl_sky_land", 0); // Get this by searching for the hex texture ids of sky images as shorts. The textures in the PSX US Demo are textures #98, #97, #96. -> 1723, 1722, 1753 -> "BB 06 BA 06 D9 06".
    }

    private void readMapConfigs() {
        if (!hasChild("MapConfig"))
            return;

        Config defaultMapConfig = getChild("MapConfig");
        this.defaultMapConfig.load(defaultMapConfig, this.defaultMapConfig);

        for (Config singleMapConfig : defaultMapConfig.getOrderedChildren()) {
            FroggerMapConfig newMapConfig = new FroggerMapConfig();
            newMapConfig.load(singleMapConfig, this.defaultMapConfig);
            this.mapConfigs.put(singleMapConfig.getName(), newMapConfig);
        }
    }

    private void loadBanks() {
        this.soundBank = loadBank("soundList", "main", "sounds", "Sound", true);
        this.animationBank = loadBank("animList", null, "anims", true, (bank, index) -> bank.size() <= 1 ? "Default Animation" : "Animation " + index);
        this.formBank = loadBank("formList", "main", "forms", "Form", true);
        this.entityBank = loadBank("entityList", "main", "entities", "Entity", true);
        this.scriptBank = loadBank("scriptList", "main", "scripts", "Script", false);
        this.scriptCallbackBank = this.scriptBank.getChildBank("CallbackNames");
    }

    private NameBank loadBank(String configKey, String defaultBank, String bankName, String unknownName, boolean addChildrenToMainBank) {
        return loadBank(configKey, defaultBank, bankName, addChildrenToMainBank, (bank, index) -> "Unknown " + unknownName + " [" + index + "]");
    }

    private NameBank loadBank(String configKey, String defaultBank, String bankName, boolean addChildrenToMainBank, BiFunction<NameBank, Integer, String> nameHandler) {
        String animBankName = getString(configKey, defaultBank);
        if (animBankName == null)
            return null;

        return NameBank.readBank(bankName, animBankName, addChildrenToMainBank, nameHandler);
    }

    private void readPickupData() {
        if (getPickupDataAddress() == 0)
            return;

        this.pickupData = new PickupData[FlyScoreType.values().length];
        getReader().setIndex(getPickupDataAddress());

        for (int i = 0; i < this.pickupData.length; i++) {
            long tempPointer = getReader().readUnsignedIntAsLong();
            getReader().jumpTemp((int) (tempPointer - getRamPointerOffset()));
            PickupData pickupData = new PickupData();
            pickupData.load(getReader());
            this.pickupData[i] = pickupData;
            getReader().jumpReturn();
        }
    }

    /**
     * Read the MWI file from the executable.
     */
    private void readMWI() {
        DataReader reader = getReader();

        reader.setIndex(getMWIOffset());
        byte[] mwiBytes = reader.readBytes(getMWILength());

        DataReader arrayReader = new DataReader(new ArraySource(mwiBytes));
        MWIFile mwiFile = new MWIFile();
        mwiFile.load(arrayReader);
        this.MWI = mwiFile;
    }

    private void readCosTable() {
        DataReader reader = new DataReader(new ArraySource(Utils.readBytesFromStream(Utils.getResourceStream("ACOSTABLE"))));
        for (int i = 0; i < ACOSTABLE_ENTRIES; i++) {
            sinEntries[i] = reader.readShort();
            cosEntries[i] = reader.readShort();
        }
    }

    private void readThemeLibrary() {
        themeLibrary = new ThemeBook[MAPTheme.values().length];

        if (getThemeBookAddress() >= 0) {
            DataReader reader = getReader();
            reader.setIndex(getThemeBookAddress());

            for (int i = 0; i < themeLibrary.length; i++) {
                ThemeBook book = TargetPlatform.makeNewThemeBook(this);
                book.setTheme(MAPTheme.values()[i]);
                book.load(reader);
                themeLibrary[i] = book;
                Constants.logExeInfo(book);
            }

            if (hasChild(CHILD_RESTORE_THEME_BOOK)) {
                Config themeBookRestore = getChild(CHILD_RESTORE_THEME_BOOK);

                for (String key : themeBookRestore.keySet()) {
                    MAPTheme theme = MAPTheme.getTheme(key);
                    Utils.verify(theme != null, "Unknown theme: '%s'", key);
                    getThemeBook(theme).handleCorrection(themeBookRestore.getString(key));
                }
            }
        }

        readFormLibrary();
    }

    private void readFormLibrary() {
        MAPTheme lastTheme = MAPTheme.values()[0];
        for (int i = 1; i < MAPTheme.values().length; i++) {
            MAPTheme currentTheme = MAPTheme.values()[i];

            ThemeBook lastBook = getThemeBook(lastTheme);
            ThemeBook currentBook = getThemeBook(currentTheme);
            if (currentBook == null || currentBook.getFormLibraryPointer() == 0)
                continue;

            // Determine number of form entries and compare with name bank.
            int nameCount = getFormBank().getChildBank(lastTheme.name()) != null ? getFormBank().getChildBank(lastTheme.name()).size() : 0;
            int byteSize = isAtOrBeforeBuild1() || isWindowsAlpha() ? FormEntry.BUILD1_BYTE_SIZE : FormEntry.BYTE_SIZE;
            int entryCount = (int) (currentBook.getFormLibraryPointer() - lastBook.getFormLibraryPointer()) / byteSize;
            if (entryCount != nameCount)
                System.out.println(lastTheme + " has " + nameCount + " configured form names but " + entryCount + " calculated form entries in the form library.");

            // Load form library.
            lastBook.loadFormLibrary(this, entryCount);
            lastTheme = currentTheme;
        }

        // Load the last theme, which we use the number of names for to determine size.
        ThemeBook lastBook = getThemeBook(lastTheme);
        if (lastBook != null) {
            int nameCount = getFormBank().getChildBank(lastTheme.name()).size();
            lastBook.loadFormLibrary(this, nameCount);
        }
    }

    private void readDemoTable() {
        if (this.demoTableAddress == -1) { // The demo table wasn't specified, so we'll search for it ourselves.
            FileEntry demoEntry = getMWI().getEntries().stream().filter(file -> file.getDisplayName().startsWith("SUB1DEMO.DAT")).findAny().orElse(null);
            if (demoEntry == null)
                return; // Couldn't find a demo by this name, so... skip.

            byte[] levelId = Utils.toByteArray(MAPLevel.SUBURBIA1.ordinal());
            byte[] demoId = Utils.toByteArray(demoEntry.getResourceId());

            byte[] searchFor = new byte[levelId.length + demoId.length];
            System.arraycopy(levelId, 0, searchFor, 0, levelId.length);
            System.arraycopy(demoId, 0, searchFor, levelId.length, demoId.length);

            int findIndex = Utils.indexOf(getExeBytes(), searchFor);
            if (findIndex == -1)
                return; // Didn't find the bytes, ABORT!

            this.demoTableAddress = findIndex;
        }

        getReader().setIndex(this.demoTableAddress);
        while (getReader().hasMore()) {
            int levelId = getReader().readInt();
            int resourceId = getReader().readInt();
            int minLevel = getReader().readInt();

            if (levelId == -1 || minLevel == -1)
                break; // Reached terminator.

            // Add demo entry.
            boolean isValid = (levelId != DemoTableEntry.SKIP_INT && minLevel != DemoTableEntry.SKIP_INT);
            demoTableEntries.add(new DemoTableEntry(isValid ? MAPLevel.values()[levelId] : null, isValid ? resourceId : -1, isValid ? MAPLevel.values()[minLevel] : null, isValid));
        }
    }

    private void readMapLibrary() {
        DataReader reader = getReader();
        reader.setIndex(getMapBookAddress());

        int themeAddress = isSonyPresentation() ? Integer.MAX_VALUE : getThemeBookAddress();
        if (themeAddress >= 0) {
            while (themeAddress > reader.getIndex()) {
                MapBook book = TargetPlatform.makeNewMapBook(this);
                book.load(reader);
                this.mapLibrary.add(book);
                Constants.logExeInfo(book);

                if (isSonyPresentation())
                    break; // There's only one map.
            }
        }

        if (!hasChild(CHILD_RESTORE_MAP_BOOK))
            return;

        Config mapBookRestore = getChild(CHILD_RESTORE_MAP_BOOK);
        for (String key : mapBookRestore.keySet()) {
            MAPLevel level = MAPLevel.getByName(key);
            Utils.verify(level != null, "Unknown level: '%s'", key);
            Utils.verify(level.isExists(), "Cannot modify %s, its level doesn't exist.", key);
            getMapBook(level).handleCorrection(mapBookRestore.getString(key));
        }
    }

    private void readScripts() {
        if (getScriptArrayAddress() == 0)
            return; // Wasn't specified.

        getReader().setIndex(getScriptArrayAddress());
        for (int i = 0; i < getScriptBank().size(); i++) {
            long address = getReader().readUnsignedIntAsLong();
            if (address == 0) { // Default / null.
                getScripts().add(FroggerScript.EMPTY_SCRIPT);
                continue;
            }

            getReader().jumpTemp((int) (address - getRamPointerOffset()));
            FroggerScript newScript = new FroggerScript();
            getScripts().add(newScript); // Adds before loading so getName() can be accessed.
            try {
                newScript.load(getReader());
            } catch (Throwable th) {
                System.out.println("Failed to load script '" + getScriptBank().getName(i) + "'");
                th.printStackTrace();

                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    // Do nothing.
                }
            } finally {
                getReader().jumpReturn();
            }
        }
    }

    private void readRemapData() {
        getMapLibrary().forEach(book -> book.readRemapData(this));
    }

    private void readMusicData() {
        if (getMusicAddress() < 0)
            return;

        getReader().setIndex(getMusicAddress());

        byte readByte;
        while ((readByte = getReader().readByte()) != MusicTrack.TERMINATOR)
            getMusicTracks().add(MusicTrack.getTrackById(this, readByte));
    }

    private void readLevelData() {
        if (getArcadeLevelAddress() == 0)
            return; // No level select is present.

        GUIMain.EXE_CONFIG = this; // Fixes logging of the below things.
        getReader().setIndex(getArcadeLevelAddress());
        LevelInfo level = null;
        while (level == null || !level.isTerminator()) {
            level = new LevelInfo();
            level.load(getReader());
            getArcadeLevelInfo().add(level);
            Constants.logExeInfo(level);
        }

        level = null;
        while (level == null || !level.isTerminator()) {
            level = new LevelInfo();
            level.load(getReader());
            getRaceLevelInfo().add(level);
            Constants.logExeInfo(level);
        }

        getAllLevelInfo().addAll(getArcadeLevelInfo());
        getAllLevelInfo().addAll(getRaceLevelInfo());

        for (LevelInfo info : getAllLevelInfo())
            levelInfoMap.put(info.getLevel(), info);
    }

    private void readBmpPointerData() {
        int firstRemap = Integer.MAX_VALUE;
        for (MapBook book : getMapLibrary())
            if (!book.isDummy())
                firstRemap = Math.min(firstRemap, book.execute(pc -> Math.min(pc.getFileLowRemapPointer(), pc.getFileHighRemapPointer()), PSXMapBook::getFileRemapPointer));

        if (getBmpPointerAddress() == 0) {
            System.out.println("First Remap: " + Utils.toHexString(firstRemap)); // Put this into a hex editor, then you can scroll up to find the first image.
            return; // Not specified.
        }

        Utils.verify(firstRemap != Integer.MAX_VALUE, "Failed to find bmp_pointer's end.");

        int nullEntries = getInt("extraBmpPointers", 0);
        int stopReading = firstRemap - (Constants.POINTER_SIZE * nullEntries);

        // Read all the pointers.
        getReader().setIndex(getBmpPointerAddress());
        int totalCount = (stopReading - getBmpPointerAddress()) / Constants.POINTER_SIZE;
        for (int i = 0; i < totalCount; i++)
            bmpTexturePointers.add(getReader().readUnsignedIntAsLong());
    }

    private void readHiddenParts() {
        this.hiddenPartIds.clear();
        if (!hasChild("HiddenParts"))
            return;

        Config hiddenPartsCfg = getChild("HiddenParts");
        for (String key : hiddenPartsCfg.keySet()) {
            int[] hiddenParts = hiddenPartsCfg.getIntArray(key);
            Arrays.sort(hiddenParts);
            this.hiddenPartIds.put(key, hiddenParts);
        }
    }

    private void readMofOverrides() {
        this.mofRenderOverrides.clear();
        if (!hasChild("MofOverride"))
            return;

        Config mofOverridesCfg = getChild("MofOverride");
        for (String key : mofOverridesCfg.keySet())
            this.mofRenderOverrides.put(key, mofOverridesCfg.getString(key));
    }

    /**
     * Patch this exe when its time to be saved.
     */
    public void patchEXE() {
        DataWriter exeWriter = getWriter();
        patchMWI(exeWriter, getMWI());
        patchThemeLibrary(exeWriter);
        patchMapLibrary(exeWriter);
        patchRemapData(exeWriter);
        patchScripts(exeWriter);
        patchDemoTable(exeWriter);
        patchMusicData(exeWriter);
        patchLevelData(exeWriter);
        patchBmpPointerData(exeWriter);
        exeWriter.closeReceiver();
    }

    /**
     * Patch Frogger.exe to use a modded MWI.
     * @param mwiFile The MWI file to save.
     */
    private void patchMWI(DataWriter exeWriter, MWIFile mwiFile) {
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter mwiWriter = new DataWriter(receiver);
        mwiFile.save(mwiWriter);
        mwiWriter.closeReceiver();

        exeWriter.setIndex(getMWIOffset());
        exeWriter.writeBytes(receiver.toArray());

        int bytesWritten = exeWriter.getIndex() - getMWIOffset();
        Utils.verify(bytesWritten == getMWILength(), "MWI Patching Failed. The size of the written MWI does not match the correct MWI size! [%d/%d]", bytesWritten, getMWILength());
    }

    private void patchThemeLibrary(DataWriter exeWriter) {
        exeWriter.setIndex(getThemeBookAddress());
        for (ThemeBook book : getThemeLibrary())
            book.save(exeWriter);
    }

    private void patchMapLibrary(DataWriter exeWriter) {
        exeWriter.setIndex(getMapBookAddress());
        getMapLibrary().forEach(book -> book.save(exeWriter));
    }

    private void patchRemapData(DataWriter exeWriter) {
        getMapLibrary().forEach(book -> book.saveRemapData(exeWriter, this));
    }

    private void patchScripts(DataWriter exeWriter) {
        if (getScriptArrayAddress() == 0)
            return; // Wasn't specified.

        getReader().setIndex(getScriptArrayAddress());
        for (int i = 0; i < getScripts().size(); i++) {
            long address = getReader().readUnsignedIntAsLong();
            if (address == 0) // Default / null.
                continue;

            FroggerScript script = getScripts().get(i);
            if (script.isTooLarge())
                System.out.println("WARNING: Saving " + script.getName() + ", which is larger than what is considered safe!");

            exeWriter.setIndex((int) (address - getRamPointerOffset()));
            script.save(exeWriter);
        }
    }

    private void patchDemoTable(DataWriter exeWriter) {
        if (this.demoTableAddress == -1)
            return;

        exeWriter.setIndex(this.demoTableAddress);
        for (DemoTableEntry entry : this.demoTableEntries)
            entry.save(exeWriter);
    }

    private void patchMusicData(DataWriter exeWriter) {
        exeWriter.setIndex(getMusicAddress());
        getMusicTracks().forEach(track -> exeWriter.writeByte(track.getTrack(this)));
        exeWriter.writeByte(MusicTrack.TERMINATOR);
    }

    private void patchLevelData(DataWriter exeWriter) {
        if (getArcadeLevelAddress() == 0)
            return; // No level select is present.

        exeWriter.setIndex(getArcadeLevelAddress());
        getArcadeLevelInfo().forEach(level -> level.save(exeWriter));
        getRaceLevelInfo().forEach(level -> level.save(exeWriter));
    }

    private void patchBmpPointerData(DataWriter exeWriter) {
        if (getBmpPointerAddress() == 0)
            return; // Not specified.

        exeWriter.setIndex(getBmpPointerAddress());
        getBmpTexturePointers().forEach(exeWriter::writeUnsignedInt);
    }

    /**
     * Override remap data in the exe.
     * @param mapEntry    The FileEntry belonging to the map to replace.
     * @param remapImages The new image remap array.
     */
    public void changeRemap(FileEntry mapEntry, List<Integer> remapImages) {
        List<Short> realRemap = getRemapTable(mapEntry);
        Utils.verify(realRemap.size() >= remapImages.size(), "New remap table cannot be larger than the old remap table.");
        for (int i = 0; i < remapImages.size(); i++)
            realRemap.set(i, (short) (int) remapImages.get(i));
    }

    /**
     * Gets a writer which rights to the executable.
     * @return writer
     */
    public DataWriter getWriter() {
        loadExeData();
        return new DataWriter(new FixedArrayReceiver(this.exeBytes));
    }

    /**
     * Get a reader which will read the input file.
     * @return dataReader
     */
    public DataReader getReader() {
        loadExeData();
        return this.reader != null ? this.reader : (this.reader = new DataReader(new ArraySource(exeBytes)));
    }

    private void loadExeData() {
        if (exeBytes != null)
            return;
        try {
            this.exeBytes = Files.readAllBytes(inputFile.toPath());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Save the Frogger executable.
     */
    public void saveExecutable(File outputFile) throws IOException {
        Utils.deleteFile(outputFile);
        applyConfigIdentifier();
        Files.write(outputFile.toPath(), this.exeBytes);
    }

    /**
     * Write the config identifier to the end of the executable, so it will automatically know which config to use when this is loaded.
     */
    private void applyConfigIdentifier() {
        if (this.hasConfigIdentifier)
            return; // Don't apply twice.

        this.hasConfigIdentifier = true;
        this.reader = null; // Destroy cached reader.

        byte[] appendBytes = getInternalName().getBytes();
        byte[] newExeBytes = new byte[exeBytes.length + appendBytes.length];
        System.arraycopy(this.exeBytes, 0, newExeBytes, 0, this.exeBytes.length);
        System.arraycopy(appendBytes, 0, newExeBytes, this.exeBytes.length, appendBytes.length);
        this.exeBytes = newExeBytes;
    }

    /**
     * Get a theme book by a theme.
     * @param theme The theme to get the book for.
     * @return themeBook
     */
    public ThemeBook getThemeBook(MAPTheme theme) {
        return this.themeLibrary != null ? this.themeLibrary[theme.ordinal()] : null;
    }

    /**
     * Get a map book by a MAPLevel.
     * @param level The level to get the book for.
     * @return mapBook
     */
    public MapBook getMapBook(MAPLevel level) {
        return this.mapLibrary.size() > 0 ? this.mapLibrary.get(level.ordinal()) : null;
    }

    /**
     * Get the FileEntry for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntry
     */
    public FileEntry getResourceEntry(int resourceId) {
        return resourceId >= 0 && resourceId < getMWI().getEntries().size() ? getMWI().getEntries().get(resourceId) : null;
    }

    /**
     * Gets the resource entry from a given name.
     * @param name The name to lookup.
     * @return foundEntry, if any.
     */
    public FileEntry getResourceEntry(String name) {
        if (name == null || name.isEmpty())
            return null;

        for (int i = 0; i < getMWI().getEntries().size(); i++) {
            FileEntry entry = getMWI().getEntries().get(i);
            if (name.equalsIgnoreCase(entry.getDisplayName()))
                return entry;
        }

        return null;
    }

    /**
     * Get the FileEntry name for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntryName
     */
    public String getResourceName(int resourceId) {
        FileEntry entry = getResourceEntry(resourceId);
        if (entry == null)
            return "NULL/" + resourceId;

        return entry.getDisplayName();
    }

    /**
     * Gets a GameFile by its resource id.
     * @param resourceId The file's resource id.
     * @return gameFile
     */
    @SuppressWarnings("unchecked")
    public <T extends GameFile> T getGameFile(int resourceId) {
        return (T) getMWD().getEntryFileMap().get(getResourceEntry(resourceId));
    }

    /**
     * Test if this is a PC release.
     * @return isPCRelease
     */
    public boolean isPC() {
        return getPlatform() == TargetPlatform.PC;
    }

    /**
     * Test if this is a PSX release.
     * @return isPSXRelease
     */
    public boolean isPSX() {
        return getPlatform() == TargetPlatform.PSX;
    }

    /**
     * Gets a texture id by its pointer.
     * @param pointer The pointer of the texture.
     * @return textureId
     */
    public int getTextureIdFromPointer(long pointer) {
        if (getBmpPointerAddress() == 0)
            throw new RuntimeException("Cannot get texture-id from pointer without bmpPointerAddress being set!");
        return getBmpTexturePointers().indexOf(pointer);
    }

    /**
     * Attempts to find an image by its pointer.
     * @param pointer The pointer get get the image for.
     * @return matchingImage - May be null.
     */
    public GameImage getImageFromPointer(long pointer) {
        return getMWD().getImageByTextureId(getTextureIdFromPointer(pointer));
    }

    /**
     * Export code to a folder.
     */
    @SneakyThrows
    public void exportCode(File folder) {
        if (isPC()) {
            System.out.println("Cannot generate headers for PC builds yet.");
            return;
        }

        @Cleanup PrintWriter frogpsxHWriter = new PrintWriter(new File(folder, "frogpsx.h"));
        saveFrogPSX(frogpsxHWriter);

        @Cleanup PrintWriter vramHWriter = new PrintWriter(new File(folder, "frogvram.h"));
        @Cleanup PrintWriter vramCWriter = new PrintWriter(new File(folder, "frogvram.c"));
        saveFrogVRAM(vramHWriter, vramCWriter);

        // Save MWI.
        DataWriter writer = new DataWriter(new FileReceiver(new File(folder, "FROGPSX.MWI")));
        getMWI().save(writer);
        writer.closeReceiver();

        System.out.println("Generated source files.");
    }

    private void saveFrogVRAM(PrintWriter vramHWriter, PrintWriter vramCWriter) {
        int maxTexId = 0;
        for (GameFile file : getMWD().getFiles()) {
            if (!(file instanceof VLOArchive))
                continue;

            VLOArchive vlo = (VLOArchive) file;
            for (GameImage image : vlo.getImages())
                maxTexId = Math.max(maxTexId, image.getTextureId());
        }

        System.out.println("Maximum Texture ID: " + maxTexId);

        String[] imageNames = new String[maxTexId + 1];
        for (int i = 0; i < imageNames.length; i++)
            imageNames[i] = "im_img" + i;

        if (hasChild(CHILD_IMAGE_NAMES)) {
            Config nameConfig = getChild(CHILD_IMAGE_NAMES);
            for (String key : nameConfig.keySet())
                imageNames[Integer.parseInt(key)] = nameConfig.getString(key);
        }

        vramHWriter.write("#ifndef __FROGVRAM_H" + Constants.NEWLINE);
        vramHWriter.write("#define __FROGVRAM_H" + Constants.NEWLINE + Constants.NEWLINE);
        vramHWriter.write("extern MR_TEXTURE* bmp_pointers[];" + Constants.NEWLINE + Constants.NEWLINE);

        vramCWriter.write("#include \"frogvram.h\"" + Constants.NEWLINE + Constants.NEWLINE);
        vramCWriter.write("MR_TEXTURE* bmp_pointers[] = {" + Constants.NEWLINE + "\t");
        for (int i = 0; i < imageNames.length; i++)
            vramCWriter.write("&" + imageNames[i] + "," + (((i % 10) == 0 && i > 0) ? Constants.NEWLINE + "\t" : " "));
        vramCWriter.write(Constants.NEWLINE + "};" + Constants.NEWLINE + Constants.NEWLINE);

        for (MapBook mapBook : getMapLibrary()) {
            if (mapBook.isDummy())
                continue;

            FileEntry mapEntry = ((PSXMapBook) mapBook).getMapEntry();
            String txlName = "txl_" + Utils.stripExtension(mapEntry.getDisplayName()).toLowerCase();

            vramHWriter.write("extern MR_USHORT " + txlName + "[];" + Constants.NEWLINE);
            vramCWriter.write("MR_USHORT " + txlName + "[] = {");
            for (short remapVal : getRemapTable(mapEntry))
                vramCWriter.write(remapVal + ", ");
            vramCWriter.write(MapBook.REMAP_TERMINATOR + "};" + Constants.NEWLINE);
        }

        vramHWriter.write("extern MR_USHORT txl_for3[];" + Constants.NEWLINE); // Apparently this remap might be in an executable.
        vramCWriter.write("MR_USHORT txl_for3[] = {};" + Constants.NEWLINE);

        vramCWriter.write(Constants.NEWLINE);
        vramHWriter.write(Constants.NEWLINE);
        for (String imageName : imageNames) {
            if (imageName == null)
                continue;
            vramCWriter.write("MR_TEXTURE " + imageName + " = {0};" + Constants.NEWLINE);
            vramHWriter.write("extern MR_TEXTURE " + imageName + ";" + Constants.NEWLINE);
        }

        // Unsure where this goes, or where to read it from.
        if (getMWD().getSkyLand().getSkyLandTextures() != null && getMWD().getSkyLand().getSkyLandTextures().length > 0) {
            vramCWriter.write(Constants.NEWLINE);
            vramHWriter.write(Constants.NEWLINE);
            vramCWriter.write("MR_USHORT txl_sky_land[] = {");

            short[] skyTxlData = getMWD().getSkyLand().getSkyLandTextures();
            if (skyTxlData != null) {
                for (int i = 0; i < skyTxlData.length; i++) {
                    if (i > 0)
                        vramCWriter.write(", ");
                    vramCWriter.write(String.valueOf(skyTxlData[i]));
                }
            }

            vramCWriter.write("};" + Constants.NEWLINE);
            vramHWriter.write("extern MR_USHORT txl_sky_land[];" + Constants.NEWLINE);
        }

        vramHWriter.write("#endif" + Constants.NEWLINE);
    }

    private void saveFrogPSX(PrintWriter writer) {
        writer.write("#ifndef __FROGPSX_H" + Constants.NEWLINE);
        writer.write("#define __FROGPSX_H" + Constants.NEWLINE + Constants.NEWLINE);

        writer.write("#define RES_FROGPSX_DIRECTORY \"E:\\\\Frogger\\\\\"" + Constants.NEWLINE);
        writer.write("#define RES_NUMBER_OF_RESOURCES " + getMWI().getEntries().size() + Constants.NEWLINE + Constants.NEWLINE);

        writer.write("enum {\n" +
                "\tFR_FTYPE_STD, // Standard?\n" +
                "\tFR_FTYPE_VLO, // VLO Files\n" +
                "\tFR_FTYPE_SPU, // Sound files \n" +
                "\tFR_FTYPE_MOF,\n" +
                "\tFR_FTYPE_MAPMOF,\n" +
                "\tFR_FTYPE_MAPANIMMOF, // Unused\n" +
                "\tFR_FTYPE_DEMODATA\n" +
                "};\n\n");

        HashSet<String> resourceNames = new HashSet<>();
        writer.write("enum {" + Constants.NEWLINE);
        for (FileEntry entry : getMWI().getEntries()) {
            String resName = "RES_" + entry.getDisplayName().replace(".", "_");
            while (!resourceNames.add(resName))
                resName += "_DUPE";

            writer.write("\t" + resName + "," + Constants.NEWLINE);
        }
        writer.write("};" + Constants.NEWLINE + Constants.NEWLINE);

        // Must happen last.
        writer.write("#endif" + Constants.NEWLINE);
    }

    /**
     * Perform rCos on an angle.
     * @param angle The angle to perform rCos on.
     * @return rCos
     */
    public int rcos(int angle) {
        return this.cosEntries[angle & 0xFFF]; // & 0xFFF cuts off the decimal point.
    }

    /**
     * Perform rSin on an angle.
     * @param angle The angle to perform rSin on.
     * @return rSin
     */
    public int rsin(int angle) {
        return this.sinEntries[angle & 0xFFF]; // & 0xFFF cuts off the decimal point.
    }

    /**
     * Get the form book for the given formBookId and MapTheme.
     * @param mapTheme   The map theme the form belongs to.
     * @param formBookId The form id in question. Normally passed to ENTITY_GET_FORM_BOOK as en_form_book_id.
     * @return formBook
     */
    public FormEntry getMapFormEntry(MAPTheme mapTheme, int formBookId) {
        if ((formBookId & FormEntry.FLAG_GENERAL) == FormEntry.FLAG_GENERAL)
            mapTheme = MAPTheme.GENERAL;

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
    public MusicTrack getMusic(MAPLevel level) {
        return this.musicTracks.size() > level.ordinal() ? this.musicTracks.get(level.ordinal()) : null;
    }

    /**
     * Gets the LevelInfo for a given MapLevel. Returns null if not found.
     * @param level The level to get the info for.
     * @return levelInfo
     */
    public LevelInfo getLevel(MAPLevel level) {
        for (LevelInfo info : getAllLevelInfo())
            if (info.getLevel() == level)
                return info;
        return null;
    }

    /**
     * Test if this is a retail version of the game.
     * @return isRetail
     */
    public boolean isRetail() {
        return !isDemo() && !isPrototype();
    }

    /**
     * Checks if this build is the E3 build or not.
     */
    public boolean isE3Build() {
        return "psx-1997-06-13-e3".equalsIgnoreCase(getInternalName());
    }

    /**
     * Test if this build is the windows alpha build from June 29 1997.
     */
    public boolean isWindowsAlpha() {
        return "pc-1997-06-29-alpha".equalsIgnoreCase(getInternalName());
    }

    /**
     * Tests if this is the sony presentation April 28th build.
     */
    public boolean isSonyPresentation() {
        return "psx-1997-04-28-sony".equalsIgnoreCase(getInternalName());
    }

    /**
     * Test if the configuration is for a build before build 01.
     */
    public boolean isBeforeBuild1() {
        return isE3Build() || isSonyPresentation();
    }

    /**
     * Tests if the build is at/before build 1.
     * @return isBuildAtOrBeforeBuild1
     */
    public boolean isAtOrBeforeBuild1() {
        return this.build == 1 || isBeforeBuild1();
    }

    /**
     * Tests if the build is at/before build 20.
     * @return isBuildAtOrBeforeBuild20
     */
    public boolean isAtOrBeforeBuild11() {
        return (this.build >= 0 && this.build <= 11) || isBeforeBuild1();
    }

    /**
     * Tests if the build is at/before build 20.
     * @return isBuildAtOrBeforeBuild20
     */
    public boolean isAtOrBeforeBuild20() {
        return (this.build >= 0 && this.build <= 20) || isBeforeBuild1();
    }

    /**
     * Get the forced VLO file for a given string.
     * @param name The name to get the vlo for.
     * @return forcedVLO
     */
    public VLOArchive getForcedVLO(String name) {
        if (!hasChild(CHILD_MOF_FORCE_VLO))
            return null;

        Config childConfig = getChild(CHILD_MOF_FORCE_VLO);
        if (!childConfig.has(name))
            return null;

        String vloName = childConfig.getString(name);
        return getMWD().resolveForEachFile(VLOArchive.class, vlo -> vlo.getFileEntry().getDisplayName().startsWith(vloName) ? vlo : null);
    }

    /**
     * Gets allowed forms for a given theme.
     * @param theme The theme to get forms for.
     * @return allowedForms
     */
    public FormEntry[] getAllowedForms(MAPTheme theme) {
        return allowedForms.computeIfAbsent(theme, safeTheme -> {
            ThemeBook themeBook = getThemeLibrary()[MAPTheme.GENERAL.ordinal()];
            if (themeBook == null)
                return new FormEntry[0];

            List<FormEntry> formType = new ArrayList<>(themeBook.getFormBook());
            if (safeTheme != null && safeTheme != MAPTheme.GENERAL) {
                ThemeBook mapBook = getThemeLibrary()[safeTheme.ordinal()];
                if (mapBook != null)
                    formType.addAll(mapBook.getFormBook());
            }

            return formType.toArray(new FormEntry[0]);
        });
    }
}