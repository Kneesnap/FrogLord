package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.tgq.loading.kcLoadContext;
import net.highwayfrogs.editor.games.tgq.model.kcModelWrapper;
import net.highwayfrogs.editor.games.tgq.script.kcAction;
import net.highwayfrogs.editor.games.tgq.script.kcCActionSequence;
import net.highwayfrogs.editor.games.tgq.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.tgq.script.kcScriptList;
import net.highwayfrogs.editor.games.tgq.toc.OTTChunk;
import net.highwayfrogs.editor.games.tgq.toc.TOCChunk;
import net.highwayfrogs.editor.games.tgq.toc.kcCResource;
import net.highwayfrogs.editor.games.tgq.toc.kcCResourceNamedHash;
import net.highwayfrogs.editor.games.tgq.toc.kcCResourceNamedHash.HashEntry;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
    private final kcPlatform platform;
    private List<String> globalPaths;
    private final List<TGQFile> files = new ArrayList<>();
    private final Map<Integer, TGQFile> nameMap = new HashMap<>();
    private final Map<Integer, List<TGQFile>> fileCollisions = new HashMap<>();

    private static final int NAME_SIZE = 0x108;

    public TGQBinFile(kcPlatform platform) {
        this.platform = platform;
    }

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

        Map<Integer, String> nameMap = new HashMap<>();
        TGQUtils.addHardcodedFileNameHashesToMap(nameMap);

        // Read unnamed files.
        for (int i = 0; i < unnamedFiles; i++) {
            int hash = reader.readInt();
            readFile(reader, nameMap.get(hash), hash, false);
        }

        // Read named files. Files are named if they have a collision with other files.
        for (int i = 0; i < namedFiles; i++) {
            String fullFilePath = reader.readTerminatedStringOfLength(NAME_SIZE);
            readFile(reader, fullFilePath, TGQUtils.hashFilePath(fullFilePath), true);
        }

        // Handle post-load setup.
        kcLoadContext context = new kcLoadContext(this);
        for (int i = 0; i < this.files.size(); i++)
            this.files.get(i).afterLoad1(context);
        for (int i = 0; i < this.files.size(); i++)
            this.files.get(i).afterLoad2(context);

        context.onComplete();
    }

    private TGQFile readFile(DataReader reader, String name, int crc, boolean hasCollision) {
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
        if (Utils.testSignature(fileBytes, TGQImageFile.SIGNATURE_STR)) {
            readFile = new TGQImageFile(this);
        } else if (Utils.testSignature(fileBytes, kcModelWrapper.SIGNATURE_STR)) {
            readFile = new kcModelWrapper(this);
        } else if (Utils.testSignature(fileBytes, "TOC\0")) {
            readFile = new TGQChunkedFile(this);
        } else if (this.files.size() > 100 && fileBytes.length > 30) {
            readFile = new TGQImageFile(this);
        } else {
            readFile = new TGQDummyFile(this, fileBytes.length);
        }

        // Setup file.
        readFile.init(name, isCompressed, crc, fileBytes, hasCollision);
        this.files.add(readFile); // Add before loading, so it can find its ID.
        if (hasCollision) {
            this.fileCollisions.computeIfAbsent(readFile.getNameHash(), key -> new ArrayList<>()).add(readFile);
        } else {
            this.nameMap.put(readFile.getNameHash(), readFile);
        }

        // Read file.
        try {
            DataReader fileReader = new DataReader(new ArraySource(fileBytes));
            readFile.load(fileReader);
        } catch (Exception ex) {
            throw new RuntimeException("There was a problem reading " + readFile.getClass().getSimpleName() + " [File " + (this.files.size() - 1) + "]", ex);
        }

        return readFile;
    }

    @Override
    public void save(DataWriter writer) {
        List<TGQFile> unnamedFiles = new ArrayList<>();
        List<TGQFile> namedFiles = new ArrayList<>();
        for (TGQFile file : getFiles())
            (file.hasFilePath() ? namedFiles : unnamedFiles).add(file);

        // Start writing file.
        writer.writeInt(unnamedFiles.size());
        writer.writeInt(namedFiles.size());
        int nameAddress = writer.writeNullPointer();

        Map<TGQFile, Integer> fileSizeTable = new HashMap<>();
        Map<TGQFile, Integer> fileZSizeTable = new HashMap<>();
        Map<TGQFile, Integer> fileOffsetTable = new HashMap<>();

        // Write file headers:
        for (TGQFile file : getFiles()) {
            if (file.hasFilePath()) {
                int endIndex = (writer.getIndex() + NAME_SIZE);
                writer.writeTerminatorString(file.getFilePath());
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

        kcPlatform platform = null;
        if (fileName.contains("PC") || fileName.contains("Windows"))
            platform = kcPlatform.PC;
        if (fileName.contains("PS2") || fileName.contains("Play"))
            platform = kcPlatform.PS2;

        if (platform == null) {
            System.out.print("Is this a PS2 build or a PC build?: ");
            String platformName = scanner.nextLine();

            try {
                platform = kcPlatform.valueOf(platformName.toUpperCase());
            } catch (Throwable th) {
                // We don't care. This is some temporary development CLI mode for devs.
            }
        }

        // Verify platform.
        if (platform == null) {
            System.out.println("That is not a valid platform.");
            return;
        }

        // Load main bin.
        System.out.println("Loading file...");
        DataReader reader = new DataReader(new FileSource(binFile));
        TGQBinFile mainFile = new TGQBinFile(platform);
        mainFile.load(reader);

        // Export.
        File exportDir = new File(binFile.getParentFile(), "Export");
        Utils.makeDirectory(exportDir);
        exportFileList(exportDir, mainFile);
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
    public TGQFile applyFileName(String filePath) {
        TGQFile file = getOptionalFileByName(filePath);
        if (file != null) {
            file.setFilePath(filePath);
            return file;
        }

        int hash = TGQUtils.hashFilePath(filePath);
        System.out.println("Attempted to apply the file path '" + filePath + "', but no file matched the hash " + Utils.to0PrefixedHexString(hash) + ".");
        return null;
    }

    /**
     * Searches for a file by its full file path, printing a message if the file is nto found.
     * @param searchFrom The file to search from, so that we can print which file wanted the file if it wasn't found.
     * @param filePath   Full file path.
     * @return the found file, if there was one.
     */
    public TGQFile getFileByName(TGQFile searchFrom, String filePath) {
        TGQFile file = getOptionalFileByName(filePath);
        if (file == null)
            System.out.println("Failed to find file " + filePath + (searchFrom != null ? " referenced in " + searchFrom.getExportName() : "") + ". (" + TGQUtils.getFileIdFromPath(filePath) + ")");
        return file;
    }

    /**
     * Searches for a file by its full file path.
     * @param filePath Full file path.
     * @return the found file, if there was one.
     */
    public TGQFile getOptionalFileByName(String filePath) {
        // Create hash.
        String abbreviatedFilePath = TGQUtils.getFileIdFromPath(filePath);
        int hash = TGQUtils.hash(abbreviatedFilePath);

        // Search for unique file without collisions.
        TGQFile file = this.nameMap.get(hash);
        if (file != null)
            return file;

        // Search colliding files.
        List<TGQFile> collidingFiles = this.fileCollisions.get(hash);
        if (collidingFiles != null) {
            for (int i = 0; i < collidingFiles.size(); i++) {
                TGQFile collidedFile = collidingFiles.get(i);
                if (TGQUtils.getFileIdFromPath(collidedFile.getFilePath()).equalsIgnoreCase(abbreviatedFilePath))
                    return collidedFile;
            }
        }

        return null;
    }

    private static void exportFileList(File baseFolder, TGQBinFile mainArchive) throws IOException {
        List<String> lines = new ArrayList<>();
        long namedCount = mainArchive.getFiles().stream().filter(file -> file.getFilePath() != null).count();
        lines.add("File List [" + mainArchive.getFiles().size() + ", " + namedCount + " named]:");

        for (int i = 0; i < mainArchive.getFiles().size(); i++) {
            TGQFile file = mainArchive.getFiles().get(i);

            lines.add(" - File #" + Utils.padNumberString(i, 4)
                    + ": " + Utils.to0PrefixedHexString(file.getNameHash())
                    + ", " + file.getClass().getSimpleName()
                    + (file.getFilePath() != null ? ", " + file.getFilePath() + ", " + TGQUtils.getFileIdFromPath(file.getFilePath()) : ""));
        }

        lines.add("");
        lines.add("Global Paths:");
        for (String globalPath : mainArchive.getGlobalPaths())
            lines.add(" - " + globalPath);

        Files.write(new File(baseFolder, "file-list.txt").toPath(), lines);
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

            // Build the name map.
            Map<Integer, String> nameMap = new HashMap<>();
            for (kcCResource testChunk : tocFile.getChunks()) {
                if (testChunk.getName() != null && testChunk.getName().length() > 0)
                    nameMap.put(TGQUtils.hash(testChunk.getName(), true), testChunk.getName());

                if (testChunk instanceof kcCResourceNamedHash) {
                    kcCResourceNamedHash namedHashChunk = (kcCResourceNamedHash) testChunk;
                    for (HashEntry entry : namedHashChunk.getEntries())
                        nameMap.put(entry.getRawHash(), entry.getName());
                }
            }

            TGQUtils.addDefaultHashesToMap(nameMap);
            kcScriptDisplaySettings settings = new kcScriptDisplaySettings(nameMap, true, true);

            // Main looking.
            OTTChunk chunk = null;
            StringBuilder sequenceBuilder = new StringBuilder();
            StringBuilder scriptBuilder = new StringBuilder();
            for (kcCResource testChunk : tocFile.getChunks()) {
                if (testChunk instanceof OTTChunk)
                    chunk = (OTTChunk) testChunk;

                if (testChunk instanceof kcCActionSequence) {
                    kcCActionSequence sequence = (kcCActionSequence) testChunk;

                    sequenceBuilder.append(sequence.getName()).append(":\n");
                    for (kcAction command : sequence.getActions()) {
                        sequenceBuilder.append(" - ");
                        command.toString(sequenceBuilder, settings);
                        sequenceBuilder.append('\n');
                    }

                    sequenceBuilder.append('\n');
                } else if (testChunk instanceof kcScriptList) {
                    kcScriptList script = (kcScriptList) testChunk;
                    scriptBuilder.append("// Script List: '").append(script.getName()).append("'\n");
                    script.toString(scriptBuilder, settings);
                    scriptBuilder.append('\n');
                }
            }

            if (chunk != null)
                chunk.exportAsObj(saveFolder, Utils.stripExtension(tocFile.getExportName()));

            File subFolder = new File(saveFolder, Utils.stripExtension(tocFile.getExportName()) + "/");

            // Save scripts to folder.
            if (sequenceBuilder.length() > 0) {
                Utils.makeDirectory(subFolder);
                File sequenceFile = new File(subFolder, "sequences.txt");
                Files.write(sequenceFile.toPath(), Arrays.asList(sequenceBuilder.toString().split("\n")));
            }

            // Save scripts to folder.
            if (scriptBuilder.length() > 0) {
                Utils.makeDirectory(subFolder);
                File scriptFile = new File(subFolder, "script.txt");
                Files.write(scriptFile.toPath(), Arrays.asList(scriptBuilder.toString().split("\n")));
            }

            if (tocFile.getChunks().get(0) instanceof TOCChunk) {
                // Export to folder.
                tocFile.exportFileToDirectory(subFolder);
                DataWriter writer = new DataWriter(new FileReceiver(new File(saveFolder, tocFile.getExportName())));
                tocFile.save(writer);
                writer.closeReceiver();

                // Save hashes to file.
                if (nameMap.size() > 0) {
                    File hashFile = new File(subFolder, "hashes.txt");

                    List<String> lines = nameMap.entrySet().stream()
                            .sorted(Comparator.comparingInt(Entry::getKey))
                            .map(entry -> Utils.to0PrefixedHexString(entry.getKey()) + "=" + entry.getValue())
                            .collect(Collectors.toList());

                    Files.write(hashFile.toPath(), lines);
                }
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
            if (!(file instanceof kcModelWrapper))
                continue;

            kcModelWrapper model = (kcModelWrapper) file;
            try {
                model.getModel().saveToFile(saveFolder, file.getExportName());
                if (model.getRawData() != null)
                    Files.write(new File(saveFolder, file.getExportName()).toPath(), model.getRawData());
            } catch (Throwable th) {
                if (model.getRawData() != null)
                    Files.write(new File(saveFolder, file.getExportName()).toPath(), model.getRawData());
                System.err.println("Failed to export '" + file.getExportName() + ".obj'.");
                th.printStackTrace();
            }
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
            if ((file instanceof TGQImageFile) || (file instanceof kcModelWrapper))
                continue;

            if (file instanceof TGQChunkedFile) {
                TGQChunkedFile chunkedFile = (TGQChunkedFile) file;
                if (chunkedFile.getChunks().size() > 0) {
                    kcCResource chunk = chunkedFile.getChunks().get(0);
                    if (chunk instanceof TOCChunk)
                        continue; // Handled elsewhere.
                }
            }

            Files.write(new File(saveFolder, file.getExportName()).toPath(), file.getRawData());
        }
    }
}