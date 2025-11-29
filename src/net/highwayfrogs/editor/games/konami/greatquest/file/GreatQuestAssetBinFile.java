package net.highwayfrogs.editor.games.konami.greatquest.file;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestModData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcFvFUtil;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Parses FTGQ's main game data file. It's called "data.bin" in all builds we've seen so far.
 * .SBR files contain sound effects. the SCK file contains only PCM data, with headers in the .IDX file.
 * .PSS (PS2) are video files. Can be opened with VLC.
 * BUFFER.DAT files (PS2) are video files, and can be opened with VLC.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class GreatQuestAssetBinFile extends GameData<GreatQuestInstance> {
    private final List<String> globalPaths = new ArrayList<>();
    private final List<GreatQuestArchiveFile> files = new ArrayList<>();
    private final Map<Integer, List<GreatQuestArchiveFile>> filesByHash = new HashMap<>();

    private static final Comparator<GreatQuestAssetBinFileHeader> HEADER_FILE_ORDERING =
            Comparator.comparingInt(GreatQuestAssetBinFileHeader::getOffset)
                    .thenComparing(value -> {
                        throw new UnsupportedOperationException("GreatQuestAssetBinFileHeader.getOffset() should never be duplicated.");
                    });

    private static final Comparator<GreatQuestArchiveFile> FILE_ORDERING =
            Comparator.comparingInt((GreatQuestArchiveFile file) -> file.getFileType().ordinal()) // #1) File Type
                    .thenComparingInt(GreatQuestAssetBinFile::getImageSortingValue) // #2a) Image Type
                    .thenComparingInt(GreatQuestAssetBinFile::getModelSortingValue) // #2b) Model has compressed vertices?
                    .thenComparing(GreatQuestAssetBinFile::getFilePathOrNeighborFilePath); // #3) File path

    public static final int NAME_SIZE = 0x108;

    public GreatQuestAssetBinFile(GreatQuestInstance gameInstance) {
        super(gameInstance);
    }

    @Override
    public void load(DataReader reader) {
        this.load(reader, null);
    }

    /**
     * Loads the file with a progress bar to show progress.
     * @param reader the reader to read from
     * @param progressBar the progress bar to update, if exists
     */
    public void load(DataReader reader, ProgressBarComponent progressBar) {
        // Read the mod data header, if it exists.
        reader.jumpTemp(reader.getSize() - Constants.INTEGER_SIZE - GreatQuestModData.SIGNATURE.length());
        byte[] signature = reader.readBytes(GreatQuestModData.SIGNATURE.length());
        int frogLordHeaderStartAddress = reader.readInt();
        reader.jumpReturn();
        int frogLordDataSize;
        if (DataUtils.testSignature(signature, GreatQuestModData.SIGNATURE)) {
            reader.jumpTemp(frogLordHeaderStartAddress);
            getGameInstance().getModData().load(reader);
            frogLordDataSize = reader.getIndex() - frogLordHeaderStartAddress;
            reader.jumpReturn();
        } else {
            frogLordHeaderStartAddress = -1;
            frogLordDataSize = 0;
        }

        // Start reading from beginning of data.bin.
        int unnamedFiles = reader.readInt();
        int namedFiles = reader.readInt();
        int globalPathStartAddress = reader.readInt(); // Located after the files.

        Map<Integer, String> filePathMap = new HashMap<>();
        GreatQuestUtils.addHardcodedFilePathHashesToMap(filePathMap);
        filePathMap.putAll(getGameInstance().getModData().getUserGlobalFilePaths()); // Copy mod-supplied file paths, and overwrite any of the hardcoded paths.

        List<GreatQuestAssetBinFileHeader> fileHeaders = new ArrayList<>();

        // Prepare unnamed files.
        if (progressBar != null) {
            progressBar.setTotalProgress(unnamedFiles);
            progressBar.setStatusMessage("Reading File List");
        }

        for (int i = 0; i < unnamedFiles; i++) {
            int hash = reader.readInt();
            fileHeaders.add(readFileHeader(reader, filePathMap.get(hash), hash, false, progressBar));
        }

        // Prepare named files. Files are named if they have a collision with other files.
        if (progressBar != null)
            progressBar.setTotalProgress(namedFiles);

        String hostRootPath = getGameInstance().getVersionConfig().getHostRootPath();
        for (int i = 0; i < namedFiles; i++) {
            String fullFilePath = reader.readNullTerminatedFixedSizeString(NAME_SIZE, GreatQuestInstance.PADDING_BYTE_CD);
            if (!fullFilePath.startsWith(hostRootPath))
                throw new RuntimeException("The file had a path '" + fullFilePath + "', which did not start with the host root path of '" + hostRootPath + "'.");

            String filePath = fullFilePath.substring(hostRootPath.length());
            GreatQuestAssetBinFileHeader newHeader = readFileHeader(reader, filePath, GreatQuestUtils.hashFilePath(filePath), true, progressBar);

            // The named headers should be sorted into the existing fileHeader list, even though they are written separately.
            // This is the order the files are written originally, and it makes it nicer when browsing files
            int insertionIndex = Collections.binarySearch(fileHeaders, newHeader, HEADER_FILE_ORDERING);
            if (insertionIndex >= 0)
                throw new RuntimeException("Did not expect to find the header already in the fileHeaders list.");

            fileHeaders.add(-(insertionIndex + 1), newHeader);
        }

        // Prepare (create) files.
        if (progressBar != null)
            progressBar.setTotalProgress(fileHeaders.size());
        this.files.clear();
        this.filesByHash.clear();
        for (int i = 0; i < fileHeaders.size(); i++)
            fileHeaders.get(i).prepareFile(reader, progressBar);

        // Read global paths.
        requireReaderIndex(reader, globalPathStartAddress, "Expected global path table");
        int globalPathCount = reader.readInt();
        this.globalPaths.clear();
        for (int i = 0; i < globalPathCount; i++)
            this.globalPaths.add(reader.readNullTerminatedFixedSizeString(NAME_SIZE, Constants.NULL_BYTE));

        // Skip FrogLord mod data.
        if (frogLordHeaderStartAddress >= 0 && frogLordDataSize > 0) {
            requireReaderIndex(reader, frogLordHeaderStartAddress, "Expected FrogLord mod data");
            reader.skipBytes(frogLordDataSize);
        }

        // Process (load) files. (File loading occurs only after we have an object for every single game file, so that file hash references can be resolved regardless of file order.)
        if (progressBar != null)
            progressBar.setTotalProgress(this.files.size());

        for (int i = 0; i < this.files.size(); i++)
            loadFile(this.files.get(i), progressBar);

        // Test ordering. (Must occur AFTER file data is loaded for some of the tiebreakers to work)
        GreatQuestArchiveFile lastFile = null;
        for (int i = 0; i < this.files.size(); i++) {
            GreatQuestArchiveFile currentFile = this.files.get(i);
            if (lastFile != null && FILE_ORDERING.compare(currentFile, lastFile) < 0)
                getLogger().warning("File '%s' is out of order with the previously loaded file: '%s'!", currentFile.getDebugName(), lastFile.getDebugName());

            lastFile = currentFile;
        }

        // Handle post-load setup.
        kcLoadContext context = new kcLoadContext(this);
        if (progressBar != null)
            progressBar.update(0, this.files.size(), "After Load Hook 1");

        if (progressBar != null)
            progressBar.setTotalProgress(this.files.size());
        for (int i = 0; i < this.files.size(); i++) {
            this.files.get(i).afterLoad1(context);
            if (progressBar != null)
                progressBar.addCompletedProgress(1);
        }

        // Second load hook.
        if (progressBar != null)
            progressBar.update(0, this.files.size(), "After Load Hook 2");

        for (int i = 0; i < this.files.size(); i++) {
            this.files.get(i).afterLoad2(context);
            if (progressBar != null)
                progressBar.addCompletedProgress(1);
        }

        context.onComplete();
    }

    private GreatQuestAssetBinFileHeader readFileHeader(DataReader reader, String filePath, int nameHash, boolean hasCollision, ProgressBarComponent progressBar) {
        GreatQuestAssetBinFileHeader header = new GreatQuestAssetBinFileHeader(this, filePath, nameHash, hasCollision);
        header.load(reader);
        if (progressBar != null)
            progressBar.addCompletedProgress(1);

        return header;
    }

    private void loadFile(GreatQuestArchiveFile file, ProgressBarComponent progressBar) {
        if (progressBar != null)
            progressBar.setStatusMessage("Reading '" + file.getExportName() + "'");

        try {
            file.loadFileFromBytes(file.getRawData());
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false);
        }

        if (progressBar != null)
            progressBar.addCompletedProgress(1);
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
        List<GreatQuestArchiveFile> unnamedFiles = new ArrayList<>();
        List<GreatQuestArchiveFile> namedFiles = new ArrayList<>();

        // Split files into named/unnamed files.
        GreatQuestArchiveFile lastFile  = null;
        for (GreatQuestArchiveFile currentFile : getFiles()) {
            if (lastFile != null && FILE_ORDERING.compare(currentFile, lastFile) < 0)
                getLogger().warning("File '%s' is out of order with the previous file: '%s'!", currentFile.getDebugName(), lastFile.getDebugName());

            List<GreatQuestArchiveFile> targetList = currentFile.hasCollision() ? namedFiles : unnamedFiles;
            targetList.add(currentFile);
            lastFile = currentFile;
        }

        // Start writing file.
        writer.writeInt(unnamedFiles.size());
        writer.writeInt(namedFiles.size());
        int globalPathTablePtr = writer.writeNullPointer();

        int fileHeaderStartIndex = writer.getIndex();
        Map<GreatQuestArchiveFile, GreatQuestAssetBinFileHeader> headersByFile = new HashMap<>();
        List<GreatQuestAssetBinFileHeader> fileHeaders = new ArrayList<>();

        // Non-colliding file-headers are written first:
        for (GreatQuestArchiveFile file : unnamedFiles) {
            writer.writeInt(file.getHash());

            // Add header which will be updated & written again later.
            GreatQuestAssetBinFileHeader newHeader = new GreatQuestAssetBinFileHeader(this, file.getFilePath(), file.getHash(), false);
            fileHeaders.add(newHeader);
            headersByFile.put(file, newHeader);
            newHeader.save(writer);
        }

        // Collision file headers are written second:
        for (GreatQuestArchiveFile file : namedFiles) {
            String fullFilePath = getGameInstance().getVersionConfig().getHostRootPath() + file.getFilePath();
            writer.writeNullTerminatedFixedSizeString(fullFilePath, NAME_SIZE, GreatQuestInstance.PADDING_BYTE_CD);

            // Add header which will be updated & written again later.
            GreatQuestAssetBinFileHeader newHeader = new GreatQuestAssetBinFileHeader(this, file.getFilePath(), file.getHash(), true);
            fileHeaders.add(newHeader);
            headersByFile.put(file, newHeader);
            newHeader.save(writer);
        }

        // Write files:
        if (progressBar != null)
            progressBar.setTotalProgress(getFiles().size());
        for (GreatQuestArchiveFile file : getFiles()) {
            if (progressBar != null)
                progressBar.setStatusMessage("Saving '" + file.getExportName() + "'");

            GreatQuestAssetBinFileHeader fileHeader = headersByFile.get(file);
            fileHeader.offset = writer.getIndex();

            // Write the file contents.
            ArrayReceiver receiver = new ArrayReceiver();
            file.save(new DataWriter(receiver));
            byte[] fileBytes = receiver.toArray();

            // Write size.
            fileHeader.size = fileBytes.length;
            fileHeader.compressedSize = 0;

            // File is compressed.
            if (file.isCompressed()) {
                fileBytes = GreatQuestUtils.zlibCompress(fileBytes); // Compress data.
                fileHeader.compressedSize = fileBytes.length;
            }

            writer.writeBytes(fileBytes);
            if (progressBar != null)
                progressBar.addCompletedProgress(1);
        }

        // After file data, write global strings.
        writer.writeAddressTo(globalPathTablePtr);
        writer.writeInt(this.globalPaths.size());
        for (String globalPath : this.globalPaths)
            writer.writeNullTerminatedFixedSizeString(globalPath, NAME_SIZE);

        // Write mod data.
        getGameInstance().getModData().save(writer);

        // Now that files have been written, let's go back to write the updated headers.
        // This is done separately in order to reduce the overall number of jumps between positions in the file to maximize write speed.
        writer.setIndex(fileHeaderStartIndex);
        for (GreatQuestAssetBinFileHeader fileHeader : fileHeaders) {
            if (fileHeader.isHasCollision()) {
                writer.skipBytes(NAME_SIZE);
            } else {
                writer.skipBytes(Constants.INTEGER_SIZE);
            }

            fileHeader.save(writer);
        }
    }

    /**
     * Returns true iff the file is currently registered in the .bin file.
     */
    public boolean isRegistered(GreatQuestArchiveFile file) {
        if (file == null)
            throw new NullPointerException("file");
        if (file.getGameInstance() != getGameInstance())
            return false;

        if (this.filesByHash.get(file.getHash()) == file)
            return true;

        List<GreatQuestArchiveFile> files = this.filesByHash.get(file.getHash());
        return files != null && files.contains(file);
    }

    /**
     * Returns true iff the file is currently registered in the .bin file and its hash collides with another file.
     */
    public boolean hasCollision(GreatQuestArchiveFile file) {
        if (file == null)
            throw new NullPointerException("file");
        if (file.getGameInstance() != getGameInstance())
            return false;
        if (!file.hasFilePath())
            return false;

        List<GreatQuestArchiveFile> files = this.filesByHash.get(file.getHash());
        return files != null && (files.size() > 1 || (!files.isEmpty() && !files.contains(file)));
    }

    /**
     * Adds a file to the .bin file.
     * Note that {@code file.init} must be called on the file before calling this function.
     * @param file the file to add
     * @return true iff the file was not previously registered, and has now been successfully registered.
     */
    public boolean addFile(GreatQuestArchiveFile file) {
        if (file == null)
            throw new NullPointerException("file");
        if (file.getGameInstance() != getGameInstance())
            throw new IllegalArgumentException("The provided file is registered to a different game instance!");
        if (file.isRegistered())
            return false;

        // Original files with unknown file paths break our ability to binary search.
        // Given at the time of writing, we have valid looking file paths for all known versions of the game, I see no reason why we wouldn't be able to find any new ones too.
        if (StringUtils.isNullOrWhiteSpace(file.getFilePath()) || !file.getFilePath().contains(".") || !file.getFilePath().contains("\\"))
            throw new IllegalArgumentException("Cannot register file, its file path is invalid. (" + file.getFilePath() + ")");

        int searchIndex = Collections.binarySearch(this.files, file, FILE_ORDERING);
        int insertionIndex = -(searchIndex + 1);
        if (searchIndex >= 0) {
            GreatQuestArchiveFile oldFile = this.files.get(searchIndex);
            if (file == oldFile) {
                throw new IllegalStateException("Internal state error! this.files contained the file, but the file did not report as registered!");
            } else if (file.getFilePath().equalsIgnoreCase(oldFile.getFilePath())) {
                throw new IllegalArgumentException("Another file with the path '" + file.getFilePath() + "' is already registered!");
            } else {
                throw new IllegalArgumentException("Unknown problem adding '" + file.getFilePath() + "', which conflicts with '" + oldFile.getFilePath() + "'!");
            }
        }

        List<GreatQuestArchiveFile> files = this.filesByHash.computeIfAbsent(file.getHash(), key -> new ArrayList<>());
        if (files.contains(file))
            throw new IllegalStateException("Internal state error! this.filesByHash contained the file, but the file did not report as registered!");

        // Validate great quest files.
        int looseFiles = getGameInstance().getLooseFiles().size();
        if (looseFiles + this.files.size() != getGameInstance().getAllFiles().size()) {
            throw new IllegalStateException("Expected getGameInstance().getAllFiles() to be getGameInstance().getLooseFiles() + this.files, but that does not appear to be the case! ("
                    + getGameInstance().getAllFiles().size() + ", " + looseFiles + ", " + this.files.size() + ")");
        }

        // Register.
        this.files.add(insertionIndex, file);
        getGameInstance().getAllFiles().add(looseFiles + insertionIndex, file);
        files.add(file);

        // Just added the file path, so track it in the mod data.
        if (files.size() > 1) {
            // Files with collisions have their file paths stored in the .bin file, there's no reason to track it.
            getGameInstance().getModData().getUserGlobalFilePaths().remove(file.getHash());
        } else {
            // Track the file path in mod data if there's only one file with this hash.
            getGameInstance().getModData().getUserGlobalFilePaths().put(file.getHash(), file.getFilePath());
        }

        return true;
    }

    /**
     * Removes a file from the asset .bin file, if it is registered
     * @param file the file to remove
     * @return true iff the file was successfully removed
     */
    @SuppressWarnings("ExtractMethodRecommender")
    public boolean removeFile(GreatQuestArchiveFile file) {
        if (file == null)
            throw new NullPointerException("file");
        if (!file.isRegistered())
            return false;

        // Search for removal index.
        int removeIndex = file.hasFilePath() ? Collections.binarySearch(this.files, file, FILE_ORDERING) : -1;
        if (removeIndex < 0 || this.files.get(removeIndex) != file) // This is possible to happen if there are files without file paths.
            removeIndex = this.files.indexOf(file);

        if (removeIndex < 0)
            throw new IllegalStateException("File reports that it isRegistered(), but we couldn't find it in this.files. (" + file.getDebugName() + ")");

        // Validate game instance file tracking.
        int looseFiles = getGameInstance().getLooseFiles().size();
        if (looseFiles + this.files.size() != getGameInstance().getAllFiles().size()) {
            throw new IllegalStateException("Expected getGameInstance().getAllFiles() to be getGameInstance().getLooseFiles() + this.files, but that does not appear to be the case! ("
                    + getGameInstance().getAllFiles().size() + ", " + looseFiles + ", " + this.files.size() + ")");
        }

        // Start removing.
        List<GreatQuestArchiveFile> files = this.filesByHash.get(file.getHash());
        if (files == null || !files.remove(file))
            throw new IllegalStateException("Internal state error! this.filesByHash did not contain the file (" + file.getDebugName() + "), even though the file reported as registered!");

        int allFilesIndex = looseFiles + removeIndex;
        GreatQuestGameFile removedFile = getGameInstance().getAllFiles().remove(allFilesIndex);
        if (removedFile != file) {
            files.add(file);
            getGameInstance().getAllFiles().add(allFilesIndex, removedFile);
            throw new IllegalStateException("Internal state error! gameInstance.allFiles did not contain the file (" + file.getDebugName() + ") at the expected position (" + allFilesIndex + "), even though the file reported as registered! (Removed: " + removedFile.getFileName() + ")");
        }

        removedFile = this.files.remove(removeIndex);
        if (removedFile != file) { // Shouldn't be possible.
            files.add(file);
            getGameInstance().getAllFiles().add(looseFiles + removeIndex, file);
            this.files.add(removeIndex, (GreatQuestArchiveFile) removedFile);
            throw new IllegalStateException("Internal state error! this.files did not contain the file (" + file.getDebugName() + ") at the expected position, even though the file reported as registered!");
        }

        // Handle file path tracking.
        if (files.size() == 1) {
            // Files without collisions have their file paths stored in the mod data.
            getGameInstance().getModData().getUserGlobalFilePaths().put(file.getHash(), files.get(0).getFilePath());
        } else if (files.isEmpty()) {
            // The last file was removed, so stop tracking the file paths.
            getGameInstance().getModData().getUserGlobalFilePaths().remove(file.getHash());
            this.filesByHash.remove(file.getHash(), files);
        }

        return true;
    }

    /**
     * Print a list of all files to stdout.
     */
    @SuppressWarnings("unused")
    public void printFileList() {
        StringBuilder fileLine = new StringBuilder();
        for (int i = 0; i < this.files.size(); i++) {
            GreatQuestArchiveFile file = this.files.get(i);
            fileLine.append(file.hasFilePath() ? file.getFilePath() : "UNKNOWN");
            fileLine.append(" # File ");
            fileLine.append(NumberUtils.padNumberString(i, 4));
            fileLine.append(", Hash: ");
            fileLine.append(file.getHashAsHexString());
            if (file.hasCollision())
                fileLine.append(" (Collision)");
            if (file.isCompressed())
                fileLine.append(", Compressed");
            getLogger().info(fileLine.toString());
            fileLine.setLength(0);
        }
    }

    /**
     * Applies a file path to the file which the file path belongs to.
     * @param filePath The path of a game file.
     * @param showMessageIfNotFound Specify if a warning should be displayed if the file is not found.
     */
    public GreatQuestArchiveFile applyFilePath(String filePath, boolean showMessageIfNotFound) {
        String abbreviatedFilePath = GreatQuestUtils.getFileIdFromPath(filePath);
        int hash = GreatQuestUtils.hash(abbreviatedFilePath);

        // Search files.
        GreatQuestArchiveFile file = null;
        List<GreatQuestArchiveFile> collidingFiles = this.filesByHash.get(hash);
        if (collidingFiles != null) {
            if (collidingFiles.size() > 1) {
                // If there's more than one file, it means the file collides with another hash, MEANING its full file name was included in the data.bin.
                // Therefore, we want to check if the file is found, and if so, we do not need to show the warning message.
                for (int i = 0; i < collidingFiles.size(); i++) {
                    if (filePath.equalsIgnoreCase(collidingFiles.get(i).getFilePath())) {
                        showMessageIfNotFound = false;
                        break;
                    }
                }
            } else if (collidingFiles.size() == 1) {
                file = collidingFiles.get(0);
            }
        }

        if (file != null) {
            file.setFilePath(filePath);
            return file;
        }

        if (showMessageIfNotFound)
            getLogger().warning("Attempted to apply the file path '%s', but no file matched the hash %08X.", filePath, hash);
        return null;
    }

    /**
     * Searches for a file by its full file path, printing a message if the file is nto found.
     * @param searchFrom The file to search from, so that we can print which file wanted the file if it wasn't found.
     * @param filePath   Full file path.
     * @return the found file, if there was one.
     */
    public GreatQuestArchiveFile getFileByPath(GreatQuestArchiveFile searchFrom, String filePath) {
        GreatQuestArchiveFile file = getOptionalFileByPath(filePath);
        if (file == null)
            getLogger().warning("Failed to find file %s. (%s)", filePath + (searchFrom != null ? " referenced in " + searchFrom.getExportName() : ""), GreatQuestUtils.getFileIdFromPath(filePath));
        return file;
    }

    /**
     * Searches for a file by its full file path.
     * @param filePath Full file path.
     * @return the found file, if there was one.
     */
    public GreatQuestArchiveFile getOptionalFileByPath(String filePath) {
        if (StringUtils.isNullOrWhiteSpace(filePath))
            throw new NullPointerException("filePath");

        // Create hash.
        String abbreviatedFilePath = GreatQuestUtils.getFileIdFromPath(filePath);
        int hash = GreatQuestUtils.hash(abbreviatedFilePath);

        // Search files.
        List<GreatQuestArchiveFile> collidingFiles = this.filesByHash.get(hash);
        if (collidingFiles != null) {
            for (int i = 0; i < collidingFiles.size(); i++) {
                GreatQuestArchiveFile collidedFile = collidingFiles.get(i);
                if (abbreviatedFilePath.equalsIgnoreCase(GreatQuestUtils.getFileIdFromPath(collidedFile.getFilePath())))
                    return collidedFile;
            }
        }

        return null;
    }

    public static void exportFileList(File baseFolder, GreatQuestAssetBinFile mainArchive) throws IOException {
        List<String> lines = new ArrayList<>();
        long namedCount = mainArchive.getFiles().stream().filter(file -> file.getFilePath() != null).count();
        lines.add("File List [" + mainArchive.getFiles().size() + ", " + namedCount + " named]:");

        for (int i = 0; i < mainArchive.getFiles().size(); i++) {
            GreatQuestArchiveFile file = mainArchive.getFiles().get(i);

            lines.add(" - File #" + NumberUtils.padNumberString(i, 4)
                    + ": " + file.getHashAsHexString()
                    + ", " + file.getClass().getSimpleName()
                    + (file.getFilePath() != null ? ", " + file.getFilePath() + ", " + GreatQuestUtils.getFileIdFromPath(file.getFilePath()) : ""));
        }

        lines.add("");
        lines.add("Global Paths:");
        for (String globalPath : mainArchive.getGlobalPaths())
            lines.add(" - " + globalPath);

        Files.write(new File(baseFolder, "file-list.txt").toPath(), lines);
    }

    @Getter
    private static class GreatQuestAssetBinFileHeader extends GameData<GreatQuestInstance> {
        private final GreatQuestAssetBinFile parentFile;
        private final String filePath;
        private final int nameHash;
        private final boolean hasCollision;
        private int size;
        private int compressedSize;
        private int offset;

        public GreatQuestAssetBinFileHeader(GreatQuestAssetBinFile parentFile, String filePath, int nameHash, boolean hasCollision) {
            super(parentFile.getGameInstance());
            this.parentFile = parentFile;
            this.filePath = filePath;
            this.nameHash = nameHash;
            this.hasCollision = hasCollision;
        }

        @Override
        public void load(DataReader reader) {
            this.size = reader.readInt();
            this.compressedSize = reader.readInt();
            this.offset = reader.readInt();
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.size);
            writer.writeInt(this.compressedSize);
            writer.writeInt(this.offset);
            writer.writeInt(0);
        }

        /**
         * Creates/prepares the file corresponding to this entry.
         * @param reader the reader to create the file for
         * @param progressBar the progress bar to update the display for
         * @return newFileObject
         */
        public GreatQuestArchiveFile prepareFile(DataReader reader, ProgressBarComponent progressBar) {
            boolean isCompressed = (this.compressedSize != 0); // ZLib compression.

            requireReaderIndex(reader, this.offset, "Expected file data for '" + this.filePath + "'");
            byte[] fileBytes;
            if (isCompressed) {
                byte[] compressedFileBytes = reader.readBytes(this.compressedSize);
                fileBytes = GreatQuestUtils.zlibDecompress(compressedFileBytes, this.size);
            } else {
                fileBytes = reader.readBytes(this.size);
            }

            GreatQuestArchiveFile readFile;
            if (DataUtils.testSignature(fileBytes, GreatQuestImageFile.SIGNATURE_STR)) {
                readFile = new GreatQuestImageFile(getGameInstance());
            } else if (DataUtils.testSignature(fileBytes, kcModelWrapper.SIGNATURE_STR)) {
                readFile = new kcModelWrapper(getGameInstance());
            } else if (DataUtils.testSignature(fileBytes, "TOC\0")) {
                readFile = new GreatQuestChunkedFile(getGameInstance());
            } else if (this.parentFile.files.size() > 100 && fileBytes.length > 30) {
                readFile = new GreatQuestImageFile(getGameInstance());
            } else {
                // Files are sorted by file-type, so use the previous
                GreatQuestArchiveFile lastFile = this.parentFile.files.size() > 0 ? this.parentFile.files.get(this.parentFile.files.size() - 1) : null;
                GreatQuestArchiveFileType lastType = lastFile != null ? lastFile.getFileType() : GreatQuestArchiveFileType.values()[0];
                GreatQuestArchiveFileType fileType = GreatQuestArchiveFileType.getFileTypeFromFilePath(this.filePath, lastType);
                readFile = new GreatQuestDummyArchiveFile(getGameInstance(), fileType, fileBytes.length);
            }

            // Setup file.
            readFile.init(this.filePath, isCompressed, this.nameHash, fileBytes);
            if (progressBar != null)
                progressBar.setStatusMessage("Preparing '" + readFile.getExportName() + "'");

            this.parentFile.files.add(readFile); // Add before loading, so it can find its ID.
            this.parentFile.filesByHash.computeIfAbsent(readFile.getHash(), key -> new ArrayList<>()).add(readFile);

            if (progressBar != null)
                progressBar.addCompletedProgress(1);
            return readFile;
        }
    }

    private static int getImageSortingValue(GreatQuestArchiveFile file) {
        if (file.getFileType() != GreatQuestArchiveFileType.IMAGE)
            return -1; // Dummy .img files will continue here.

        // PS2 compressed models are sorted before non-compressed models.
        GreatQuestImageFile imageFile = getSelfOrValidNeighborFile(file,
                testFile -> testFile instanceof GreatQuestImageFile,
                testFile -> (GreatQuestImageFile) testFile, null);

        return imageFile != null ? ((GreatQuestImageFile) file).getFileFormat().ordinal() : -1;
    }

    private static int getModelSortingValue(GreatQuestArchiveFile file) {
        if (file.getFileType() != GreatQuestArchiveFileType.MODEL)
            return -1; // Dummy .VTX files will continue here.

        // PS2 compressed models are sorted before non-compressed models.
        kcModel model = getSelfOrValidNeighborFile(file,
                testFile -> testFile instanceof kcModelWrapper,
                testFile -> ((kcModelWrapper) testFile).getModel(), null);

        return model != null ? (((model.getFvf() & kcFvFUtil.FVF_FLAG_COMPRESSED) != kcFvFUtil.FVF_FLAG_COMPRESSED) ? 1 : 0) : -1;
    }

    private static String getFilePathOrNeighborFilePath(GreatQuestArchiveFile file) {
        return getSelfOrValidNeighborFile(file,
                testFile -> !StringUtils.isNullOrWhiteSpace(testFile.getFilePath()),
                GreatQuestAssetBinFile::getSortableFilePath, "");
    }

    private static String getSortableFilePath(GreatQuestArchiveFile file) {
        // The extension is stripped because the files are already sorted by file type and thus there's no reason to include the extension.
        // Or at least that's my guess for why the original developers sorted this way.
        return FileUtils.stripExtension(file.getFilePath().toLowerCase());
    }

    private static <T> T getSelfOrValidNeighborFile(GreatQuestArchiveFile file, Predicate<GreatQuestArchiveFile> fileTester, Function<GreatQuestArchiveFile, T> valueGetter, T defaultValue) {
        if (fileTester.test(file))
            return valueGetter.apply(file);

        // In the case of a file without a name, we want to preserve the existing order, so we'll get a neighbor.
        GreatQuestAssetBinFile binFile = file.getMainArchive();
        int fileIndex = binFile.getFiles().indexOf(file);
        if (fileIndex < 0)
            throw new UnsupportedOperationException("Cannot sort a GreatQuestArchiveFile without a filePath which isn't already registered.");

        GreatQuestArchiveFile testFile;
        int maxSearch = Math.max(fileIndex, binFile.getFiles().size() - fileIndex);
        for (int i = 0; i < maxSearch; i++) {
            int minIndex = fileIndex - i - 1;
            if (minIndex > 0 && fileTester.test(testFile = binFile.getFiles().get(minIndex)))
                return valueGetter.apply(testFile);

            int maxIndex = fileIndex + i + 1;
            if (maxIndex < binFile.getFiles().size() && fileTester.test(testFile = binFile.getFiles().get(maxIndex)))
                return valueGetter.apply(testFile);
        }

        return defaultValue; // No files exist which we can try.
    }

}