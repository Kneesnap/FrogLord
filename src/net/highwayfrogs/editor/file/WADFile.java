package net.highwayfrogs.editor.file;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.packers.PP20Packer;
import net.highwayfrogs.editor.file.packers.PP20Unpacker;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.ui.file.WADController;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a WAD file.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
public class WADFile extends SCSharedGameFile {
    private final List<WADEntry> files = new ArrayList<>();
    private FroggerMapTheme theme;

    public static String CURRENT_FILE_NAME = null;
    public static final int TYPE_ID = -1;
    private static final int TERMINATOR = -1;

    public WADFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        ThemeBook themeBook = getIndexEntry().getThemeBook();
        this.theme = themeBook != null ? themeBook.getTheme() : FroggerMapTheme.getTheme(getFileDisplayName());

        while (true) {
            int resourceId = reader.readInt();
            int fileType = reader.readInt();
            int size = reader.readInt();
            reader.skipInt(); // The number of files in the wad, until the last one which is zero.
            if (resourceId == TERMINATOR)
                break; // There are no more files.

            FileEntry wadFileEntry = getGameInstance().getResourceEntryByID(resourceId);
            String fileName = wadFileEntry.getDisplayName();
            CURRENT_FILE_NAME = fileName;

            // Decompress if compressed.
            byte[] data = reader.readBytes(size);
            reader.align(4);

            boolean compressed = PP20Unpacker.isCompressed(data);
            if (compressed)
                data = PP20Unpacker.unpackData(data);

            // Calculate the SHA1 hash.
            if (FroggerVersionComparison.isEnabled() && wadFileEntry.getSha1Hash() == null)
                wadFileEntry.setSha1Hash(Utils.calculateSHA1Hash(data));

            // Create file.
            SCGameFile<?> file = getGameInstance().createFile(wadFileEntry, data);
            if (file == null) {
                file = new DummyFile(getGameInstance(), data.length);
                getLogger().warning("File '" + fileName + "' was of an unknown file type. (" + fileType + ")");
            }

            // Setup file.
            WADEntry newEntry = new WADEntry(getGameInstance(), resourceId, fileType, compressed, null);
            this.files.add(newEntry);
            newEntry.setFile(file);
            file.setRawFileData(data);

            try {
                DataReader wadFileReader = new DataReader(new ArraySource(data));
                file.load(wadFileReader);
                if (wadFileReader.hasMore() && file.warnIfEndNotReached())
                    file.getLogger().warning("File contents were read to index " + Utils.toHexString(wadFileReader.getIndex()) + ", leaving " + wadFileReader.getRemaining() + " bytes unread. (Length: " + Utils.toHexString(wadFileReader.getSize()) + ")");
            } catch (Exception ex) {
                Utils.handleError(getLogger(), ex, false, "Failed to load %s. (%d)", CURRENT_FILE_NAME, resourceId);

                // Make it a dummy file instead since it failed.
                file = new DummyFile(getGameInstance(), data.length);
                newEntry.setFile(file);
                file.load(new DataReader(new ArraySource(data)));
            }
        }

        CURRENT_FILE_NAME = null;
    }

    @Override
    public void save(DataWriter writer) {
        for (WADEntry entry : getFiles()) {
            writer.writeInt(entry.getResourceId());
            writer.writeInt(entry.getFileType());

            CURRENT_FILE_NAME = entry.getFileEntry().getDisplayName();
            ArrayReceiver receiver = new ArrayReceiver();
            entry.getFile().save(new DataWriter(receiver));

            byte[] fileBytes = receiver.toArray();
            if (entry.isCompressed())
                fileBytes = PP20Packer.packData(fileBytes);

            writer.writeInt(fileBytes.length); // File length.
            writer.writeNull(Constants.INTEGER_SIZE); // Padding
            writer.writeBytes(fileBytes); // Write file contents.
        }
        CURRENT_FILE_NAME = null;

        writer.writeInt(TERMINATOR);
        writer.writeNull(Constants.INTEGER_SIZE * 3);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.ZIPPED_FOLDER_32.getFxImage();
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void exportAlternateFormat(FileEntry entry) {
        getArchive().promptVLOSelection(getTheme(), vlo -> {

            File folder = new File(GUIMain.getWorkingDirectory(), "mof_" + (getTheme() != null ? getTheme() : "unknown") + File.separator);
            if (!folder.exists())
                folder.mkdirs();

            if (vlo != null)
                vlo.exportAllImages(folder, MOFFile.MOF_EXPORT_FILTER);

            setVLO(vlo);
            for (WADEntry wadEntry : getFiles()) {
                SCGameFile<?> file = wadEntry.getFile();
                if (file instanceof MOFHolder)
                    ((MOFHolder) file).exportObject(folder, vlo);
            }
        }, true);
    }

    /**
     * Set the VLO file of the mof files inside this wad.
     * @param vloArchive The new VLO archive.
     */
    public void setVLO(VLOArchive vloArchive) {
        for (WADEntry wadEntry : getFiles()) {
            SCGameFile<?> file = wadEntry.getFile();
            if (file instanceof MOFHolder)
                ((MOFHolder) file).setVloFile(vloArchive);
        }
    }

    @Override
    public WADController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-wad", new WADController(getGameInstance()), this);
    }

    @Getter
    public static class WADEntry extends SCSharedGameObject {
        private final int resourceId;
        private final int fileType;
        private final boolean compressed;
        private SCGameFile<?> file;

        public WADEntry(SCGameInstance instance, int resourceId, int fileType, boolean compressed, SCGameFile<?> file) {
            super(instance);
            this.resourceId = resourceId;
            this.fileType = fileType;
            this.compressed = compressed;
            this.file = file;
        }

        /**
         * Get the FileEntry for this WAD Entry.
         * @return fileEntry
         */
        public FileEntry getFileEntry() {
            return getGameInstance().getResourceEntryByID(this.resourceId);
        }

        /**
         * Check if this is a dummied MOF Entry.
         * @return isDummyMOF
         */
        public boolean isDummy() {
            return getFile() == null || ((getFile() instanceof MOFHolder) && ((MOFHolder) getFile()).isDummy());
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
            if (this.file != null) {
                getGameInstance().getFileEntriesByFileObjects().remove(this.file, getFileEntry());
                getGameInstance().getFileObjectsByFileEntries().remove(getFileEntry(), this.file);
            }

            this.file = newFile;
            getGameInstance().getFileEntriesByFileObjects().put(newFile, getFileEntry());
            getGameInstance().getFileObjectsByFileEntries().put(getFileEntry(), newFile);
        }
    }
}