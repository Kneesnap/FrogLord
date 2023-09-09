package net.highwayfrogs.editor.games.sony.frogger;

import javafx.scene.image.Image;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.DemoFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.PALFile;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.config.TargetPlatform;
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
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.SkyLand;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.gui.MainController.SCDisplayedFileType;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

/**
 * Represents an instance of the game files for Frogger (1997).
 * Created by Kneesnap on 9/7/2023.
 */
@Getter
public class FroggerGameInstance extends SCGameInstance {
    private final List<MapBook> mapLibrary = new ArrayList<>();
    private final List<MusicTrack> musicTracks = new ArrayList<>();
    private final List<LevelInfo> arcadeLevelInfo = new ArrayList<>();
    private final List<LevelInfo> raceLevelInfo = new ArrayList<>();
    private final List<LevelInfo> allLevelInfo = new ArrayList<>();
    private final Map<MAPLevel, LevelInfo> levelInfoMap = new HashMap<>();
    private final List<DemoTableEntry> demoTableEntries = new ArrayList<>();
    private final List<FormEntry> fullFormBook = new ArrayList<>();
    private final List<FroggerScript> scripts = new ArrayList<>();
    private final Map<MAPLevel, Image> levelImageMap = new HashMap<>();
    private final Map<MAPTheme, FormEntry[]> allowedForms = new HashMap<>();
    private final ThemeBook[] themeLibrary = new ThemeBook[MAPTheme.values().length];
    private final PickupData[] pickupData = new PickupData[FlyScoreType.values().length];

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
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    protected SCGameConfig makeConfig(String internalName) {
        return new FroggerConfig(internalName);
    }

    @Override
    public SCGameFile<?> createFile(FileEntry fileEntry, byte[] fileData) {
        if (fileEntry.getTypeId() == FILE_TYPE_ANY && fileEntry.getDisplayName().startsWith(Constants.SKY_LAND_PREFIX)) {
            return new SkyLand(this);
        } else if ((fileEntry.getTypeId() == FILE_TYPE_ANY && fileEntry.hasExtension("map")) || Utils.testSignature(fileData, MAPFile.SIGNATURE)) {
            return new MAPFile(this);
        } else if (fileEntry.getTypeId() == FILE_TYPE_PAL || fileEntry.hasExtension("pal")) {
            return new PALFile(this);
        } else if (fileEntry.getTypeId() == FILE_TYPE_DEMO_DATA || fileEntry.hasExtension("dat")) {
            return new DemoFile(this);
        } else if (fileEntry.getTypeId() == FILE_TYPE_SOUND) {
            return SCUtils.makeSound(fileEntry, fileData);
        } else if (fileEntry.getTypeId() == FILE_TYPE_MOF || fileEntry.getTypeId() == FILE_TYPE_MAPMOF) {
            MOFHolder newMof = SCUtils.makeMofHolder(fileEntry);
            // TODO: Find the WAD file holding the current resource, then set the theme to be that wad file's theme.
            return newMof;
        } else {
            return SCUtils.createSharedGameFile(fileEntry, fileData);
        }
    }

    @Override
    protected void readTextureRemapData(DataReader exeReader, MWIFile mwiFile) {
        this.mapLibrary.forEach(book -> book.readRemapData(this));
    }

    @Override
    protected void writeTextureRemapData(DataWriter exeWriter) {
        this.mapLibrary.forEach(book -> book.saveRemapData(exeWriter, this));
    }

    @Override
    public void setupFileTypes(List<SCDisplayedFileType> fileTypes) {
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_VLO, "VLO"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_SOUND, "VB/VH"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_MOF, "Models"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_MAPMOF, "Models"));
        // 5
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_DEMO_DATA, "DAT"));
        fileTypes.add(new SCDisplayedFileType(FILE_TYPE_PAL, "PAL"));
    }

    /**
     * Gets this MWD's SkyLand file.
     * @return skyLand
     */
    public SkyLand getSkyLand() {
        for (SCGameFile<?> file : getMainArchive().getFiles())
            if (file instanceof SkyLand)
                return (SkyLand) file;
        throw new RuntimeException("Sky Land was not found.");
    }

    /**
     * Get a theme book by a theme.
     * @param theme The theme to get the book for.
     * @return themeBook
     */
    public ThemeBook getThemeBook(MAPTheme theme) {
        return theme != null ? this.themeLibrary[theme.ordinal()] : null;
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
        getArchiveIndex().save(writer);
        writer.closeReceiver();

        System.out.println("Generated source files.");
    }

    private void saveFrogVRAM(PrintWriter vramHWriter, PrintWriter vramCWriter) {
        int maxTexId = 0;
        for (VLOArchive vlo : getMainArchive().getAllFiles(VLOArchive.class))
            for (GameImage image : vlo.getImages())
                maxTexId = Math.max(maxTexId, image.getTextureId());

        System.out.println("Maximum Texture ID: " + maxTexId);

        String[] imageNames = new String[maxTexId + 1];
        for (int i = 0; i < imageNames.length; i++)
            imageNames[i] = "im_img" + i;

        // Apply image names.
        if (getConfig().getImageNames().size() > 0)
            for (Entry<Short, String> imageEntry : getConfig().getImageNames().entrySet())
                imageNames[imageEntry.getKey()] = imageEntry.getValue();

        vramHWriter.write("#ifndef __FROGVRAM_H" + Constants.NEWLINE);
        vramHWriter.write("#define __FROGVRAM_H" + Constants.NEWLINE + Constants.NEWLINE);
        vramHWriter.write("extern MR_TEXTURE* bmp_pointers[];" + Constants.NEWLINE + Constants.NEWLINE);

        vramCWriter.write("#include \"frogvram.h\"" + Constants.NEWLINE + Constants.NEWLINE);
        vramCWriter.write("MR_TEXTURE* bmp_pointers[] = {" + Constants.NEWLINE + "\t");
        for (int i = 0; i < imageNames.length; i++)
            vramCWriter.write("&" + imageNames[i] + "," + (((i % 10) == 0 && i > 0) ? Constants.NEWLINE + "\t" : " "));
        vramCWriter.write(Constants.NEWLINE + "};" + Constants.NEWLINE + Constants.NEWLINE);

        for (MapBook mapBook : this.mapLibrary) {
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
        SkyLand skyLand = getSkyLand();
        short[] skyLandTextures = skyLand != null ? skyLand.getSkyLandTextures() : null;
        if (skyLandTextures != null && skyLandTextures.length > 0) {
            vramCWriter.write(Constants.NEWLINE);
            vramHWriter.write(Constants.NEWLINE);
            vramCWriter.write("MR_USHORT txl_sky_land[] = {");

            for (int i = 0; i < skyLandTextures.length; i++) {
                if (i > 0)
                    vramCWriter.write(", ");
                vramCWriter.write(String.valueOf(skyLandTextures[i]));
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
        writer.write("#define RES_NUMBER_OF_RESOURCES " + getArchiveIndex().getEntries().size() + Constants.NEWLINE + Constants.NEWLINE);

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
        for (FileEntry entry : getArchiveIndex().getEntries()) {
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
        for (LevelInfo info : this.allLevelInfo)
            if (info.getLevel() == level)
                return info;
        return null;
    }

    /**
     * Gets allowed forms for a given theme.
     * @param theme The theme to get forms for.
     * @return allowedForms
     */
    public FormEntry[] getAllowedForms(MAPTheme theme) {
        return allowedForms.computeIfAbsent(theme, safeTheme -> {
            ThemeBook themeBook = this.themeLibrary[MAPTheme.GENERAL.ordinal()];
            if (themeBook == null)
                return new FormEntry[0];

            List<FormEntry> formType = new ArrayList<>(themeBook.getFormBook());
            if (safeTheme != null && safeTheme != MAPTheme.GENERAL) {
                ThemeBook mapBook = this.themeLibrary[safeTheme.ordinal()];
                if (mapBook != null)
                    formType.addAll(mapBook.getFormBook());
            }

            return formType.toArray(new FormEntry[0]);
        });
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
    protected void onMWILoad(MWIFile mwi) {
        super.onMWILoad(mwi);
        readDemoTable(mwi, getExecutableReader());
    }

    @Override
    public void writeExecutableData(DataWriter writer, MWIFile mwiFile) {
        super.writeExecutableData(writer, mwiFile);
        writeThemeLibrary(writer);
        writeMapLibrary(writer);
        writeScripts(writer);
        writeDemoTable(writer);
        writeMusicData(writer);
        writeLevelData(writer);
    }

    private void readPickupData(DataReader reader) {
        if (getConfig().getPickupDataAddress() <= 0)
            return;

        reader.setIndex(getConfig().getPickupDataAddress());
        for (int i = 0; i < this.pickupData.length; i++) {
            long tempPointer = reader.readUnsignedIntAsLong();
            reader.jumpTemp((int) (tempPointer - getConfig().getRamPointerOffset()));
            PickupData pickupData = new PickupData(this);
            pickupData.load(reader);
            this.pickupData[i] = pickupData;
            reader.jumpReturn();
        }
    }

    private void readThemeLibrary(DataReader reader, Config config) {
        if (getConfig().getThemeBookAddress() <= 0)
            return;

        reader.setIndex(getConfig().getThemeBookAddress());
        for (int i = 0; i < this.themeLibrary.length; i++) {
            ThemeBook book = TargetPlatform.makeNewThemeBook(this);
            book.setTheme(MAPTheme.values()[i]);
            book.load(reader);
            this.themeLibrary[i] = book;
            Constants.logExeInfo(book);
        }

        if (config.hasChild(CHILD_RESTORE_THEME_BOOK)) {
            Config themeBookRestore = config.getChild(CHILD_RESTORE_THEME_BOOK);

            for (String key : themeBookRestore.keySet()) {
                MAPTheme theme = MAPTheme.getTheme(key);
                Utils.verify(theme != null, "Unknown theme: '%s'", key);
                getThemeBook(theme).handleCorrection(themeBookRestore.getString(key));
            }
        }
    }

    private void writeThemeLibrary(DataWriter exeWriter) {
        if (getConfig().getThemeBookAddress() <= 0)
            return;

        exeWriter.setIndex(getConfig().getThemeBookAddress());
        for (ThemeBook book : this.themeLibrary)
            book.save(exeWriter);
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
            NameBank formBank = getConfig().getFormBank().getChildBank(lastTheme.name());
            int nameCount = formBank != null ? formBank.size() : 0;
            int byteSize = getConfig().isAtOrBeforeBuild4() ? FormEntry.OLD_BYTE_SIZE : FormEntry.BYTE_SIZE;
            int entryCount = (int) (currentBook.getFormLibraryPointer() - lastBook.getFormLibraryPointer()) / byteSize;
            if (entryCount != nameCount)
                System.out.println(lastTheme + " has " + nameCount + " configured form names but " + entryCount + " calculated form entries in the form library.");

            // Load form library.
            lastBook.loadFormLibrary(this, nameCount);
            lastTheme = currentTheme;
        }

        // Load the last theme, which we use the number of names for to determine size.
        ThemeBook lastBook = getThemeBook(lastTheme);
        if (lastBook != null) {
            int nameCount = getConfig().getFormBank().getChildBank(lastTheme.name()).size();
            lastBook.loadFormLibrary(this, nameCount);
        }
    }

    private void readMapLibrary(DataReader reader, Config config) {
        if (getConfig().getMapBookAddress() <= 0)
            return;

        reader.setIndex(getConfig().getMapBookAddress());
        int themeAddress = getConfig().isSonyPresentation() ? Integer.MAX_VALUE : getConfig().getThemeBookAddress();
        if (themeAddress >= 0) {
            while (themeAddress > reader.getIndex()) {
                MapBook book = TargetPlatform.makeNewMapBook(this);
                book.load(reader);
                this.mapLibrary.add(book);
                Constants.logExeInfo(book);

                if (getConfig().isSonyPresentation())
                    break; // There's only one map.
            }
        }

        if (!config.hasChild(CHILD_RESTORE_MAP_BOOK))
            return;

        Config mapBookRestore = config.getChild(CHILD_RESTORE_MAP_BOOK);
        for (String key : mapBookRestore.keySet()) {
            MAPLevel level = MAPLevel.getByName(key);
            Utils.verify(level != null, "Unknown level: '%s'", key);
            Utils.verify(level.isExists(), "Cannot modify %s, its level doesn't exist.", key);
            getMapBook(level).handleCorrection(mapBookRestore.getString(key));
        }
    }

    private void writeMapLibrary(DataWriter exeWriter) {
        if (getConfig().getMapBookAddress() <= 0)
            return;

        exeWriter.setIndex(getConfig().getMapBookAddress());
        this.mapLibrary.forEach(book -> book.save(exeWriter));
    }

    private void readDemoTable(MWIFile mwiFile, DataReader reader) {
        if (getConfig().getDemoTableAddress() <= 0) { // The demo table wasn't specified, so we'll search for it ourselves.
            FileEntry demoEntry = mwiFile.getEntries().stream().filter(file -> file.getDisplayName().startsWith("SUB1DEMO.DAT")).findAny().orElse(null);
            if (demoEntry == null)
                return; // Couldn't find a demo by this name, so... skip.

            byte[] levelId = Utils.toByteArray(MAPLevel.SUBURBIA1.ordinal());
            byte[] demoId = Utils.toByteArray(demoEntry.getResourceId());

            byte[] searchFor = new byte[levelId.length + demoId.length];
            System.arraycopy(levelId, 0, searchFor, 0, levelId.length);
            System.arraycopy(demoId, 0, searchFor, levelId.length, demoId.length);

            int findIndex = Utils.indexOf(getExecutableBytes(), searchFor);
            if (findIndex == -1) {
                System.out.println("Failed to automatically find the demo table.");
                return; // Didn't find the bytes, ABORT!
            }

            getConfig().setDemoTableAddress(findIndex);
            System.out.println("Found the demo table address at " + Utils.toHexString(findIndex));
        }

        reader.setIndex(getConfig().getDemoTableAddress());
        while (reader.hasMore()) {
            int levelId = reader.readInt();
            int resourceId = reader.readInt();
            int minLevel = reader.readInt();

            if (levelId == -1 || minLevel == -1)
                break; // Reached terminator.

            // Add demo entry.
            boolean isValid = (levelId != DemoTableEntry.SKIP_INT && minLevel != DemoTableEntry.SKIP_INT);
            this.demoTableEntries.add(new DemoTableEntry(isValid ? MAPLevel.values()[levelId] : null, isValid ? resourceId : -1, isValid ? MAPLevel.values()[minLevel] : null, isValid));
        }
    }

    private void writeDemoTable(DataWriter exeWriter) {
        if (getConfig().getDemoTableAddress() <= 0)
            return;

        exeWriter.setIndex(getConfig().getDemoTableAddress());
        for (DemoTableEntry entry : this.demoTableEntries)
            entry.save(exeWriter);
    }

    private void readScripts(DataReader reader) {
        if (getConfig().getScriptArrayAddress() <= 0)
            return; // Wasn't specified.

        reader.setIndex(getConfig().getScriptArrayAddress());
        for (int i = 0; i < getConfig().getScriptBank().size(); i++) {
            long address = reader.readUnsignedIntAsLong();
            if (address == 0) { // Default / null.
                this.scripts.add(FroggerScript.EMPTY_SCRIPT);
                continue;
            }

            reader.jumpTemp((int) (address - getConfig().getRamPointerOffset()));
            FroggerScript newScript = new FroggerScript(this);
            this.scripts.add(newScript); // Adds before loading so getName() can be accessed.
            try {
                newScript.load(reader);
            } catch (Throwable th) {
                System.out.println("Failed to load script '" + getConfig().getScriptBank().getName(i) + "'");
                th.printStackTrace();

                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    // Do nothing.
                }
            } finally {
                reader.jumpReturn();
            }
        }
    }

    private void writeScripts(DataWriter exeWriter) {
        if (getConfig().getScriptArrayAddress() <= 0)
            return; // Wasn't specified.

        DataReader reader = getExecutableReader();
        reader.setIndex(getConfig().getScriptArrayAddress());
        for (int i = 0; i < this.scripts.size(); i++) {
            long address = reader.readUnsignedIntAsLong();
            if (address == 0) // Default / null.
                continue;

            FroggerScript script = this.scripts.get(i);
            if (script.isTooLarge())
                System.out.println("WARNING: Saving " + script.getName() + ", which is larger than what is considered safe!");

            exeWriter.setIndex((int) (address - getConfig().getRamPointerOffset()));
            script.save(exeWriter);
        }
    }

    private void readMusicData(DataReader reader) {
        if (getConfig().getMusicAddress() <= 0)
            return;

        byte readByte;
        reader.setIndex(getConfig().getMusicAddress());
        while ((readByte = reader.readByte()) != MusicTrack.TERMINATOR)
            this.musicTracks.add(MusicTrack.getTrackById(this, readByte));
    }

    private void writeMusicData(DataWriter exeWriter) {
        if (getConfig().getMusicAddress() <= 0)
            return;

        exeWriter.setIndex(getConfig().getMusicAddress());
        this.musicTracks.forEach(track -> exeWriter.writeByte(track.getTrack(this)));
        exeWriter.writeByte(MusicTrack.TERMINATOR);
    }

    private void readLevelData(DataReader reader) {
        if (getConfig().getArcadeLevelAddress() <= 0)
            return; // No level select is present.

        reader.setIndex(getConfig().getArcadeLevelAddress());
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
        if (getConfig().getArcadeLevelAddress() <= 0)
            return; // No level select is present.

        exeWriter.setIndex(getConfig().getArcadeLevelAddress());
        this.arcadeLevelInfo.forEach(level -> level.save(exeWriter));
        this.raceLevelInfo.forEach(level -> level.save(exeWriter));
    }
}