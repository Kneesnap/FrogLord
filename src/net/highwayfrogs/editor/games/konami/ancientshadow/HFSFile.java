package net.highwayfrogs.editor.games.konami.ancientshadow;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileSystem;
import net.highwayfrogs.editor.games.konami.hudson.file.HudsonRwStreamFile;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionTreeViewComponent.CollectionViewTreeNode;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Represents the HFS file format.
 * Created by Kneesnap on 6/7/2020.
 */
@Getter
public class HFSFile extends HudsonGameFile implements IHudsonFileSystem {
    private final List<List<HudsonGameFile>> hfsFiles = new ArrayList<>();
    public static final String SIGNATURE = "hfs\n"; // Version 11?

    public HFSFile(IHudsonFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public void load(DataReader reader) {
        load(reader, null);
    }

    @Override
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
            List<HudsonGameFile> localFileGroup = new ArrayList<>(header.getFileEntries().size());
            header.requireReaderIndex(reader, header.getFileDataStartAddress(), "Expected file data start position");

            // Read files.
            for (int j = 0; j < header.getFileEntries().size(); j++) {
                HFSHeaderFileEntry fileEntry = header.getFileEntries().get(j);
                IHudsonFileDefinition fileDefinition = new HFSFileDefinition(this, i, j);

                // Read file contents.
                fileEntry.requireReaderIndex(reader, header.getFileDataStartAddress() + (fileEntry.getCdSector() * Constants.CD_SECTOR_SIZE), "Expected file data");
                HudsonGameFile newGameFile = getGameInstance().readGameFile(reader, fileEntry, fileDefinition, progressBar);
                reader.alignRequireEmpty(Constants.CD_SECTOR_SIZE);
                localFileGroup.add(newGameFile);
            }

            this.hfsFiles.add(localFileGroup);
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.save(writer, null);
    }

    @Override
    public void save(DataWriter writer, ProgressBarComponent progressBar) {
        // Create HFS headers.
        int fullFileCount = 0;
        List<HFSHeader> headers = new ArrayList<>();
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            List<HudsonGameFile> gameFiles = this.hfsFiles.get(i);

            HFSHeader newHeader = new HFSHeader(this);
            for (int j = 0; j < gameFiles.size(); j++)
                newHeader.getFileEntries().add(new HFSHeaderFileEntry(newHeader));
            fullFileCount += newHeader.getFileEntries().size();

            headers.add(newHeader);
        }

        // Write HFS headers. (Writes invalid data, but creates the space which will later contain the correct values.)
        if (progressBar != null)
            progressBar.update(0, fullFileCount, "Saving '" + getDisplayName() + "'...");

        int headerStartIndex = writer.getIndex();
        writeHeaders(writer, headers);

        // Write game files.
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            List<HudsonGameFile> gameFiles = this.hfsFiles.get(i);
            HFSHeader header = headers.get(i);
            header.fileDataStartAddress = writer.getIndex();

            // Write file contents.
            for (int j = 0; j < gameFiles.size(); j++) {
                HudsonGameFile gameFile = gameFiles.get(j);
                HFSHeaderFileEntry fileEntry = header.getFileEntries().get(j);

                fileEntry.cdSectorWithFlags = (writer.getIndex() - header.fileDataStartAddress) / Constants.CD_SECTOR_SIZE;
                if (!getGameInstance().saveGameFile(writer, gameFile, fileEntry, progressBar))
                    return;

                writer.align(Constants.CD_SECTOR_SIZE);
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
    }*/ // TODO: IMPLEMENT. (Share UI options with a RenderWareStream list?)

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.ZIPPED_FOLDER_15.getFxImage();
    }

    @Override
    public List<HudsonGameFile> getGameFiles() {
        List<HudsonGameFile> gameFiles = new ArrayList<>();
        for (int i = 0; i < this.hfsFiles.size(); i++)
            gameFiles.addAll(this.hfsFiles.get(i));

        return gameFiles;
    }

    @Override
    public void export(File exportFolder) {
        File filesExportDir = new File(exportFolder, "Files [" + getDisplayName() + "]");
        Utils.makeDirectory(filesExportDir);

        for (int i = 0; i < this.hfsFiles.size(); i++) {
            File groupFolder = new File(filesExportDir, "GROUP" + String.format("%02d", i));
            List<HudsonGameFile> groupFiles = this.hfsFiles.get(i);
            Utils.makeDirectory(groupFolder);

            for (int j = 0; j < groupFiles.size(); j++) {
                File outputFile = new File(groupFolder, "FILE" + String.format("%03d", j));

                try {
                    Files.write(outputFile.toPath(), groupFiles.get(j).getRawData());
                } catch (IOException ex) {
                    Utils.handleError(getLogger(), ex, false, "Failed to export file '%s'.", Utils.toLocalPath(exportFolder, outputFile, true));
                }
            }
        }

        File imagesExportDir = new File(exportFolder, "Images [" + getDisplayName() + "]");
        Utils.makeDirectory(imagesExportDir);

        Map<String, AtomicInteger> nameCountMap = new HashMap<>();
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            File groupFolder = new File(filesExportDir, "GROUP" + String.format("%02d", i));
            List<HudsonGameFile> groupFiles = this.hfsFiles.get(i);

            for (int j = 0; j < groupFiles.size(); j++) {
                HudsonGameFile gameFile = groupFiles.get(j);
                if (gameFile instanceof HudsonRwStreamFile)
                    ((HudsonRwStreamFile) gameFile).exportTextures(groupFolder, nameCountMap);
            }
        }
    }

    /**
     * Represents an HFS header chunk.
     */
    @Getter
    public static class HFSHeader extends GameData<HudsonGameInstance> {
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
            reader.verifyString(SIGNATURE);
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
            writer.writeStringBytes(SIGNATURE);
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
    public static class HFSHeaderFileEntry extends net.highwayfrogs.editor.games.konami.hudson.HFSHeaderFileEntry {
        private final HFSHeader parent;

        public HFSHeaderFileEntry(HFSHeader parent) {
            super(parent.getGameInstance());
            this.parent = parent;
        }

        /**
         * Gets the logger string.
         */
        public String getLoggerString() {
            return this.parent != null ? this.parent.getLoggerString() + "/FileEntry{" + Utils.getLoadingIndex(this.parent.fileEntries, this) + "}" : Utils.getSimpleName(this);
        }

        @Override
        public Logger getLogger() {
            return this.parent != null ? Logger.getLogger(getLoggerString()) : super.getLogger();
        }
    }

    @Getter
    public static class HFSFileDefinition extends GameObject<HudsonGameInstance> implements IHudsonFileDefinition, Comparable<HFSFileDefinition> {
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
            return getNameSuffix(false);
        }

        @Override
        public String getFullFileName() {
            return this.hfsFile.getFullDisplayName() + getNameSuffix(true);
        }

        @Override
        public CollectionViewTreeNode<HudsonGameFile> getOrCreateTreePath(CollectionViewTreeNode<HudsonGameFile> rootNode, HudsonGameFile gameFile) {
            IHudsonFileDefinition fileDefinition = this.hfsFile.getFileDefinition();
            CollectionViewTreeNode<HudsonGameFile> hfsNode = fileDefinition != null ? fileDefinition.getOrCreateTreePath(rootNode, this.hfsFile) : rootNode;
            if (this.groupIndex != 0 || this.hfsFile.getHfsFiles().size() > 1)
                hfsNode = hfsNode.getOrCreateChildNode("subHfsFile=" + this.groupIndex);

            return hfsNode.addChildNode(gameFile);
        }

        private String getNameSuffix(boolean includeGroup) {
            if (!includeGroup || (this.groupIndex == 0 && this.hfsFile.getHfsFiles().size() == 1)) {
                return "{file=" + this.fileIndex + "}";
            } else {
                return "{group=" + this.groupIndex + ",file=" + this.fileIndex + "}";
            }
        }

        @Override
        public int compareTo(HFSFileDefinition other) {
            if (other == null)
                return 1;

            int value = Integer.compare(this.groupIndex, other.groupIndex);
            if (value != 0)
                return value;

            return Integer.compare(this.fileIndex, other.fileIndex);
        }
    }
}