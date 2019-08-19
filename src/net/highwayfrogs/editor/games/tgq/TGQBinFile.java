package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Parses Frogger TGQ's main game data file.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class TGQBinFile extends GameObject {
    private List<String> globalPaths; // It is unknown what these are used for.
    private List<TGQFile> files = new ArrayList<>();

    private static final int NAME_SIZE = 0x108;

    @Override
    public void load(DataReader reader) {
        int unnamedFiles = reader.readInt();
        int namedFiles = reader.readInt();

        // Read Names. (Located after all the files)
        int nameAddress = reader.readInt();
        reader.jumpTemp(nameAddress);
        int nameCount = reader.readInt();
        this.globalPaths = new ArrayList<>(nameCount);
        for (int i = 0; i < nameCount; i++)
            this.globalPaths.add(reader.readTerminatedStringOfLength(NAME_SIZE));
        reader.jumpReturn();

        // Read unnamed files.
        for (int i = 0; i < unnamedFiles; i++) {
            int crc = reader.readInt();
            files.add(readFile(reader, null));
        }

        // Read named files.
        for (int i = 0; i < namedFiles; i++)
            files.add(readFile(reader, reader.readTerminatedStringOfLength(NAME_SIZE)));
    }

    private TGQFile readFile(DataReader reader, String name) {
        int size = reader.readInt();
        int zSize = reader.readInt();
        int offset = reader.readInt();
        int zero = reader.readInt();
        if (zero != 0)
            throw new RuntimeException("File field was supposed to be zero! Was: " + zero);

        boolean isCompressed = (zSize != 0); // ZLib compression.

        reader.jumpTemp(offset);
        byte[] fileBytes = isCompressed ? TGQUtils.zlibDecompress(reader.readBytes(zSize), size) : reader.readBytes(size);
        reader.jumpReturn();

        TGQFile readFile;
        if (Utils.testSignature(fileBytes, TGQImageFile.SIGNATURE)) {
            readFile = new TGQImageFile(this);
        } else {
            readFile = new TGQDummyFile(this, fileBytes.length);
        }

        // Read file.
        readFile.init(name, isCompressed);

        try {
            DataReader fileReader = new DataReader(new ArraySource(fileBytes));
            readFile.load(fileReader);
        } catch (Exception ex) {
            throw new RuntimeException("There was a problem reading " + readFile.getClass().getSimpleName() + " [File " + this.files.size() + "]", ex);
        }
        return readFile;
    }

    @Override
    public void save(DataWriter writer) {
        List<TGQFile> unnamedFiles = new ArrayList<>();
        List<TGQFile> namedFiles = new ArrayList<>();
        for (TGQFile file : getFiles())
            (file.hasName() ? namedFiles : unnamedFiles).add(file);

        // Start writing file.
        writer.writeInt(unnamedFiles.size());
        writer.writeInt(namedFiles.size());
        int nameAddress = writer.writeNullPointer();

        //Map<TGQFile, Integer> fileCrcTable = new HashMap<>();
        Map<TGQFile, Integer> fileSizeTable = new HashMap<>();
        Map<TGQFile, Integer> fileZSizeTable = new HashMap<>();
        Map<TGQFile, Integer> fileOffsetTable = new HashMap<>();

        // Write file headers:
        for (TGQFile file : getFiles()) {
            if (file.hasName()) {
                int endIndex = (writer.getIndex() + NAME_SIZE);
                writer.writeTerminatorString(file.getRawName());
                writer.writeTo(endIndex, (byte) 0xCD);
            } else {
                //fileCrcTable.put(file, writer.getIndex());
                writer.writeInt(0); // CRC
            }

            fileSizeTable.put(file, writer.getIndex());
            writer.writeInt(0); // Size.
            fileZSizeTable.put(file, writer.getIndex());
            writer.writeInt(0); // ZSize.
            fileOffsetTable.put(file, writer.getIndex());
            writer.writeInt(0); // Offset.
            writer.writeInt(0); // Zero.
        }

        // Write files:
        for (TGQFile file : getFiles()) {
            //if (fileCrcTable.containsKey(file)) {
            // WARNING: It is unknown how to calculate this CRC value. This should be added.
            //}

            writer.writeAddressTo(fileOffsetTable.get(file));

            ArrayReceiver receiver = new ArrayReceiver();
            file.save(new DataWriter(receiver));
            byte[] rawBytes = receiver.toArray();

            // Write size.
            writer.jumpTemp(fileSizeTable.get(file));
            writer.writeInt(rawBytes.length);
            writer.jumpReturn();

            // File is compressed.
            if (file.isCompressed()) {
                rawBytes = TGQUtils.zlibCompress(rawBytes); // Compress data.

                // Write z size.
                writer.jumpTemp(fileZSizeTable.get(file));
                writer.writeInt(rawBytes.length);
                writer.jumpReturn();
            }

            writer.writeBytes(rawBytes);
        }

        // Lastly, write global strings.
        writer.writeAddressTo(nameAddress);
        writer.writeInt(this.globalPaths.size());
        for (String name : getGlobalPaths()) {
            int endIndex = (writer.getIndex() + NAME_SIZE);
            writer.writeStringBytes(name);
            writer.writeTo(endIndex);
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Please enter the file path to data.bin: ");
        File binFile = new File(scanner.nextLine());

        if (!binFile.exists() || !binFile.isFile()) {
            System.out.println("That is not a valid file!");
            return;
        }

        // Load main bin.
        System.out.println("Loading file...");
        DataReader reader = new DataReader(new FileSource(binFile));
        TGQBinFile mainFile = new TGQBinFile();
        mainFile.load(reader);

        // Export.
        File exportDir = new File(binFile.getParentFile(), "Export");
        Utils.makeDirectory(exportDir);
        exportImages(exportDir, mainFile);
        exportDummyFiles(exportDir, mainFile);
        System.out.println("Done.");
    }

    private static void exportImages(File baseFolder, TGQBinFile mainArchive) throws IOException {
        System.out.println("Exporting Textures...");

        File saveFolder = new File(baseFolder, "Textures");
        Utils.makeDirectory(saveFolder);

        for (TGQFile file : mainArchive.getFiles()) {
            if (!(file instanceof TGQImageFile))
                continue;

            TGQImageFile imageFile = (TGQImageFile) file;
            imageFile.saveImageToFile(new File(saveFolder, imageFile.getExportName() + ".png"));
        }
    }

    private static void exportDummyFiles(File baseFolder, TGQBinFile mainArchive) throws IOException {
        File saveFolder = new File(baseFolder, "Dummy");
        Utils.makeDirectory(saveFolder);

        System.out.println("Exporting Everything Else...");
        for (TGQFile file : mainArchive.getFiles()) {
            if (!(file instanceof TGQDummyFile))
                continue;

            DataWriter writer = new DataWriter(new FileReceiver(new File(saveFolder, file.getExportName())));
            file.save(writer);
            writer.closeReceiver();
        }
    }
}
