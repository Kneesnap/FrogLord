package net.highwayfrogs.editor.file.config;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FixedArrayReceiver;
import net.highwayfrogs.editor.system.Tuple2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Information about a specific frogger.exe file.
 * TODO: Setup other configs.
 * TODO: Can we stop using remaps and use this instead?
 * Created by Kneesnap on 8/18/2018.
 */
@Getter
public class FroggerEXEInfo extends Config {
    private MWIFile MWI;
    private ThemeBook[] themeLibrary;
    private List<MapBook> mapLibrary = new ArrayList<>();

    private byte[] exeBytes;
    private File inputFile;
    private List<String> fallbackFileNames;

    private static final String REMAP_SPLIT = "|";
    private static final String NO_REMAP_DATA = "PLACEHOLDER";
    private static final int DEFAULT_REMAP_SIZE = 250;

    public static final String FIELD_NAME = "name";
    private static final String FIELD_FILE_NAMES = "Files";
    private static final String FIELD_REMAP_DATA = "Remaps";

    public FroggerEXEInfo(File inputExe, InputStream inputStream) throws IOException {
        super(inputStream);
        this.inputFile = inputExe;
    }

    /**
     * Prints a calculated remap config.
     * @param mwdFile The mwd file to get maps from.
     */
    public void printRemapConfig(MWDFile mwdFile) {
        List<Tuple2<Integer, String>> remapDataHolder = new ArrayList<>();
        for (GameFile file : mwdFile.getFiles()) {
            if (!(file instanceof MAPFile))
                continue;

            MAPFile mapFile = (MAPFile) file;
            String mapName = Utils.getRawFileName(mwdFile.getEntryMap().get(mapFile).getDisplayName());
            if (!hasRemapInfo(mapName))
                continue;

            int startAddress = getRemapInfo(mapName).getA();
            remapDataHolder.add(new Tuple2<>(startAddress, mapName + Config.VALUE_SPLIT + Utils.toHexString(startAddress) + REMAP_SPLIT + mapFile.getMaxRemap()));
        }

        remapDataHolder.sort(Comparator.comparingInt(Tuple2::getA));
        for (Tuple2<Integer, String> data : remapDataHolder)
            System.out.println(data.getB());
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
     * Gets the byte-location of this executable's MWI.
     * @return mwiOffset
     */
    public int getMWIOffset() {
        return getInt("mwiOffset");
    }

    /**
     * Get the byte-length of this executable's MWI.
     * @return mwiLength
     */
    public int getMWILength() {
        return getInt("mwiLength");
    }

    /**
     * Get the address of the theme book.
     */
    public int getThemeBookAddress() {
        return getInt("themeBook");
    }

    /**
     * Get the address of the map book.
     */
    public int getMapBookAddress() {
        return getInt("mapBook");
    }

    /**
     * Get the platform this was released on.
     * @return platform
     */
    public TargetPlatform getPlatform() {
        return getEnum("platform", TargetPlatform.class);
    }

    /**
     * Gets if this is a major demo release.
     * @return isDemo
     */
    public boolean isDemo() {
        return getBoolean("demo");
    }

    /**
     * Get remap info for a specific level.
     * @param levelName The level to get remap info for.
     * @return remapInfo
     */
    public Tuple2<Integer, Integer> getRemapInfo(String levelName) {
        String remapData = getChild(FIELD_REMAP_DATA).getString(levelName);
        Utils.verify(remapData != null && !remapData.equalsIgnoreCase(NO_REMAP_DATA), "There is no remap data for %s.", levelName);
        String[] split = remapData.split(Pattern.quote(REMAP_SPLIT));
        int remapAddress = Integer.decode(split[0]);
        int remapCount = split.length > 1 ? Integer.parseInt(split[1]) : DEFAULT_REMAP_SIZE; // Amount of texture remaps. (Bytes / SHORT_SIZE).
        return new Tuple2<>(remapAddress, remapCount);
    }

    /**
     * Test if this config has remap data for a given level name.
     * @param levelName The name of the level to test.
     * @return hasRemapInfo
     */
    public boolean hasRemapInfo(String levelName) {
        return hasChild(FIELD_REMAP_DATA) && getChild(FIELD_REMAP_DATA).has(levelName);
    }

    /**
     * Loads the remap table from the Frogger EXE.
     * @param levelName The name of the level.
     * @return remapTable
     */
    public List<Short> getRemapTable(String levelName) {
        Tuple2<Integer, Integer> data = getRemapInfo(levelName);
        return readRemapTable(data.getA(), data.getB());
    }

    /**
     * Read a remap table from the exe.
     * @param remapAddress The address to start reading from.
     * @param remapCount   The amount of remaps to read.
     * @return remapTable
     */
    public List<Short> readRemapTable(int remapAddress, int remapCount) {
        DataReader reader = getReader();
        reader.setIndex(remapAddress);

        List<Short> remapTable = new ArrayList<>();
        for (int i = 0; i < remapCount; i++)
            remapTable.add(reader.readShort());

        return remapTable;
    }

    /**
     * Read data from the EXE which needs reading.
     */
    public void setup() {
        readMWI();
        readThemeLibrary();
        readMapLibrary();
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
    }

    /**
     * Patch this exe when its time to be saved.
     */
    public void patchEXE() {
        patchMWI(getMWI());
        patchThemeLibrary();
        patchMapLibrary();
    }

    /**
     * Patch Frogger.exe to use a modded MWI.
     * @param mwiFile The MWI file to save.
     */
    private void patchMWI(MWIFile mwiFile) {
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter mwiWriter = new DataWriter(receiver);
        mwiFile.save(mwiWriter);
        mwiWriter.closeReceiver();

        int mwiOffset = getMWIOffset();
        DataWriter exeWriter = getWriter();
        exeWriter.setIndex(mwiOffset);
        exeWriter.writeBytes(receiver.toArray());
        exeWriter.closeReceiver();

        int bytesWritten = exeWriter.getIndex() - mwiOffset;
        Utils.verify(bytesWritten == getMWILength(), "MWI Patching Failed. The size of the written MWI does not match the correct MWI size! [%d/%d]", bytesWritten, getMWILength());
    }

    private void patchThemeLibrary() {
        DataWriter exeWriter = getWriter();
        exeWriter.setIndex(getThemeBookAddress());
        for (ThemeBook book : getThemeLibrary())
            book.save(exeWriter);
        exeWriter.closeReceiver();
    }

    private void patchMapLibrary() {
        DataWriter exeWriter = getWriter();
        exeWriter.setIndex(getMapBookAddress());
        getMapLibrary().forEach(book -> book.save(exeWriter));
        exeWriter.closeReceiver();
    }

    /**
     * Override remap data in the exe.
     * @param levelName   The name of the level to patch a remap for.
     * @param remapImages The new image remap array.
     */
    public void patchRemapInExe(String levelName, List<Integer> remapImages) {
        DataWriter writer = getWriter();
        Tuple2<Integer, Integer> data = getRemapInfo(levelName);
        int remapAddress = data.getA();
        int remapCount = data.getB();
        Utils.verify(remapCount >= remapImages.size(), "New remap table is larger than the old remap table.");

        writer.jumpTemp(remapAddress);
        remapImages.forEach(writer::writeUnsignedShort);
        writer.jumpReturn();
        writer.closeReceiver();
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
        return new DataReader(new ArraySource(exeBytes));
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
}
