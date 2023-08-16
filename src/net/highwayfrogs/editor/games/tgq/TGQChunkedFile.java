package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.loading.kcLoadContext;
import net.highwayfrogs.editor.games.tgq.map.kcEnvironment;
import net.highwayfrogs.editor.games.tgq.script.kcAction;
import net.highwayfrogs.editor.games.tgq.script.kcCActionSequence;
import net.highwayfrogs.editor.games.tgq.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.tgq.script.kcScriptList;
import net.highwayfrogs.editor.games.tgq.toc.*;
import net.highwayfrogs.editor.games.tgq.toc.kcCResourceNamedHash.HashEntry;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Handles the Frogger TGQ TOC files. (They are maps, but may also have other data(?))
 * These files may appear sorted, but they are not. "\GameData\Level00Global\Text\globaltext.dat", "\GameData\Level07TreeKnowledge\Level\PS2_Level07.dat", and more contain proof that the sorting was just a convention, and not enforced by anything.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class TGQChunkedFile extends TGQFile implements IFileExport {
    private final List<kcCResource> chunks = new ArrayList<>();

    public TGQChunkedFile(TGQBinFile mainArchive) {
        super(mainArchive);
    }

    @Override
    public void load(DataReader reader) {
        while (reader.hasMore()) {
            String magic = reader.readString(4);
            int length = reader.readInt() + 0x20; // 0x20 and not 0x24 because we're reading from the start of the data, not the length.
            byte[] readBytes = reader.readBytes(Math.min(reader.getRemaining(), length));

            // Read chunk.
            KCResourceID readType = KCResourceID.getByMagic(magic);

            kcCResource newChunk;
            if (readType == KCResourceID.RAW && Utils.testSignature(readBytes, kcEnvironment.ENVIRONMENT_NAME)) {
                newChunk = new kcEnvironment(this);
            } else if (readType == KCResourceID.RAW && Utils.testSignature(readBytes, kcScriptList.GLOBAL_SCRIPT_NAME)) {
                newChunk = new kcScriptList(this);
            } else if (readType != null && readType.getMaker() != null) {
                newChunk = readType.getMaker().apply(this);
            } else {
                newChunk = new TGQDummyFileChunk(this, magic);
            }

            DataReader chunkReader = new DataReader(new ArraySource(readBytes));
            try {
                newChunk.load(chunkReader);

                // Warn if not all data is read.
                if (chunkReader.hasMore())
                    System.out.println("TGQ Chunk " + Utils.stripAlphanumeric(newChunk.getChunkMagic()) + "/'" + newChunk.getName() + "' in '" + getDebugName() + "' had " + chunkReader.getRemaining() + " remaining unread bytes.");
            } catch (Throwable th) {
                th.printStackTrace();
                System.err.println("Failed to read " + newChunk.getChunkType() + " chunk from " + getDebugName() + ".");
            }

            this.chunks.add(newChunk);
        }
    }

    @Override
    public void afterLoad1(kcLoadContext context) {
        super.afterLoad1(context);
        for (int i = 0; i < this.chunks.size(); i++)
            this.chunks.get(i).afterLoad1(context);
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);
        for (int i = 0; i < this.chunks.size(); i++)
            this.chunks.get(i).afterLoad2(context);
    }

    @Override
    public String getDefaultFolderName() {
        return "ChunkedDataFiles";
    }

    @Override
    public void save(DataWriter writer) {
        for (kcCResource chunk : getChunks()) {
            writer.writeStringBytes(chunk.getChunkMagic());
            int lengthAddress = writer.writeNullPointer();

            // Write chunk data.
            int dataStart = writer.getIndex();
            chunk.save(writer);
            int dataEnd = writer.getIndex();
            writer.writeAddressAt(lengthAddress, (dataEnd - dataStart) - 0x20);
        }
    }

    /**
     * Create a map of hash numbers to corresponding strings from files present in the chunks.
     * @return localHashes
     */
    public Map<Integer, String> calculateLocalHashes() {
        Map<Integer, String> nameMap = new HashMap<>();
        for (kcCResource testChunk : this.chunks) {
            if (testChunk.getName() != null && testChunk.getName().length() > 0)
                nameMap.put(TGQUtils.hash(testChunk.getName(), true), testChunk.getName());

            if (testChunk instanceof kcCResourceNamedHash) {
                kcCResourceNamedHash namedHashChunk = (kcCResourceNamedHash) testChunk;
                for (HashEntry entry : namedHashChunk.getEntries())
                    nameMap.put(entry.getRawHash(), entry.getName());
            }
        }

        return nameMap;
    }

    @Override
    public String getExportFolderName() {
        return Utils.stripExtension(getExportName());
    }

    @Override
    public void exportToFolder(File folder) throws IOException {
        // Build the name map.
        Map<Integer, String> nameMap = calculateLocalHashes();

        TGQUtils.addDefaultHashesToMap(nameMap);
        kcScriptDisplaySettings settings = new kcScriptDisplaySettings(nameMap, true, true);

        // Main looking.
        kcCResOctTreeSceneMgr mainModel = null;
        StringBuilder sequenceBuilder = new StringBuilder();
        StringBuilder scriptBuilder = new StringBuilder();
        for (kcCResource testChunk : this.chunks) {
            if (testChunk instanceof kcCResOctTreeSceneMgr)
                mainModel = (kcCResOctTreeSceneMgr) testChunk;

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

        // Ensure the map is exported as a 3D model.
        if (mainModel != null)
            mainModel.exportAsObj(folder, Utils.stripExtension(getExportName()));

        // Save scripts to folder.
        if (sequenceBuilder.length() > 0) {
            File sequenceFile = new File(folder, "sequences.txt");
            Files.write(sequenceFile.toPath(), Arrays.asList(sequenceBuilder.toString().split("\n")));
        }

        // Save scripts to folder.
        if (scriptBuilder.length() > 0) {
            File scriptFile = new File(folder, "script.txt");
            Files.write(scriptFile.toPath(), Arrays.asList(scriptBuilder.toString().split("\n")));
        }

        exportChunksToDirectory(folder);
        saveInfo(new File(folder, "info.txt"));

        // Save hashes to file.
        if (nameMap.size() > 0) {
            File hashFile = new File(folder, "hashes.txt");

            List<String> lines = nameMap.entrySet().stream()
                    .sorted(Comparator.comparingInt(Entry::getKey))
                    .map(entry -> Utils.to0PrefixedHexString(entry.getKey()) + "=" + entry.getValue())
                    .collect(Collectors.toList());

            Files.write(hashFile.toPath(), lines);
        }
    }

    /**
     * Saves information about the chunked file contents to a text file.
     * @param textFile The file to save the information to.
     */
    public void saveInfo(File textFile) {
        // Export Info.
        StringBuilder builder = new StringBuilder();
        builder.append("Chunked File Dump").append(Constants.NEWLINE);
        if (hasFilePath())
            builder.append("Name: ").append(getFilePath()).append(Constants.NEWLINE);

        builder.append("File ID: #").append(getArchiveIndex()).append(Constants.NEWLINE);
        builder.append("Name Hash: ").append(Utils.to0PrefixedHexString(getNameHash())).append(Constants.NEWLINE);
        builder.append("Has Compression: ").append(isCompressed()).append(Constants.NEWLINE);

        if (this.chunks.size() > 0) {
            // Write environment info
            for (kcCResource chunk : this.chunks) {
                if (!(chunk instanceof kcEnvironment))
                    continue;

                builder.append(Constants.NEWLINE);
                builder.append("kcEnvironment:");
                builder.append(Constants.NEWLINE);
                ((kcEnvironment) chunk).writeInfo(builder, " ");
            }

            builder.append(Constants.NEWLINE);
            builder.append("Chunks (");
            builder.append(this.chunks.size());
            builder.append("):");
            builder.append(Constants.NEWLINE);
            for (kcCResource chunk : this.chunks) {
                builder.append(" - [");
                builder.append(Utils.to0PrefixedHexString(chunk.getHash()));
                builder.append("|");
                builder.append(Utils.stripAlphanumeric(chunk.getChunkMagic()));
                builder.append("|");
                builder.append(chunk.getClass().getSimpleName());
                builder.append("]: '");
                builder.append(chunk.getName());
                builder.append("', ");
                builder.append(chunk.getRawData() != null ? chunk.getRawData().length : 0);
                builder.append(" bytes");
                builder.append(Constants.NEWLINE);
            }
        }

        try {
            Files.write(textFile.toPath(), Arrays.asList(builder.toString().split(Constants.NEWLINE)));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save chunked file information to '" + textFile + "'.", ex);
        }
    }

    /**
     * Exports this file and all of its chunks to the directory.
     * This makes debugging easier.
     * @param directory The folder to export chunks to.
     */
    @SneakyThrows
    public void exportChunksToDirectory(File directory) {
        Map<String, Integer> countMap = new HashMap<>();
        for (kcCResource chunk : this.chunks) {
            String signature = Utils.stripAlphanumeric(chunk.getChunkMagic());
            int count = countMap.getOrDefault(signature, 0) + 1;
            countMap.put(signature, count);

            // Check there is data.
            String fileName = Utils.padNumberString(count, 3);
            if (chunk.getRawData() == null) {
                System.out.println("Skipping chunk with null data: '" + signature + "-" + fileName + "'.");
                continue;
            }

            // Create sub-directory.
            File subDirectory = new File(directory, signature);
            Utils.makeDirectory(subDirectory);

            // Save file contents.
            File outputFile = new File(subDirectory, fileName);
            if (!outputFile.exists())
                Files.write(outputFile.toPath(), chunk.getRawData());
        }
    }
}
