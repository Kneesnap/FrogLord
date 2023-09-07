package net.highwayfrogs.editor.file;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VHFile;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MediEvil Wad Index: Holds information about the MWD file.
 * Located in game executable (Such as frogger.exe)
 * This should always export exactly the same size as the original MWI, as this gets pasted directly in the executable.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class MWIFile extends GameObject {
    private final List<FileEntry> entries = new ArrayList<>();
    private int fileSize;

    private static final int ENTRY_LENGTH = 32;
    private static final int STR_TERMINATOR = 4;
    private static final int CODE_NO_FILE_NAME = 0xFFFFFFFF;

    @Override
    public void load(DataReader reader) {
        this.fileSize = reader.getSize();
        AtomicInteger nameStartAddress = null;

        int loadingId = 0;
        while (reader.hasMore() && (nameStartAddress == null || nameStartAddress.get() > reader.getIndex())) { // Read entries until we reach file-names.
            int nameOffset = reader.readInt();

            FroggerEXEInfo exe = getConfig();
            FileEntry entry = new FileEntry(exe, loadingId++);

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

            if (exe.isPostMediEvil()) { // Discard checksum from post-MediEvil MWIs.
                reader.skipInt();
            }

            getEntries().add(entry);
        }

        // 84 WADS. All have Type ID -1, Flag: 20 (Automatic compression and is group) Type = STD, Manual handling.
        // 82 MAPS. All have Type ID 0, Flags: 33 (Manual Compression with single access)
        // 44 VLOS. All have Type ID 1, Flag: 17 (Automatic compression and is group)
        // 20 VB/VH All have Type ID 2, Flag: 1 (Single Access)
        // 90 ENTITY All are Type ID 3, Flag: 2 (Group Access) MAP_MOF?
        // 344 ENTITY_WIN95  Type ID 4, Flag: 2 (Group Access) MAP_ANIM_MOF?
        // Type ID 5 doesn't exist. According to the code, it was a SPU file, which appears to have been a sound file type.
        // 42  DAT. All have Type ID 6, Flag: 1 (Single Access)
        // 40  PAL. All have Type ID 7, Flag: 1 (Single Access)
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
    public static class FileEntry {
        private int flags; // Is a group? In a group? Compressed?
        private int typeId; //
        private int sectorOffset; // The file's starting address in the MWD.
        private int packedSize;
        private int unpackedSize;
        private String filePath;
        private String sha1Hash;
        private transient int resourceId;
        private transient FroggerEXEInfo config;

        public static final int FLAG_SINGLE_ACCESS = Constants.BIT_FLAG_0; // I assume this is for files loaded individually, by themselves.
        public static final int FLAG_GROUP_ACCESS = Constants.BIT_FLAG_1; // Cannot be loaded individually / by itself. Presumably this is for files in child-WADs.
        public static final int FLAG_IS_GROUP = Constants.BIT_FLAG_2; // Presumably this marks a child archive, where the next entries belong to this file.
        public static final int FLAG_ABSOLUTE_PATH = Constants.BIT_FLAG_3; // Appears to let you use absolute file paths instead of relative paths. However, this is functionality is likely not in the retail build.
        public static final int FLAG_AUTOMATIC_COMPRESSION = Constants.BIT_FLAG_4;
        public static final int FLAG_MANUAL_COMPRESSION = Constants.BIT_FLAG_5;

        public FileEntry(FroggerEXEInfo config, int resourceId) {
            this.resourceId = resourceId;
            this.config = config;
        }

        /**
         * Gets the type id, but allow changing the type id in case it's wrong.
         * @return spoofedTypeId
         */
        public int getSpoofedTypeId() {
            int id = this.typeId;
            if (id == 0 && getDisplayName().endsWith(".VLO"))
                id = VLOArchive.TYPE_ID;
            if (id == 0 && (getDisplayName().endsWith(".VH") || getDisplayName().endsWith(".VB")))
                id = VHFile.TYPE_ID;
            return id;
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
            } else if (getConfig().getFileNames().size() > this.resourceId) {
                return getConfig().getFileNames().get(this.resourceId);
            } else {
                return null;
            }
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
            for (MapBook book : getConfig().getMapLibrary())
                if (book != null && book.isEntry(this))
                    return book;
            return null;
        }
    }
}