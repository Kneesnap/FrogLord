package net.highwayfrogs.editor.file.config;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.data.MusicTrack;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FixedArrayReceiver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Information about a specific frogger.exe file.
 * Created by Kneesnap on 8/18/2018.
 */
@Getter
public class FroggerEXEInfo extends Config {
    private MWDFile MWD;
    private MWIFile MWI;
    private ThemeBook[] themeLibrary;
    private List<MapBook> mapLibrary = new ArrayList<>();
    private Map<FileEntry, List<Short>> remapTable = new HashMap<>();
    private List<MusicTrack> musicTracks = new ArrayList<>();
    private List<LevelInfo> arcadeLevelInfo = new ArrayList<>();
    private List<LevelInfo> raceLevelInfo = new ArrayList<>();

    private long ramPointerOffset;
    private int MWIOffset;
    private int MWILength;
    private int mapBookAddress;
    private int themeBookAddress;
    private int arcadeLevelAddress;
    private int musicAddress;
    private boolean prototype;
    private boolean demo;
    private TargetPlatform platform;

    private DataReader reader;
    private byte[] exeBytes;
    private File inputFile;
    private List<String> fallbackFileNames;

    public static final String FIELD_NAME = "name";
    private static final String FIELD_FILE_NAMES = "Files";

    private static final String CHILD_RESTORE_MAP_BOOK = "MapBookRestore";
    private static final String CHILD_RESTORE_THEME_BOOK = "ThemeBookRestore";

    public FroggerEXEInfo(File inputExe, InputStream inputStream) throws IOException {
        super(inputStream);
        this.inputFile = inputExe;
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
        readConfig();
        readMWI();
        readThemeLibrary();
        readMapLibrary();
        readRemapData();
        readMusicData();
        readLevelData();
        this.MWD = new MWDFile(getMWI());
    }

    private void readConfig() {
        this.demo = getBoolean("demo");
        this.prototype = getBoolean("prototype");
        this.platform = getEnum("platform", TargetPlatform.class);
        this.MWIOffset = getInt("mwiOffset");
        this.MWILength = getInt("mwiLength");
        this.themeBookAddress = getInt("themeBook");
        this.mapBookAddress = getInt("mapBook");
        this.ramPointerOffset = getLong("ramOffset");
        this.arcadeLevelAddress = getInt("arcadeLevelAddress", 0);
        this.musicAddress = getInt("musicAddress");
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

    private void readThemeLibrary() {
        themeLibrary = new ThemeBook[MAPTheme.values().length];

        DataReader reader = getReader();
        reader.setIndex(getThemeBookAddress());

        for (int i = 0; i < themeLibrary.length; i++) {
            ThemeBook book = getPlatform().getThemeBookMaker().get();
            book.load(reader);
            themeLibrary[i] = book;
        }

        if (!hasChild(CHILD_RESTORE_THEME_BOOK))
            return;

        Config themeBookRestore = getChild(CHILD_RESTORE_THEME_BOOK);

        for (String key : themeBookRestore.keySet()) {
            MAPTheme theme = MAPTheme.getTheme(key);
            Utils.verify(theme != null, "Unknown theme: '%s'", key);
            getThemeBook(theme).handleCorrection(themeBookRestore.getString(key));
        }
    }

    private void readMapLibrary() {
        DataReader reader = getReader();
        reader.setIndex(getMapBookAddress());

        int themeAddress = getThemeBookAddress();
        while (themeAddress > reader.getIndex()) {
            MapBook book = getPlatform().getMapBookMaker().get();
            book.load(reader);
            this.mapLibrary.add(book);
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

    private void readRemapData() {
        getMapLibrary().forEach(book -> book.readRemapData(this));
    }

    private void readMusicData() {
        getReader().setIndex(getMusicAddress());

        byte readByte;
        while ((readByte = getReader().readByte()) != MusicTrack.TERMINATOR)
            getMusicTracks().add(MusicTrack.getTrackById(getPlatform(), readByte));
    }

    private void readLevelData() {
        if (getArcadeLevelAddress() == 0)
            return; // No level select is present.

        getReader().setIndex(getArcadeLevelAddress());
        LevelInfo level = null;
        while (level == null || !level.isTerminator()) {
            level = new LevelInfo();
            level.load(getReader());
            getArcadeLevelInfo().add(level);
            System.out.println(level.toString());
        }

        level = null;
        while (level == null || !level.isTerminator()) {
            level = new LevelInfo();
            level.load(getReader());
            getArcadeLevelInfo().add(level);
            System.out.println(level.toString());
        }
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
        patchMusicData(exeWriter);
        patchLevelData(exeWriter);
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

    private void patchMusicData(DataWriter exeWriter) {
        exeWriter.setIndex(getMusicAddress());
        getMusicTracks().forEach(track -> exeWriter.writeByte(track.getTrack(getPlatform())));
        exeWriter.writeByte(MusicTrack.TERMINATOR);
    }

    private void patchLevelData(DataWriter exeWriter) {
        if (getArcadeLevelAddress() == 0)
            return; // No level select is present.

        getWriter().setIndex(getArcadeLevelAddress());
        getArcadeLevelInfo().forEach(level -> level.save(exeWriter));
        getRaceLevelInfo().forEach(level -> level.save(exeWriter));
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
    private DataWriter getWriter() {
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
    public void saveExecutable(File outputFile) {
        try {
            Utils.deleteFile(outputFile);
            Files.write(outputFile.toPath(), this.exeBytes);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get a theme book by a theme.
     * @param theme The theme to get the book for.
     * @return themeBook
     */
    public ThemeBook getThemeBook(MAPTheme theme) {
        return this.themeLibrary[theme.ordinal()];
    }

    /**
     * Get a map book by a MAPLevel.
     * @param level The level to get the book for.
     * @return mapBook
     */
    public MapBook getMapBook(MAPLevel level) {
        return this.mapLibrary.get(level.ordinal());
    }

    /**
     * Get the FileEntry for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntry
     */
    public FileEntry getResourceEntry(int resourceId) {
        return getMWI().getEntries().get(resourceId);
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
}
