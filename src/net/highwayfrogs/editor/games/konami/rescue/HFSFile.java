package net.highwayfrogs.editor.games.konami.rescue;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.hudson.*;
import net.highwayfrogs.editor.games.konami.hudson.file.HudsonRwStreamFile;
import net.highwayfrogs.editor.gui.ImageResource;
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

/**
 * Represents the HFS file format.
 * Created by Kneesnap on 6/7/2020.
 */
@Getter
public class HFSFile extends HudsonGameFile implements IHudsonFileSystem {
    private final List<HudsonGameFile> hfsFiles = new ArrayList<>();
    public static final String SIGNATURE = "hfs\7"; // Version 7

    public HFSFile(IHudsonFileDefinition fileDefinition) {
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
            IHudsonFileDefinition fileDefinition = new HFSFileDefinition(this, i);

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
    }*/ // TODO: IMPLEMENT.

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.ZIPPED_FOLDER_15.getFxImage();
    }

    @Override
    public List<HudsonGameFile> getGameFiles() {
        return this.hfsFiles;
    }

    @Override
    public void export(File exportFolder) {
        File filesExportDir = new File(exportFolder, "Files [" + getDisplayName() + "]");
        Utils.makeDirectory(filesExportDir);

        for (int i = 0; i < this.hfsFiles.size(); i++) {
            File outputFile = new File(filesExportDir, "FILE" + String.format("%03d", i));

            try {
                Files.write(outputFile.toPath(), this.hfsFiles.get(i).getRawData());
            } catch (IOException ex) {
                Utils.handleError(getLogger(), ex, false, "Failed to export file '%s'.", Utils.toLocalPath(exportFolder, outputFile, true));
            }
        }

        File imagesExportDir = new File(exportFolder, "Images [" + getDisplayName() + "]");
        Map<String, AtomicInteger> nameCountMap = new HashMap<>();
        for (int i = 0; i < this.hfsFiles.size(); i++) {
            HudsonGameFile gameFile = this.hfsFiles.get(i);
            if (gameFile instanceof HudsonRwStreamFile)
                ((HudsonRwStreamFile) gameFile).exportTextures(imagesExportDir, nameCountMap);
        }
    }

    @Getter
    public static class HFSFileDefinition extends GameObject<HudsonGameInstance> implements IHudsonFileDefinition {
        private final IHudsonFileSystem hfsFile;
        private final int fileIndex;

        public HFSFileDefinition(IHudsonFileSystem hfsFile, int fileIndex) {
            super(hfsFile.getGameInstance());
            this.hfsFile = hfsFile;
            this.fileIndex = fileIndex;
        }

        @Override
        public String getFileName() {
            return this.hfsFile.getDisplayName() + getNameSuffix();
        }

        @Override
        public String getFullFileName() {
            return this.hfsFile.getFullDisplayName() + getNameSuffix();
        }

        private String getNameSuffix() {
            return "{file=" + this.fileIndex + "}";
        }
    }
}