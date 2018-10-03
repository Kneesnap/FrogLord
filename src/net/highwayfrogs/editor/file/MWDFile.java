package net.highwayfrogs.editor.file;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VABHeaderFile;
import net.highwayfrogs.editor.file.sound.VBFile;
import net.highwayfrogs.editor.file.sound.VHFile;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * MWAD File Format: Medieval WAD Archive.
 * This represents a loaded MWAD file.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class MWDFile extends GameObject {
    private MWIFile wadIndexTable;
    private List<GameFile> files = new ArrayList<>();
    private Map<GameFile, FileEntry> entryMap = new HashMap<>();
    @Setter private BiConsumer<FileEntry, GameFile> saveCallback;

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
        AtomicInteger soundId = new AtomicInteger(0);

        for (FileEntry entry : wadIndexTable.getEntries()) {
            if (entry.testFlag(FileEntry.FLAG_GROUP_ACCESS))
                continue; // This file is part of a WAD archive, and isn't a file entry in the MWD, so we can't load it here.

            reader.setIndex(entry.getArchiveOffset());

            // Read the file. Decompress if needed.
            byte[] fileBytes = reader.readBytes(entry.getArchiveSize());
            if (entry.isCompressed())
                fileBytes = PP20Unpacker.unpackData(fileBytes);

            GameFile file = loadFile(fileBytes, entry, lastVB);

            try {
                DataReader newReader = new DataReader(new ArraySource(fileBytes));
                if (file instanceof VHFile) {
                    ((VHFile) file).load(newReader, soundId);
                } else {
                    file.load(newReader);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load " + entry.getDisplayName(), ex);
            }

            entryMap.put(file, entry);
            files.add(file);
            lastVB = file instanceof VBFile ? (VBFile) file : null;
        }
    }

    /**
     * Create a GameFile instance.
     * @param fileBytes The data to read
     * @param entry     The file entry being loaded.
     * @param lastVB    The lastVB value.
     * @return loadedFile
     */
    @SuppressWarnings("unchecked")
    public <T extends GameFile> T loadFile(byte[] fileBytes, FileEntry entry, VBFile lastVB) {
        // Turn the byte data into the appropriate game-file.
        GameFile file;

        if (entry.getTypeId() == VLOArchive.TYPE_ID || (entry.hasFilePath() && entry.getDisplayName().startsWith("LS_ALL"))) { // For some reason, Level Select vlos are registered as maps. This loads them as their proper VLO.
            file = new VLOArchive();
        } else /*if (entry.getTypeId() == MAPFile.TYPE_ID) { // Disabled until fully supported.
                if (fileName.startsWith("SKY_LAND")) { // These maps are entered as a map, even though it is not. It should be loaded as a DummyFile for now.
                    file = new DummyFile(fileBytes.length);
                } else {
                    file = new MAPFile();
                }
            } else */ if (entry.getTypeId() == WADFile.TYPE_ID) { // Disabled until fully supported.
            file = new WADFile(this);
        } else if (entry.getTypeId() == DemoFile.TYPE_ID) {
            file = new DemoFile();
        } else if (entry.getTypeId() == PALFile.TYPE_ID) {
            file = new PALFile();
        } else if (entry.getTypeId() == VHFile.TYPE_ID && !testSignature(fileBytes, 0, VABHeaderFile.SIGNATURE)) { // PSX support is disabled until it is complete.
            if (lastVB != null) {
                VHFile vhFile = new VHFile();
                vhFile.setVB(lastVB);
                file = vhFile;
            } else {
                file = new VBFile();
            }

        } else {
            file = new DummyFile(fileBytes.length);
        }

        return (T) file;
    }

    private static boolean testSignature(byte[] data, int startIndex, String test) {
        byte[] testBytes = test.getBytes();
        byte[] signatureBytes = new byte[testBytes.length];
        System.arraycopy(data, startIndex, signatureBytes, 0, signatureBytes.length);
        return Arrays.equals(testBytes, signatureBytes);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(MARKER.getBytes());
        writer.writeInt(0);

        Date date = Date.from(Calendar.getInstance().toInstant());
        writer.writeTerminatorString("\nCreation Date: " + DATE_FORMAT.format(date)
                + "\nCreation Time: " + TIME_FORMAT.format(date)
                + "\nThis MWD was changed using FrogLord.\n");

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
            if (getSaveCallback() != null)
                getSaveCallback().accept(entry, file);

            ArrayReceiver receiver = new ArrayReceiver();
            file.save(new DataWriter(receiver));

            byte[] transfer = receiver.toArray();
            entry.setUnpackedSize(transfer.length);
            if (entry.isCompressed())
                transfer = PP20Packer.packData(transfer);

            entry.setPackedSize(transfer.length);

            writer.writeBytes(transfer);
            System.out.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s.");
        }
        System.out.println("MWD Built. Total Time: " + ((System.currentTimeMillis() - mwdStart) / 1000) + "s.");

        // Fill the rest of the file with null bytes.
        GameFile lastFile = files.get(files.size() - 1);
        writer.writeNull(Constants.CD_SECTOR_SIZE - (entryMap.get(lastFile).getArchiveSize() % Constants.CD_SECTOR_SIZE));
    }
}
