package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQTOCFile;

/**
 * Holds entity instances? NST probably stands for instance.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class NSTChunk extends TOCChunk {
    private String name;
    private float x;
    private float y;
    private float z;

    private static final int NAME_SIZE = 32;

    public NSTChunk(TGQTOCFile parentFile) {
        super(parentFile, TOCChunkType.NST);
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
        reader.skipBytes(36);
        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();

        System.out.println(this.name + " -> [" + this.x + ", " + this.y + ", " + this.z + "]");

        //TODO
    }

    @Override
    public void save(DataWriter writer) {
        //TODO
    }
}
