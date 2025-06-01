package net.highwayfrogs.editor.games.konami.rescue;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.hudson.HFSHeaderFileEntry;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.konami.hudson.file.HudsonRwStreamFile;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.IVirtualFileSystem;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionTreeViewComponent.CollectionViewTreeNode;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents the HFS file format.
 * Created by Kneesnap on 6/7/2020.
 */
@Getter
public class HFSFile extends HudsonGameFile implements IVirtualFileSystem {
    private final List<HudsonGameFile> hfsFiles = new ArrayList<>();
    public static final String SIGNATURE = "hfs\7"; // Version 7

    public HFSFile(IGameFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public void load(DataReader reader) {
        load(reader, null);
    }

    @Override
    public void load(DataReader reader, ProgressBarComponent progressBar) {
        reader.verifyString(SIGNATURE);
        int fullFileSize = reader.readInt();
        int fileEntryCount = reader.readInt();
        int unknownMightBeZero = reader.readInt();

        // Validation.
        if (fullFileSize != reader.getSize())
            throw new RuntimeException("Read file size did not match real file size! (Read: " + fullFileSize + ", Real: " + reader.getSize() + ")");
        if (unknownMightBeZero != 0)
            throw new RuntimeException("Expected the third value to always be zero, but this file had " + unknownMightBeZero + " instead.");
        if (progressBar != null)
            progressBar.update(0, fileEntryCount, "Reading '" + getDisplayName() + "'...");

        // Read file entries.
        List<HFSHeaderFileEntry> fileEntries = new ArrayList<>();
        for (int i = 0; i < fileEntryCount; i++) {
            HFSHeaderFileEntry fileEntry = new HFSHeaderFileEntry(getGameInstance());
            fileEntries.add(fileEntry);
            fileEntry.load(reader);
        }

        // Read file contents.
        reader.align(Constants.CD_SECTOR_SIZE);
        for (int i = 0; i < fileEntries.size(); i++) {
            HFSHeaderFileEntry fileEntry = fileEntries.get(i);
            IGameFileDefinition fileDefinition = new HFSFileDefinition(this, i);

            // Read data.
            fileEntry.requireReaderIndex(reader, (fileEntry.getCdSector() * Constants.CD_SECTOR_SIZE), "Expected file data");
            HudsonGameFile newGameFile = getGameInstance().readGameFile(reader, fileEntry, fileDefinition, progressBar);
            this.hfsFiles.add(newGameFile);
            reader.alignRequireEmpty(Constants.CD_SECTOR_SIZE);
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.save(writer, null);
    }

    @Override
    public void save(DataWriter writer, ProgressBarComponent progressBar) {
        if (progressBar != null)
            progressBar.update(0, this.hfsFiles.size(), "Saving '" + getDisplayName() + "'...");

        // 1) Save header.
        writer.writeStringBytes(SIGNATURE);
        int fullFileSizePtr = writer.writeNullPointer();
        writer.writeInt(this.hfsFiles.size());
        writer.writeInt(0); // Change this if it's not always zero.

        // 2) Write file entries.
        int fileEntryStartIndex = writer.getIndex();
        List<HFSHeaderFileEntry> fileEntries = new ArrayList<>();
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            HFSHeaderFileEntry newEntry = new HFSHeaderFileEntry(getGameInstance());
            fileEntries.add(newEntry);
            newEntry.save(writer);
        }

        // 3) Write game files.
        writer.align(Constants.CD_SECTOR_SIZE);
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            HudsonGameFile gameFile = this.hfsFiles.get(i);
            HFSHeaderFileEntry fileEntry = fileEntries.get(i);

            fileEntry.cdSectorWithFlags = writer.getIndex() / Constants.CD_SECTOR_SIZE;
            if (!getGameInstance().saveGameFile(writer, gameFile, fileEntry, progressBar))
                return;

            writer.align(Constants.CD_SECTOR_SIZE);
        }

        // 4) Update file entries to include the new sizes.
        writer.jumpTemp(fileEntryStartIndex);
        for (int i = 0; i < this.hfsFiles.size(); i++)
            fileEntries.get(i).save(writer);
        writer.jumpReturn();

        // 5) Write final file size.
        writer.writeAddressTo(fullFileSizePtr);
    }

    /*@Override
    public GameUIController<?> makeEditorUI() {
        return null;
    }*/ // TODO: IMPLEMENT. (Share UI options with a RenderWareStream list?)

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.ZIPPED_FOLDER_16.getFxImage();
    }

    @Override
    public List<HudsonGameFile> getGameFiles() {
        return this.hfsFiles;
    }

    @Override
    public void export(File exportFolder) {
        File rawFilesFolder = new File(exportFolder, "Raw Files");
        FileUtils.makeDirectory(rawFilesFolder);

        for (int i = 0; i < this.hfsFiles.size(); i++) {
            File outputFile = new File(rawFilesFolder, "FILE" + String.format("%03d", i));

            try {
                Files.write(outputFile.toPath(), this.hfsFiles.get(i).getRawData());
            } catch (IOException ex) {
                Utils.handleError(getLogger(), ex, false, "Failed to export file '%s'.", FileUtils.toLocalPath(exportFolder, outputFile, true));
            }
        }

        File imagesFolder = new File(exportFolder, "Images");
        Map<String, AtomicInteger> nameCountMap = new HashMap<>();
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            HudsonGameFile gameFile = this.hfsFiles.get(i);
            if (gameFile instanceof HudsonRwStreamFile)
                ((HudsonRwStreamFile) gameFile).getRwStreamFile().exportTextures(imagesFolder, nameCountMap);
        }

        // Export others.
        for (int i = 0; i < this.hfsFiles.size(); i++)
            this.hfsFiles.get(i).export(exportFolder);
    }

    @Getter
    public static class HFSFileDefinition extends GameObject<HudsonGameInstance> implements IGameFileDefinition, Comparable<HFSFileDefinition> {
        private final HFSFile hfsFile;
        private final int fileIndex;

        public HFSFileDefinition(HFSFile hfsFile, int fileIndex) {
            super(hfsFile.getGameInstance());
            this.hfsFile = hfsFile;
            this.fileIndex = fileIndex;
        }

        @Override
        public String getFileName() {
            return "{file=" + this.fileIndex + "}";
        }

        @Override
        public String getFullFilePath() {
            return this.hfsFile.getFullDisplayName() + getFileName();
        }

        @Override
        public File getFile() {
            return null; // This is virtual and returns no file.
        }

        @Override
        public CollectionViewTreeNode<BasicGameFile<?>> getOrCreateTreePath(CollectionViewTreeNode<BasicGameFile<?>> rootNode, BasicGameFile<?> gameFile) {
            IGameFileDefinition fileDefinition = this.hfsFile.getFileDefinition();
            CollectionViewTreeNode<BasicGameFile<?>> hfsNode = fileDefinition != null ? fileDefinition.getOrCreateTreePath(rootNode, this.hfsFile) : rootNode;
            return hfsNode.addChildNode(gameFile);
        }

        @Override
        public int compareTo(HFSFileDefinition other) {
            if (other == null)
                return 1;

            return Integer.compare(this.fileIndex, other.fileIndex);
        }
    }
}