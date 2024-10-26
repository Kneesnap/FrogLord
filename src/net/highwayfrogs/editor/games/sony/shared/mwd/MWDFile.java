package net.highwayfrogs.editor.games.sony.shared.mwd;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.packers.PP20Unpacker;
import net.highwayfrogs.editor.file.packers.PP20Unpacker.UnpackResult;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Millennium WAD File Format
 * Shared between all Millennium games, and contains pretty much all non-sound non-code game assets in their games.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class MWDFile extends SCSharedGameData {
    private String buildNotes;
    private final List<SCGameFile<?>> files = new ArrayList<>();

    private final transient Map<FroggerMapTheme, VLOArchive> vloThemeCache = new HashMap<>();

    private static final String MARKER = "DAWM";
    private static final int BUILD_NOTES_SIZE = 2040;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, d MMMM yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final ImageFilterSettings VLO_ICON_SETTING = new ImageFilterSettings(ImageState.EXPORT);

    public MWDFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        loadMwdFile(reader, null);
    }

    /**
     * Loads the MWD file with a progress bar to show progress.
     * @param reader the reader to read from
     * @param progressBar the progress bar to update, if exists
     */
    public void loadMwdFile(DataReader reader, ProgressBarComponent progressBar) {
        List<MWIResourceEntry> mwiEntries = getGameInstance().getArchiveIndex().getEntries();
        if (progressBar != null)
            progressBar.setTotalProgress(mwiEntries.size());

        // Read header.
        reader.verifyString(MARKER);
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE);
        this.buildNotes = reader.readNullTerminatedFixedSizeString(BUILD_NOTES_SIZE);
        getGameInstance().getLogger().info("Build Notes: \n" + this.buildNotes + (this.buildNotes.endsWith("\n") ? "" : "\n"));

        boolean lastFileLoadSuccess = false;
        for (MWIResourceEntry entry : mwiEntries) {
            if (entry.testFlag(MWIResourceEntry.FLAG_GROUP_ACCESS)) {
                if (progressBar != null)
                    progressBar.addCompletedProgress(1);
                continue; // This file is part of a WAD archive, and isn't a file entry in the MWD, so we can't load it here.
            }

            // Validate position.
            if (lastFileLoadSuccess) {
                reader.requireIndex(getLogger(), entry.getArchiveOffset(), "Expected file contents for '" + entry.getDisplayName() + "'");
            } else {
                reader.setIndex(entry.getArchiveOffset());
            }

            // Read next file.
            byte[] fileBytes = reader.readBytes(entry.getArchiveSize());
            lastFileLoadSuccess = loadNextFile(fileBytes, entry, progressBar);
            reader.align(Constants.CD_SECTOR_SIZE);
        }
    }

    /**
     * Loads the file entries from a local folder with a progress bar to show progress.
     * Seen in the Frogger PC Milestone 3 build.
     * @param progressBar the progress bar to update, if exists
     */
    public void loadFilesFromDirectory(ProgressBarComponent progressBar) {
        List<MWIResourceEntry> mwiEntries = getGameInstance().getArchiveIndex().getEntries();
        if (progressBar != null)
            progressBar.setTotalProgress(mwiEntries.size());

        this.buildNotes = "";
        for (MWIResourceEntry entry : mwiEntries) {
            if (entry.testFlag(MWIResourceEntry.FLAG_GROUP_ACCESS)) {
                if (progressBar != null)
                    progressBar.addCompletedProgress(1);
                continue; // This file is part of a WAD archive, and isn't a file entry in the MWD, so we can't load it here.
            }

            if (!entry.hasFullFilePath() || entry.getFullFilePath().isEmpty()) {
                getLogger().warning("When loading files by file name, the MWIResourceEntry with resource ID " + entry.getResourceId() + " did not have a file path!");
                if (progressBar != null)
                    progressBar.addCompletedProgress(1);
                continue; // This file is part of a WAD archive, and isn't a file entry in the MWD, so we can't load it here.
            }

            if (entry.getArchiveOffset() != 0)
                getLogger().warning("Expected archiveOffset to be zero for entry '" + entry.getDisplayName() + "', but it was: " + entry.getSectorOffset() + "/" + NumberUtils.toHexString(entry.getArchiveOffset()) + ".");

            // NOTE: The file names are fully upper-case in the MWI, which may not match the correct file casing.
            // So, on case-sensitive file-systems (eg: Linux), this will fail to find the files. Not sure what we can do about that.
            // Perhaps we can make a function to handle it.
            String localFilePath = FileUtils.ensureValidPathSeparator(entry.getFullFilePath(), true);
            File localFile = new File(getGameInstance().getMainGameFolder(), localFilePath);
            if (!localFile.exists() || !localFile.isFile()) {
                getLogger().severe("Could not find the file '" + localFilePath + "'. If you are on Linux");
                getLogger().severe("If you are using Linux and have the file, rename the folders & files to match the exact case (test vs TEST).");
                if (progressBar != null)
                    progressBar.addCompletedProgress(1);

                continue; // Couldn't find the file.
            }

            // Read file contents.
            byte[] fileBytes;
            try {
                fileBytes = Files.readAllBytes(localFile.toPath());
            } catch (IOException ex) {
                Utils.handleError(getLogger(), ex, true, "Failed to read file '%s'.", localFilePath);
                if (progressBar != null)
                    progressBar.addCompletedProgress(1);
                continue; // Couldn't find the file.
            }

            loadNextFile(fileBytes, entry, progressBar);
        }
    }

    /**
     * Loads the next file corresponding to the MWI resource.
     * @param fileBytes the bytes of the file to load. May be compressed.
     * @param mwiEntry the MWI entry corresponding to the file
     * @param progressBar the progress bar to update, if there is one.
     */
    private boolean loadNextFile(byte[] fileBytes, MWIResourceEntry mwiEntry, ProgressBarComponent progressBar) {
        if (progressBar != null)
            progressBar.setStatusMessage("Reading '" + mwiEntry.getDisplayName() + "'");

        // Read the file. Decompress if it is PP20 compression.
        int safetyMarginWordCount = 0;
        byte[] compressedBytes = null;
        if (mwiEntry.isCompressed()) {
            if (PP20Unpacker.isCompressed(fileBytes)) {
                compressedBytes = fileBytes;
                UnpackResult unpackResult = PP20Unpacker.unpackData(fileBytes);
                fileBytes = unpackResult.getUnpackedBytes();
                safetyMarginWordCount = unpackResult.getSafetyMarginWordCount();
            } else {
                getLogger().severe("ERROR: File is marked as compressed, but does not appear to be using PowerPacker (PP20) compression.");
            }
        }

        if (mwiEntry.getUnpackedSize() != fileBytes.length)
            getLogger().severe("ERROR: File is marked as being " + mwiEntry.getUnpackedSize() + " bytes large, but is actually " + fileBytes.length + " bytes large.");

        mwiEntry.onLoadData(fileBytes, compressedBytes, safetyMarginWordCount);
        SCGameFile<?> file = loadFile(fileBytes, mwiEntry);
        this.files.add(file);

        boolean success = true;
        try {
            DataReader singleFileReader = new DataReader(new ArraySource(fileBytes));
            file.load(singleFileReader);
            if (singleFileReader.hasMore() && file.warnIfEndNotReached()) // Warn if the full file is not read.
                file.getLogger().warning("File contents were read to index " + NumberUtils.toHexString(singleFileReader.getIndex()) + ", leaving " + singleFileReader.getRemaining() + " bytes unread. (Length: " + NumberUtils.toHexString(fileBytes.length) + ")");
        } catch (Exception ex) {
            success = false;
            Utils.handleError(getLogger(), ex, false, "Failed to load %s (%d)", mwiEntry.getDisplayName(), mwiEntry.getResourceId());
        }

        if (progressBar != null)
            progressBar.addCompletedProgress(1);

        return success;
    }

    /**
     * Create a replacement file. (Does not actually update MWD)
     * @param fileBytes The bytes to replace the file with.
     * @param oldFile   The file to replace.
     * @return replacementFile
     */
    @SuppressWarnings("unchecked")
    public <T extends SCGameFile<?>> T replaceFile(byte[] fileBytes, MWIResourceEntry entry, SCGameFile<?> oldFile, boolean isInsideWadFile) {
        T newFile;

        if (oldFile instanceof MOFHolder) {
            MOFHolder oldHolder = (MOFHolder) oldFile;
            newFile = (T) new MOFHolder(getGameInstance(), oldHolder.getTheme(), oldHolder.getCompleteMOF());
        } else {
            newFile = this.loadFile(fileBytes, entry);
        }

        if (oldFile != null) {
            getGameInstance().getFileObjectsByFileEntries().remove(entry, oldFile);
            oldFile.setFileDefinition(null);
        }

        getGameInstance().getFileObjectsByFileEntries().put(entry, newFile);
        newFile.setFileDefinition(entry);

        // Replace file.
        if (!isInsideWadFile) {
            int fileIndex = this.files.indexOf(oldFile);
            if (fileIndex >= 0)
                this.files.set(fileIndex, newFile);
        }

        // Load new file data.
        try {
            newFile.load(new DataReader(new ArraySource(fileBytes)));
        } catch (Exception ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to load %s (%d)", entry.getDisplayName(), entry.getResourceId());
        }

        return newFile;
    }

    /**
     * Create a GameFile instance.
     * @param fileBytes The data to read
     * @param entry     The file entry being loaded.
     * @return loadedFile
     */
    @SuppressWarnings("unchecked")
    public <T extends SCGameFile<?>> T loadFile(byte[] fileBytes, MWIResourceEntry entry) {
        // Turn the byte data into the appropriate game-file.
        SCGameFile<?> file = getGameInstance().createFile(entry, fileBytes);
        if (file == null)
            file = new DummyFile(getGameInstance(), fileBytes.length);

        getGameInstance().getFileObjectsByFileEntries().put(entry, file);
        file.setFileDefinition(entry);
        file.setRawFileData(fileBytes);
        return (T) file;
    }

    @Override
    public void save(DataWriter writer) {
        this.save(writer, null);
    }

    /**
     * Saves the file with a progress bar to show progress.
     * @param writer the writer to write to
     * @param progressBar the progress bar to update, if exists
     */
    public void save(DataWriter writer, ProgressBarComponent progressBar) {
        if (progressBar != null)
            progressBar.setTotalProgress(this.files.size());

        writer.writeBytes(MARKER.getBytes());
        writer.writeInt(0);

        Date date = Date.from(Calendar.getInstance().toInstant());
        writer.writeTerminatorString("\nCreated by FrogLord"
                + "\nCreation Date: " + DATE_FORMAT.format(date)
                + "\nCreation Time: " + TIME_FORMAT.format(date)
                + "\n[BuildInfo]"
                + "\ngame=" + getGameInstance().getGameType().getIdentifier()
                + "\ngameVersion=" + getGameInstance().getVersionConfig().getInternalName()
                + "\nversion=1"
                + "\n");
        writer.align(Constants.CD_SECTOR_SIZE);

        long mwdStart = System.currentTimeMillis();
        for (SCGameFile<?> file : this.files) {
            if ((writer.getIndex() % Constants.CD_SECTOR_SIZE) != 0)
                throw new RuntimeException("Writer index (" + NumberUtils.toHexString(writer.getIndex()) + ") was not aligned to CD sector size!");

            MWIResourceEntry entry = file.getIndexEntry();
            int currentSector = writer.getIndex() / Constants.CD_SECTOR_SIZE;
            entry.setSectorOffset(currentSector);

            file.saveFile(writer, progressBar);
        }
        getLogger().info("MWD Built. Total Time: " + (System.currentTimeMillis() - mwdStart) + " ms.");

        // Fill the rest of the file with null bytes.
        writer.align(Constants.CD_SECTOR_SIZE);
    }

    /**
     * Grabs the first VLO we can find.
     */
    public VLOArchive findFirstVLO() {
        List<VLOArchive> allVLOs = getFiles().stream()
                .filter(VLOArchive.class::isInstance)
                .map(VLOArchive.class::cast)
                .collect(Collectors.toList());

        return allVLOs.size() > 0 ? allVLOs.get(0) : null;
    }

    /**
     * Get the VLO for a given map theme.
     * @param theme     The theme to get it for. Can be null, will prompt user then.
     * @param handler   The handler for when the VLO is determined.
     * @param allowNull Are null VLOs allowed?
     */
    public void promptVLOSelection(FroggerMapTheme theme, Consumer<VLOArchive> handler, boolean allowNull) {
        List<VLOArchive> allVLOs = getAllFiles(VLOArchive.class);

        if (allowNull)
            allVLOs.add(0, null);

        if (theme != null) {
            List<VLOArchive> movedVLOs = allVLOs.stream()
                    .filter(vlo -> {
                        MWIResourceEntry entry = vlo != null ? vlo.getIndexEntry() : null;
                        return entry != null && entry.getDisplayName().startsWith(theme.getInternalName());
                    })
                    .collect(Collectors.toList());
            allVLOs.removeAll(movedVLOs);
            allVLOs.addAll(0, movedVLOs);
        }

        VLOArchive cachedVLO = this.vloThemeCache.get(theme); // Move cached vlo to the top.
        if (cachedVLO != null && allVLOs.remove(cachedVLO))
            allVLOs.add(0, cachedVLO);

        SelectionMenu.promptSelection(getGameInstance(), "Select " + (theme != null ? theme.name() + "'s" : "a") + " VLO.", vlo -> {
                    if (vlo != null && theme != null)
                        this.vloThemeCache.put(theme, vlo);
                    handler.accept(vlo);
                }, allVLOs,
                vlo -> vlo != null ? vlo.getFileDisplayName() : "No Textures",
                vlo -> vlo.getImages().get(0).toFXImage(VLO_ICON_SETTING));
    }

    /**
     * Get each file of a given class type, including those found in wads.
     * @param fileClass The type to iterate over.
     */
    public <T extends SCGameFile<?>> List<T> getAllFiles(Class<T> fileClass) {
        List<T> results = new ArrayList<>();

        for (SCGameFile<?> file : getFiles()) {
            if (fileClass.isInstance(file))
                results.add(fileClass.cast(file));

            if (file instanceof WADFile) {
                WADFile wadFile = (WADFile) file;
                for (WADEntry entry : wadFile.getFiles()) {
                    SCGameFile<?> testFile = entry.getFile();
                    if (fileClass.isInstance(testFile))
                        results.add(fileClass.cast(testFile));
                }
            }
        }

        return results;
    }

    /**
     * Iterate over each file of a given type.
     * @param fileClass The type to iterate over.
     * @param handler   The behavior to apply.
     */
    public <T extends SCGameFile<?>> void forEachFile(Class<T> fileClass, Consumer<T> handler) {
        for (SCGameFile<?> file : getFiles()) {
            if (fileClass.isInstance(file))
                handler.accept(fileClass.cast(file));

            if (file instanceof WADFile) {
                WADFile wadFile = (WADFile) file;
                for (WADEntry entry : wadFile.getFiles()) {
                    SCGameFile<?> testFile = entry.getFile();
                    if (fileClass.isInstance(testFile))
                        handler.accept(fileClass.cast(testFile));
                }
            }
        }
    }

    /**
     * Iterate over each file of a given type.
     * @param fileClass The type to iterate over.
     * @param handler   The behavior to apply.
     */
    public <T extends SCGameFile<?>, R> R resolveForEachFile(Class<T> fileClass, Function<T, R> handler) {
        for (SCGameFile<?> file : getFiles()) {
            if (fileClass.isInstance(file)) {
                R result = handler.apply(fileClass.cast(file));
                if (result != null)
                    return result; // If there's a result.
            }

            if (file instanceof WADFile) {
                WADFile wadFile = (WADFile) file;
                for (WADEntry wadEntry : wadFile.getFiles()) {
                    if (!fileClass.isInstance(wadEntry.getFile()))
                        continue;
                    R result = handler.apply(fileClass.cast(wadEntry.getFile()));
                    if (result != null)
                        return result;
                }
            }
        }

        return null; // Nothing found.
    }

    /**
     * Gets a game file by the given file name.
     * @param fileName The name of the file to lookup.
     * @return foundFile
     */
    @SuppressWarnings("unchecked")
    public <TGameFile extends SCGameFile<? extends SCGameInstance>> TGameFile getFileByName(String fileName) {
        for (SCGameFile<?> gameFile : getFiles()) {
            if (matchesFileName(gameFile.getIndexEntry(), fileName))
                return (TGameFile) gameFile;

            if (gameFile instanceof WADFile)
                for (WADEntry wadFileEntry : ((WADFile) gameFile).getFiles())
                    if (matchesFileName(wadFileEntry.getFileEntry(), fileName))
                        return (TGameFile) wadFileEntry.getFile();
        }

        return null;
    }

    private static boolean matchesFileName(MWIResourceEntry resourceEntry, String fileName) {
        if (resourceEntry == null)
            return false;

        String fileDisplayName = resourceEntry.getDisplayName();
        if (fileDisplayName != null && fileDisplayName.equalsIgnoreCase(fileName))
            return true;

        String fileNameWithoutExtension = fileDisplayName != null ? FileUtils.stripExtension(fileDisplayName) : null;
        if (fileNameWithoutExtension != null && fileNameWithoutExtension.equalsIgnoreCase(fileName))
            return true;

        String fullFilePath = resourceEntry.getFullFilePath();
        return fullFilePath != null && fullFilePath.equalsIgnoreCase(fileName);
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public GameImage getImageByTextureId(int textureId) {
        if (textureId < 0)
            textureId = 0; // This is a hack to allow for loading maps without remaps on build 20. In new FrogLord, this should be null / return blank texture.

        for (VLOArchive vlo : getAllFiles(VLOArchive.class))
            for (GameImage testImage : vlo.getImages())
                if (testImage.getTextureId() == textureId)
                    return testImage;

        return null;
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public List<GameImage> getImagesByTextureId(int textureId) {
        List<GameImage> results = new ArrayList<>();

        for (VLOArchive vlo : getAllFiles(VLOArchive.class))
            for (GameImage testImage : vlo.getImages())
                if (testImage.getTextureId() == textureId)
                    results.add(testImage);

        return results;
    }
}