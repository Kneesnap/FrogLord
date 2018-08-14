package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * MWAD File Format: Medieval WAD Archive.
 * This represents a loaded MWAD file.
 *
 * Created by Kneesnap on 8/10/2018.
 */
public class MWDFile extends GameObject {
    private MWIFile wadIndexTable;
    private List<GameFile> files = new ArrayList<>();
    private Map<GameFile, FileEntry> entryMap = new HashMap<>();

    private static final String MARKER = "DAWM";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, d MMMM yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public MWDFile(MWIFile table) {
        this.wadIndexTable = table;
    }

    @Override
    public void load(DataReader reader) {
        String marker = reader.readString(MARKER.length());
        Utils.verify(marker.equals(MARKER), "MWD Identifier %s was incorrectly read as %s!", MARKER, marker);

        for (FileEntry entry : wadIndexTable.getEntries()) {
            if (entry.testFlag(FileEntry.FLAG_GROUP_ACCESS))
                continue; // This file is part of a WAD archive, and isn't a file entry in the MWD, so we can't load it here.

            reader.setIndex(entry.getArchiveOffset());

            // Read the file. Decompress if needed.
            byte[] fileBytes = reader.readBytes(entry.getArchiveSize());
            if (entry.isCompressed())
                fileBytes = PP20Unpacker.unpackData(fileBytes);

            // Turn the byte data into the appropriate game-file.
            DummyFile file = new DummyFile(entry); //TODO: Support actual file-types.
            file.load(new DataReader(new ArraySource(fileBytes)));

            entryMap.put(file, entry);
            files.add(file);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(MARKER.getBytes());
        writer.writeInt(0);

        Date date = Date.from(Calendar.getInstance().toInstant());
        writer.writeTerminatorString("\nCreation Date: " + DATE_FORMAT.format(date)
                + "\nCreation Time: " + TIME_FORMAT.format(date)
                + "\nThis map was changed using FrogLord.\n");

        //TODO: If the existing offsets need to expand, alert the user, so they can replace the MWI. The MWI needs to export with exactly the same size as before... This should probably go in a seperate method, not directly here.
        for (GameFile file : files) {
            FileEntry entry = entryMap.get(file);
            writer.jumpTo(entry.getArchiveOffset());

            System.out.println("Saving " + entry.getFilePath() + " to MWD. (" + (files.indexOf(file) + 1) + "/" + files.size() + ")");
            ArrayReceiver receiver = new ArrayReceiver();
            file.save(new DataWriter(receiver));

            byte[] transfer = receiver.toArray();
            writer.writeBytes(entry.isCompressed() ? PP20Packer.packData(transfer) : transfer);
        }

        // Fill the rest of the file with null bytes.
        GameFile lastFile = files.get(files.size() - 1);
        writer.writeNull(Constants.CD_SECTOR_SIZE - (entryMap.get(lastFile).getArchiveSize() % Constants.CD_SECTOR_SIZE));
    }
}
