package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.tgq.toc.*;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Parses Frogger TGQ's main game data file.
 * Notes: PS2 bin is way smaller than PC file. Support it eventually.
 * .SBR files contain sound headers. the SCK file seems to contain .wav files sequentially, likely using header data from the .SBR. TODO: Support exporting these to .wav
 * .PSS (PS2) are video files. Can be opened with VLC.
 * BUFFER.DAT files (PS2) are video files, and can be openned with VLC.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class TGQBinFile extends GameObject {
    private List<String> globalPaths; // TODO: These are used to determine the full file path of a file. Figure out how to determine which files go in which path.
    private List<TGQFile> files = new ArrayList<>();
    private Map<Integer, TGQFile> nameMap = new HashMap<>();

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
        for (int i = 0; i < unnamedFiles; i++)
            readFile(reader, null, reader.readInt());

        // Read named files.
        for (int i = 0; i < namedFiles; i++)
            readFile(reader, reader.readTerminatedStringOfLength(NAME_SIZE), 0);

        //TODO: Clean this up, maybe move it somewhere else.
        for (TGQFile file : getFiles()) {
            if (!(file instanceof TGQChunkedFile))
                continue;

            TGQChunkedFile chunkedFile = (TGQChunkedFile) file;
            for (TGQFileChunk chunk : chunkedFile.getChunks()) {
                if (chunk instanceof TEXChunk)
                    applyFileName(((TEXChunk) chunk).getPath());
                if (chunk instanceof VTXChunk && ((VTXChunk) chunk).getFullReferenceName() != null) {
                    applyFileName(((VTXChunk) chunk).getFullReferenceName());

                    TGQFile tgqFile = getFileByName(((VTXChunk) chunk).getFullReferenceName());
                    if (tgqFile instanceof TGQChunkedFile)
                        ((VTXChunk) ((TGQChunkedFile) tgqFile).getChunks().get(0)).setEnvironmentFile(chunkedFile);
                }
            }
        }
    }

    private TGQFile readFile(DataReader reader, String name, int crc) {
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
        } else if (Utils.testSignature(fileBytes, "6YTV") || Utils.testSignature(fileBytes, "TOC\0")) { //TODO: Fix up.
            readFile = new TGQChunkedFile(this);
        } else {
            readFile = new TGQDummyFile(this, fileBytes.length);
        }

        // Read file.
        readFile.init(name, isCompressed, crc);

        try {
            DataReader fileReader = new DataReader(new ArraySource(fileBytes));
            readFile.load(fileReader);
        } catch (Exception ex) {
            throw new RuntimeException("There was a problem reading " + readFile.getClass().getSimpleName() + " [File " + this.files.size() + "]", ex);
        }

        this.files.add(readFile);
        if (readFile.getNameHash() != 0)
            this.nameMap.put(readFile.getNameHash(), readFile);
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
                writer.writeInt(file.getNameHash());
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

        String fileName;
        if (args.length > 0) {
            fileName = String.join(" ", args);
        } else {
            System.out.print("Please enter the file path to data.bin: ");
            fileName = scanner.nextLine();
        }

        File binFile = new File(fileName);
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
        exportMaps(exportDir, mainFile);
        exportModels(exportDir, mainFile);
        exportDummyFiles(exportDir, mainFile);
        System.out.println("Done.");
    }


    /**
     * Activates the filename
     * @param filePath The path of a game file.
     */
    public void applyFileName(String filePath) {
        int hash = TGQUtils.hash(TGQUtils.getFileIdFromPath(filePath), true);
        TGQFile file = getNameMap().get(hash);
        if (file != null) // What causes a file to have its name put in, instead of the hash? It doesn't seem to be collisions. Could it be if it doesn't fall under the file path used for the given level?
            file.setRawName(filePath);
    }

    public TGQFile getFileByName(String filePath) {
        int hash = TGQUtils.hash(TGQUtils.getFileIdFromPath(filePath), true);
        TGQFile file = getNameMap().get(hash);
        if (file != null)
            return file;

        for (TGQFile testFile : getFiles())
            if (testFile.getRawName() != null && testFile.getRawName().toLowerCase().contains(filePath.toLowerCase()))
                return testFile;

        System.out.println("Failed to find " + filePath + " in " + getNameMap().size() + ". (" + TGQUtils.getFileIdFromPath(filePath) + ")");
        return null;
    }

    private static void exportImages(File baseFolder, TGQBinFile mainArchive) throws IOException {
        File saveFolder = new File(baseFolder, "Textures");
        if (saveFolder.exists()) {
            System.out.println("Skipping Textures, they already exist.");
            return;
        }

        System.out.println("Exporting Textures...");
        Utils.makeDirectory(saveFolder);

        for (TGQFile file : mainArchive.getFiles()) {
            if (!(file instanceof TGQImageFile))
                continue;

            TGQImageFile imageFile = (TGQImageFile) file;
            imageFile.saveImageToFile(new File(saveFolder, imageFile.getExportName() + ".png"));
        }
    }

    private static void exportMaps(File baseFolder, TGQBinFile mainArchive) throws IOException {
        File saveFolder = new File(baseFolder, "Maps");
        if (saveFolder.exists()) {
            System.out.println("Skipping maps, they already exist.");
            return;
        }

        Utils.makeDirectory(saveFolder);

        System.out.println("Exporting Maps...");
        for (TGQFile file : mainArchive.getFiles()) {
            if (!(file instanceof TGQChunkedFile))
                continue;

            TGQChunkedFile tocFile = (TGQChunkedFile) file;

            OTTChunk chunk = null;
            for (TGQFileChunk testChunk : tocFile.getChunks())
                if (testChunk instanceof OTTChunk)
                    chunk = (OTTChunk) testChunk;

            if (chunk != null)
                chunk.exportAsObj(saveFolder, Utils.stripExtension(tocFile.getExportName()));

            if (tocFile.getChunks().get(0) instanceof TOCChunk) {
                tocFile.exportFileToDirectory(new File(saveFolder, Utils.stripExtension(tocFile.getExportName()) + "/"));
                DataWriter writer = new DataWriter(new FileReceiver(new File(saveFolder, tocFile.getExportName())));
                tocFile.save(writer);
                writer.closeReceiver();
            }
        }
    }

    private static void exportModels(File baseFolder, TGQBinFile mainArchive) throws IOException {
        File saveFolder = new File(baseFolder, "Models");
        if (saveFolder.exists()) {
            System.out.println("Skipping models, they already exist.");
            return;
        }

        Utils.makeDirectory(saveFolder);

        System.out.println("Exporting Models...");
        for (TGQFile file : mainArchive.getFiles()) {
            if (!(file instanceof TGQChunkedFile))
                continue;

            TGQChunkedFile chunkedFile = (TGQChunkedFile) file;
            for (TGQFileChunk testChunk : chunkedFile.getChunks())
                if (testChunk instanceof VTXChunk && testChunk.isRootChunk())
                    ((VTXChunk) testChunk).saveToFile(new File(saveFolder, file.getExportName() + ".obj"));
        }
    }

    private static void exportDummyFiles(File baseFolder, TGQBinFile mainArchive) throws IOException {
        File saveFolder = new File(baseFolder, "Dummy");
        if (saveFolder.exists()) {
            System.out.println("Skipping dummy files, they already exist.");
            return;
        }

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
