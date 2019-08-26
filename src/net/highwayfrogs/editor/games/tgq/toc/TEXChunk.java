package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQTOCFile;

/**
 * Represents a texture.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class TEXChunk extends TOCChunk {
    private String name;
    private String path;

    private static final int NAME_SIZE = 32;
    private static final int PATH_SIZE = 260;
    private static final byte PATH_TERMINATOR = (byte) 0xCD;

    public TEXChunk(TGQTOCFile parentFile) {
        super(parentFile, TOCChunkType.TEX);
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
        this.path = reader.readTerminatedStringOfLength(PATH_SIZE);

    }

    @Override
    public void save(DataWriter writer) {
        int nameEndIndex = (writer.getIndex() + NAME_SIZE);
        writer.writeStringBytes(this.name);
        writer.writeTo(nameEndIndex);

        int pathEndIndex = (writer.getIndex() + PATH_SIZE);
        writer.writeTerminatorString(this.path); // Include the null byte after the string data before using 0xCD.
        writer.writeTo(pathEndIndex, PATH_TERMINATOR);
    }
}
