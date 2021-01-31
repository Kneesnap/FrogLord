package net.highwayfrogs.editor.games.shadow;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.DummyFile;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.rescue.PRS1Unpacker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the HFS file format.
 * Created by Kneesnap on 6/7/2020.
 */
@Getter
public class HFSFile extends GameObject {
    private List<List<DummyFile>> hfsFiles = new ArrayList<>();
    private static final String MAGIC = "hfs\n";

    @Override
    public void load(DataReader reader) {
        int stopReadingHfsAt = Integer.MAX_VALUE;

        while (stopReadingHfsAt > reader.getIndex()) {
            reader.verifyString(MAGIC);
            int fullFileSize = reader.readInt();
            int entryCount = reader.readInt();
            int startAddress = reader.readInt();
            if (stopReadingHfsAt == Integer.MAX_VALUE)
                stopReadingHfsAt = startAddress;

            // Read file entries.
            List<DummyFile> fileData = new ArrayList<>(entryCount);
            for (int i = 0; i < entryCount; i++) {
                int cdSector = (reader.readInt() & 0xFEFFFFFF); // Remove bit 24.
                int dataLength = reader.readInt();

                reader.jumpTemp(startAddress + (cdSector * Constants.CD_SECTOR_SIZE)); // Jumps to the CD sector.
                DummyFile newFile = new DummyFile(dataLength);
                newFile.load(reader);
                fileData.add(newFile);
                reader.jumpReturn();
            }

            this.hfsFiles.add(fileData);
            int newSector = ((reader.getIndex() / Constants.CD_SECTOR_SIZE) + 1) * Constants.CD_SECTOR_SIZE;
            reader.setIndex(newSector); // Skip to the next sector.
        }
    }

    @Override
    public void save(DataWriter writer) {
        int cdSector = this.hfsFiles.size();
        for (List<DummyFile> files : this.hfsFiles) {
            writer.writeStringBytes(MAGIC);
            int fullFileSizePtr = writer.writeNullPointer();
            writer.writeInt(files.size());

            int startSector = (this.hfsFiles.size() == 1) ? 0 : cdSector;
            writer.writeInt(startSector * Constants.CD_SECTOR_SIZE);

            for (DummyFile dFile : files) {
                writer.writeInt((cdSector - startSector) | (PRS1Unpacker.isCompressedPRS1(dFile.getArray()) ? 0x01000000 : 0));
                int dataSizePtr = writer.writeNullPointer();

                // Save file data.
                writer.jumpTemp(cdSector * Constants.CD_SECTOR_SIZE);
                int startIndex = writer.getIndex();
                dFile.save(writer);
                int writtenBytes = (writer.getIndex() - startIndex);
                cdSector += (writtenBytes / Constants.CD_SECTOR_SIZE) + ((writtenBytes % Constants.CD_SECTOR_SIZE) != 0 ? 1 : 0);
                writer.writeTo(cdSector * Constants.CD_SECTOR_SIZE);
                writer.jumpReturn();
                writer.writeAddressAt(dataSizePtr, writtenBytes);
            }

            writer.writeAddressAt(fullFileSizePtr, cdSector * Constants.CD_SECTOR_SIZE);
        }
    }

    public static void saveToFile(File file, List<DummyFile> files) {
        DataWriter writer = new DataWriter(new FileReceiver(file));

        writer.writeStringBytes(MAGIC);
        int fullFileSizePtr = writer.writeNullPointer();
        writer.writeInt(files.size());
        writer.writeInt(0); // sector zero.

        int cdSector = 1;
        for (DummyFile dFile : files) {
            writer.writeInt(cdSector | 0x01000000);
            int dataSizePtr = writer.writeNullPointer();

            // Save file data.
            writer.jumpTemp(cdSector * Constants.CD_SECTOR_SIZE);
            int startIndex = writer.getIndex();
            dFile.save(writer);
            int writtenBytes = (writer.getIndex() - startIndex);
            cdSector += (writtenBytes / Constants.CD_SECTOR_SIZE) + ((writtenBytes % Constants.CD_SECTOR_SIZE) != 0 ? 1 : 0);
            writer.writeTo(cdSector * Constants.CD_SECTOR_SIZE);
            writer.jumpReturn();
            writer.writeAddressAt(dataSizePtr, writtenBytes);
        }

        writer.writeAddressAt(fullFileSizePtr, cdSector * Constants.CD_SECTOR_SIZE);
        writer.closeReceiver();
    }

    public static HFSFile readTable(File file) throws Exception {
        DataReader reader = new DataReader(new FileSource(file));
        HFSFile tableFile = new HFSFile();

        int stopReadingHfsAt = Integer.MAX_VALUE;

        while (reader.hasMore()) {
            reader.verifyString(MAGIC);
            int fullFileSize = reader.readInt();
            int entryCount = reader.readInt();
            int startAddress = reader.readInt();
            if (stopReadingHfsAt == Integer.MAX_VALUE)
                stopReadingHfsAt = startAddress;

            // Read file entries.
            List<DummyFile> fileData = new ArrayList<>(entryCount);
            for (int i = 0; i < entryCount; i++) {
                int cdSector = (reader.readInt() & 0xFEFFFFFF); // Remove bit 24.
                int dataLength = reader.readInt();
                fileData.add(new DummyFile(dataLength));
            }

            tableFile.hfsFiles.add(fileData);
            int newSector = ((reader.getIndex() / (Constants.CD_SECTOR_SIZE + 16)) + 1) * (Constants.CD_SECTOR_SIZE + 16);
            reader.setIndex(newSector); // Skip to the next sector.
        }

        return tableFile;
    }
}
