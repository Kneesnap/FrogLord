package net.highwayfrogs.editor.file;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Medieval Wad Index: Holds information about the MWD file.
 * Located in frogger.exe
 * This should always export exactly the same size as the original MWI, as this gets pasted directly in the executable.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class MWIFile extends GameObject {
    private List<FileEntry> entries = new ArrayList<>();
    private int fileSize;

    private static final int ENTRY_LENGTH = 32;
    private static final int STR_TERMINATOR = 4;

    @Override
    public void load(DataReader reader) {
        this.fileSize = reader.getSize();
        AtomicInteger nameStartAddress = null;

        while (nameStartAddress == null || nameStartAddress.get() > reader.getIndex()) { // Read entries until we reach file-names.
            int nameOffset = reader.readInt();
            if (nameStartAddress == null) // Use the first name address as the address which starts the name table.
                nameStartAddress = new AtomicInteger(nameOffset);

            FileEntry entry = new FileEntry();
            reader.jumpTemp(nameOffset);
            entry.setFilePath(reader.readNullTerminatedString());
            reader.jumpReturn();

            entry.setFlags(reader.readInt());
            entry.setTypeId(reader.readInt());
            entry.setSectorOffset(reader.readInt());
            entry.setLoadedAddress(reader.readInt());
            entry.setDepackedAddress(reader.readInt());
            entry.setPackedSize(reader.readInt());
            entry.setUnpackedSize(reader.readInt());
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
            writer.writeInt(nameOffset);
            int pathByteLength = entry.getFilePath().getBytes().length;
            nameOffset += pathByteLength; // The amount of bytes written.
            nameOffset += getNullCount(pathByteLength); // The terminator bytes predicts the offset it actually goes.

            writer.writeInt(entry.getFlags());
            writer.writeInt(entry.getTypeId());
            writer.writeInt(entry.getSectorOffset());
            writer.writeInt(entry.getLoadedAddress());
            writer.writeInt(entry.getDepackedAddress());
            writer.writeInt(entry.getPackedSize());
            writer.writeInt(entry.getUnpackedSize());
        }

        getEntries().stream()
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
        private int loadedAddress; // Should always be 0. This is used by the frogger.exe to locate where in RAM the file is located.
        private int depackedAddress; // Should always be 0. This is used by frogger.exe to locate where in RAM the file is depacked at.
        private int packedSize;
        private int unpackedSize;
        private String filePath;

        public static final int FLAG_SINGLE_ACCESS = 1; // I assume this is for files loaded individually, by themselves.
        public static final int FLAG_GROUP_ACCESS = 2; // Cannot be loaded individually / by itself. Presumably this is for files in child-WADs.
        public static final int FLAG_IS_GROUP = 4; // Presumably this marks a child archive, where the next entries belong to this file.
        public static final int FLAG_ABSOLUTE_PATH = 8; // Appears to let you use absolute file paths instead of relative paths. However, this is functionality is likely not in the retail build.
        public static final int FLAG_AUTOMATIC_COMPRESSION = 16;
        public static final int FLAG_MANUAL_COMPRESSION = 32;

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
         * Test if a flag is valid for this entry.
         * @param flag The flag to test.
         * @return isFlagPresent
         */
        public boolean testFlag(int flag) {
            return (this.flags & flag) == flag;
        }

        @Override
        public String toString() {
            return "{" + getFilePath() + " Type: " + getTypeId() + ", Flags: " + getFlags() + ", Unpacked Size: "
                    + getUnpackedSize() + ", " + (isCompressed() ? "" : "Not ") + "Compressed}";
        }
    }
}
