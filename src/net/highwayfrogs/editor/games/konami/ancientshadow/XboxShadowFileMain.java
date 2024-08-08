package net.highwayfrogs.editor.games.konami.ancientshadow;

import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.konami.FroggerBeyondUtil;
import net.highwayfrogs.editor.games.konami.FroggerBeyondUtil.FroggerBeyondPlatform;
import net.highwayfrogs.editor.games.konami.hudson.PRS1Unpacker;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * A hack I've created to try to port the assets from the Xbox version of Ancient Shadow to the PC version.
 * This is super messy, and probably not very self-explanatory. Oh well.
 * Created by Kneesnap on 8/21/2020.
 */
public class XboxShadowFileMain {

    public static void exportBeyondMusic() throws Exception {
        System.out.print("Sound Location: ");
        Scanner scanner = new Scanner(System.in);
        File file = new File(scanner.nextLine());

        FroggerBeyondUtil.exportVoices(file, new File(file.getName() + "_EXPORT/"), FroggerBeyondPlatform.WINDOWS);
    }

    @SneakyThrows
    public static void exportHFS(File file) {
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        byte[] packed = Files.readAllBytes(file.toPath());
        HFSFile hfsFile = new HFSFile();
        hfsFile.load(new DataReader(new ArraySource(packed)));

        // Save data.
        int id = 0;
        for (List<DummyFile> dummyFiles : hfsFile.getHfsFiles()) {
            for (DummyFile dummyFile : dummyFiles) {
                boolean compressed = PRS1Unpacker.isCompressedPRS1(dummyFile.getArray());
                File outputFile = new File(GUIMain.getMainApplicationFolder(), "./" + id++ + "-" + (compressed ? "UNPACKED" : "RAW"));

                byte[] data = (compressed ? PRS1Unpacker.decompressPRS1(dummyFile.getArray()) : dummyFile.getArray());
                Files.write(outputFile.toPath(), data);
                System.out.println("Saved to: " + outputFile);
            }
        }

        System.out.println("Done.");
    }

    public static void main(String[] args) throws Exception {
        exportBeyondMusic();
        System.out.print("gamedata.bin Location File: ");

        Scanner scanner = new Scanner(System.in);
        File file = new File(scanner.nextLine());

        if (!file.exists() || !file.isFile()) {
            System.out.println("File does not exist.");
            return;
        }

        System.out.print("Location of unmodified hfstable.dat file: ");
        File tableFile = new File(scanner.nextLine());
        if (!tableFile.exists() || !tableFile.isFile()) {
            System.out.println("File does not exist.");
            return;
        }
        HFSFile hfsTable = HFSFile.readTable(tableFile);

        File outputDir = new File("AncientShadowOutput/");
        if (!outputDir.exists())
            Utils.makeDirectory(outputDir);

        byte[] readData = Files.readAllBytes(tableFile.toPath());
        int sectionCount = readData.length / 0x810;
        File testDir = new File("C:\\Users\\Drew\\Downloads\\KonamiLive - Copy\\KonamiLive\\Dashboard\\Game\\");
        for (File testFile : testDir.listFiles()) {
            if (!testFile.isFile() || testFile.length() < Constants.CD_SECTOR_SIZE)
                continue;
            byte[] fullFileBytes = Files.readAllBytes(testFile.toPath());
            byte[] headerBytes = new byte[Constants.CD_SECTOR_SIZE];
            System.arraycopy(fullFileBytes, 0, headerBytes, 0, headerBytes.length);

            boolean found = false;
            for (int i = 0; i < sectionCount; i++) {
                if (Utils.testSignature(readData, 0x810 * i, headerBytes)) {
                    found = true;
                    System.out.println(testFile.getName() + " found at [" + i + "].");
                }
            }

            if (!found)
                System.out.println(testFile.getName() + " was not found.");
        }


        byte[] hfsFileData = Files.readAllBytes(file.toPath());
        HFSFile hfsFile = new HFSFile();
        hfsFile.load(new DataReader(new ArraySource(hfsFileData)));

        // SNDAREA#.HFS Files:
        createHfsFile(outputDir, hfsFile, 62, new int[]{12, 13, 29, 30, 31}, "SNDAREA2");
        createHfsFile(outputDir, hfsFile, 62, new int[]{14, 15, 32, 33, 34, 48}, "SNDAREA3"); // 48 is the rail surfing minigame. (NOTE: THIS ONE NEEDS HFSTABLE UPDATING)
        createHfsFile(outputDir, hfsFile, 62, new int[]{16, 17, 35, 36, 49}, "SNDAREA4"); // Needs hfstable updating.
        createHfsFile(outputDir, hfsFile, 62, new int[]{18, 19, 37, 38, 50}, "SNDAREA5"); // Needs hfstable updating. (Yes, this isn't copy-pasted.)
        createHfsFile(outputDir, hfsFile, 62, new int[]{20, 21, 39, 40, 41}, "SNDAREA6");
        createHfsFile(outputDir, hfsFile, 62, new int[]{22, 23, 42, 43, 44, 51}, "SNDAREA7");

        // Minigame Sounds:
        createHfsFile(outputDir, hfsFile, 62, new int[]{3, 6, 25}, "SNDMNG5"); // Arcade
        createHfsFile(outputDir, hfsFile, 62, new int[]{45}, "SNDMNG6"); // [Needs HFS Table Updating] Jumprope
        createHfsFile(outputDir, hfsFile, 62, new int[]{47}, "SNDMNG8"); // Needs HFS Table Updating Dodgeball

        // Other:
        // findSizeMatches(hfsFile, hfsTable, 47);
        createFile(hfsFile, hfsTable, outputDir, "ENDING", 59, 47, 128); // insertOffset: 23, Count: 30
        createHfsFile(outputDir, hfsFile, 62, new int[]{1}, "SNDBERRY"); // Needs HFS Table Updating

        // Generate Areas.
        createValidArea0(hfsFile, hfsTable, outputDir, 3); // 20
        createValidArea0(hfsFile, hfsTable, outputDir, 4);
        createValidArea0(hfsFile, hfsTable, outputDir, 5); // Requires HFS Table update.
        createValidArea0(hfsFile, hfsTable, outputDir, 6);
        createValidArea0(hfsFile, hfsTable, outputDir, 7); // Requires HFS Table update.

        // AREA73.HFS
        List<DummyFile> area73Files = new ArrayList<>(hfsFile.getHfsFiles().get(44));
        area73Files.remove(0);
        area73Files.remove(0);

        HFSFile output = new HFSFile();
        output.getHfsFiles().add(area73Files);
        DataWriter writer = new DataWriter(new FileReceiver(new File(outputDir, "AREA73.HFS")));
        output.save(writer);
        writer.closeReceiver();

        System.out.println("Done.");
    }

    public static void createValidArea0(HFSFile gameDataHfs, HFSFile hfsTable, File outputDir, int area) throws Exception {
        int srcAreaFile = 11 + ((area - 1) * 5);
        int srcOffset = 8 + (15 * area);
        createFile(gameDataHfs, hfsTable, outputDir, "AREA" + area + "0", 59, srcAreaFile, srcOffset);
    }

    public static void createFile(HFSFile gameDataHfs, HFSFile hfsTable, File outputDir, String outputFileName, int extraFilesSrcId, int srcAreaFile, int srcOffset) throws Exception {
        List<DummyFile> files = gameDataHfs.getHfsFiles().get(srcAreaFile);
        List<DummyFile> tableEntry = hfsTable.getHfsFiles().get(srcAreaFile); // While this doesn't work for every file, everything before sounds should have matching ids so this should be ok.

        int insertOffset = -1;
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getLength() != tableEntry.get(i).getLength()) {
                insertOffset = i;
                break;
            }
        }

        System.out.println("Calculated " + outputFileName + "'s Insert Offset: " + insertOffset);
        createFile(gameDataHfs, hfsTable, outputDir, outputFileName, extraFilesSrcId, srcAreaFile, srcOffset, insertOffset);
    }

    public static void createFile(HFSFile gameDataHfs, HFSFile hfsTable, File outputDir, String outputFileName, int extraFilesSrcId, int srcAreaFile, int srcOffset, int insertOffset) throws Exception {
        List<DummyFile> files = new ArrayList<>(gameDataHfs.getHfsFiles().get(srcAreaFile));
        List<DummyFile> srcTextureFiles = gameDataHfs.getHfsFiles().get(extraFilesSrcId);  // The files seem to be in here.
        List<DummyFile> tableEntry = hfsTable.getHfsFiles().get(srcAreaFile); // While this doesn't work for every file, everything before sounds should have matching ids so this should be ok.

        int count = tableEntry.size() - files.size();
        System.out.println("Calculated " + outputFileName + "'s Count: " + count);
        for (int i = 0; i < count; i++)
            files.add(i + insertOffset, srcTextureFiles.get(srcOffset + i));

        HFSFile outputHfs = new HFSFile();
        outputHfs.getHfsFiles().add(files);

        DataWriter writer = new DataWriter(new FileReceiver(new File(outputDir, outputFileName + ".HFS")));
        outputHfs.save(writer);
        writer.closeReceiver();
    }

    private static void createHfsFile(File outputDir, HFSFile bulkBin, int sourceFile, int[] banks, String fileName) {
        List<DummyFile> newFiles = new ArrayList<>();
        for (int useBank : banks)
            newFiles.add(bulkBin.getHfsFiles().get(sourceFile).get(useBank));

        HFSFile outputHfs = new HFSFile();
        outputHfs.getHfsFiles().add(newFiles);

        DataWriter writer = new DataWriter(new FileReceiver(new File(outputDir, fileName + ".HFS")));
        outputHfs.save(writer);
        writer.closeReceiver();
    }

    // Used for debugging + file analysis:

    private static void dumpHfsData(File outputDir, HFSFile hfsFile) throws Exception {
        for (int i = 0; i < hfsFile.getHfsFiles().size(); i++) {
            File outputFileHfs = new File(outputDir, "A-" + Utils.padNumberString(i, 2) + ".hfs");
            HFSFile.saveToFile(outputFileHfs, hfsFile.getHfsFiles().get(i));

            for (int j = 0; j < hfsFile.getHfsFiles().get(i).size(); j++) {
                DummyFile dummyFile = hfsFile.getHfsFiles().get(i).get(j);
                boolean compressed = PRS1Unpacker.isCompressedPRS1(dummyFile.getArray());
                File outputFile = new File(outputDir, "FILE-" + i + "-" + j);

                byte[] data = (compressed ? PRS1Unpacker.decompressPRS1(dummyFile.getArray()) : dummyFile.getArray());
                Files.write(outputFile.toPath(), data);
                System.out.println("Saved to: " + outputFile);
            }
        }
    }

    // Finds files in a large game.bin which have a matching file-size to the files in the hfstable header entry. (Used so we could figure out how to splice files together.)
    public static void findSizeMatches(HFSFile gameDataHfs, HFSFile hfsTable, int fileId) throws Exception {
        // Build map of all file lengths -> locations.
        Map<Integer, List<String>> hashedMap = new HashMap<>();
        for (int i = 0; i < gameDataHfs.getHfsFiles().size(); i++) {
            List<DummyFile> tmpFiles = gameDataHfs.getHfsFiles().get(i);
            for (int j = 0; j < tmpFiles.size(); j++) {
                DummyFile tmpFile = tmpFiles.get(j);
                hashedMap.computeIfAbsent(tmpFile.getLength(), key -> new ArrayList<>()).add("FILE-" + i + "-" + j);
            }
        }

        // Find matches.
        int lastSelfIndex = -1;
        List<DummyFile> tmpFiles = hfsTable.getHfsFiles().get(fileId);
        for (int j = 0; j < tmpFiles.size(); j++) {
            DummyFile tmpFile = tmpFiles.get(j);
            List<String> match = hashedMap.get(tmpFile.getLength());
            if (match != null) {
                String displayLocation;
                if (match.contains("FILE-" + fileId + "-" + j)) {
                    displayLocation = "SELF";
                    lastSelfIndex = j;
                } else if (lastSelfIndex != -1 && match.contains("FILE-" + fileId + "-" + (lastSelfIndex + 1))) {
                    displayLocation = "RELOCATED SELF";
                    lastSelfIndex++;
                } else {
                    displayLocation = String.join(", ", match);
                }

                System.out.println("MATCH(ES) FOUND [File Entry " + j + "]: " + displayLocation);
            } else {
                System.out.println("NO MATCHES FOUND [File Entry " + j + "]");
            }
        }
    }

    // Used to find the matches in the real PC AREAX0.HFS files, for splicing the files in.
    public static void findHashMatches(HFSFile gameDataHfs, File otherHfs) throws Exception {
        HFSFile compareHfs = new HFSFile();
        compareHfs.load(new DataReader(new FileSource(otherHfs)));

        // Build map.
        Map<Long, String> hashedMap = new HashMap<>();
        for (int i = 0; i < gameDataHfs.getHfsFiles().size(); i++) {
            List<DummyFile> files = gameDataHfs.getHfsFiles().get(i);
            for (int j = 0; j < files.size(); j++) {
                DummyFile file = files.get(j);
                byte[] data = PRS1Unpacker.isCompressedPRS1(file.getArray()) ? PRS1Unpacker.decompressPRS1(file.getArray()) : file.getArray();
                long crc32 = Utils.getCRC32(data);
                if (hashedMap.containsKey(crc32))
                    System.out.println("MATCHING FILES!!! [" + hashedMap.get(crc32) + "/" + j + "]");
                hashedMap.put(crc32, "FILE-" + i + "-" + j + " (" + file.getLength() + ")");
            }
        }

        // Find matches.
        for (int i = 0; i < compareHfs.getHfsFiles().size(); i++) {
            List<DummyFile> files = compareHfs.getHfsFiles().get(i);
            for (int j = 0; j < files.size(); j++) {
                DummyFile file = files.get(j);
                byte[] data = PRS1Unpacker.isCompressedPRS1(file.getArray()) ? PRS1Unpacker.decompressPRS1(file.getArray()) : file.getArray();
                String match = hashedMap.get(Utils.getCRC32(data));
                if (match != null)
                    System.out.println("MATCH FOUND [HFS " + match + "] [GameData FILE-" + i + "-" + j + "] (" + file.getLength() + ")");
                else
                    System.out.println("NO MATCH FOUND FOR [FILE" + i + "-" + j + "]");
            }
        }
    }

    /**
     * Represents the HFS file format.
     * This is an old version of the FrogLord HFS representation, as it was written for this file.
     * We'll eventually go through and rewrite this file to support newer representations later.
     * Created by Kneesnap on 6/7/2020.
     */
    @Getter
    private static class HFSFile extends GameObject {
        private final List<List<DummyFile>> hfsFiles = new ArrayList<>();
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
                    DummyFile newFile = new DummyFile(null, dataLength);
                    newFile.load(reader);
                    fileData.add(newFile);
                    reader.jumpReturn();
                }

                this.hfsFiles.add(fileData);
                reader.alignRequireEmpty(Constants.CD_SECTOR_SIZE); // Each HFS header seems padded
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
                    fileData.add(new DummyFile(null, dataLength));
                }

                tableFile.hfsFiles.add(fileData);
                int newSector = ((reader.getIndex() / (Constants.CD_SECTOR_SIZE + 16)) + 1) * (Constants.CD_SECTOR_SIZE + 16);
                reader.setIndex(newSector); // Skip to the next sector.
            }

            return tableFile;
        }
    }
}