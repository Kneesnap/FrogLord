package net.highwayfrogs.editor.games.konami.ancientshadow;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.games.konami.ancientshadow.file.AncientShadowDummyFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonFileUserFSDefinition;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.games.konami.rescue.PRS1Unpacker;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents the HFS file format.
 * Created by Kneesnap on 6/7/2020.
 */
@Getter
public class HFSFile extends AncientShadowGameFile {
    private final List<List<AncientShadowGameFile>> hfsFiles = new ArrayList<>();
    private static final String MAGIC = "hfs\n";

    public HFSFile(HudsonFileUserFSDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public void load(DataReader reader) {
        load(reader, null);
    }

    /**
     * Loads the contents of the HFS file while updating the progress bar.
     * @param reader the reader to read data from
     * @param progressBar the progress bar to update, if not null
     */
    public void load(DataReader reader, ProgressBarComponent progressBar) {
        // Read HFS headers.
        int fullFileCount = 0;
        int hfsFileDataStartAddress = Integer.MAX_VALUE;
        List<HFSHeader> headers = new ArrayList<>();
        while (hfsFileDataStartAddress > reader.getIndex()) {
            HFSHeader newHeader = new HFSHeader(this);
            newHeader.load(reader);
            if (hfsFileDataStartAddress > newHeader.getFileDataStartAddress())
                hfsFileDataStartAddress = newHeader.getFileDataStartAddress();

            headers.add(newHeader);
            fullFileCount += newHeader.getFileEntries().size();
            reader.alignRequireEmpty(Constants.CD_SECTOR_SIZE); // Each HFS header seems padded
        }

        // Read file entries.
        if (progressBar != null)
            progressBar.update(0, fullFileCount, "Reading '" + getDisplayName() + "'...");
        this.hfsFiles.clear();
        for (int i = 0; i < headers.size(); i++) {
            HFSHeader header = headers.get(i);
            List<AncientShadowGameFile> localFileGroup = new ArrayList<>(header.getFileEntries().size());
            header.requireReaderIndex(reader, header.getFileDataStartAddress(), "Expected file data start position");

            // Read files.
            for (int j = 0; j < header.getFileEntries().size(); j++) {
                HFSHeaderFileEntry fileEntry = header.getFileEntries().get(j);

                // Setup new file.
                AncientShadowGameFile newGameFile = new AncientShadowDummyFile(new HFSFileDefinition(this, i, j));
                newGameFile.setCompressionEnabled(fileEntry.isCompressed());

                if (progressBar != null)
                    progressBar.setStatusMessage("Reading '" + newGameFile.getDisplayName() + "'...");

                // Read data.
                fileEntry.requireReaderIndex(reader, header.getFileDataStartAddress() + (fileEntry.getCdSector() * Constants.CD_SECTOR_SIZE), "Expected file data");
                byte[] rawFileData = reader.readBytes(fileEntry.getFileDataLength());
                byte[] readFileData = fileEntry.isCompressed() ? PRS1Unpacker.decompressPRS1(rawFileData) : rawFileData;
                newGameFile.setRawData(readFileData);
                DataReader fileReader = new DataReader(new ArraySource(readFileData));

                try {
                    // Load file.
                    newGameFile.load(fileReader);
                } catch (Exception ex) {
                    Utils.handleError(getLogger(), ex, true, "Failed to load '%s'.", newGameFile.getDisplayName());
                }

                localFileGroup.add(newGameFile);
                reader.alignRequireEmpty(Constants.CD_SECTOR_SIZE);

                if (progressBar != null)
                    progressBar.addCompletedProgress(1);
            }

            this.hfsFiles.add(localFileGroup);
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.save(writer, null);
    }

    /**
     * Saves the contents of the HFS file while updating the progress bar.
     * @param writer writer to write data to
     * @param progressBar the progress bar to update, if not null
     */
    public void save(DataWriter writer, ProgressBarComponent progressBar) {
        // Create HFS headers.
        int fullFileCount = 0;
        List<HFSHeader> headers = new ArrayList<>();
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            List<AncientShadowGameFile> gameFiles = this.hfsFiles.get(i);

            HFSHeader newHeader = new HFSHeader(this);
            for (int j = 0; j < gameFiles.size(); j++)
                newHeader.getFileEntries().add(new HFSHeaderFileEntry(newHeader));
            fullFileCount += newHeader.getFileEntries().size();

            headers.add(newHeader);
        }

        // Write HFS headers. (Writes invalid data, but creates the space which will later contain the correct values.)
        if (progressBar != null)
            progressBar.update(0, fullFileCount, "Reading '" + getDisplayName() + "'...");

        int headerStartIndex = writer.getIndex();
        writeHeaders(writer, headers);

        // Write game files.
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            List<AncientShadowGameFile> gameFiles = this.hfsFiles.get(i);
            HFSHeader header = headers.get(i);
            header.fileDataStartAddress = writer.getIndex();

            // Write file contents.
            for (int j = 0; j < gameFiles.size(); j++) {
                AncientShadowGameFile gameFile = gameFiles.get(j);
                HFSHeaderFileEntry fileEntry = header.getFileEntries().get(j);
                if (progressBar != null)
                    progressBar.setStatusMessage("Writing '" + gameFile.getDisplayName() + "'...");

                fileEntry.cdSectorWithFlags = (writer.getIndex() - header.fileDataStartAddress) / Constants.CD_SECTOR_SIZE;


                ArrayReceiver fileByteArray = new ArrayReceiver();
                DataWriter fileWriter = new DataWriter(fileByteArray);

                try {
                    gameFile.save(fileWriter);
                } catch (Throwable th) {
                    Utils.handleError(getLogger(), th, true, "Failed to save file '%s' to HFS.", gameFile.getDisplayName());
                    return;
                }

                byte[] writtenFileBytes = fileByteArray.toArray();
                if (gameFile.isCompressionEnabled()) {
                    // TODO: Add compression behavior, and apply the flag.
                    if (PRS1Unpacker.isCompressedPRS1(writtenFileBytes))
                        fileEntry.cdSectorWithFlags |= HFSHeaderFileEntry.FLAG_IS_COMPRESSED;
                }

                // Write file contents.
                fileEntry.fileDataLength = writtenFileBytes.length;
                writer.writeBytes(writtenFileBytes);
                writer.align(Constants.CD_SECTOR_SIZE);

                if (progressBar != null)
                    progressBar.addCompletedProgress(1);
            }

            header.fullFileSize = writer.getIndex() - header.fileDataStartAddress;
        }

        // Write valid HFS headers. (Writes invalid data, but gets the amount of data written correct.)
        writer.jumpTemp(headerStartIndex);
        writeHeaders(writer, headers);
        writer.jumpReturn();
    }

    private void writeHeaders(DataWriter writer, List<HFSHeader> headers) {
        for (int i = 0; i < headers.size(); i++) {
            headers.get(i).save(writer);
            writer.align(Constants.CD_SECTOR_SIZE);
        }
    }

    /*@Override
    public GameUIController<?> makeEditorUI() {
        return null;
    }*/ // TODO: IMPLEMENT.

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.ZIPPED_FOLDER_15.getFxImage();
    }

    /**
     * Gets all game files tracked within this HFS file.
     */
    public List<AncientShadowGameFile> getGameFiles() {
        List<AncientShadowGameFile> gameFiles = new ArrayList<>();
        for (int i = 0; i < this.hfsFiles.size(); i++)
            gameFiles.addAll(this.hfsFiles.get(i));

        return gameFiles;
    }

    /**
     * Represents an HFS header chunk.
     */
    @Getter
    public static class HFSHeader extends GameData<AncientShadowInstance> {
        private final HFSFile parent;
        private final List<HFSHeaderFileEntry> fileEntries = new ArrayList<>();
        private int fullFileSize = -1; // Amount of bytes covered by the file entries.
        private int fileDataStartAddress = -1;

        public HFSHeader(HFSFile parent) {
            super(parent != null ? parent.getGameInstance() : null);
            this.parent = parent;
        }

        @Override
        public void load(DataReader reader) {
            reader.verifyString(MAGIC);
            this.fullFileSize = reader.readInt();
            int fileEntryCount = reader.readInt();
            this.fileDataStartAddress = reader.readInt();

            // Read file entries.
            this.fileEntries.clear();
            for (int i = 0; i < fileEntryCount; i++) {
                HFSHeaderFileEntry fileEntry = new HFSHeaderFileEntry(this);
                this.fileEntries.add(fileEntry);
                fileEntry.load(reader);
            }
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeStringBytes(MAGIC);
            writer.writeInt(this.fullFileSize);
            writer.writeInt(this.fileEntries.size());
            writer.writeInt(this.fileDataStartAddress);

            // Write file entries.
            for (int i = 0; i < this.fileEntries.size(); i++)
                this.fileEntries.get(i).save(writer);
        }

        /**
         * Gets the logger string.
         */
        public String getLoggerString() {
            // TODO: Use new Utils indexOf method once it's been merged.
            return this.parent != null ? this.parent.getLoggerString() + "/HFSHeader{" + this.parent.hfsFiles.size() + "}" : Utils.getSimpleName(this);
        }

        @Override
        public Logger getLogger() {
            return this.parent != null ? Logger.getLogger(getLoggerString()) : super.getLogger();
        }
    }

    /**
     * Represents an HFS header file entry.
     */
    public static class HFSHeaderFileEntry extends GameData<AncientShadowInstance> {
        private final HFSHeader parent;
        private int cdSectorWithFlags = -1;
        @Getter private int fileDataLength = -1;

        public static final int CD_SECTOR_FLAG_MASK = 0xFFFFFF;
        public static final int FLAG_IS_COMPRESSED = Constants.BIT_FLAG_24;
        public static final int VALIDATION_FLAG_MASK = FLAG_IS_COMPRESSED | CD_SECTOR_FLAG_MASK; // Lower 24 bits are valid.

        public HFSHeaderFileEntry(HFSHeader parent) {
            super(parent.getGameInstance());
            this.parent = parent;
        }

        /**
         * Gets the logger string.
         */
        public String getLoggerString() {
            // TODO: Use new Utils indexOf method once it's been merged.
            return this.parent != null ? this.parent.getLoggerString() + "/FileEntry{" + this.parent.fileEntries.indexOf(this) + "}" : Utils.getSimpleName(this);
        }

        @Override
        public Logger getLogger() {
            return this.parent != null ? Logger.getLogger(getLoggerString()) : super.getLogger();
        }

        @Override
        public void load(DataReader reader) {
            this.cdSectorWithFlags = reader.readInt();
            this.fileDataLength = reader.readInt();
            warnAboutInvalidBitFlags(this.cdSectorWithFlags, VALIDATION_FLAG_MASK, "HFSHeaderFileEntry");
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.cdSectorWithFlags);
            writer.writeInt(this.fileDataLength);
        }

        /**
         * Returns the CD sector with the flags removed.
         */
        public int getCdSector() {
            return this.cdSectorWithFlags & CD_SECTOR_FLAG_MASK;
        }

        /**
         * Returns true iff compression is enabled for this entry.
         */
        public boolean isCompressed() {
            return (this.cdSectorWithFlags & FLAG_IS_COMPRESSED) == FLAG_IS_COMPRESSED;
        }
    }

    @Getter
    public static class HFSFileDefinition extends GameObject<AncientShadowInstance> implements IHudsonFileDefinition {
        private final HFSFile hfsFile;
        private final int groupIndex;
        private final int fileIndex;

        public HFSFileDefinition(HFSFile hfsFile, int groupIndex, int fileIndex) {
            super(hfsFile.getGameInstance());
            this.hfsFile = hfsFile;
            this.groupIndex = groupIndex;
            this.fileIndex = fileIndex;
        }

        @Override
        public String getFileName() {
            return this.hfsFile.getDisplayName() + "{group=" + this.groupIndex + ",file=" + this.fileIndex + "}";
        }

        @Override
        public String getFullFileName() {
            return this.hfsFile.getFullDisplayName() + "{group=" + this.groupIndex + ",file=" + this.fileIndex + "}";
        }
    }
}