package net.highwayfrogs.editor.games.sony.shared.mwd.mwi;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.packers.PP20Packer.PackResult;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.*;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Represents an entry in the MWI.
 * Created by Kneesnap on 7/18/2024.
 */
public class MWIResourceEntry extends SCSharedGameData implements ISCFileDefinition {
    @Getter private final int resourceId;
    @Getter private int flags; // Is a group? In a group? Compressed?
    @Getter private int typeId; // A numerical ID which is supposed to indicate what kind of file the entry is, but in practice it's a little messy.
    @Getter @Setter private int sectorOffset; // The file's starting address in the MWD.
    @Getter private int packedSize; // The size of the file while compressed.
    @Getter private int unpackedSize; // The size of the file after it is decompressed.
    @Getter private int safetyMarginWordCount; // This is automatically calculated, and is required to make in-place decompression work. Basically, we calculate the minimum distance between the compressed data read pointer and the uncompressed data write pointer needed to ensure they never overlap.
    @Getter @Setter private int checksum;
    @Getter private String filePath;
    @Getter private String sha1Hash;
    private transient Logger logger;
    transient int filePathPointerAddress = NO_FILE_NAME_MARKER;

    // This flag set seems to be consistent.
    public static final int FLAG_SINGLE_ACCESS = Constants.BIT_FLAG_0; // I assume this is for files loaded individually, by themselves.
    public static final int FLAG_GROUP_ACCESS = Constants.BIT_FLAG_1; // Cannot be loaded individually / by itself. Presumably this is for files in child-WADs.
    public static final int FLAG_IS_GROUP = Constants.BIT_FLAG_2; // Presumably this marks a child archive, where the next entries belong to this file.
    public static final int FLAG_ABSOLUTE_PATH = Constants.BIT_FLAG_3; // Appears to let you use absolute file paths instead of relative paths. However, this is functionality appears disabled when CD routines are enabled. Added September 13, 1996.
    public static final int FLAG_AUTOMATIC_COMPRESSION = Constants.BIT_FLAG_4;
    public static final int FLAG_MANUAL_COMPRESSION = Constants.BIT_FLAG_5;
    public static final int OLD_FORMAT_VALIDATION_MASK = 0b1111;
    public static final int FLAG_VALIDATION_MASK = 0b111111;
    public static final int NO_FILE_NAME_MARKER = 0xFFFFFFFF;

    // Byte Sizes:
    // Old Frogger: 64
    // 1997: 32
    // 1999: 36
    private static final int OLD_FIXED_FILE_PATH_LENGTH = 40;

    public MWIResourceEntry(SCGameInstance instance, int resourceId) {
        super(instance);
        this.resourceId = resourceId;
    }

    @Override
    public void load(DataReader reader) {
        this.filePathPointerAddress = NO_FILE_NAME_MARKER;
        this.checksum = 0;

        int unpackedSizeWithSafetyMargin;
        if (isOldFormat()) {
            this.filePath = reader.readNullTerminatedFixedSizeString(OLD_FIXED_FILE_PATH_LENGTH);
            this.flags = reader.readInt();
            warnAboutInvalidBitFlags(this.flags, OLD_FORMAT_VALIDATION_MASK);
            this.typeId = reader.readInt();
            this.sectorOffset = reader.readInt();
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Not sure, but appears always empty in Old Frogger.
            unpackedSizeWithSafetyMargin = reader.readInt();
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Not sure, but appears always empty in Old Frogger.
        } else {
            this.filePathPointerAddress = reader.readInt();
            this.flags = reader.readInt();
            warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);
            this.typeId = reader.readInt();
            this.sectorOffset = reader.readInt();
            reader.skipBytesRequireEmpty(2 * Constants.POINTER_SIZE); // Runtime pointers, always zero.
            this.packedSize = reader.readInt();
            unpackedSizeWithSafetyMargin = reader.readInt();
            if (hasChecksum())
                this.checksum = reader.readInt();
        }

        this.safetyMarginWordCount = (unpackedSizeWithSafetyMargin >>> 24);
        this.unpackedSize = (unpackedSizeWithSafetyMargin & 0xFFFFFF);
    }

    /**
     * Verify the bit flags don't appear to have unusual combinations.
     */
    public void validateFlags() {
        if (testFlag(FLAG_ABSOLUTE_PATH)) // This most likely isn't used.
            getLogger().info("Entry has the ABSOLUTE_PATH flag, which was thought to be unused.");
        if (testFlag(FLAG_MANUAL_COMPRESSION) && testFlag(FLAG_AUTOMATIC_COMPRESSION))
            getLogger().warning("Entry has BOTH the manual compression flag and the automatic compression flag.");
        if (testFlag(FLAG_IS_GROUP) && (testFlag(FLAG_SINGLE_ACCESS) || testFlag(FLAG_GROUP_ACCESS))) {
            getLogger().warning("Entry has group indicator flag set in addition in addition to an incompatible flag. (Single Access: " + testFlag(FLAG_SINGLE_ACCESS) + ", " + testFlag(FLAG_GROUP_ACCESS) + ")");
        } else if (testFlag(FLAG_SINGLE_ACCESS) && testFlag(FLAG_GROUP_ACCESS)) {
            getLogger().warning("Entry has BOTH the single access flag and the group access flag set.");
        }
    }

    /**
     * Reads the file path from the current reader position, if the MWI file path reading is queued.
     * @param reader the reader to read the file path from.
     */
    void readFilePath(DataReader reader) {
        if (this.filePathPointerAddress == NO_FILE_NAME_MARKER) {
            if (!isOldFormat())
                this.filePath = null;

            validateFlags(); // Warn about weird flags. We do it here since we prefer to do it once we have file names.
            return; // No address to write the file name pointer to.
        }

        reader.requireIndex(getLogger(), this.filePathPointerAddress, "Expected file path");
        this.filePath = reader.readNullTerminatedString();
        this.filePathPointerAddress = NO_FILE_NAME_MARKER;
        reader.align(Constants.INTEGER_SIZE);
        validateFlags(); // Warn about weird flags. We do it here since we prefer to do it once we have file names.
    }

    @Override
    public void save(DataWriter writer) {
        this.filePathPointerAddress = NO_FILE_NAME_MARKER;
        if (isOldFormat()) {
            writer.writeNullTerminatedFixedSizeString(this.filePath != null ? this.filePath : "", OLD_FIXED_FILE_PATH_LENGTH);
            writer.writeInt(this.flags);
            writer.writeInt(this.typeId);
            writer.writeInt(this.sectorOffset);
            writer.writeNull(Constants.INTEGER_SIZE); // Not sure, but appears always empty in Old Frogger.
            writer.writeInt(getUnpackedSizeWithSafetyMargin());
            writer.writeNull(Constants.INTEGER_SIZE); // Not sure, but appears always empty in Old Frogger.
        } else {
            if (hasFilePath()) {
                this.filePathPointerAddress = writer.writeNullPointer();
            } else {
                writer.writeInt(NO_FILE_NAME_MARKER);
            }

            writer.writeInt(this.flags);
            writer.writeInt(this.typeId);
            writer.writeInt(this.sectorOffset);
            writer.writeNull(2 * Constants.POINTER_SIZE); // Runtime pointers.
            writer.writeInt(this.packedSize);
            writer.writeInt(getUnpackedSizeWithSafetyMargin());
            if (hasChecksum())
                writer.writeInt(this.checksum);
        }
    }

    private int getUnpackedSizeWithSafetyMargin() {
        int result = this.unpackedSize;
        if (isCompressed())
            result |= (this.safetyMarginWordCount << 24);

        return result;
    }

    /**
     * Writes the file path to the current writer position, if the MWI file path writing is queued.
     * @param writer the writer to write the file path to.
     */
    void writeFilePath(DataWriter writer) {
        if (this.filePathPointerAddress == NO_FILE_NAME_MARKER)
            return; // No address to write the file name pointer to.

        if (!hasFilePath()) {
            writer.writeIntAtPos(this.filePathPointerAddress, NO_FILE_NAME_MARKER);
            this.filePathPointerAddress = NO_FILE_NAME_MARKER;
            return;
        }

        // Write the current address to the file path location.
        writer.writeAddressTo(this.filePathPointerAddress);
        this.filePathPointerAddress = NO_FILE_NAME_MARKER;
        writer.writeTerminatorString(this.filePath);
        writer.align(Constants.INTEGER_SIZE);
    }

    @Override
    public Logger getLogger() {
        if (this.logger != null)
            return this.logger;

        return this.logger = Logger.getLogger(getLoggerString());
    }

    @Override
    public String getLoggerString() {
        return getDisplayName();
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
        return this.sectorOffset * Constants.CD_SECTOR_SIZE;
    }

    /**
     * Determines if this entry is compressed.
     * @return isCompressed
     */
    public boolean isCompressed() {
        return testFlag(FLAG_AUTOMATIC_COMPRESSION) || testFlag(FLAG_MANUAL_COMPRESSION);
    }

    /**
     * Does this MWI entry have an associated file path?
     * @return hasFilePath
     */
    public boolean hasFilePath() {
        return this.filePath != null && !this.filePath.isEmpty();
    }

    @Override
    public String getFullFilePath() {
        if (hasFilePath()) {
            return this.filePath;
        } else if (getConfig().getFallbackFileNames().size() > this.resourceId) {
            return getConfig().getFallbackFileNames().get(this.resourceId);
        } else {
            return null;
        }
    }

    @Override
    public MWIResourceEntry getIndexEntry() {
        return this;
    }

    @Override
    public String getDisplayName() {
        String fileName = getFullFilePath();
        if (fileName == null)
            return "File " + this.resourceId;

        fileName = fileName.substring(fileName.lastIndexOf("\\") + 1); // Remove \ paths.
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1); // Remove / paths.
        return fileName;
    }

    @Override
    public SCGameFile<?> getGameFile() {
        return getGameInstance().getGameFile(this);
    }

    /**
     * Test if a flagMask is set for this entry.
     * @param flagMask The flag bit mask to test.
     * @return isFlagMaskPresent
     */
    public boolean testFlag(int flagMask) {
        return (this.flags & flagMask) == flagMask;
    }

    @Override
    public String toString() {
        return getDisplayName() + "-{" + getFilePath() + " Type: " + getTypeId() + ", Flags: " + getFlags() + "}";
    }

    /**
     * Returns true iff the MWI entry uses the "old format".
     * The "old format" is currently defined as the format seen in pre-recode Frogger.
     * This could theoretically extend to 1996/early 1997 prototypes of MediEvil, Beast Wars, or Head Rush.
     * No other games
     */
    public boolean isOldFormat() {
        return getGameInstance().getGameType().isAtOrBefore(SCGameType.OLD_FROGGER);
    }

    /**
     * Returns true iff the MWI resource entry has a checksum field.
     */
    public boolean hasChecksum() {
        return getGameInstance().getGameType().doesMwiHaveChecksum();
    }

    /**
     * Called when we load the resource data.
     * @param fileBytes the file bytes to load
     * @param compressedFileBytes the compressed file bytes to load from, if the file was compressed
     * @param safetyMarginWordCount the safety margin word count calculated during decompression
     */
    public void onLoadData(byte[] fileBytes, byte[] compressedFileBytes, int safetyMarginWordCount) {
        if (hasChecksum() && this.checksum != 0) {
            byte[] rawBytes = compressedFileBytes != null ? compressedFileBytes : fileBytes;
            int calculatedChecksum = SCUtils.calculateChecksum(rawBytes);
            if (calculatedChecksum != this.checksum)
                getLogger().warning("Checksum Mismatch!! MWI Checksum: " + NumberUtils.toHexString(this.checksum) + ", Calculated Checksum: " + NumberUtils.toHexString(calculatedChecksum) + " [Compressed: " + isCompressed() + "]");
        }

        if (safetyMarginWordCount != this.safetyMarginWordCount)
            getLogger().warning("Safety Margin mismatch!! Read Safety Margin: " + this.safetyMarginWordCount + ", Calculated Safety Margin: " + safetyMarginWordCount);

        // Calculate the SHA1 hash.
        if (FroggerVersionComparison.isEnabled() && this.sha1Hash == null)
            this.sha1Hash = Utils.calculateSHA1Hash(fileBytes);
    }

    /**
     * Called when we save the resource data.
     * @param fileBytes the raw resource data to save
     * @param packResult the compressed resource data to save, if compression has occurred
     */
    public void onSaveData(byte[] fileBytes, PackResult packResult) {
        if (hasChecksum() && this.checksum != 0) {
            byte[] rawBytes = packResult != null ? packResult.getPackedBytes() : fileBytes;
            this.checksum = SCUtils.calculateChecksum(rawBytes);
        }

        this.unpackedSize = fileBytes.length;
        this.packedSize = (packResult != null) ? packResult.getPackedBytes().length : fileBytes.length;
        this.safetyMarginWordCount = packResult != null ? packResult.getSafetyMarginWordCount() : 0;
    }
}
