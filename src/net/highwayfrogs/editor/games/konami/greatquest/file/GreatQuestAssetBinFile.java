package net.highwayfrogs.editor.games.konami.greatquest.file;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Parses FTGQ's main game data file. It's called "data.bin" in all of the builds we've seen.
 * .SBR files contain sound effects. the SCK file contains only PCM data, with headers in the .IDX file.
 * .PSS (PS2) are video files. Can be opened with VLC.
 * BUFFER.DAT files (PS2) are video files, and can be opened with VLC.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class GreatQuestAssetBinFile extends GameData<GreatQuestInstance> {
    private final List<String> globalPaths = new ArrayList<>();
    private final List<GreatQuestArchiveFile> files = new ArrayList<>();
    private final Map<Integer, GreatQuestArchiveFile> nameMap = new HashMap<>();
    private final Map<Integer, List<GreatQuestArchiveFile>> fileCollisions = new HashMap<>();

    private static final Comparator<GreatQuestAssetBinFileHeader> FILE_ORDERING =
            Comparator.comparingInt(GreatQuestAssetBinFileHeader::getOffset)
                    .thenComparing(Comparator.nullsFirst(Comparator.comparing(GreatQuestAssetBinFileHeader::getName)))
                    .thenComparing(GreatQuestAssetBinFileHeader::getName)
                    .thenComparingInt(GreatQuestAssetBinFileHeader::hashCode);

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
        int unnamedFiles = reader.readInt();
        int namedFiles = reader.readInt();
        int globalPathStartAddress = reader.readInt(); // Located after the files.

        Map<Integer, String> nameMap = new HashMap<>();
        GreatQuestUtils.addHardcodedFileNameHashesToMap(nameMap);

        List<GreatQuestAssetBinFileHeader> fileHeaders = new ArrayList<>();

        // Prepare unnamed files.
        if (progressBar != null) {
            progressBar.setTotalProgress(unnamedFiles);
            progressBar.setStatusMessage("Reading File List");
        }

        for (int i = 0; i < unnamedFiles; i++) {
            int hash = reader.readInt();
            fileHeaders.add(readFileHeader(reader, nameMap.get(hash), hash, false, progressBar));
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
            int insertionIndex = Collections.binarySearch(fileHeaders, newHeader, FILE_ORDERING);
            if (insertionIndex >= 0)
                throw new RuntimeException("Did not expect to find the header already in the fileHeaders list.");

            fileHeaders.add(-(insertionIndex + 1), newHeader);
        }

        // Prepare (create) files.
        if (progressBar != null)
            progressBar.setTotalProgress(fileHeaders.size());
        this.files.clear();
        this.fileCollisions.clear();
        this.nameMap.clear();
        for (int i = 0; i < fileHeaders.size(); i++)
            fileHeaders.get(i).prepareFile(reader, progressBar);

        // Read global paths.
        requireReaderIndex(reader, globalPathStartAddress, "Expected global path table");
        int globalPathCount = reader.readInt();
        this.globalPaths.clear();
        for (int i = 0; i < globalPathCount; i++)
            this.globalPaths.add(reader.readNullTerminatedFixedSizeString(NAME_SIZE, Constants.NULL_BYTE));

        // Read FrogLord mod data.
        if (reader.hasMore())
            getGameInstance().getModData().load(reader);

        // Process (load) files. (File loading occurs only after we have an object for every single game file, so that file hash references can be resolved regardless of file order.)
        if (progressBar != null)
            progressBar.setTotalProgress(this.files.size());
        for (int i = 0; i < this.files.size(); i++)
            loadFile(this.files.get(i), progressBar);

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

    private GreatQuestAssetBinFileHeader readFileHeader(DataReader reader, String name, int nameHash, boolean hasCollision, ProgressBarComponent progressBar) {
        GreatQuestAssetBinFileHeader header = new GreatQuestAssetBinFileHeader(this, name, nameHash, hasCollision);
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
        for (GreatQuestArchiveFile file : getFiles())
            (file.hasFilePath() && file.isCollision() ? namedFiles : unnamedFiles).add(file);

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
            GreatQuestAssetBinFileHeader newHeader = new GreatQuestAssetBinFileHeader(this, file.getFilePath(), file.getHash(), file.isCollision());
            fileHeaders.add(newHeader);
            headersByFile.put(file, newHeader);
            newHeader.save(writer);
        }

        // Collision file headers are written second:
        for (GreatQuestArchiveFile file : namedFiles) {
            String fullFilePath = getGameInstance().getVersionConfig().getHostRootPath() + file.getFilePath();
            writer.writeNullTerminatedFixedSizeString(fullFilePath, NAME_SIZE, GreatQuestInstance.PADDING_BYTE_CD);

            // Add header which will be updated & written again later.
            GreatQuestAssetBinFileHeader newHeader = new GreatQuestAssetBinFileHeader(this, file.getFilePath(), file.getHash(), file.isCollision());
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
            if (file.isCollision())
                fileLine.append(" (Collision)");
            if (file.isCompressed())
                fileLine.append(", Compressed");
            getLogger().info(fileLine.toString());
            fileLine.setLength(0);
        }
    }

    /**
     * Activates the filename
     * @param filePath              The path of a game file.
     * @param showMessageIfNotFound Specify if a warning should be displayed if the file is not found.
     */
    public GreatQuestArchiveFile applyFileName(String filePath, boolean showMessageIfNotFound) {
        GreatQuestArchiveFile file = getOptionalFileByName(filePath);
        if (file != null) {
            file.setFilePath(filePath);
            return file;
        }

        int hash = GreatQuestUtils.hashFilePath(filePath);
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
    public GreatQuestArchiveFile getFileByName(GreatQuestArchiveFile searchFrom, String filePath) {
        GreatQuestArchiveFile file = getOptionalFileByName(filePath);
        if (file == null)
            getLogger().warning("Failed to find file %s. (%s)", filePath + (searchFrom != null ? " referenced in " + searchFrom.getExportName() : ""), GreatQuestUtils.getFileIdFromPath(filePath));
        return file;
    }

    /**
     * Searches for a file by its full file path.
     * @param filePath Full file path.
     * @return the found file, if there was one.
     */
    public GreatQuestArchiveFile getOptionalFileByName(String filePath) {
        // Create hash.
        String abbreviatedFilePath = GreatQuestUtils.getFileIdFromPath(filePath);
        int hash = GreatQuestUtils.hash(abbreviatedFilePath);

        // Search for unique file without collisions.
        GreatQuestArchiveFile file = this.nameMap.get(hash);
        if (file != null)
            return file;

        // Search colliding files.
        List<GreatQuestArchiveFile> collidingFiles = this.fileCollisions.get(hash);
        if (collidingFiles != null) {
            for (int i = 0; i < collidingFiles.size(); i++) {
                GreatQuestArchiveFile collidedFile = collidingFiles.get(i);
                if (GreatQuestUtils.getFileIdFromPath(collidedFile.getFilePath()).equalsIgnoreCase(abbreviatedFilePath))
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
        private final String name;
        private final int nameHash;
        private final boolean hasCollision;
        private int size;
        private int compressedSize;
        private int offset;

        public GreatQuestAssetBinFileHeader(GreatQuestAssetBinFile parentFile, String name, int nameHash, boolean hasCollision) {
            super(parentFile.getGameInstance());
            this.parentFile = parentFile;
            this.name = name;
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

            requireReaderIndex(reader, this.offset, "Expected file data for '" + this.name + "'");
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
                readFile = new GreatQuestDummyArchiveFile(getGameInstance(), fileBytes.length);
            }

            // Setup file.
            readFile.init(this.name, isCompressed, this.nameHash, fileBytes, this.hasCollision);
            if (progressBar != null)
                progressBar.setStatusMessage("Preparing '" + readFile.getExportName() + "'");

            this.parentFile.files.add(readFile); // Add before loading, so it can find its ID.
            if (hasCollision) {
                this.parentFile.fileCollisions.computeIfAbsent(readFile.getHash(), key -> new ArrayList<>()).add(readFile);
            } else {
                this.parentFile.nameMap.put(readFile.getHash(), readFile);
            }

            if (progressBar != null)
                progressBar.addCompletedProgress(1);
            return readFile;
        }

    }
}