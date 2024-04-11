package net.highwayfrogs.editor.games.konami.rescue;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.DummyFile;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the HFS file format.
 * Created by Kneesnap on 6/7/2020.
 */
@Getter
public class HFSFile extends GameObject {
    private final List<DummyFile> fileData = new ArrayList<>();
    private static final String MAGIC = "hfs\7";

    @Override
    public void load(DataReader reader) {
        reader.verifyString(MAGIC);
        int fullFileSize = reader.readInt();
        if (fullFileSize != reader.getSize())
            throw new RuntimeException("Read file size did not match real file size! (Read: " + fullFileSize + ", Real: " + reader.getSize() + ")");

        int entryCount = reader.readInt();
        int unknownMightBeZero = reader.readInt();
        if (unknownMightBeZero != 0)
            throw new RuntimeException("It's not always zero. " + unknownMightBeZero);

        // Read file entries.
        for (int i = 0; i < entryCount; i++) {
            int cdSector = (reader.readInt() & 0xFEFFFFFF); // Remove bit 24.
            int dataLength = reader.readInt();

            reader.jumpTemp(cdSector * Constants.CD_SECTOR_SIZE); // Jumps to the CD sector.
            DummyFile newFile = new DummyFile(null, dataLength);
            newFile.load(reader);
            this.fileData.add(newFile);
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(MAGIC);
        int fullFileSizePtr = writer.writeNullPointer();
        writer.writeInt(this.fileData.size());
        writer.writeInt(0); // Change this if it's not always zero.

        int cdSector = 1;
        for (DummyFile file : this.fileData) {
            writer.writeInt(cdSector | 0x01000000);
            int dataSizePtr = writer.writeNullPointer();

            // Save file data.
            writer.jumpTemp(cdSector * Constants.CD_SECTOR_SIZE);
            int startIndex = writer.getIndex();
            file.save(writer);
            int writtenBytes = (writer.getIndex() - startIndex);
            cdSector += (writtenBytes / Constants.CD_SECTOR_SIZE) + ((writtenBytes % Constants.CD_SECTOR_SIZE) != 0 ? 1 : 0);
            writer.writeTo(cdSector * Constants.CD_SECTOR_SIZE);
            writer.jumpReturn();
            writer.writeAddressAt(dataSizePtr, writtenBytes);
        }

        writer.writeAddressAt(fullFileSizePtr, cdSector * Constants.CD_SECTOR_SIZE);
    }
}