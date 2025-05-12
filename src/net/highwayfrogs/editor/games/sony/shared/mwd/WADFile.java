package net.highwayfrogs.editor.games.sony.shared.mwd;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.pp20.PP20Packer;
import net.highwayfrogs.editor.games.sony.shared.pp20.PP20Packer.PackResult;
import net.highwayfrogs.editor.games.sony.shared.pp20.PP20Unpacker;
import net.highwayfrogs.editor.games.sony.shared.pp20.PP20Unpacker.UnpackResult;
import net.highwayfrogs.editor.games.sony.shared.ui.file.WADController;
import net.highwayfrogs.editor.games.sony.shared.utils.DynamicMeshObjExporter;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents a WAD file.
 * A WAD file contains various other files loaded together.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
public class WADFile extends SCSharedGameFile {
    private final List<WADEntry> files = new ArrayList<>();

    public static final int TYPE_ID = -1;
    private static final int TERMINATOR = -1;
    private static final SavedFilePath WAD_FILE_EXPORT_PATH = new SavedFilePath("wadExportPath", "Select the directory to export WAD contents to.");
    private static final SavedFilePath WAD_FILE_IMPORT_PATH = new SavedFilePath("wadImportPath", "Select the directory to import WAD contents from.");

    public WADFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.files.clear();
        int lastFileCount = -1;
        while (reader.hasMore()) {
            int resourceId = reader.readInt();
            if (resourceId == TERMINATOR) {
                reader.skipBytesRequireEmpty(3 * Constants.INTEGER_SIZE); // It's a full entry, but these are zero.
                break; // There are no more files.
            }

            int fileTypeId = reader.readInt();
            int fileSizeInBytes = reader.readInt();
            int fileCount = reader.readInt(); // The number of files in the wad, until the last one which is zero. (But all of them are zero)
            if (this.files.size() > 0 && lastFileCount != -1 && lastFileCount != fileCount)
                getLogger().warning("The WAD 'fileCount' value " + fileCount + " did not match the previously seen value of " + lastFileCount + ". (This probably won't cause problems, but it does indicate our understanding of this value is wrong.)");

            MWIResourceEntry fileMwiEntry = getGameInstance().getResourceEntryByID(resourceId);
            String fileName = fileMwiEntry.getDisplayName();
            if (fileTypeId != fileMwiEntry.getTypeId())
                getLogger().severe("The MWI file entry for '" + fileName + "' had a type ID of " + fileMwiEntry.getTypeId() + ", but the WAD Entry had a type ID of " + fileTypeId);

            // Read file contents.
            byte[] fileBytes = reader.readBytes(fileSizeInBytes);
            reader.alignRequireEmpty(Constants.INTEGER_SIZE);

            // Decompress if compressed.
            byte[] compressedFileBytes = null;
            int safetyMarginWordCount = 0;
            boolean dataAppearsCompressed = PP20Unpacker.isCompressed(fileBytes);
            if (dataAppearsCompressed != fileMwiEntry.isCompressed())
                getLogger().severe("The wad entry '" + fileMwiEntry.getDisplayName() + "' appears" + (dataAppearsCompressed ? "" : " NOT") + " to be compressed, but the MWI entry disagrees.");
            if (dataAppearsCompressed) {
                compressedFileBytes = fileBytes;
                UnpackResult unpackResult = PP20Unpacker.unpackData(fileBytes);
                fileBytes = unpackResult.getUnpackedBytes();
                safetyMarginWordCount = unpackResult.getSafetyMarginWordCount();
            }

            // Run load data hook.
            fileMwiEntry.onLoadData(fileBytes, compressedFileBytes, safetyMarginWordCount);

            // Create file.
            SCGameFile<?> file = getGameInstance().createFile(fileMwiEntry, fileBytes);
            if (file == null) {
                file = new DummyFile(getGameInstance(), fileBytes.length);
                getLogger().warning("File '" + fileName + "' was of an unknown file type. (" + fileMwiEntry.getTypeId() + ")");
            }

            // Setup file.
            WADEntry newEntry = new WADEntry(getGameInstance(), resourceId, dataAppearsCompressed, null);
            newEntry.setFile(file);
            this.files.add(newEntry);
            file.setRawFileData(fileBytes);

            try {
                DataReader wadFileReader = new DataReader(new ArraySource(fileBytes));
                file.load(wadFileReader);
                if (wadFileReader.hasMore() && file.warnIfEndNotReached())
                    file.getLogger().warning("File contents were read to index " + NumberUtils.toHexString(wadFileReader.getIndex()) + ", leaving " + wadFileReader.getRemaining() + " bytes unread. (Length: " + NumberUtils.toHexString(wadFileReader.getSize()) + ")");
            } catch (Exception ex) {
                Utils.handleError(getLogger(), ex, false, "Failed to load %s. (%d)", fileName, resourceId);

                // Make it a dummy file instead since it failed.
                file = new DummyFile(getGameInstance(), fileBytes.length);
                file.setRawFileData(fileBytes);
                newEntry.setFile(file);
                file.load(new DataReader(new ArraySource(fileBytes)));
            }

            lastFileCount = fileCount;
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (WADEntry entry : this.files) {
            writer.writeInt(entry.getResourceId());
            writer.writeInt(entry.getFileEntry().getTypeId());

            MWIResourceEntry mwiEntry = entry.getFileEntry();
            ArrayReceiver receiver = new ArrayReceiver();
            entry.getFile().save(new DataWriter(receiver));

            byte[] fileBytes = receiver.toArray();
            PackResult packResult = entry.isCompressed() ? PP20Packer.packData(fileBytes) : null;
            mwiEntry.onSaveData(fileBytes, packResult);

            byte[] writtenBytes = packResult != null ? packResult.getPackedBytes() : fileBytes;
            writer.writeInt(writtenBytes.length); // File length.
            writer.writeInt(this.files.size()); // File count.
            writer.writeBytes(writtenBytes); // Write file contents.
            writer.align(Constants.INTEGER_SIZE);
        }

        writer.writeInt(TERMINATOR);
        writer.writeNull(Constants.INTEGER_SIZE * 3);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.ZIPPED_FOLDER_32.getFxImage();
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportOriginalFiles = new MenuItem("Export Original Files");
        contextMenu.getItems().add(exportOriginalFiles);
        exportOriginalFiles.setOnAction(event -> exportAllFiles(true));

        MenuItem exportFiles = new MenuItem("Export Files");
        contextMenu.getItems().add(exportFiles);
        exportFiles.setOnAction(event -> exportAllFiles(false));

        MenuItem exportAsObjFiles = new MenuItem("Export 3D models in .obj format.");
        contextMenu.getItems().add(exportAsObjFiles);
        exportAsObjFiles.setOnAction(event -> {
            File outputBaseDir = FileUtils.askUserToSelectFolder(getGameInstance(), DynamicMeshObjExporter.OBJ_EXPORT_FOLDER_PATH);
            if (outputBaseDir == null)
                return;

            File outputDir = new File(outputBaseDir, FileUtils.stripExtension(getFileDisplayName()).toLowerCase(Locale.ROOT));
            FileUtils.makeDirectory(outputDir);

            for (WADEntry wadEntry : this.files) {
                SCGameFile<?> file = wadEntry.getFile();
                if (file instanceof MRModel)
                    ((MRModel) file).exportObject(outputDir, ((MRModel) file).getVloFile());
            }
        });
    }

    /**
     * Export all files to the destination folder.
     */
    public void exportAllFiles(boolean original) {
        File selectedFolder = FileUtils.askUserToSelectFolder(getGameInstance(), WAD_FILE_EXPORT_PATH);
        if (selectedFolder == null)
            return; // Cancelled.

        for (WADEntry wadEntry : this.files) {
            MWIResourceEntry resourceEntry = wadEntry.getFileEntry();
            File outputFile = FileUtils.getNonExistantFile(new File(selectedFolder, resourceEntry.getDisplayName()));
            resourceEntry.getGameFile().saveToFile(outputFile, original, false);
        }
    }

    /**
     * Gets the Frogger map theme which the WAD corresponds to.
     * TODO: Perhaps move to FroggerUtils.java instead?
     * Returns null if the game is not Frogger, or there is no map theme.
     * @return froggerMapTheme
     */
    public FroggerMapTheme getFroggerMapTheme() {
        if (!getGameInstance().isFrogger())
            return null;

        ThemeBook themeBook = null;
        for (ThemeBook book : ((FroggerGameInstance) getGameInstance()).getThemeLibrary()) {
            if (book != null && book.isEntry(this)) {
                themeBook = book;
                if (themeBook.getTheme() != null)
                    break;
            }
        }

        return themeBook != null && themeBook.getTheme() != null
                ? themeBook.getTheme() : FroggerMapTheme.getTheme(getFileDisplayName());
    }

    /**
     * Set the VLO file of the mof files inside this wad.
     * @param vloArchive The new VLO archive.
     */
    public void setVLO(VLOArchive vloArchive) {
        for (WADEntry wadEntry : this.files) {
            SCGameFile<?> file = wadEntry.getFile();
            if (file instanceof MRModel)
                ((MRModel) file).setVloFile(vloArchive);
        }
    }

    @Override
    public WADController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-wad", new WADController(getGameInstance()), this);
    }

    @Getter
    public static class WADEntry extends SCSharedGameObject {
        private final int resourceId;
        private final boolean compressed;
        private SCGameFile<?> file;

        public WADEntry(SCGameInstance instance, int resourceId, boolean compressed, SCGameFile<?> file) {
            super(instance);
            this.resourceId = resourceId;
            this.compressed = compressed;
            this.file = file;
        }

        /**
         * Get the MWIResourceEntry for this WAD Entry.
         * @return fileEntry
         */
        public MWIResourceEntry getFileEntry() {
            return getGameInstance().getResourceEntryByID(this.resourceId);
        }

        /**
         * Check if this is a dummied MOF Entry.
         * @return isDummyMOF
         */
        public boolean isDummy() {
            return getFile() == null || ((getFile() instanceof MRModel) && ((MRModel) getFile()).isDummy());
        }

        /**
         * Get the display name of this WADEntry.
         * @return displayName
         */
        public String getDisplayName() {
            if (isDummy())
                return "Empty";

            String displayName = getFileEntry().getDisplayName();
            return displayName.equals(Constants.DUMMY_FILE_NAME) ? "Imported MOF File" : displayName;
        }

        /**
         * Set the file linked to this wad entry.
         * @param newFile The new file
         */
        public void setFile(SCGameFile<?> newFile) {
            MWIResourceEntry mwiEntry = getFileEntry();
            if (this.file != null) {
                this.file.setFileDefinition(null);
                getGameInstance().getFileObjectsByFileEntries().remove(mwiEntry, this.file);
            }

            this.file = newFile;
            getGameInstance().getFileObjectsByFileEntries().put(mwiEntry, newFile);
            if (newFile != null)
                newFile.setFileDefinition(mwiEntry);
        }
    }
}