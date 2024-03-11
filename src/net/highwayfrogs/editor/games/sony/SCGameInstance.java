package net.highwayfrogs.editor.games.sony;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
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
import net.highwayfrogs.editor.games.sony.shared.LinkedTextureRemap;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.MainController.SCDisplayedFileType;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

/**
 * Represents an instance of a game created by Sony Cambridge / Millennium Interactive.
 * TODO: Let's add a logger object to this one, and start to use it for logging.
 * TODO: Let's add a function to resolve what VLO should be used for a particular file (generally map, mof, or wad). This differs per-game so it could be great to have it here.
 * TODO: Let's fix the MOF UI to rotate collision boxes and collprim boxes with the part rotation.
 * TODO: https://docs.oracle.com/javase/8/javafx/graphics-tutorial/sampleapp3d.htm#CJAHFAF shows how to show a unit axis. this gives me an idea, perhaps we can use this for 3D position and rotation widgets.
 * TODO: Mess around with JavaFX TriangleMesh.getFaceSmoothingGroups(). This might have something to do with the reason stuff looks bad from a far? It's not currently clear. We may consider using POINT_NORMAL_TEXCOORD to disable it, with calculated (or dummy?) vertices.
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
    @Getter private long ramOffset;
    private Logger cachedLogger;

    // Instance data read from game files:
    private boolean loadingAllRemaps;
    @Getter private final List<TextureRemapArray> textureRemaps = new ArrayList<>();
    private final Map<FileEntry, LinkedTextureRemap<?>> linkedTextureMaps = new HashMap<>();
    @Getter private final List<Long> bmpTexturePointers = new ArrayList<>();

    private byte[] cachedExecutableBytes;
    private DataReader cachedExecutableReader;

    public SCGameInstance(SCGameType gameType) {
        this.gameType = gameType;
        this.fileEntriesByFileObjects = new HashMap<>();
        this.fileObjectsByFileEntries = new HashMap<>();
    }

    /**
     * Gets the logger for this game instance.
     */
    public Logger getLogger() {
        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = Logger.getLogger(Utils.getSimpleName(this));
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
        DataReader exeReader = getExecutableReader();
        readExecutableHeader(exeReader);

        // Read data. (Should occur after we know the executable header info)
        this.readBmpPointerData(exeReader);
    }

    /**
     * Called when an MWI file finishes loading using this configuration.
     * @param mwiFile The mwi file which loaded.
     */
    protected void onMWILoad(MWIFile mwiFile) {
        readTextureRemaps();
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
     * Finds and configures texture remap data.
     * @param exeReader The reader to read texture remap data from.
     * @param mwiFile   The index to use for file access.
     */
    protected abstract void setupTextureRemaps(DataReader exeReader, MWIFile mwiFile);

    /**
     * Reads texture remap data.
     */
    public final void readTextureRemaps() {
        // Reset all texture remaps.
        this.textureRemaps.clear();
        this.linkedTextureMaps.clear();

        // Add & load all texture remaps.
        try {
            this.loadingAllRemaps = true;
            setupTextureRemaps(getExecutableReader(), getArchiveIndex());

            // Read remap data.
            DataReader reader = getExecutableReader();
            for (int i = 0; i < this.textureRemaps.size(); i++)
                this.updateRemap(reader, i);
        } catch (Throwable th) {
            this.textureRemaps.clear();
            this.linkedTextureMaps.clear();
            throw new RuntimeException("Failed to read texture remaps.", th);
        } finally {
            // Mark it as okay to update individual remaps when they are added now.
            this.loadingAllRemaps = false;
        }
    }

    /**
     * Get the remap table for a particular file.
     * @param file the file which has an associated remap to lookup.
     * @return remapTable
     */
    public LinkedTextureRemap<?> getLinkedTextureRemap(FileEntry file) {
        return this.linkedTextureMaps.get(file);
    }

    /**
     * Check if the end of a remap has been reached.
     * @param current The current remap.
     * @param next    The remap located after this one, if one exists.
     * @param reader  The reader at the position to test.
     * @param value   The texture id value read.
     * @return Has the end of the remap been reached?
     */
    protected boolean isEndOfRemap(TextureRemapArray current, TextureRemapArray next, DataReader reader, short value) {
        if ((value == 0) || (next != null && (reader.getIndex() - Constants.SHORT_SIZE) >= next.getReaderIndex()))
            return true;

        // Look a pointer beyond the end of the remap table.
        if (next == null && isPSX()) {
            reader.jumpTemp(reader.getIndex());
            long nextValue = reader.readUnsignedIntAsLong();
            reader.jumpReturn();

            return SCUtils.isValidLookingPointer(SCGamePlatform.PLAYSTATION, nextValue); // If the next value is a pointer, abort!
        }

        // Doesn't look like the end of a remap.
        return false;
    }

    /**
     * Writes texture remap data.
     * @param writer The writer to write texture remap data to.
     */
    public final void writeTextureRemaps(DataWriter writer) {
        for (int i = 0; i < this.textureRemaps.size(); i++) {
            TextureRemapArray textureRemap = this.textureRemaps.get(i);
            TextureRemapArray nextTextureRemap = this.textureRemaps.size() > i + 1 ? this.textureRemaps.get(i + 1) : null;

            // Verify there is enough space.
            if (nextTextureRemap != null) {
                int availableSlots = (nextTextureRemap.getLoadAddress() - textureRemap.getLoadAddress()) / Constants.SHORT_SIZE;
                if (textureRemap.getTextureIds().size() > availableSlots) {
                    getLogger().warning("'" + textureRemap.getDebugName() + "' has been skipped because it has " + textureRemap.getTextureIds().size() + "texture ids, but there is only room for " + availableSlots + ".");
                    continue;
                }
            }

            writer.jumpTemp(textureRemap.getReaderIndex());
            for (int j = 0; j < textureRemap.getTextureIds().size(); j++)
                writer.writeShort(textureRemap.getTextureIds().get(j));
            writer.jumpReturn();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void updateRemap(DataReader reader, int index) {
        TextureRemapArray textureRemap = this.textureRemaps.get(index);
        TextureRemapArray nextTextureRemap = this.textureRemaps.size() > index + 1 ? this.textureRemaps.get(index + 1) : null;

        // Clear texture ids.
        textureRemap.getTextureIds().clear();

        // Read new texture ids.
        short value = 0;
        reader.jumpTemp(textureRemap.getReaderIndex());
        while (reader.hasMore() && !isEndOfRemap(textureRemap, nextTextureRemap, reader, value = reader.readShort()))
            textureRemap.getTextureIds().add(value);

        // The position we want to pass to the hook is the position the remap ends. '0' is included implicitly as padding by the compiler if it's not aligned.
        // So, if the value is 0 we can stay put, but if it wasn't zero, we read further remap data and should go back.
        if (value != 0) {
            if (textureRemap.getTextureIds().size() > 0)
                reader.setIndex(reader.getIndex() - Constants.SHORT_SIZE);
        } else {
            int endIndex = nextTextureRemap != null ? nextTextureRemap.getReaderIndex() : reader.getSize();

            // Value is 0, keep reading until we hit something which isn't 0 (Or we hit another remap)
            while (endIndex >= reader.getIndex() && reader.readShort() == 0) ;
            reader.setIndex(reader.getIndex() - Constants.SHORT_SIZE);
        }

        // Run hook (Allows adding new remaps after this one, but NOT before)
        onRemapRead(textureRemap, reader);

        // Check there aren't any gaps in data.
        nextTextureRemap = this.textureRemaps.size() > index + 1 ? this.textureRemaps.get(index + 1) : null;
        int extraBytes = nextTextureRemap != null ? nextTextureRemap.getReaderIndex() - reader.getIndex() : 0;
        if (extraBytes != 0)
            getLogger().warning(textureRemap + " has " + extraBytes + " unread bytes between it and " + nextTextureRemap + ".");

        // Return, but only after calling hook.
        reader.jumpReturn();
    }

    /**
     * Registers a texture remap to the game instance.
     * @param remap The remap to register
     * @return If the remap was added successfully. If a remap exists at this position, it will return false.
     */
    public boolean addRemap(TextureRemapArray remap) {
        if (remap == null)
            throw new IllegalArgumentException("Cannot add a null remap.");

        // Ensure remap isn't already registered.
        int index = Collections.binarySearch(this.textureRemaps, remap, Comparator.comparingInt(TextureRemapArray::getLoadAddress));
        if (index >= 0)
            return false;

        // Register remap.
        index = -(index + 1);
        this.textureRemaps.add(index, remap);
        onRemapRegistered(remap);

        // Update remap tracking.
        if (!this.loadingAllRemaps) {
            // Read contents of current remap
            updateRemap(getExecutableReader(), index);

            // Update the previous remap to ensure it ends at the proper spot.
            if (index > 0)
                updateRemap(getExecutableReader(), index - 1);
        }

        return true;
    }

    /**
     * Called when a remap is registered.
     * @param remap The remap in question.
     */
    protected void onRemapRegistered(TextureRemapArray remap) {
        if (remap instanceof LinkedTextureRemap<?>) {
            LinkedTextureRemap<?> linkedRemap = (LinkedTextureRemap<?>) remap;
            LinkedTextureRemap<?> oldLinkedRemap = this.linkedTextureMaps.put(linkedRemap.getFileEntry(), linkedRemap);
            if (oldLinkedRemap != null)
                getLogger().warning("A remap (" + oldLinkedRemap + ") that was previously linked to '" + linkedRemap.getFileEntry().getDisplayName() + "' has been overwritten by " + linkedRemap + ".");
        }
    }

    /**
     * Called when a remap is read.
     * @param remap  The remap in question.
     * @param reader The reader it was read from.
     */
    protected void onRemapRead(TextureRemapArray remap, DataReader reader) {

    }

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

    private void readExecutableHeader(DataReader reader) {
        if (getConfig().getOverrideRamOffset() != 0) {
            this.ramOffset = getConfig().getOverrideRamOffset();
        } else if (isPSX()) {
            reader.jumpTemp(0);
            reader.verifyString("PS-X EXE"); // Ensure it's a PSX executable.
            reader.skipBytes(16);
            this.ramOffset = reader.readUnsignedIntAsLong() - 0x800; // 0x800 is a CD sector. It's also the distance between the exe header and the start of the executable data put in memory.
            reader.jumpReturn();
        } else {
            throw new RuntimeException("Failed to load ramOffset for '" + getConfig().getInternalName() + "', it may need to be added to the configuration.");
        }
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
            getLogger().warning("We found pointers to " + this.bmpTexturePointers.size() + " textures, but only the highest texture ID we saw suggests there should have been " + (highestTextureId + 1) + ".");
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
        int bytesWritten = mwiWriter.getIndex();
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
        this.writeTextureRemaps(writer);
    }
}