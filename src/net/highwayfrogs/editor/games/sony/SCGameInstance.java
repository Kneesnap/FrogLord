package net.highwayfrogs.editor.games.sony;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FixedArrayReceiver;
import net.highwayfrogs.editor.gui.MainController.SCDisplayedFileType;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an instance of a game created by Sony Cambridge / Millennium Interactive.
 * TODO: Let's add a logger object to this one, and start to use it for logging.
 * TODO: Let's add a function to resolve what VLO should be used for a particular file (generally map, mof, or wad). This differs per-game so it could be great to have it here.
 * Created by Kneesnap on 9/7/2023.
 */
public abstract class SCGameInstance {
    @Getter private final SCGameType gameType;
    @Getter private final Map<SCGameFile<?>, FileEntry> fileEntriesByFileObjects;
    @Getter private final Map<FileEntry, SCGameFile<?>> fileObjectsByFileEntries;
    @Getter private SCGameConfig config;
    @Getter private MWDFile mainArchive;
    @Getter private MWIFile archiveIndex;
    @Getter private File mwdFile;
    @Getter private File exeFile;

    // Instance data read from game files:
    private final Map<FileEntry, List<Short>> remapTable = new HashMap<>();
    @Getter private final List<Long> bmpTexturePointers = new ArrayList<>();

    private byte[] cachedExecutableBytes;
    private DataReader cachedExecutableReader;

    public SCGameInstance(SCGameType gameType) {
        this.gameType = gameType;
        this.fileEntriesByFileObjects = new HashMap<>();
        this.fileObjectsByFileEntries = new HashMap<>();
    }

    /**
     * Associate a new object with a FileEntry.
     * @param entry   The entry to associate the file with.
     * @param newFile The file to associate with the entry.
     * @return The old file associated with the entry.
     */
    public SCGameFile<?> trackFile(FileEntry entry, SCGameFile<?> newFile) {
        SCGameFile<?> oldFile = this.fileObjectsByFileEntries.remove(entry);
        if (oldFile != null)
            this.fileEntriesByFileObjects.remove(oldFile, entry);

        this.fileObjectsByFileEntries.put(entry, newFile);
        this.fileEntriesByFileObjects.put(newFile, entry);
        return oldFile;
    }

    /**
     * Load and setup all instance data relating to the game such as version configuration and game files.
     * @param configName The name of the version configuration to load.
     * @param config     The config to load.
     * @param mwdFile    The file representing the Millennium WAD.
     * @param exeFile    The main game executable file, containing the MWI.
     */
    public void loadGame(String configName, Config config, File mwdFile, File exeFile) {
        if (this.config != null || this.mainArchive != null)
            throw new RuntimeException("The game instance has already been loaded.");

        if (mwdFile == null || !mwdFile.exists())
            throw new RuntimeException("The MWD file '" + mwdFile + "' does not exist.");
        if (exeFile == null || !exeFile.exists())
            throw new RuntimeException("The executable file '" + exeFile + "' does not exist.");

        this.mwdFile = mwdFile;
        this.exeFile = exeFile;
        this.config = makeConfig(configName);
        this.config.loadData(config);
        this.onConfigLoad(config);
        this.archiveIndex = this.readMWI();
        this.mainArchive = this.readMWD();
    }

    /**
     * Called when configuration data is loaded.
     * @param configObj The config object which data is loaded from.
     */
    protected void onConfigLoad(Config configObj) {
        this.readBmpPointerData(getExecutableReader());
    }

    /**
     * Called when an MWI file finishes loading using this configuration.
     * @param mwiFile The mwi file which loaded.
     */
    protected void onMWILoad(MWIFile mwiFile) {
        readTextureRemapData(getExecutableReader(), mwiFile);
        // Do nothing. (Overrides will do stuff probably)
    }

    /**
     * Called when a MWD file finishes loading using this configuration.
     * @param mwdFile The mwd file which loaded.
     */
    protected void onMWDLoad(MWDFile mwdFile) {
        validateBmpPointerData(mwdFile);
    }

    /**
     * Makes a new game config instance for this game.
     */
    protected abstract SCGameConfig makeConfig(String internalName);

    /**
     * Creates a SCGameFile object for the given file entry.
     * @param fileEntry The file entry to create the file from.
     * @param fileData  The raw file data to test the file with.
     * @return newFile
     */
    public abstract SCGameFile<?> createFile(FileEntry fileEntry, byte[] fileData);

    /**
     * Reads texture remap data from the reader.
     * @param exeReader The reader to read texture remap data from.
     * @param mwiFile   The index to use for file access.
     */
    protected abstract void readTextureRemapData(DataReader exeReader, MWIFile mwiFile);

    /**
     * Writes texture remap data to the target data writer.
     * @param exeWriter The writer to write texture remap data to.
     */
    protected abstract void writeTextureRemapData(DataWriter exeWriter);

    /**
     * Setup a list of supported file types.
     * @param fileTypes The list to setup.
     */
    public abstract void setupFileTypes(List<SCDisplayedFileType> fileTypes);

    /**
     * Tests if a given unsigned 32 bit number passed as a long looks like a valid pointer to memory present in the executable.
     * @param testPointer The pointer to test.
     * @return If it looks good or not.
     */
    public boolean isValidLookingPointer(long testPointer) {
        return SCUtils.isValidLookingPointer(getPlatform(), testPointer);
    }

    /**
     * Get the FileEntry for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntry
     */
    public FileEntry getResourceEntryByID(int resourceId) {
        if (this.archiveIndex == null)
            throw new RuntimeException("The MWI was not loaded, so we cannot yet search.");

        return this.archiveIndex.getResourceEntryByID(resourceId);
    }

    /**
     * Gets the resource entry from a given name.
     * @param name The name to lookup.
     * @return foundEntry, if any.
     */
    public FileEntry getResourceEntryByName(String name) {
        if (this.archiveIndex == null)
            throw new RuntimeException("The MWI was not loaded, so we cannot yet search.");

        return this.archiveIndex.getResourceEntryByName(name);
    }

    /**
     * Get the FileEntry name for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntryName
     */
    public String getResourceName(int resourceId) {
        FileEntry entry = this.archiveIndex.getResourceEntryByID(resourceId);
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
    public <T extends SCGameFile<?>> T getGameFile(int resourceId) {
        return (T) this.fileObjectsByFileEntries.get(getResourceEntryByID(resourceId));
    }

    /**
     * Gets a GameFile by its resource entry.
     * @param fileEntry The file resource entry.
     * @return gameFile
     */
    @SuppressWarnings("unchecked")
    public <T extends SCGameFile<?>> T getGameFile(FileEntry fileEntry) {
        return (T) this.fileObjectsByFileEntries.get(fileEntry);
    }

    /**
     * Gets a texture id by its pointer.
     * @param pointer The pointer of the texture.
     * @return textureId
     */
    public int getTextureIdFromPointer(long pointer) {
        if (this.bmpTexturePointers.isEmpty())
            throw new RuntimeException("Cannot get texture-id from pointer without bmpPointerAddress being set!");
        return this.bmpTexturePointers.indexOf(pointer);
    }

    /**
     * Attempts to find an image by its pointer.
     * @param pointer The pointer get get the image for.
     * @return matchingImage - May be null.
     */
    public GameImage getImageFromPointer(long pointer) {
        if (this.mainArchive == null)
            throw new RuntimeException("The MWD was not loaded, so we cannot yet search.");

        return this.mainArchive.getImageByTextureId(getTextureIdFromPointer(pointer));
    }

    /**
     * Get the remap table for a particular file.
     * @param file the file which has an associated remap to lookup.
     * @return remapTable
     */
    public List<Short> getRemapTable(FileEntry file) {
        return this.remapTable.get(file);
    }

    /**
     * Override remap data in the exe.
     * @param mapEntry    The FileEntry belonging to the map to replace.
     * @param remapImages The new image remap array.
     */
    public void setRemap(FileEntry mapEntry, List<Short> remapImages) {
        List<Short> realRemap = this.remapTable.get(mapEntry);
        if (realRemap == null) {
            this.remapTable.put(mapEntry, remapImages);
            return;
        }

        Utils.verify(realRemap.size() >= remapImages.size(), "New remap table cannot be larger than the old remap table.");
        for (int i = 0; i < remapImages.size(); i++)
            realRemap.set(i, remapImages.get(i));
    }

    /**
     * Gets the FPS this game will run at.
     * @return fps
     */
    public int getFPS() {
        return isPSX() ? 30 : 25;
    }

    /**
     * Get the target platform this game version runs on.
     */
    public SCGamePlatform getPlatform() {
        return this.config.getPlatform();
    }

    /**
     * Test if this is a game version intended for Windows.
     * @return isPCRelease
     */
    public boolean isPC() {
        return getPlatform() == SCGamePlatform.WINDOWS;
    }

    /**
     * Test if this is a game version intended for the PlayStation.
     * @return isPSXRelease
     */
    public boolean isPSX() {
        return getPlatform() == SCGamePlatform.PLAYSTATION;
    }

    /**
     * Tests if the game currently being read is Frogger.
     */
    public boolean isOldFrogger() {
        return this.gameType == SCGameType.OLD_FROGGER;
    }

    /**
     * Tests if the game currently being read is Frogger.
     */
    public boolean isFrogger() {
        return this.gameType == SCGameType.FROGGER;
    }

    /**
     * Tests if the game currently being read is Beast Wars.
     */
    public boolean isBeastWars() {
        return this.gameType == SCGameType.BEAST_WARS;
    }

    /**
     * Tests if the game currently being read is MediEvil.
     */
    public boolean isMediEvil() {
        return this.gameType == SCGameType.MEDIEVIL;
    }

    /**
     * Get a byte array of the bytes comprising the game executable.
     * If this array is modified, the changes will be kept for any future save of the executable.
     */
    public byte[] getExecutableBytes() {
        if (this.cachedExecutableBytes == null) {
            try {
                if (this.exeFile == null || !this.exeFile.exists() || !this.exeFile.isFile())
                    throw new FileNotFoundException("The game executable file '" + this.exeFile + " appears to not exist.");

                this.cachedExecutableBytes = Files.readAllBytes(this.exeFile.toPath());
            } catch (IOException ex) {
                throw new RuntimeException("Failed to read game executable '" + this.exeFile + "'.", ex);
            }
        }

        return this.cachedExecutableBytes;
    }

    /**
     * Gets a data reader which can read data from the game executable.
     */
    public DataReader getExecutableReader() {
        if (this.cachedExecutableReader != null) {
            this.cachedExecutableReader.setIndex(0);
        } else {
            this.cachedExecutableReader = new DataReader(new ArraySource(getExecutableBytes()));
        }

        return this.cachedExecutableReader;
    }

    /**
     * Creates a data writer which can write data to the game executable.
     */
    public DataWriter createExecutableWriter() {
        byte[] cachedData = getExecutableBytes(); // Ensure the bytes have been read.
        return new DataWriter(new FixedArrayReceiver(cachedData));
    }

    // Beyond here are functions for handling game data from game files, and potentially also configuration data.
    private void readBmpPointerData(DataReader reader) {
        this.bmpTexturePointers.clear();
        if (this.config.getBmpPointerAddress() <= 0)
            return; // Not specified.

        reader.setIndex(this.config.getBmpPointerAddress());

        long nextPossiblePtr;
        while (reader.hasMore() && isValidLookingPointer(nextPossiblePtr = reader.readUnsignedIntAsLong()))
            this.bmpTexturePointers.add(nextPossiblePtr);

        System.out.println("Read " + this.bmpTexturePointers.size() + " texture pointers."); // TODO: TOSS later..?
    }

    private void validateBmpPointerData(MWDFile mwdFile) {
        if (this.bmpTexturePointers.isEmpty())
            return;

        short highestTextureId = -1;
        for (VLOArchive vloArchive : mwdFile.getAllFiles(VLOArchive.class))
            for (GameImage image : vloArchive.getImages())
                if (image.getTextureId() > highestTextureId)
                    highestTextureId = image.getTextureId();

        // Another option for this is that texture remap tables appear to occur immediately after the texture array.
        // In the interest of cross-game compatibility, it was easier to do it this way.
        if (highestTextureId >= 0 && highestTextureId + 1 != this.bmpTexturePointers.size())
            System.out.println("We found pointers to " + this.bmpTexturePointers.size() + " textures, but only the highest texture ID we saw suggests there should have been " + (highestTextureId + 1) + ".");
    }

    private void writeBmpPointerData(DataWriter exeWriter) {
        if (this.config.getBmpPointerAddress() <= 0 || this.bmpTexturePointers.isEmpty())
            return; // Not specified.

        exeWriter.setIndex(this.config.getBmpPointerAddress());
        this.bmpTexturePointers.forEach(exeWriter::writeUnsignedInt);
    }

    /**
     * Read the MWI file from the executable.
     */
    public MWIFile readMWI() {
        if (this.config.getMWIOffset() <= 0)
            throw new RuntimeException("The MWI cannot be read because either no MWI offset was specified or the configuration hasn't been loaded yet.");

        // Read MWI bytes.
        DataReader reader = getExecutableReader();
        reader.setIndex(this.config.getMWIOffset());
        byte[] mwiBytes = reader.readBytes(this.config.getMWILength());

        // Load an MWI file.
        DataReader arrayReader = new DataReader(new ArraySource(mwiBytes));
        MWIFile mwiFile = new MWIFile(this);
        this.archiveIndex = mwiFile;
        mwiFile.load(arrayReader);
        this.onMWILoad(mwiFile);
        return mwiFile;
    }

    /**
     * Write the MWI to the provided writer.
     * @param writer  The writer to write the MWI to.
     * @param mwiFile The MWI file to save.
     */
    public void writeMWI(DataWriter writer, MWIFile mwiFile) {
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter mwiWriter = new DataWriter(receiver);
        mwiFile.save(mwiWriter);
        mwiWriter.closeReceiver();

        // Verify MWI size ok.
        int bytesWritten = writer.getIndex() - this.config.getMWIOffset();
        Utils.verify(bytesWritten == this.config.getMWILength(), "Saving the MWI failed. The size of the written MWI does not match the correct MWI size! [%d/%d]", bytesWritten, this.config.getMWILength());

        // Write MWI to the provided writer.
        writer.setIndex(this.config.getMWIOffset());
        writer.writeBytes(receiver.toArray());
    }

    /**
     * Read the MWD file.
     */
    public MWDFile readMWD() {
        if (this.config.getMWIOffset() <= 0)
            throw new RuntimeException("The MWI cannot be read because either no MWI offset was specified or the configuration hasn't been loaded yet.");

        FileSource fileSource;

        try {
            fileSource = new FileSource(this.mwdFile);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read MWD file '" + this.mwdFile + "'.");
        }

        // Load the MWD file.
        DataReader arrayReader = new DataReader(fileSource);
        MWDFile mwdFile = new MWDFile(this);
        this.mainArchive = mwdFile;
        mwdFile.load(arrayReader);
        this.onMWDLoad(mwdFile);
        return mwdFile;
    }

    /**
     * Saves the cached executable bytes with any modifications applied to a file.
     * @param outputFile         The file to save to.
     * @param writeModifications If modifications to instance executable data should be applied. (Changes previously applied will be written regardless)
     * @throws IOException Thrown when writing to the file failed.
     */
    public void saveExecutable(File outputFile, boolean writeModifications) throws IOException {
        if (writeModifications)
            writeExecutableData(getArchiveIndex());

        byte[] data = getExecutableBytes();
        data = this.config.applyConfigIdentifier(data);

        // Write file.
        Utils.deleteFile(outputFile);
        Files.write(outputFile.toPath(), data);
    }

    /**
     * Write potentially modified data from the instance object to the executable.
     * @param mwiFile The mwi file to write.
     */
    public void writeExecutableData(MWIFile mwiFile) {
        DataWriter writer = createExecutableWriter();
        try {
            this.writeExecutableData(writer, mwiFile);
        } catch (Throwable th) {
            throw new RuntimeException("Failed to write instance data to the executable.", th);
        }

        writer.closeReceiver();
    }

    /**
     * Write potentially modified data from the instance object to the executable.
     * @param writer  The writer to write the data to.
     * @param mwiFile The mwi file to write.
     */
    public void writeExecutableData(DataWriter writer, MWIFile mwiFile) {
        if (mwiFile != null)
            this.writeMWI(writer, mwiFile);

        this.writeBmpPointerData(writer);
        writeTextureRemapData(writer);
    }
}