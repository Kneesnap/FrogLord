package net.highwayfrogs.editor.games.sony;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.shared.basic.GameBuildInfo;
import net.highwayfrogs.editor.games.sony.shared.LinkedTextureRemap;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MillenniumWadIndex;
import net.highwayfrogs.editor.games.sony.shared.overlay.SCOverlayTable;
import net.highwayfrogs.editor.games.sony.shared.ui.SCGameFileGroupedListViewComponent;
import net.highwayfrogs.editor.games.sony.shared.ui.SCMainMenuUIController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.reader.FileSource;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FixedArrayReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

/**
 * Represents an instance of a game created by Sony Cambridge / Millennium Interactive.
 * TODO: Let's fix the MOF UI to rotate collision boxes and collprim boxes with the part rotation.
 * Created by Kneesnap on 9/7/2023.
 */
public abstract class SCGameInstance extends GameInstance {
    @Getter private final Map<MWIResourceEntry, SCGameFile<?>> fileObjectsByFileEntries;
    @Getter private final SCOverlayTable overlayTable;
    @Getter private MWDFile mainArchive;
    @Getter private MillenniumWadIndex archiveIndex;
    @Getter private File mwdFile;
    @Getter private File exeFile;
    @Getter private long ramOffset;

    // Instance data read from game files:
    private boolean loadingAllRemaps;
    @Getter private final List<TextureRemapArray> textureRemaps = new ArrayList<>();
    private final Map<MWIResourceEntry, LinkedTextureRemap<?>> linkedTextureMaps = new HashMap<>();
    @Getter private final List<Long> bmpTexturePointers = new ArrayList<>();

    private byte[] cachedExecutableBytes;
    private DataReader cachedExecutableReader;

    public SCGameInstance(SCGameType gameType) {
        super(gameType);
        this.fileObjectsByFileEntries = new HashMap<>();
        this.overlayTable = new SCOverlayTable(this);
    }

    @Override
    public File getMainGameFolder() {
        if (this.exeFile != null) {
            return this.exeFile.getParentFile();
        } else if (this.mwdFile != null) {
            return this.mwdFile.getParentFile();
        } else {
            throw new IllegalStateException("Cannot get the game folder, no game files are loaded.");
        }
    }

    /**
     * Associate a new object with a MWIResourceEntry.
     * @param entry   The entry to associate the file with.
     * @param newFile The file to associate with the entry.
     * @return The old file associated with the entry.
     */
    public SCGameFile<?> trackFile(MWIResourceEntry entry, SCGameFile<?> newFile) {
        SCGameFile<?> oldFile = this.fileObjectsByFileEntries.remove(entry);
        if (oldFile != null)
            oldFile.setFileDefinition(null);

        this.fileObjectsByFileEntries.put(entry, newFile);
        if (newFile != null)
            newFile.setFileDefinition(entry);
        return oldFile;
    }

    /**
     * Load and setup all instance data relating to the game such as version configuration and game files.
     * @param versionConfigName The name of the version configuration to load.
     * @param instanceConfig The configuration stored for the user on a per-game-version basis.
     * @param mwdFile The file representing the Millennium WAD.
     * @param exeFile The main game executable file, containing the MWI.
     * @param progressBar the progress bar to display load progress on, if it exists
     */
    public void loadGame(String versionConfigName, net.highwayfrogs.editor.system.Config instanceConfig, File mwdFile, File exeFile, ProgressBarComponent progressBar) {
        if (this.mainArchive != null)
            throw new RuntimeException("The game instance has already been loaded.");

        // Verify files.
        if (exeFile == null || !exeFile.exists() || !exeFile.isFile())
            throw new RuntimeException("The executable file '" + exeFile + "' does not exist.");

        this.mwdFile = mwdFile;
        this.exeFile = exeFile;
        loadGameConfig(versionConfigName, instanceConfig);
        if (!this.getVersionConfig().isMwdLooseFiles() && (mwdFile == null || !mwdFile.exists() || !mwdFile.isFile()))
            throw new RuntimeException("The MWD file '" + mwdFile + "' does not exist.");

        this.archiveIndex = this.readMWI();
        this.mainArchive = this.readMWD(progressBar);
        resolveModelVloFiles();
    }

    @Override
    protected void onConfigLoad(Config configObj) {
        super.onConfigLoad(configObj);

        DataReader exeReader = getExecutableReader();
        readExecutableHeader(exeReader);

        // Read data. (Should occur after we know the executable header info)
        this.readOverlayTable(exeReader);
        this.readBmpPointerData(exeReader);
    }

    @Override
    public boolean isShowSaveWarning() {
        return getGameType() == null || getGameType().isShowSaveWarning();
    }

    @Override
    public SCGameType getGameType() {
        return (SCGameType) super.getGameType();
    }

    @Override
    public SCGameConfig getVersionConfig() {
        return (SCGameConfig) super.getVersionConfig();
    }

    @Override
    public URL getFXMLTemplateURL(String template) {
        URL templateUrl = super.getFXMLTemplateURL(template);
        if (templateUrl == null) // Lookup from shared fxml templates if the lookup in this game fails.
            templateUrl = FileUtils.getResourceURL("games/sony/fxml/" + template + ".fxml");

        return templateUrl;
    }

    @Override
    protected void setupScriptEngine(NoodleScriptEngine engine) {
        super.setupScriptEngine(engine);
        engine.addWrapperTemplates(SCGameData.class, SCGameFile.class, SCGameInstance.class, SCGameObject.class, SCGameConfig.class);
        engine.addWrapperTemplates(SCMath.class, SCUtils.class, MWDFile.class, MWIResourceEntry.class, TextureRemapArray.class, SCChunkedFile.class, SCByteTextureUV.class, LinkedTextureRemap.class);
        engine.addWrapperTemplates(VLOArchive.class, GameImage.class, MRModel.class, MRStaticMof.class, MRAnimatedMof.class, WADFile.class, WADEntry.class);
    }

    @Override
    public SCMainMenuUIController<?> getMainMenuController() {
        return (SCMainMenuUIController<?>) super.getMainMenuController();
    }

    @Override
    protected SCMainMenuUIController<?> makeMainMenuController() {
        return new SCMainMenuUIController<>(this);
    }

    /**
     * Called when an MWI file finishes loading using this configuration.
     * @param wadIndex The mwi file which loaded.
     */
    protected void onMWILoad(MillenniumWadIndex wadIndex) {
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
     * Creates a SCGameFile object for the given file entry.
     * @param resourceEntry The file entry to create the file from.
     * @param fileData  The raw file data to test the file with.
     * @return newFile
     */
    public abstract SCGameFile<?> createFile(MWIResourceEntry resourceEntry, byte[] fileData);

    /**
     * Finds and configures texture remap data.
     * @param exeReader The reader to read texture remap data from.
     * @param wadIndex   The index to use for file access.
     */
    protected abstract void setupTextureRemaps(DataReader exeReader, MillenniumWadIndex wadIndex);

    /**
     * Resolves the .VLO files used by each model in the game.
     */
    protected void resolveModelVloFiles() {
        int missingCount = 0;
        for (MRModel model : this.mainArchive.getAllFiles(MRModel.class)) {
            if (model.getVloFile() != null)
                continue;

            VLOArchive mainVlo = resolveMainVlo(model);
            if (mainVlo != null) {
                model.setVloFile(mainVlo);
            } else {
                missingCount++;
            }
        }

        if (missingCount > 0)
            getLogger().warning("Unable to resolve main VLO for %d model file(s).", missingCount);
    }

    /**
     * Resolves the main VLO file used by a particular model.
     * @param model the model to resolve
     * @return modelVlo
     */
    protected VLOArchive resolveMainVlo(MRModel model) {
        // Attempt to resolve a VLO with the same name as the .WAD file which holds the file.
        WADFile wadFile = model.getParentWadFile();
        if (wadFile != null) {
            String searchFileName = FileUtils.stripExtension(wadFile.getFileDisplayName()) + ".VLO";
            return getMainArchive().getFileByName(searchFileName);
        }

        return null;
    }

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
    public LinkedTextureRemap<?> getLinkedTextureRemap(MWIResourceEntry file) {
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

            return isValidLookingPointer(nextValue); // If the next value is a pointer, abort!
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
                int availableSlots = (int) ((nextTextureRemap.getLoadAddress() - textureRemap.getLoadAddress()) / Constants.SHORT_SIZE);
                if (textureRemap.getTextureIds().size() > availableSlots) {
                    getLogger().warning("'%s' has been skipped because it has %d texture ids, but there is only room for %d.", textureRemap.getDebugName(), textureRemap.getTextureIds().size(), availableSlots);
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

        // Setup the number of slots available.
        textureRemap.initTextureSlotsAvailable(Math.max(textureRemap.getTextureIds().size(), ((reader.getIndex() - textureRemap.getReaderIndex()) / Constants.SHORT_SIZE)));

        // Run hook (Allows adding new remaps after this one, but NOT before)
        onRemapRead(textureRemap, reader);

        // Check there aren't any gaps in data.
        nextTextureRemap = this.textureRemaps.size() > index + 1 ? this.textureRemaps.get(index + 1) : null;
        int extraBytes = nextTextureRemap != null ? nextTextureRemap.getReaderIndex() - reader.getIndex() : 0;
        if (extraBytes != 0)
            getLogger().warning( "%s has %d unread bytes between it and %s.", textureRemap, extraBytes, nextTextureRemap);

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
        int index = Collections.binarySearch(this.textureRemaps, remap, Comparator.comparingLong(TextureRemapArray::getLoadAddress));
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
            LinkedTextureRemap<?> oldLinkedRemap = this.linkedTextureMaps.put(linkedRemap.getResourceEntry(), linkedRemap);
            if (oldLinkedRemap != null)
                getLogger().warning("A remap (%s) that was previously linked to '%s' has been overwritten by %s.", oldLinkedRemap, linkedRemap.getResourceEntry().getDisplayName(), linkedRemap);
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
     * Populate the file groups for the main menu file list.
     * @param fileListView The file list view to register files for.
     */
    public abstract void setupFileGroups(SCGameFileGroupedListViewComponent<? extends SCGameInstance> fileListView);

    /**
     * Get the MWIResourceEntry for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntry
     */
    public MWIResourceEntry getResourceEntryByID(int resourceId) {
        if (this.archiveIndex == null)
            throw new RuntimeException("The MWI was not loaded, so we cannot yet search.");

        return this.archiveIndex.getResourceEntryByID(resourceId);
    }

    /**
     * Gets the resource entry from a given name.
     * @param name The name to lookup.
     * @return foundEntry, if any.
     */
    public MWIResourceEntry getResourceEntryByName(String name) {
        if (this.archiveIndex == null)
            throw new RuntimeException("The MWI was not loaded, so we cannot yet search.");

        return this.archiveIndex.getResourceEntryByName(name);
    }

    /**
     * Get the MWIResourceEntry name for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntryName
     */
    public String getResourceName(int resourceId) {
        MWIResourceEntry entry = this.archiveIndex.getResourceEntryByID(resourceId);
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
     * Gets a GameFile by its resource id.
     * @param resourceId The file's resource id.
     * @return gameFile
     */
    public <T extends SCGameFile<?>> T getGameFileByResourceID(int resourceId, Class<T> fileClass, boolean allowNull) {
        MWIResourceEntry resourceEntry = getResourceEntryByID(resourceId);
        if (resourceEntry == null) {
            if (allowNull)
                return null;

            throw new IllegalArgumentException("There was no file entry for resource ID: " + resourceId);
        }

        SCGameFile<?> gameFile = this.fileObjectsByFileEntries.get(resourceEntry);
        if (!fileClass.isInstance(gameFile)) {
            if (allowNull)
                return null;

            throw new ClassCastException("The file '" + resourceEntry.getDisplayName() + "'/" + resourceId + " was expected to be " + Utils.getSimpleName(fileClass) + ", but was actually " + Utils.getSimpleName(gameFile));
        }

        return fileClass.cast(gameFile);
    }

    /**
     * Gets a GameFile by its resource entry.
     * @param resourceEntry The file resource entry.
     * @return gameFile
     */
    @SuppressWarnings("unchecked")
    public <T extends SCGameFile<?>> T getGameFile(MWIResourceEntry resourceEntry) {
        return (T) this.fileObjectsByFileEntries.get(resourceEntry);
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
     * @param pointer The pointer get the image for.
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
        if (isPC()) {
            // Beast Wars PC has not had its frame-rate tested.
            return 25; // Frogger's PC release is locked to 25 FPS, regardless of EU/NA.
        } else if (isPSX()) {
            switch (getVersionConfig().getRegion()) {
                case UNSPECIFIED: // By default, they seemed to make NTSC builds (prototypes).
                case USA:
                case JAPAN:
                    return 30; // 30 FPS.
                case EUROPE:
                    return 25;
                default:
                    throw new RuntimeException("Don't know what the FPS should be for the " + getVersionConfig().getRegion() + " region.");
            }
        } else {
            throw new RuntimeException("Don't know what the frame-rate for this game is.");
        }
    }

    /**
     * Tests if the game currently being read is Frogger.
     */
    public boolean isOldFrogger() {
        return getGameType() == SCGameType.OLD_FROGGER;
    }

    /**
     * Tests if the game currently being read is Frogger.
     */
    public boolean isFrogger() {
        return getGameType() == SCGameType.FROGGER;
    }

    /**
     * Tests if the game currently being read is Beast Wars.
     */
    public boolean isBeastWars() {
        return getGameType() == SCGameType.BEAST_WARS;
    }

    /**
     * Tests if the game currently being read is MediEvil.
     */
    public boolean isMediEvil() {
        return getGameType() == SCGameType.MEDIEVIL;
    }

    /**
     * Tests if the game currently being read is MediEvil2.
     */
    public boolean isMediEvil2() {
        return getGameType() == SCGameType.MEDIEVIL2;
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
        if (this.getVersionConfig().getOverrideRamOffset() != 0) {
            this.ramOffset = this.getVersionConfig().getOverrideRamOffset();
        } else if (isPSX()) {
            reader.jumpTemp(0);
            reader.verifyString("PS-X EXE"); // Ensure it's a PSX executable.
            reader.skipBytes(16);
            this.ramOffset = reader.readUnsignedIntAsLong() - 0x800; // 0x800 is a CD sector. It's also the distance between the exe header and the start of the executable data put in memory.
            reader.jumpReturn();
        } else {
            throw new RuntimeException("Failed to load ramOffset for '" + this.getVersionConfig().getInternalName() + "', it may need to be added to the configuration.");
        }
    }

    private void readOverlayTable(DataReader reader) {
        if (this.getVersionConfig().getOverlayTableOffset() <= 0 || !isPSX())
            return;

        reader.jumpTemp((int) this.getVersionConfig().getOverlayTableOffset());
        this.overlayTable.load(reader);
        reader.jumpReturn();
        getLogger().info("Read %d overlay entries from the table.", this.overlayTable.getEntries().size());
    }

    // Beyond here are functions for handling game data from game files, and potentially also configuration data.
    private void readBmpPointerData(DataReader reader) {
        this.bmpTexturePointers.clear();
        if (this.getVersionConfig().getBmpPointerAddress() <= 0)
            return; // Not specified.

        reader.setIndex(this.getVersionConfig().getBmpPointerAddress());

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
            getLogger().warning("We found pointers to %d textures, but only the highest texture ID we saw suggests there should have been %d.", this.bmpTexturePointers.size(), highestTextureId + 1);
    }

    private void writeBmpPointerData(DataWriter exeWriter) {
        if (this.getVersionConfig().getBmpPointerAddress() <= 0 || this.bmpTexturePointers.isEmpty())
            return; // Not specified.

        exeWriter.setIndex(this.getVersionConfig().getBmpPointerAddress());
        this.bmpTexturePointers.forEach(exeWriter::writeUnsignedInt);
    }

    /**
     * Read the MWI file from the executable.
     */
    public MillenniumWadIndex readMWI() {
        if (this.getVersionConfig().getMWIOffset() <= 0)
            throw new RuntimeException("The MWI cannot be read because either no MWI offset was specified or the configuration hasn't been loaded yet.");

        // Read MWI bytes.
        DataReader reader = getExecutableReader();
        reader.setIndex(this.getVersionConfig().getMWIOffset());
        byte[] mwiBytes = reader.readBytes(this.getVersionConfig().getMWILength());

        // Load an MWI file.
        DataReader arrayReader = new DataReader(new ArraySource(mwiBytes));
        MillenniumWadIndex wadIndex = new MillenniumWadIndex(this);
        this.archiveIndex = wadIndex;
        wadIndex.load(arrayReader);
        this.onMWILoad(wadIndex);
        return wadIndex;
    }

    /**
     * Write the MWI to the provided writer.
     * @param writer  The writer to write the MWI to.
     * @param wadIndex The MWI file to save.
     */
    public void writeMWI(DataWriter writer, MillenniumWadIndex wadIndex) {
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter mwiWriter = new DataWriter(receiver);
        wadIndex.save(mwiWriter);
        mwiWriter.closeReceiver();

        // Verify MWI size ok.
        int bytesWritten = mwiWriter.getIndex();
        Utils.verify(bytesWritten == this.getVersionConfig().getMWILength(), "Saving the MWI failed. The size of the written MWI does not match the correct MWI size! [%d/%d]", bytesWritten, this.getVersionConfig().getMWILength());

        // Write MWI to the provided writer.
        writer.setIndex(this.getVersionConfig().getMWIOffset());
        writer.writeBytes(receiver.toArray());
    }

    /**
     * Read the MWD file.
     * @param progressBar the progress bar to display load progress on, if it exists
     */
    public MWDFile readMWD(ProgressBarComponent progressBar) {
        if (this.getVersionConfig().getMWIOffset() <= 0)
            throw new RuntimeException("The MWI cannot be read because either no MWI offset was specified or the configuration hasn't been loaded yet.");

        MWDFile mwdFile = new MWDFile(this);
        this.mainArchive = mwdFile;

        // Read the MWD.
        if (this.getVersionConfig().isMwdLooseFiles()) {
            mwdFile.loadFilesFromDirectory(progressBar);
        } else {
            FileSource fileSource;

            try {
                fileSource = new FileSource(this.mwdFile);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to read MWD file '" + this.mwdFile + "'.");
            }

            // Load the MWD file.
            mwdFile.loadMwdFile(new DataReader(fileSource), progressBar);
        }

        this.onMWDLoad(mwdFile);
        return mwdFile;
    }

    private byte[] writeConfigToExecutable(byte[] executableBytes) {
        net.highwayfrogs.editor.system.Config rootConfig = new net.highwayfrogs.editor.system.Config(null);
        rootConfig.addChildConfig(new GameBuildInfo<>(this).toConfig());
        return FileUtils.saveConfigDataToExecutable(this, executableBytes, rootConfig);
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
        data = writeConfigToExecutable(data);

        // Write file.
        FileUtils.deleteFile(outputFile);
        Files.write(outputFile.toPath(), data);
    }

    /**
     * Write potentially modified data from the instance object to the executable.
     * @param wadIndex The mwi file to write.
     */
    public void writeExecutableData(MillenniumWadIndex wadIndex) {
        DataWriter writer = createExecutableWriter();
        try {
            this.writeExecutableData(writer, wadIndex);
        } catch (Throwable th) {
            throw new RuntimeException("Failed to write instance data to the executable.", th);
        }

        writer.closeReceiver();
    }

    /**
     * Write potentially modified data from the instance object to the executable.
     * @param writer  The writer to write the data to.
     * @param wadIndex The mwi file to write.
     */
    public void writeExecutableData(DataWriter writer, MillenniumWadIndex wadIndex) {
        if (wadIndex != null)
            this.writeMWI(writer, wadIndex);

        this.writeBmpPointerData(writer);
        this.writeTextureRemaps(writer);
    }
}