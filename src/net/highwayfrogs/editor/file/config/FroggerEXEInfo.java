package net.highwayfrogs.editor.file.config;

import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
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
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.SkyLand;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.GameSound;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.file.writer.FixedArrayReceiver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;

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
    private List<Long> bmpTexturePointers = new ArrayList<>();
    private short[] cosEntries = new short[ACOSTABLE_ENTRIES];
    private short[] sinEntries = new short[ACOSTABLE_ENTRIES];

    private String name;
    private long ramPointerOffset;
    private int MWIOffset;
    private int MWILength;
    private int mapBookAddress;
    private int themeBookAddress;
    private int arcadeLevelAddress;
    private int bmpPointerAddress;
    private int cosTableAddress;
    private int musicAddress;
    private boolean prototype;
    private boolean demo;
    private TargetPlatform platform;

    private DataReader reader;
    private byte[] exeBytes;
    private File inputFile;
    private List<String> fallbackFileNames;

    private static final int ACOSTABLE_ENTRIES = 4096;

    public static final String FIELD_NAME = "name";
    private static final String FIELD_FILE_NAMES = "Files";

    private static final String CHILD_RESTORE_MAP_BOOK = "MapBookRestore";
    private static final String CHILD_RESTORE_THEME_BOOK = "ThemeBookRestore";
    private static final String CHILD_IMAGE_NAMES = "ImageNames";

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
        GameSound.loadSoundNames(getString("soundList", GameSound.MAIN_KEY)); // Load the sound config.
        readConfig();
        readMWI();
        readCosTable();
        readThemeLibrary();
        readMapLibrary();
        readRemapData();
        readMusicData();
        readLevelData();
        readBmpPointerData();
        this.MWD = new MWDFile(getMWI());
    }

    private void readConfig() {
        this.name = getString(FIELD_NAME);
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
        this.bmpPointerAddress = getInt("bmpPointerAddress", 0);
        this.cosTableAddress = getInt("cosTableAddress");
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
        DataReader reader = getReader();

        reader.setIndex(getCosTableAddress());
        for (int i = 0; i < ACOSTABLE_ENTRIES; i++) {
            sinEntries[i] = reader.readShort();
            cosEntries[i] = reader.readShort();
        }
    }

    private void readThemeLibrary() {
        themeLibrary = new ThemeBook[MAPTheme.values().length];

        DataReader reader = getReader();
        reader.setIndex(getThemeBookAddress());

        for (int i = 0; i < themeLibrary.length; i++) {
            ThemeBook book = TargetPlatform.makeNewThemeBook(this);
            book.setTheme(MAPTheme.values()[i]);
            book.load(reader);
            themeLibrary[i] = book;
            Constants.logExeInfo(book);
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
            MapBook book = TargetPlatform.makeNewMapBook(this);
            book.load(reader);
            this.mapLibrary.add(book);
            Constants.logExeInfo(book);
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
            getMusicTracks().add(MusicTrack.getTrackById(this, readByte));
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
            Constants.logExeInfo(level);
        }

        level = null;
        while (level == null || !level.isTerminator()) {
            level = new LevelInfo();
            level.load(getReader());
            getArcadeLevelInfo().add(level);
            Constants.logExeInfo(level);
        }
    }

    private void readBmpPointerData() {
        int firstRemap = Integer.MAX_VALUE;
        for (MapBook book : getMapLibrary())
            if (!book.isDummy())
                firstRemap = Math.min(firstRemap, book.execute(pc -> Math.min(pc.getFileLowRemapPointer(), pc.getFileHighRemapPointer()), PSXMapBook::getFileRemapPointer));

        if (getBmpPointerAddress() == 0) {
            System.out.println("First Remap: " + Utils.toHexString(firstRemap));
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
        return getMWI().getEntries().get(resourceId);
    }

    /**
     * Get the FileEntry name for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntryName
     */
    public String getResourceName(int resourceId) {
        return getResourceEntry(resourceId).getDisplayName();
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
        return getBmpTexturePointers().indexOf(pointer);
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
            vramCWriter.write("MR_TEXTURE " + imageName + " = {0};" + Constants.NEWLINE);
            vramHWriter.write("extern MR_TEXTURE " + imageName + ";" + Constants.NEWLINE);
        }

        // Unsure where this goes, or where to read it from.
        vramCWriter.write(Constants.NEWLINE);
        vramHWriter.write(Constants.NEWLINE);
        vramCWriter.write("MR_USHORT txl_sky_land[] = {");

        SkyLand skyLand = getMWD().getSkyLand();
        for (int i = 0; i < skyLand.getMaxIndex(); i++) {
            vramCWriter.write("1791");
            vramCWriter.write(", ");
        }

        vramCWriter.write("};" + Constants.NEWLINE);
        vramHWriter.write("extern MR_USHORT txl_sky_land[];" + Constants.NEWLINE);

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
}
