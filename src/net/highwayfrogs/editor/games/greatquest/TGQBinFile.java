package net.highwayfrogs.editor.games.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.LargeFileReceiver;
import net.highwayfrogs.editor.games.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.greatquest.generic.kcCResourceString;
import net.highwayfrogs.editor.games.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.greatquest.script.action.kcActionFlag;
import net.highwayfrogs.editor.games.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.greatquest.script.action.kcActionTemplate;
import net.highwayfrogs.editor.games.greatquest.script.cause.kcScriptCauseNumber;
import net.highwayfrogs.editor.games.greatquest.script.cause.kcScriptCauseNumber.kcScriptCauseNumberOperation;
import net.highwayfrogs.editor.games.greatquest.script.effect.kcScriptEffectActor;
import net.highwayfrogs.editor.games.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Parses Frogger TGQ's main game data file.
 * Notes: PS2 bin is way smaller than PC file. Support it eventually.
 * .SBR files contain sound effects. the SCK file contains only PCM data, with headers in the .IDX file.
 * .PSS (PS2) are video files. Can be opened with VLC.
 * BUFFER.DAT files (PS2) are video files, and can be opened with VLC.
 * TODO: kcOpen() accesses a global flag that tells the game if it should read files from the filesystem or from a .bin file. We could consider a mod to the game to have it load from the file system.
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
            (file.hasFilePath() && file.isCollision() ? namedFiles : unnamedFiles).add(file);

        // Start writing file.
        writer.writeInt(unnamedFiles.size());
        writer.writeInt(namedFiles.size());
        int nameAddress = writer.writeNullPointer();

        Map<TGQFile, Integer> fileSizeTable = new HashMap<>();
        Map<TGQFile, Integer> fileZSizeTable = new HashMap<>();
        Map<TGQFile, Integer> fileOffsetTable = new HashMap<>();

        // Write file headers:
        for (TGQFile file : getFiles()) {
            if (file.hasFilePath() && file.isCollision()) {
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
            System.out.println("Writing " + file.getFilePath());

            byte[] rawBytes; // TODO :TOSS
            if (!(file instanceof TGQChunkedFile) && file.getRawData() != null) {
                // TODO: Seems both models and images are busted.
                // TODO: We're missing nearly 100MB of texture data when we let textures save themselves.
                rawBytes = file.getRawData();
                System.out.println(rawBytes.length + " raw bytes.");
            } else {
                ArrayReceiver receiver = new ArrayReceiver();
                file.save(new DataWriter(receiver));
                rawBytes = receiver.toArray();
                System.out.println(rawBytes.length + " written data.");
            }

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

    /**
     * Print a list of all files to stdout.
     */
    @SuppressWarnings("unused")
    public void printFileList() {
        for (int i = 0; i < this.files.size(); i++) {
            TGQFile file = this.files.get(i);
            System.out.print(file.hasFilePath() ? file.getFilePath() : "UNKNOWN");
            System.out.print(" # File ");
            System.out.print(Utils.padNumberString(i, 4));
            System.out.print(", Hash: ");
            System.out.print(Utils.to0PrefixedHexString(file.getNameHash()));
            if (file.isCollision())
                System.out.print(" (Collision)");
            if (file.isCompressed())
                System.out.print(", Compressed");
            System.out.println();
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

        if (Utils.stripAlphanumeric(fileName).equalsIgnoreCase("hash"))
            TGQHashReverser.runHashPlayground();

        File binFile = new File(fileName);
        if (!binFile.exists() || !binFile.isFile()) {
            System.out.println("That is not a valid file!");
            return;
        }

        // Determine platform.
        kcPlatform platform = TGQRunners.getPlatform(binFile, scanner);
        if (platform == null)
            return;

        // Load main bin.
        System.out.println("Loading file...");
        DataReader reader = new DataReader(new FileSource(binFile));
        TGQBinFile mainFile = new TGQBinFile(platform);
        mainFile.load(reader);

        // Export.
        File exportDir = new File(binFile.getParentFile(), "Export");
        Utils.makeDirectory(exportDir);
        exportFileList(exportDir, mainFile);
        for (TGQFile file : mainFile.getFiles())
            file.export(exportDir);

        // Modify script in rolling rapids creek.
        TGQChunkedFile rollingRapidsCreek = (TGQChunkedFile) mainFile.files.get(16);
        kcScript script = rollingRapidsCreek.getScriptList().getScripts().get(33);

        int injectAfter = 1;

        int executionStartNumber = 1337;
        int executionNumber = executionStartNumber;
        for (int i = 0; i < 32; i++) {
            if (i == 0)
                continue; // Skip

            // Create clear flag function.
            kcScriptCauseNumber clearFlagDialogCause = new kcScriptCauseNumber(kcScriptCauseNumberOperation.EQUALS, executionNumber++);
            kcScriptFunction clearFlagFunc = new kcScriptFunction(clearFlagDialogCause);

            // Add dialog resource.
            kcCResourceGeneric clearFlagDialog = new kcCResourceGeneric(rollingRapidsCreek, kcCResourceGenericType.STRING_RESOURCE, new kcCResourceString("Knee Flag Clear Test: " + i));
            clearFlagDialog.setName("FgClr" + Utils.padNumberString(i, 2));
            int clearFlagDialogHash = TGQUtils.hash(clearFlagDialog.getName());
            clearFlagDialog.setHash(clearFlagDialogHash);
            rollingRapidsCreek.getChunks().add(clearFlagDialog);
            rollingRapidsCreek.getFirstTOCChunk().getHashes().add(clearFlagDialogHash);

            // Add dialog action.
            kcActionTemplate actionClearFlagDialog = (kcActionTemplate) kcActionID.DIALOG.newInstance();
            actionClearFlagDialog.getArguments().add(new kcParam(clearFlagDialogHash));
            clearFlagFunc.getEffects().add(new kcScriptEffectActor(actionClearFlagDialog, 0x68FF0A2));

            // Add clear action.
            kcActionFlag actionClearFlag = new kcActionFlag(kcActionID.SET_FLAGS);
            actionClearFlag.getArguments().add(new kcParam(1 << i));
            clearFlagFunc.getEffects().add(new kcScriptEffectActor(actionClearFlag, 0x68FF0A2));

            // Add increment function.
            // TODO

            // Created set flag function
            kcScriptCauseNumber setFlagDialogCause = new kcScriptCauseNumber(kcScriptCauseNumberOperation.EQUALS, executionNumber++);
            kcScriptFunction setFlagFunc = new kcScriptFunction(setFlagDialogCause);

            // Add dialog resource.
            kcCResourceGeneric setFlagDialog = new kcCResourceGeneric(rollingRapidsCreek, kcCResourceGenericType.STRING_RESOURCE, new kcCResourceString("Knee Flag Set: " + i));
            setFlagDialog.setName("FgSet" + Utils.padNumberString(i, 2));
            int setFlagDialogHash = TGQUtils.hash(setFlagDialog.getName());
            setFlagDialog.setHash(setFlagDialogHash);
            rollingRapidsCreek.getChunks().add(setFlagDialog);
            rollingRapidsCreek.getFirstTOCChunk().getHashes().add(setFlagDialogHash);

            // Add dialog action.
            kcActionTemplate actionSetFlagDialog = (kcActionTemplate) kcActionID.DIALOG.newInstance();
            actionSetFlagDialog.getArguments().add(new kcParam(setFlagDialogHash));
            setFlagFunc.getEffects().add(new kcScriptEffectActor(actionSetFlagDialog, 0x68FF0A2));

            // Add set flag action.
            kcActionFlag actionSetFlag = new kcActionFlag(kcActionID.SET_FLAGS);
            actionSetFlag.getArguments().add(new kcParam(1 << i));
            setFlagFunc.getEffects().add(new kcScriptEffectActor(actionSetFlag, 0x68FF0A2));

            // Add increment function.
            // TODO

            // TODO: If last one, set variable to normal trigger.

            // Register functions.
            script.getFunctions().add(injectAfter++, setFlagFunc);
            script.getFunctions().add(injectAfter++, clearFlagFunc);
        }

        File outputFile = new File(binFile.getParent(), "Playable\\data.bin");
        DataWriter writer = new DataWriter(new LargeFileReceiver(outputFile));
        mainFile.save(writer);
        writer.closeReceiver();

        System.out.println("Done.");
    }

    /**
     * Activates the filename
     * @param filePath              The path of a game file.
     * @param showMessageIfNotFound Specify if a warning should be displayed if the file is not found.
     */
    public TGQFile applyFileName(String filePath, boolean showMessageIfNotFound) {
        TGQFile file = getOptionalFileByName(filePath);
        if (file != null) {
            file.setFilePath(filePath);
            return file;
        }

        int hash = TGQUtils.hashFilePath(filePath);
        if (showMessageIfNotFound)
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
}