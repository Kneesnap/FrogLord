package net.highwayfrogs.editor.games.konami.rescue.file;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.hudson.HudsonFileUserFSDefinition;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.games.konami.rescue.FroggerRescueInstance;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sound bank as seen on the Windows version.
 * The file "sound\sfx_frogger.bin" seen on the PC version appears unused, it doesn't look like the buffer the file is read into is ever used. Also, it's mostly constant data anyways, as there's a function which can make/write the file if it does not exist?
 * Created by Kneesnap on 8/9/2024.
 */
public class FroggerRescueSoundBank extends HudsonGameFile {
    private final List<FroggerRescueSoundBankEntry> entries = new ArrayList<>();

    public static final String SIGNATURE = "SBNK";

    public FroggerRescueSoundBank(IHudsonFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public FroggerRescueInstance getGameInstance() {
        return (FroggerRescueInstance) super.getGameInstance();
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE);
        int soundCount = reader.readInt();
        int fileLength = reader.readInt();

        // Read entries.
        this.entries.clear();
        for (int i = 0; i < soundCount; i++) {
            FroggerRescueSoundBankEntry newEntry = new FroggerRescueSoundBankEntry(getGameInstance());
            newEntry.load(reader);
            this.entries.add(newEntry);
        }

        // Read raw file contents.
        for (int i = 0; i < this.entries.size(); i++) {
            this.entries.get(i).readFileContents(reader);
            reader.alignRequireEmpty(16);
        }

        requireReaderIndex(reader, fileLength, "Expected end of file");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeNull(Constants.INTEGER_SIZE);
        writer.writeInt(this.entries.size());
        int fileLengthPtr = writer.writeNullPointer();

        // Read entries.
        for (int i = 0; i < this.entries.size(); i++) {
            this.entries.get(i).save(writer);
            writer.align(16);
        }

        // Read raw file contents.
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).writeFileContents(writer);

        writer.writeAddressTo(fileLengthPtr);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.MUSIC_NOTE_16.getFxImage();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Entry Count", this.entries.size());
        return propertyList;
    }

    @Override
    public void export(File exportFolder) {
        if (!(getFileDefinition() instanceof HudsonFileUserFSDefinition))
            exportFolder = new File(exportFolder, "Sounds [" + getDisplayName() + "]");
        Utils.makeDirectory(exportFolder);

        for (int i = 0; i < this.entries.size(); i++) {
            File outputFile = new File(exportFolder, "SOUND" + String.format("%03d", i) + "_" + this.entries.get(i).getUnknownValue() + ".wav");

            try {
                Files.write(outputFile.toPath(), this.entries.get(i).getRawFileContents());
            } catch (IOException ex) {
                Utils.handleError(getLogger(), ex, false, "Failed to export file '%s'.", Utils.toLocalPath(exportFolder, outputFile, true));
            }
        }
    }

    public static class FroggerRescueSoundBankEntry extends GameData<FroggerRescueInstance> {
        private int fileSize = -1;
        private int alwaysOne = 1; // This is NOT channel count, since some stereo wavs still have this as one.
        @Getter private int unknownValue; // TODO: SOLVE THIS -> Could it be related to the .bin file?
        private int startAddress = -1;
        @Getter private byte[] rawFileContents;

        public FroggerRescueSoundBankEntry(FroggerRescueInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.fileSize = reader.readInt();
            this.alwaysOne = reader.readInt(); // This isn't the channel count.
            if (this.alwaysOne != 1)
                getLogger().warning("Value thought to always be one was read as " + this.alwaysOne + ".");

            this.unknownValue = reader.readInt();
            this.startAddress = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.rawFileContents != null ? this.rawFileContents.length : 0);
            writer.writeInt(this.alwaysOne);
            writer.writeInt(this.unknownValue);
            this.startAddress = writer.writeNullPointer();
        }

        /**
         * Reads the raw file contents from the current position.
         * @param reader the reader to read it from
         */
        public void readFileContents(DataReader reader) {
            if (this.startAddress <= 0)
                throw new RuntimeException("Cannot read file data, the pointer " + Utils.toHexString(this.startAddress) + " is invalid.");
            if (this.fileSize < 0)
                throw new RuntimeException("Cannot read file data, the file size " + Utils.toHexString(this.fileSize) + " is invalid.");

            // There isn't actually any static entity list saved, so we'll just validate the pointer as a sanity check and continue.
            reader.requireIndex(getLogger(), this.startAddress, "Expected raw file contents");
            this.rawFileContents = reader.readBytes(this.fileSize);
            this.startAddress = -1;
            this.fileSize = -1;
        }

        /**
         * Writes the raw file contents to the current position.
         * @param writer the writer to write it to
         */
        public void writeFileContents(DataWriter writer) {
            if (this.startAddress <= 0)
                throw new RuntimeException("Cannot write raw file contents, the pointer " + Utils.toHexString(this.startAddress) + " is invalid.");

            // This data doesn't actually exist, so just write the pointer and be done with it.
            writer.writeAddressTo(this.startAddress);
            if (this.rawFileContents != null)
                writer.writeBytes(this.rawFileContents);
            this.startAddress = -1;
        }
    }
}