package net.highwayfrogs.editor.file;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Millennium Wad Index: Holds information about all game files, usually in a MWD.
 * Located in game executable (Such as frogger.exe)
 * This should always export exactly the same size as the original MWI, as this gets pasted directly in the executable.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class MWIFile extends SCSharedGameData {
    private final List<FileEntry> entries = new ArrayList<>();

    private static final int ENTRY_LENGTH = 32;
    private static final int STR_TERMINATOR = 4;
    private static final int CODE_NO_FILE_NAME = 0xFFFFFFFF;

    public MWIFile(SCGameInstance instance) {
        super(instance);
    }

    /**
     * Get the FileEntry for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntry
     */
    public FileEntry getResourceEntryByID(int resourceId) {
        return resourceId >= 0 && resourceId < this.entries.size() ? this.entries.get(resourceId) : null;
    }

    /**
     * Gets the resource entry from a given name.
     * @param name The name to lookup.
     * @return foundEntry, if any.
     */
    public FileEntry getResourceEntryByName(String name) {
        if (name == null || name.isEmpty())
            return null;

        for (int i = 0; i < this.entries.size(); i++) {
            FileEntry entry = this.entries.get(i);
            if (name.equalsIgnoreCase(entry.getDisplayName()))
                return entry;
        }

        return null;
    }

    @Override
    public void load(DataReader reader) {
        AtomicInteger nameStartAddress = null;

        int loadingId = 0;
        while (reader.hasMore() && (nameStartAddress == null || nameStartAddress.get() > reader.getIndex())) { // Read entries until we reach file-names.
            FileEntry entry = new FileEntry(getGameInstance(), loadingId++);

            if (getGameInstance().isOldFrogger()) {
                entry.setFilePath(reader.readTerminatedStringOfLength(40));

                entry.setFlags(reader.readInt());
                entry.setTypeId(reader.readInt());
                entry.setSectorOffset(reader.readInt());
                reader.skipInt();
                entry.unpackedSize = reader.readInt();
                reader.skipInt();
            } else {
                int nameOffset = reader.readInt();
                if (nameOffset != CODE_NO_FILE_NAME) { // If the file name is present, read the file name. (File-names are present on the PC version, but not the PSX version.)
                    if (nameStartAddress == null) // Use the first name address as the address which starts the name table.
                        nameStartAddress = new AtomicInteger(nameOffset);

                    reader.jumpTemp(nameOffset);
                    entry.setFilePath(reader.readNullTerminatedString());
                    reader.jumpReturn();
                }

                entry.setFlags(reader.readInt());
                entry.setTypeId(reader.readInt());
                entry.setSectorOffset(reader.readInt());
                reader.skipInt(); // Should always be 0. This is used by the frogger.exe to locate where in RAM the file is located.
                reader.skipInt(); // Should always be 0. This is used by frogger.exe to locate where in RAM the file is depacked at.
                entry.setPackedSize(reader.readInt());
                entry.unpackedSize = reader.readInt(); // Set the raw value, not through the setter.

                if (getGameInstance().getGameType().doesMwiHaveChecksum()) // Discard checksum from post-MediEvil MWIs.
                    reader.skipInt();
            }

            getEntries().add(entry);
        }
    }

    @Override
    public void save(DataWriter writer) {
        int nameOffset = getEntries().size() * ENTRY_LENGTH;

        for (FileEntry entry : getEntries()) {

            if (entry.hasFilePath()) {
                writer.writeInt(nameOffset);
                int pathByteLength = entry.getFilePath().getBytes().length;
                nameOffset += pathByteLength; // The amount of bytes written.
                nameOffset += getNullCount(pathByteLength); // The terminator bytes predicts the offset it actually goes.
            } else {
                writer.writeInt(CODE_NO_FILE_NAME);
            }

            writer.writeInt(entry.getFlags());
            writer.writeInt(entry.getTypeId());
            writer.writeInt(entry.getSectorOffset());
            writer.writeInt(0);
            writer.writeInt(0);
            writer.writeInt(entry.getPackedSize());
            writer.writeInt(entry.unpackedSize); // Get the raw value, not through the getter.
        }

        getEntries().stream()
                .filter(FileEntry::hasFilePath)
                .map(FileEntry::getFilePath)
                .forEach(fileName -> {
                    byte[] bytes = fileName.getBytes();
                    writer.writeBytes(bytes);
                    writer.writeNull(getNullCount(bytes.length));
                });
    }

    private static int getNullCount(int strByteLength) {
        return STR_TERMINATOR - (strByteLength % STR_TERMINATOR);
    }

    @Setter
    @Getter
    public static class FileEntry extends SCSharedGameObject {
        private int flags; // Is a group? In a group? Compressed?
        private int typeId; //
        private int sectorOffset; // The file's starting address in the MWD.
        private int packedSize;
        private int unpackedSize;
        private String filePath;
        private String sha1Hash;
        private transient int resourceId;

        public static final int FLAG_SINGLE_ACCESS = Constants.BIT_FLAG_0; // I assume this is for files loaded individually, by themselves.
        public static final int FLAG_GROUP_ACCESS = Constants.BIT_FLAG_1; // Cannot be loaded individually / by itself. Presumably this is for files in child-WADs.
        public static final int FLAG_IS_GROUP = Constants.BIT_FLAG_2; // Presumably this marks a child archive, where the next entries belong to this file.
        public static final int FLAG_ABSOLUTE_PATH = Constants.BIT_FLAG_3; // Appears to let you use absolute file paths instead of relative paths. However, this is functionality is likely not in the retail build.
        public static final int FLAG_AUTOMATIC_COMPRESSION = Constants.BIT_FLAG_4;
        public static final int FLAG_MANUAL_COMPRESSION = Constants.BIT_FLAG_5;

        public FileEntry(SCGameInstance instance, int resourceId) {
            super(instance);
            this.resourceId = resourceId;
        }

        /**
         * Get the config active for the game instance.
         */
        public SCGameConfig getConfig() {
            return getGameInstance().getConfig();
        }

        /**
         * Test if the file ends with a particular extension.
         * @param extension The extension to test.
         * @return If the file has an extension.
         */
        public boolean hasExtension(String extension) {
            String fullFilePath = getFullFilePath();
            if (fullFilePath == null)
                return false;

            return fullFilePath.toLowerCase(Locale.ROOT).endsWith("." + extension.toLowerCase(Locale.ROOT));
        }

        /**
         * Get the amount of bytes this file will take up in the game archive.
         * @return byteCount
         */
        public int getArchiveSize() {
            return isCompressed() ? getPackedSize() : getUnpackedSize();
        }

        /**
         * Get the address where this file's data will start in the MWD file.
         * @return archiveOffset
         */
        public int getArchiveOffset() {
            return sectorOffset * Constants.CD_SECTOR_SIZE;
        }

        /**
         * Determines if this entry is compressed.
         * @return isCompressed
         */
        public boolean isCompressed() {
            return testFlag(FLAG_AUTOMATIC_COMPRESSION) || testFlag(FLAG_MANUAL_COMPRESSION);
        }

        /**
         * Get the unpacked file size in bytes.
         * @return unpackedSize
         */
        public int getUnpackedSize() {
            return isCompressed() ? this.unpackedSize & 0xFFFFFF : this.unpackedSize;
        }

        /**
         * Sets the unpacked file size in bytes.
         * @param newSize The new byte count for this file.
         */
        public void setUnpackedSize(int newSize) {
            this.unpackedSize = newSize;
            if (isCompressed())
                this.unpackedSize |= (3 << 24);
        }

        /**
         * Does this MWI entry have an associated file path?
         * @return hasFilePath
         */
        public boolean hasFilePath() {
            return getFilePath() != null;
        }

        /**
         * Gets the full file path of the file.
         * @return fullFilePath
         */
        public String getFullFilePath() {
            if (hasFilePath()) {
                return getFilePath();
            } else if (getConfig().getFallbackFileNames().size() > this.resourceId) {
                return getConfig().getFallbackFileNames().get(this.resourceId);
            } else {
                return null;
            }
        }

        /**
         * Test if this has a full file path.
         * @return
         */
        public boolean hasFullFilePath() {
            return getFullFilePath() != null;
        }

        /**
         * Get the display name of this file entry.
         * @return displayName
         */
        public String getDisplayName() {
            String fileName = getFullFilePath();
            if (fileName == null)
                return "File " + this.resourceId;

            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1); // Remove \ paths.
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1); // Remove / paths.
            return fileName;
        }

        /**
         * Test if a flag is valid for this entry.
         * @param flag The flag to test.
         * @return isFlagPresent
         */
        public boolean testFlag(int flag) {
            return (this.flags & flag) == flag;
        }

        @Override
        public String toString() {
            return getDisplayName() + "-{" + getFilePath() + " Type: " + getTypeId() + ", Flags: " + getFlags() + "}";
        }

        /**
         * Get the book which holds this FileEntry.
         * @return book, Can be null if not found.
         */
        public MapBook getMapBook() {
            if (!getGameInstance().isFrogger())
                return null;

            for (MapBook book : ((FroggerGameInstance) getGameInstance()).getMapLibrary())
                if (book != null && book.isEntry(this))
                    return book;
            return null;
        }

        /**
         * Get the book which holds this FileEntry.
         * @return book
         */
        public ThemeBook getThemeBook() {
            if (!getGameInstance().isFrogger())
                return null;

            for (ThemeBook book : ((FroggerGameInstance) getGameInstance()).getThemeLibrary())
                if (book != null && book.isEntry(this))
                    return book;
            return null;
        }
    }
}