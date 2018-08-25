package net.highwayfrogs.editor.file;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VBFile;
import net.highwayfrogs.editor.file.sound.VHFile;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * MWAD File Format: Medieval WAD Archive.
 * This represents a loaded MWAD file.
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

        VBFile lastVB = null; // VBs are indexed before VHs, but need to be loaded after VH. This allows us to do that.
        VBFile.SOUND_ID = 0; // This must be reset when an MWD is loaded. Unfortunately, this also means we can only load one MWD at once. But, why would we do otherwise?

        for (FileEntry entry : wadIndexTable.getEntries()) {
            if (entry.testFlag(FileEntry.FLAG_GROUP_ACCESS))
                continue; // This file is part of a WAD archive, and isn't a file entry in the MWD, so we can't load it here.

            reader.setIndex(entry.getArchiveOffset());

            // Read the file. Decompress if needed.
            byte[] fileBytes = reader.readBytes(entry.getArchiveSize());
            if (entry.isCompressed())
                fileBytes = PP20Unpacker.unpackData(fileBytes);

            // Turn the byte data into the appropriate game-file.
            GameFile file;

            String fileName = entry.getFilePath();
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);

            if (entry.getTypeId() == VLOArchive.TYPE_ID || fileName.startsWith("LS_ALL")) { // For some reason, Level Select vlos are registered as maps. This loads them as their proper VLO.
                file = new VLOArchive();
            } else if (entry.getTypeId() == MAPFile.TYPE_ID) {
                if (fileName.startsWith("SKY_LAND")) { // These maps are entered as a map, even though it is not. It should be loaded as a DummyFile for now.
                    file = new DummyFile(fileBytes.length); // TODO: Parse this file later.
                } else {
                    file = new MAPFile();
                }
            } else if (entry.getTypeId() == WADFile.TYPE_ID) {
                file = new WADFile();
            } else if (entry.getTypeId() == DemoFile.TYPE_ID) {
                file = new DemoFile();
            } else if (entry.getTypeId() == PALFile.TYPE_ID) {
                file = new PALFile();
            } else if (entry.getTypeId() == VHFile.TYPE_ID) {
                if (lastVB != null) {
                    VHFile vhFile = new VHFile();
                    vhFile.setVB(lastVB);
                    file = vhFile;
                } else {
                    file = new VBFile();
                }

            } else {
                file = new DummyFile(fileBytes.length);  //TODO: Support actual file-types.
            }

            try {
                file.load(new DataReader(new ArraySource(fileBytes)));
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load " + entry.getFilePath(), ex);
            }

            entryMap.put(file, entry);
            files.add(file);
            lastVB = file instanceof VBFile ? (VBFile) file : null;
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

        int sectorOffset = 0;
        long mwdStart = System.currentTimeMillis();
        for (GameFile file : files) {
            FileEntry entry = entryMap.get(file);

            do { // Find the next unused sector, to write the next entry.
                entry.setSectorOffset(++sectorOffset);
            } while (writer.getIndex() > entry.getArchiveOffset());
            writer.jumpTo(entry.getArchiveOffset());

            long start = System.currentTimeMillis();
            System.out.print("Saving " + entry.getFilePath() + " to MWD. (" + (files.indexOf(file) + 1) + "/" + files.size() + ") ");
            ArrayReceiver receiver = new ArrayReceiver();
            file.save(new DataWriter(receiver));

            byte[] transfer = receiver.toArray();
            if (entry.isCompressed()) {
                transfer = PP20Packer.packData(transfer);
                entry.setPackedSize(transfer.length);
            }

            writer.writeBytes(transfer);
            System.out.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s.");
        }
        System.out.println("MWD Built. Total Time: " + ((System.currentTimeMillis() - mwdStart) / 1000) + "s.");

        // Fill the rest of the file with null bytes.
        GameFile lastFile = files.get(files.size() - 1);
        writer.writeNull(Constants.CD_SECTOR_SIZE - (entryMap.get(lastFile).getArchiveSize() % Constants.CD_SECTOR_SIZE));

        try {
            DataWriter mwiWriter = new DataWriter(new FileReceiver(new File("./debug/MODDED.MWI")));
            wadIndexTable.save(mwiWriter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
