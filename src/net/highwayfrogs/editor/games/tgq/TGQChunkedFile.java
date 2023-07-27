package net.highwayfrogs.editor.games.tgq;

import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.loading.kcLoadContext;
import net.highwayfrogs.editor.games.tgq.script.kcScriptList;
import net.highwayfrogs.editor.games.tgq.toc.KCResourceID;
import net.highwayfrogs.editor.games.tgq.toc.TGQDummyFileChunk;
import net.highwayfrogs.editor.games.tgq.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the Frogger TGQ TOC files. (They are maps, but may also have other data(?))
 * These files may appear sorted, but they are not. "\GameData\Level00Global\Text\globaltext.dat", "\GameData\Level07TreeKnowledge\Level\PS2_Level07.dat", and more contain proof that the sorting was just a convention, and not enforced by anything.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class TGQChunkedFile extends TGQFile {
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
            if (readType == KCResourceID.RAW && Utils.testSignature(readBytes, kcScriptList.GLOBAL_SCRIPT_NAME)) {
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
     * Exports this file and all of its chunks to the directory.
     * This makes debugging easier.
     * @param directory The folder to export chunks to.
     */
    @SneakyThrows
    public void exportFileToDirectory(File directory) {
        Utils.makeDirectory(directory);

        // Export Info.
        @Cleanup PrintWriter infoWriter = new PrintWriter(new File(directory, "info.txt"));
        infoWriter.write("Chunked File Dump" + Constants.NEWLINE);
        if (hasFilePath())
            infoWriter.write("Name: " + getFilePath() + Constants.NEWLINE);

        infoWriter.write("File ID: #" + getArchiveIndex() + Constants.NEWLINE);
        infoWriter.write("Name Hash: " + Utils.to0PrefixedHexString(getNameHash()) + Constants.NEWLINE);
        infoWriter.write("Has Compression: " + isCompressed() + Constants.NEWLINE);

        if (this.chunks.size() > 0) {
            infoWriter.write(Constants.NEWLINE);
            infoWriter.write("Chunks (");
            infoWriter.write(String.valueOf(this.chunks.size()));
            infoWriter.write("):");
            infoWriter.write(Constants.NEWLINE);
            for (kcCResource chunk : this.chunks) {
                infoWriter.write(" - [");
                infoWriter.write(Utils.to0PrefixedHexString(chunk.getHash()));
                infoWriter.write("|");
                infoWriter.write(Utils.stripAlphanumeric(chunk.getChunkMagic()));
                infoWriter.write("|");
                infoWriter.write(chunk.getClass().getSimpleName());
                infoWriter.write("]: '");
                infoWriter.write(chunk.getName());
                infoWriter.write("', ");
                infoWriter.write(String.valueOf(chunk.getRawData() != null ? chunk.getRawData().length : 0));
                infoWriter.write(" bytes");
                infoWriter.write(Constants.NEWLINE);
            }
        }

        infoWriter.close(); // Close.

        // Export Chunks.
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

            File subDirectory = new File(directory, signature);
            Utils.makeDirectory(subDirectory);
            Files.write(new File(subDirectory, fileName).toPath(), chunk.getRawData());
        }
    }
}
