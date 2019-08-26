package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.toc.TOCChunk;
import net.highwayfrogs.editor.games.tgq.toc.TOCChunkType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the Frogger TGQ TOC files. (They are maps, but may also have other data(?))
 * TODO: Support the remaining chunks.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class TGQTOCFile extends TGQFile {
    private List<TOCChunk> chunks = new ArrayList<>();

    public static final String SIGNATURE = "TOC\0";

    public TGQTOCFile(TGQBinFile mainArchive) {
        super(mainArchive);
    }

    @Override
    public void load(DataReader reader) {
        while (reader.hasMore()) {
            String magic = reader.readString(4);
            int length = reader.readInt() + 0x20; // 0x20 and not 0x24 because we're reading from the start of the data, not the length.
            byte[] readBytes = reader.readBytes(length);

            // Read chunk.
            TOCChunkType readType = TOCChunkType.getByMagic(magic);
            if (readType == TOCChunkType.DUMMY)
                System.out.println("Unknown chunk: " + magic);

            TOCChunk newChunk = readType.getMaker().apply(this, magic);
            newChunk.load(new DataReader(new ArraySource(readBytes)));
            this.chunks.add(newChunk);
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (TOCChunk chunk : getChunks()) {
            writer.writeStringBytes(chunk.getSignature());
            int lengthAddress = writer.writeNullPointer();

            // Write chunk data.
            int dataStart = writer.getIndex();
            chunk.save(writer);
            int dataEnd = writer.getIndex();
            writer.writeAddressAt(lengthAddress, (dataEnd - dataStart) - 0x20);
        }
    }
}
