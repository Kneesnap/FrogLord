package net.highwayfrogs.editor.file;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.PLTFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.packers.PP20Packer;
import net.highwayfrogs.editor.file.packers.PP20Unpacker;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.editor.WADController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a WAD file.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
public class WADFile extends GameFile {
    private final List<WADEntry> files = new ArrayList<>();
    private MAPTheme theme;

    private static final Image ICON = loadIcon("packed");
    public static String CURRENT_FILE_NAME = null;
    public static final int TYPE_ID = -1;
    private static final int TERMINATOR = -1;

    @Override
    public void load(DataReader reader) {
        this.theme = null;

        MWIFile mwiTable = getConfig().getMWI();

        MOFHolder lastCompleteMOF = null;
        while (true) {
            int resourceId = reader.readInt();
            if (resourceId == TERMINATOR)
                break; // There are no more files.

            int fileType = reader.readInt();
            int size = reader.readInt();
            reader.skipInt(); // Padding.

            CURRENT_FILE_NAME = getConfig().getResourceEntry(resourceId).getDisplayName();

            // Decompress if compressed.
            byte[] data = reader.readBytes(size);
            if (reader.getIndex() % 4 != 0)
                reader.skipBytes(4 - (reader.getIndex() % 4)); // Alignment.

            boolean compressed = PP20Unpacker.isCompressed(data);
            if (compressed)
                data = PP20Unpacker.unpackData(data);

            GameFile file;
            if (Constants.ENABLE_WAD_FORMATS) {
                if ((fileType == VLOArchive.WAD_TYPE || fileType == 1) && data[0] == (byte) '2') {
                    file = new VLOArchive();
                } else if (fileType == MOFHolder.MOF_ID || fileType == 2) {
                    file = new MOFHolder(theme, lastCompleteMOF);
                    if (data[0] == (byte) 'G') // Rolling Demo.
                        file = new DummyFile(data.length);
                } else if (fileType == PLTFile.FILE_TYPE) {
                    file = new PLTFile();
                } else {
                    if (fileType == 0) {
                        getConfig().getResourceEntry(resourceId).setFilePath(getConfig().getResourceEntry(resourceId).getDisplayName() + "-UNK_TYPE" + fileType);
                    } else if (fileType == 4) {
                        getConfig().getResourceEntry(resourceId).setFilePath(getConfig().getResourceEntry(resourceId).getDisplayName() + "-QTREE");
                    } else if (fileType == 5) {
                        getConfig().getResourceEntry(resourceId).setFilePath(getConfig().getResourceEntry(resourceId).getDisplayName() + "-GRID");
                    } else if (fileType == 6) {
                        getConfig().getResourceEntry(resourceId).setFilePath(getConfig().getResourceEntry(resourceId).getDisplayName() + "-MAP_RELATED" + fileType);
                    } else {
                        System.out.println("Unexpected WAD file-type: " + fileType + ".");
                    }
                    file = new DummyFile(data.length);
                }
            }

            try {
                WADEntry newEntry = new WADEntry(resourceId, fileType, compressed, null, mwiTable);
                newEntry.setFile(file);
                files.add(newEntry);

                file.load(new DataReader(new ArraySource(data)));

                if (file instanceof MOFHolder) {
                    MOFHolder newHolder = (MOFHolder) file;
                    if (!newHolder.isIncomplete())
                        lastCompleteMOF = newHolder;
                }
            } catch (Exception ex) {
                System.out.println("Failed to load WAD File " + CURRENT_FILE_NAME + ". (" + resourceId + ")");
                ex.printStackTrace();
                //throw new RuntimeException("Failed to load " + CURRENT_FILE_NAME + ".", ex);
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
    public Image getIcon() {
        return ICON;
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void exportAlternateFormat(FileEntry entry) {
        getMWD().promptVLOSelection(getTheme(), vlo -> {

            File folder = new File(GUIMain.getWorkingDirectory(), "mof_" + (getTheme() != null ? getTheme() : "unknown") + File.separator);
            if (!folder.exists())
                folder.mkdirs();

            if (vlo != null)
                vlo.exportAllImages(folder, MOFFile.MOF_EXPORT_FILTER);

            setVLO(vlo);
            for (WADEntry wadEntry : getFiles()) {
                GameFile file = wadEntry.getFile();
                if (file instanceof MOFHolder)
                    ((MOFHolder) file).exportObject(folder, vlo);
            }
        }, true);
    }

    /**
     * Set the VLO file of all of the mofs inside this wad.
     * @param vloArchive The new VLO archive.
     */
    public void setVLO(VLOArchive vloArchive) {
        for (WADEntry wadEntry : getFiles()) {
            GameFile file = wadEntry.getFile();
            if (file instanceof MOFHolder)
                ((MOFHolder) file).setVloFile(vloArchive);
        }
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new WADController(), "wad", this);
    }

    @Getter
    @AllArgsConstructor
    public static class WADEntry {
        private final int resourceId;
        private final int fileType;
        private final boolean compressed;
        private GameFile file;
        private final MWIFile mwiFile;

        /**
         * Get the FileEntry for this WAD Entry.
         * @return fileEntry
         */
        public FileEntry getFileEntry() {
            return mwiFile.getEntries().get(resourceId);
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
        public void setFile(GameFile newFile) {
            this.file = newFile;
            newFile.getMWD().getEntryMap().put(newFile, getFileEntry());
            newFile.getMWD().getEntryFileMap().put(getFileEntry(), newFile);
        }
    }
}
