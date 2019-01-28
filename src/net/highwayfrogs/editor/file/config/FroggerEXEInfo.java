package net.highwayfrogs.editor.file.config;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
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
 * TODO: Setup other configs.
 * TODO: PSX support is a little dinged.
 * Created by Kneesnap on 8/18/2018.
 */
@Getter
public class FroggerEXEInfo extends Config {
    private MWDFile MWD;
    private MWIFile MWI;
    private ThemeBook[] themeLibrary;
    private List<MapBook> mapLibrary = new ArrayList<>();
    private Map<FileEntry, List<Short>> remapTable = new HashMap<>();

    private DataReader reader;
    private byte[] exeBytes;
    private File inputFile;
    private List<String> fallbackFileNames;

    public static final String FIELD_NAME = "name";
    private static final String FIELD_FILE_NAMES = "Files";

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
     * Gets the pointer offset when a value is in ram vs in the file.
     * @return ramPointerOffset
     */
    public long getRamPointerOffset() {
        return getLong("ramOffset");
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
     * Test if this is a prototype build.
     * @return isPrototype
     */
    public boolean isPrototype() {
        return getBoolean("prototype");
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
        readMWI();
        readThemeLibrary();
        readMapLibrary();
        readRemapData();
        this.MWD = new MWDFile(getMWI());
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

    private void readRemapData() {
        getMapLibrary().forEach(book -> book.readRemapData(this));
    }

    /**
     * Patch this exe when its time to be saved.
     */
    public void patchEXE() {
        patchMWI(getMWI());
        patchThemeLibrary();
        patchMapLibrary();
        patchRemapData();
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

    private void patchRemapData() {
        getMapLibrary().forEach(book -> book.saveRemapData(this));
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
